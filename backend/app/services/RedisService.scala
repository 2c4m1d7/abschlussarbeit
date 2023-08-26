package services

import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import sys.process._
import monitors.RedisInstanceMonitor
import akka.actor.ActorSystem
import scredis.Redis
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import utils.FileUtils
import org.slf4j.LoggerFactory;
import java.util.UUID
import models.dtos.DatabaseResponse
import repositories.DatabaseRepository
import models.dtos.DatabaseListResponse
import models.RedisDatabase
import models.dtos.DatabaseListInfo
import scala.util.Success
import scala.util.Failure
import models.User
import utils.RedisConfigGenerator
import utils.RedisConfigReader
import scredis.protocol.AuthConfig
import scala.util.Try
import repositories.UserRepository
import akka.actor.CoordinatedShutdown
import akka.Done

import akka.pattern.ask
import akka.util.Timeout
import akka.pattern.AskTimeoutException
import utils.ConnectionUtils

class RedisService @Inject() (implicit
    configuration: Configuration,
    databaseRepository: DatabaseRepository,
    userRepository: UserRepository,
    system: ActorSystem,
    ec: ExecutionContext,
    cs: CoordinatedShutdown
) {

  private val redisMonitors
      : scala.collection.concurrent.Map[UUID, RedisInstanceMonitor] =
    scala.collection.concurrent.TrieMap()
  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")
  val log = LoggerFactory.getLogger(this.getClass());

  def create(redisDb: RedisDatabase, user: User, redisConf: String): Future[Int] = {
    databaseRepository
      .getDatabaseByNameAndUserId(redisDb.name, redisDb.userId)
      .flatMap {
        case Some(existingDb) =>
          Future.failed(new RuntimeException("Database already exists"))

        case None =>
          val redisDir = new File(redisDirPath)
          if (!redisDir.exists() && !redisDir.mkdir()) {
            return Future.failed(
              new RuntimeException(
                s"Failed to create directory: ${redisDir.getPath}"
              )
            )
          }

          val userDir = new File(redisDirPath + "/" + user.username)
          if (!userDir.exists() && !userDir.mkdir()) {
            return Future.failed(
              new RuntimeException(
                s"Failed to create directory: ${userDir.getPath}"
              )
            )
          }

          val dbDir =
            new File(redisDirPath + "/" + user.username + "/" + redisDb.name)
          if (!dbDir.exists() && !dbDir.mkdir()) {
            return Future.failed(
              new RuntimeException(
                s"Failed to create directory: ${dbDir.getPath}"
              )
            )
          }

          RedisConfigGenerator.saveToFile(
            dbDir.getPath() + "/redis.conf",
            redisConf
          )

          databaseRepository.addDatabase(redisDb).flatMap { _ =>
            start(redisDb, user)
          }
      }
  }

  def start(db: RedisDatabase, user: User): Future[Int] = {
    databaseRepository
      .getPort(db.id)
      .flatMap {
        case Some(port) => Future.successful(port)
        case None       => startRedisInstance(db, user)
      }
      .recoverWith { case e =>
        Future.failed(
          new Exception(
            s"Failed to start database: ${db.name} for user: ${user.username}. Error: ${e.getMessage}"
          )
        )
      }
  }

  private def startRedisInstance(
      db: RedisDatabase,
      user: User,
      startPort: Int = 49152
  ): Future[Int] = synchronized {
    val dbPath = s"$redisDirPath/${user.username}/${db.name}"

    ConnectionUtils.findAvailablePort(redisHost, startPort, 65535) match {
      case Some(redisPort) =>
        val configPath = s"$dbPath/redis.conf"
        val startRedisCmd = s"bash ./sh/start_redis.sh $redisPort $configPath"
        val process = startRedisCmd.run()

        if (process.exitValue() == 0) {
          val authPassword = RedisConfigReader
            .extractPassword(s"$dbPath/redis.conf")
            .getOrElse("")

          val redisInstance = Redis(
            host = redisHost,
            port = redisPort,
            authOpt =
              if (authPassword.nonEmpty) Some(AuthConfig(None, authPassword))
              else None
          )

          try {
            var stopCommand = ""
            if (authPassword.nonEmpty) {
              stopCommand =
                s"redis-cli -a $authPassword -h $redisHost -p ${redisPort} shutdown"
            } else {
              stopCommand = s"redis-cli -h $redisHost -p ${redisPort} shutdown"
            }

            val manager =
              new RedisInstanceMonitor(redisInstance, db, stopCommand)
            redisMonitors.put(db.id, manager)
            new Thread(manager).start()
            databaseRepository.updateDatabasePort(db.id, Some(redisPort))
            Future.successful(redisPort)
          } catch {
            case ex: Exception =>
              redisInstance.shutdown()
              databaseRepository.updateDatabasePort(db.id, None)
              Future.failed(ex)
          }
        } else {
          startRedisInstance(db, user, redisPort + 1)
        }

      case None =>
        Future.failed(
          new RuntimeException(
            "An error occurred while starting Redis instance"
          )
        )
    }
  }

  def deleteDatabasesByIds(databaseIds: Seq[UUID], user: User)(implicit
      ec: ExecutionContext
  ): Future[Unit] = {

    val deleteTasks = databaseIds.map { id =>
      for {
        redisDbOption <- databaseRepository.getDatabaseById(id)
        _ <- redisDbOption match {
          case Some(redisDb) if redisDb.port.isDefined => stopRedis(redisDb)
          case _                                   => Future.successful(())
        }
        _ <- deleteFromRepository(id)
        _ <- deleteDirectory(redisDbOption.map(_.name).getOrElse(""), user)
      } yield ()
    }

    Future.sequence(deleteTasks).map(_ => ())
  }

  private def deleteFromRepository(id: UUID): Future[Unit] = {
    databaseRepository.deleteDatabaseById(id).map(_ => ()).recoverWith {
      case e =>
        Future.failed(
          new RuntimeException(
            s"Failed to delete database with ID: $id. Error: ${e.getMessage}"
          )
        )
    }
  }

  private def deleteDirectory(dbName: String, user: User): Future[Unit] =
    Future {
      Thread.sleep(1000)
      val directoryPath = Path.of(redisDirPath, user.username, dbName)
      FileUtils.deleteDir(directoryPath)
      val userDirPath = Path.of(redisDirPath, user.username)
      if (userDirPath.toFile().listFiles.length == 0) {
        FileUtils.deleteDir(userDirPath)
      }
    }.recover { case e =>
      Future.failed(
        new Exception(
          s"Failed to delete directory for database: $dbName. Error: ${e.getMessage}"
        )
      )
    }

  def dbExists(name: String, user: User): Future[Boolean] = {
    databaseRepository.existsByNameAndUserId(name, user.id)
  }

  def getDb(dbId: UUID, user: User): Future[DatabaseResponse] = {

    databaseRepository
      .getDatabaseByIdAndUserId(dbId, user.id)
      .recoverWith { case t => Future.failed(t) }
      .flatMap {
        case Some(db) =>
          if (db.port.isDefined) {
            Future.successful(
              DatabaseResponse(
                id = db.id,
                name = db.name,
                port = db.port.getOrElse(0),
                createdAt = db.createdAt
              )
            )
          } else {
            start(db, user)
              .map(port =>
                DatabaseResponse(
                  id = db.id,
                  name = db.name,
                  port = port,
                  createdAt = db.createdAt
                )
              )
          }

        case None =>
          Future.failed(
            UserService.Exceptions
              .NotFound(
                s"There is no database with id: ${dbId
                    .toString()} for user: ${user.username.toString()}"
              )
          )
      }

  }

  def getDatabaseByUserId(userId: UUID): Future[DatabaseListResponse] = {
    databaseRepository
      .getDatabasesByUserId(userId)
      .map(dbs => {
        DatabaseListResponse(
          databases = dbs.map(db => DatabaseListInfo(db.id, db.name))
        )
      })
  }

  private def stopRedis(redisDb: RedisDatabase): Future[Unit] = {
    for {
      userOpt <- userRepository.getUserById(redisDb.userId)
      user = userOpt.getOrElse(
        throw new RuntimeException("User not found")
      )
      redisConfigPath =
        s"$redisDirPath/${user.username}/${redisDb.name}/redis.conf"
      authPassword = RedisConfigReader
        .extractPassword(redisConfigPath)
        .getOrElse("")
      _ <- {
        val managerOpt = redisMonitors.get(redisDb.id)
        managerOpt match {
          case Some(manager) =>
            manager.stop()
            manager.whenStopped().map { _ =>
              redisMonitors.remove(redisDb.id)
            }
          case None => Future.successful(())
        }
      }
    } yield ()
  }

  def shutdownAllRedisInstances: Future[Unit] = {
    databaseRepository.getAllActiveDatabases.flatMap { dbs =>
      val shutdownFutures = dbs.map(redisDb => stopRedis(redisDb))
      Future.sequence(shutdownFutures).map(_ => ())
    }
  }

  cs.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "my-shutdown-task") {
    () =>
      shutdownAllRedisInstances.map(_ => Done)
  }

}
