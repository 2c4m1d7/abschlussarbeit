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
import play.api.inject.ApplicationLifecycle
import java.util.concurrent.CompletableFuture

class RedisService @Inject() (implicit
    configuration: Configuration,
    databaseRepository: DatabaseRepository,
    userRepository: UserRepository,
    ec: ExecutionContext,
    cs: CoordinatedShutdown
) {

  private val redisMonitors
      : scala.collection.concurrent.Map[UUID, RedisInstanceMonitor] =
    scala.collection.concurrent.TrieMap()
  lazy val redisHost = configuration.get[String]("redis_host")
  lazy val redisDirPath = configuration.get[String]("redis_directory")
  val log = LoggerFactory.getLogger(this.getClass());

  def create(
      redisDb: RedisDatabase,
      user: User,
      redisConf: String
  ): Future[Int] = {

    def checkOrCreateDir(path: String): Future[Unit] = {
      val dir = new File(path)
      if (!dir.exists() && !dir.mkdir()) {
        log.error(s"Failed to create directory: $path")
        Future.failed(
          new RuntimeException(s"Failed to create directory: $path")
        )
      } else {
        Future.successful(())
      }
    }

    databaseRepository
      .getDatabaseByNameAndUserId(redisDb.name, redisDb.userId)
      .flatMap {
        case Some(_) =>
          Future.failed(new RuntimeException("Database already exists"))

        case None =>
          for {
            _ <- checkOrCreateDir(redisDirPath)
            _ <- checkOrCreateDir(s"$redisDirPath/${user.username}")
            _ <- checkOrCreateDir(
              s"$redisDirPath/${user.username}/${redisDb.name}"
            )
            _ = RedisConfigGenerator.saveToFile(
              s"$redisDirPath/${user.username}/${redisDb.name}/redis.conf",
              redisConf
            )
            _ <- databaseRepository.addDatabase(redisDb)
            port <- start(redisDb, user)
          } yield port
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

            val monitor =
              new RedisInstanceMonitor(redisInstance, db)
            redisMonitors.put(db.id, monitor)
            new Thread(monitor).start()
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
        log.warn("No available ports found")
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
          case _                                       => Future.successful(())
        }
        _ <- deleteDirectory(redisDbOption.map(_.name).getOrElse(""), user)
      } yield ()
    }

    Future
      .sequence(deleteTasks)
      .map(_ => deleteFromDatabase(databaseIds, user.id))
  }

  private def deleteFromDatabase(
      databaseIds: Seq[UUID],
      userId: UUID
  ): Future[Unit] = {
    databaseRepository
      .deleteDatabaseByIdsIn(databaseIds, userId)
      .map(_ => ())
      .recoverWith { case e =>
        log.error(s"Failed to delete databases with IDs: $databaseIds", e)
        Future.failed(
          new RuntimeException(
            s"Failed to delete databases"
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
      log.error(s"Failed to delete directory for database: $dbName", e)
      Future.failed(
        new RuntimeException(
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
            if (!redisMonitors.contains(db.id)) {
              val redis = connectToRedis(db, user)
              var monitor = new RedisInstanceMonitor(redis, db)
              new Thread(monitor).start()
              redisMonitors.getOrElseUpdate(
                db.id,
                monitor
              )
            }
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
      _ <- {
        val monitorOpt = redisMonitors.get(redisDb.id)
        monitorOpt match {
          case Some(monitor) =>
            monitor.stop()
            monitor.whenStopped().map { _ =>
              redisMonitors.remove(redisDb.id)
            }
          case None =>
            connectToRedis(redisDb, user)
              .shutdown()
              .recover { case e => log.error("Redis is not running", e) }
            databaseRepository.updateDatabasePort(redisDb.id, None)
            Future.successful(())
        }
      }
    } yield ()
  }

  private def connectToRedis(
      db: RedisDatabase,
      user: User
  ): Redis = {
    val redisConfigPath =
      s"$redisDirPath/${user.username}/${db.name}/redis.conf"
    val authPassword = RedisConfigReader
      .extractPassword(redisConfigPath)
      .getOrElse("")
    Redis(
      host = redisHost,
      port = db.port.get,
      authOpt =
        if (authPassword.nonEmpty) Some(AuthConfig(None, authPassword))
        else None
    )

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
