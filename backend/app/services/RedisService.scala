package services

import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import sys.process._
import utils.ConnectionUtils
import services.RedisInstanceManager
import scredis.Client
import akka.actor.ActorSystem
import scredis.Redis
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import utils.FileUtils
import org.slf4j.Logger;
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
  var redisInstances: Map[UUID, Redis] = Map()
  val log = LoggerFactory.getLogger(this.getClass());

  def create(dbRow: DatabaseRow, user: User, redisConf: String): Future[Int] = {
    val existingDb = Await.result(
      databaseRepository.getDatabaseByNameAndUserId(dbRow.name, dbRow.userId),
      Duration.Inf
    ) match {
      case Some(db) => db
      case None     => null
    }

    if (existingDb != null) {
      return start(existingDb, user)
    } else if (existingDb != null) {
      return Future.failed(new RuntimeException("Database already exists"))
    }

    val redisDir = new File(redisDirPath)
    redisDir.mkdir()

    val userDir = new File(redisDirPath + "/" + user.username)
    userDir.mkdir()

    val db = new File(redisDirPath + "/" + user.username + "/" + dbRow.name)
    db.mkdir()
    RedisConfigGenerator.saveToFile(db.getPath() + "/redis.conf", redisConf)

    databaseRepository.addDatabase(dbRow)

    return start(dbRow, user);
  }

  // def start(db: DatabaseRow, user: User): Future[Int] = {
  //   if (databaseRepository.getPort(db.id) != None) {
  //     return Future.successful(redisInstances(db.id).port)
  //   }

  //   val dbPath = redisDirPath + "/" + user.username + "/" + db.name

  //   var exitCode = 1
  //   var redisPort = -1
  //   var startPort = 49152
  //   do {

  //     redisPort = ConnectionUtils
  //       .findAvailablePort(redisHost, startPort, 65535)
  //       .getOrElse(-1)

  //     if (redisPort == -1) {
  //       return Future.failed(
  //         new RuntimeException("Redis port not found")
  //       ); // TODO: handle this
  //     }
  //     val configPath = dbPath + "/redis.conf"
  //     val startRedis =
  //       s"bash ./sh/start_redis.sh $redisPort $configPath"

  //     val process = startRedis.run()
  //     exitCode = process.exitValue()

  //     if (exitCode != 0) {
  //       startPort = redisPort + 1
  //     }
  //   } while (exitCode != 0)

  //   val redisInstance = Redis(
  //     host = redisHost,
  //     port = redisPort,
  //     authOpt = Some(
  //       AuthConfig(
  //         None,
  //         RedisConfigReader
  //           .extractPassword(dbPath + "/redis.conf")
  //           .getOrElse("")
  //       )
  //     )
  //   )

  //   redisInstances = redisInstances + ((db.id, redisInstance))
  //   new Thread(new RedisInstanceManager(redisInstance)).start()
  //   return Future.successful(redisPort);
  // }

  def start(db: DatabaseRow, user: User): Future[Int] = {
    databaseRepository.getPort(db.id).flatMap {
      case Some(port) => Future.successful(port)
      case None       => startRedisInstance(db, user)
    }
  }

  private def startRedisInstance(
      db: DatabaseRow,
      user: User,
      startPort: Int = 49152
  ): Future[Int] = {
    val dbPath = s"$redisDirPath/${user.username}/${db.name}"

    ConnectionUtils.findAvailablePort(redisHost, startPort, 65535) match {
      case Some(redisPort) =>
        val configPath = s"$dbPath/redis.conf"
        val startRedisCmd = s"bash ./sh/start_redis.sh $redisPort $configPath"
        val process = startRedisCmd.run()

        if (process.exitValue() == 0) {
          val redisInstance = Redis(
            host = redisHost,
            port = redisPort,
            authOpt = Some(
              AuthConfig(
                None,
                RedisConfigReader
                  .extractPassword(s"$dbPath/redis.conf")
                  .getOrElse("")
              )
            )
          )

          // redisInstances += db.id -> redisInstance
          databaseRepository.updateDatabasePort(db.id, Some(redisPort))
          new Thread(new RedisInstanceManager(redisInstance)).start()
          Future.successful(redisPort)
        } else {
          startRedisInstance(db, user, redisPort + 1)
        }

      case None =>
        Future.failed(new RuntimeException("Redis port not found"))
    }
  }

  // def deleteDatabasesByIds(
  //     databaseIds: Seq[UUID],
  //     user: User
  // ): Future[Unit] = {
  //   redisInstances.foreach({ case (id, instance) =>
  //     if (databaseIds.contains(id)) {
  //       val dbNameTry: Try[String] = Try {
  //         val dbName = Await.result(
  //           instance.configGet("dbfilename").map(_.values.head),
  //           10.seconds
  //         )
  //         dbName
  //       }

  //       val dbName = dbNameTry match {
  //         case Success(dbName) =>
  //           dbName
  //         case Failure(ex) =>
  //           println(s"Fehler beim Abrufen des Datenbanknamens: $ex")
  //           ""
  //       }

  //       instance
  //         .shutdown()
  //         .recover({
  //           case e => {
  //             log.warn(e.getMessage(), e)
  //             val pass = RedisConfigReader
  //               .extractPassword(
  //                 s"$redisDirPath/${user.username}/$dbName/redis.conf"
  //               )
  //               .getOrElse("")
  //             s"redis-cli -a $pass -h ${instance.host} -p ${instance.port} shutdown".!
  //           }
  //         })
  //     }
  //   })

  //   redisInstances = redisInstances.removedAll(databaseIds)

  //   databaseRepository
  //     .getDatabaseByIdsIn(databaseIds, user.id)
  //     .andThen {
  //       case Success(dbs) => {
  //         dbs.foreach(db => {
  //           val directoryPath = Path.of(redisDirPath, user.username, db.name)
  //           FileUtils.deleteDir(directoryPath)
  //           val userDirPath = Path.of(redisDirPath, user.username)
  //           if (userDirPath.toFile().listFiles.length == 0) {
  //             FileUtils.deleteDir(userDirPath)
  //           }
  //         })
  //         databaseRepository.deleteDatabaseByIdsIn(databaseIds, user.id)
  //       }
  //       case Failure(exception) => Future.failed(exception)
  //     }
  //     .collect({ case _ => () })
  // }

  // def deleteDatabasesByIds(databaseIds: Seq[UUID], user: User)(implicit
  //     ec: ExecutionContext
  // ): Future[Unit] = {
  //   val deleteTasks = redisInstances.collect {
  //     case (id, instance) if databaseIds.contains(id) =>
  //       for {
  //         dbName <- getDbNameById(id)
  //         _ <- shutdownInstance(instance, dbName, user)
  //         _ <- deleteFromRepository(id)
  //         _ <- deleteDirectory(dbName, user)
  //       } yield ()
  //   }
  //   Future.sequence(deleteTasks).map(_ => ())
  // }

  def deleteDatabasesByIds(databaseIds: Seq[UUID], user: User)(implicit
      ec: ExecutionContext
  ): Future[Unit] = {

 val deleteTasks = databaseIds.map { id =>
  for {
    dbRowOption <- databaseRepository.getDatabaseById(id)
    _ <- dbRowOption match {
      case Some(dbRow) if dbRow.port.isDefined => stopRedis(dbRow)
      case _ => Future.successful(())
    }
    _ <- deleteFromRepository(id)
    _ <- deleteDirectory(dbRowOption.map(_.name).getOrElse(""), user)
  } yield ()
}


    Future.sequence(deleteTasks).map(_ => ())
  }

  // private def getDbNameById(id: UUID): Future[String] = {
  //   databaseRepository.getDatabaseById(id).flatMap {
  //     case Some(dbRow) => Future.successful(dbRow.name)
  //     case None =>
  //       log.warn(s"Keine Datenbank mit ID: $id gefunden.")
  //       Future.failed(new Exception(s"Keine Datenbank mit ID: $id gefunden."))
  //   }
  // }

  // private def shutdownInstance(
  //     instance: Redis,
  //     dbName: String,
  //     user: User
  // ): Future[Unit] = {
  //   instance.shutdown().recoverWith { case e =>
  //     log.warn(e.getMessage(), e)
  //     val pass = RedisConfigReader
  //       .extractPassword(s"$redisDirPath/${user.username}/$dbName/redis.conf")
  //       .getOrElse("")
  //     Future {
  //       s"redis-cli -a $pass -h ${instance.host} -p ${instance.port} shutdown".!
  //     }
  //   }
  // }

  private def deleteFromRepository(id: UUID): Future[Unit] = {

    databaseRepository.deleteDatabaseById(id).map(_ => ()).recoverWith {
      case e =>
        log.error(
          s"Fehler beim Löschen der Datenbank mit ID: $id. Fehler: ${e.getMessage}",
          e
        )
        Future.failed(e)
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
      log.error(
        s"Fehler beim Löschen des Verzeichnisses für Datenbank: $dbName. Fehler: ${e.getMessage}",
        e
      )
      Future.failed(e)
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

  // private def stopRedis(dbRow: DatabaseRow): Unit = {
  //   val user =
  //     Await.result(userRepository.getUserById(dbRow.userId), Duration.Inf).get
  //   val redisConfigPath =
  //     s"$redisDirPath/${user.username}/${dbRow.name}/redis.conf"
  //   val authPassword =
  //     RedisConfigReader.extractPassword(redisConfigPath).getOrElse("")
  //   val redisInstance = Redis(
  //     host = redisHost,
  //     port = dbRow.port.getOrElse(0),
  //     authOpt = Some(AuthConfig(None, authPassword))
  //   )
  //   redisInstance.shutdown()

  //   Await.result(
  //     databaseRepository.updateDatabasePort(dbRow.id, None),
  //     Duration.Inf
  //   )
  // }

  private def stopRedis(dbRow: DatabaseRow): Future[Unit] = {

    for {
      userOpt <- userRepository.getUserById(dbRow.userId)
      user = userOpt.getOrElse(
        throw new Exception("User not found")
      ) // Hier könnten Sie eine genauere Exception werfen
      redisConfigPath =
        s"$redisDirPath/${user.username}/${dbRow.name}/redis.conf"
      authPassword = RedisConfigReader
        .extractPassword(redisConfigPath)
        .getOrElse("")
      // redisInstance = Redis(
      //   host = redisHost,
      //   port = dbRow.port.getOrElse(0),
      //   authOpt = Some(AuthConfig(None, authPassword))
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

  // def shutdownAllRedisInstances: Unit = {
  //   Await
  //     .result(databaseRepository.getAllActiveDatabases, Duration.Inf)
  //     .foreach { dbRow =>
  //       stopRedis(dbRow)
  //     }
  // }

  def shutdownAllRedisInstances: Future[Unit] = {
    databaseRepository.getAllActiveDatabases.flatMap { dbs =>
      val shutdownFutures = dbs.map(dbRow => stopRedis(dbRow))
      Future.sequence(shutdownFutures).map(_ => ())
    }
  }

  // Runtime.getRuntime.addShutdownHook(new Thread(() => {
  //   Await
  //     .result(databaseRepository.getAllActiveDatabases, Duration.Inf)
  //     .foreach { dbRow =>
  //       stopRedis(dbRow)
  //     }
  // }))

  cs.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "my-shutdown-task") {
    () =>
      shutdownAllRedisInstances.map(_ => Done)
  }

}
