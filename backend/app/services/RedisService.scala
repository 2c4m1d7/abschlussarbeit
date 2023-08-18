package services

import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import sys.process._
import services.RedisInstanceManager
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
import models.DatabaseRow
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


  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")
  val log = LoggerFactory.getLogger(this.getClass());

  def create(dbRow: DatabaseRow, user: User, redisConf: String): Future[Int] = {
    databaseRepository
      .getDatabaseByNameAndUserId(dbRow.name, dbRow.userId)
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
            new File(redisDirPath + "/" + user.username + "/" + dbRow.name)
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

          databaseRepository.addDatabase(dbRow).flatMap { _ =>
            start(dbRow, user)
          }
      }
  }

  def start(db: DatabaseRow, user: User): Future[Int] = {
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
      db: DatabaseRow,
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
            databaseRepository.updateDatabasePort(db.id, Some(redisPort))
            new Thread(new RedisInstanceManager(redisInstance, db)).start()
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
        dbRowOption <- databaseRepository.getDatabaseById(id)
        _ <- dbRowOption match {
          case Some(dbRow) if dbRow.port.isDefined => stopRedis(dbRow)
          case _                                   => Future.successful(())
        }
        _ <- deleteFromRepository(id)
        _ <- deleteDirectory(dbRowOption.map(_.name).getOrElse(""), user)
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

  private def stopRedis(dbRow: DatabaseRow): Future[Unit] = {

    for {
      userOpt <- userRepository.getUserById(dbRow.userId)
      user = userOpt.getOrElse(
        throw new Exception("User not found")
      ) // Hier könnten genauere Exception geworfen werden
      redisConfigPath =
        s"$redisDirPath/${user.username}/${dbRow.name}/redis.conf"
      authPassword = RedisConfigReader
        .extractPassword(redisConfigPath)
        .getOrElse("")
      // redisInstance = Redis(
      //   host = redisHost,
      //   port = dbRow.port.getOrElse(0),
      //   authOpt =
      //     if (authPassword.nonEmpty) Some(AuthConfig(None, authPassword))
      //     else None
      // )
      _ <-
        // redisInstance.shutdown().recoverWith { case e =>
        //   log.warn(e.getMessage(), e)
        if (authPassword.nonEmpty) {
          Future {
            s"redis-cli -a $authPassword -h $redisHost -p ${dbRow.port.get} shutdown".!
          }
        } else {
          Future {
            s"redis-cli  -h $redisHost -p ${dbRow.port.get} shutdown".!
          }
        }
      // }
      _ <- databaseRepository.updateDatabasePort(dbRow.id, None)
    } yield ()
  }

  def shutdownAllRedisInstances: Future[Unit] = {
    databaseRepository.getAllActiveDatabases.flatMap { dbs =>
      val shutdownFutures = dbs.map(dbRow => stopRedis(dbRow))
      Future.sequence(shutdownFutures).map(_ => ())
    }
  }

  cs.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "my-shutdown-task") {
    () =>
      shutdownAllRedisInstances.map(_ => Done)
  }

}
