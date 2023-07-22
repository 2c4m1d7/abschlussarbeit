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

class RedisService @Inject() (implicit
    configuration: Configuration,
    databaseRepository: DatabaseRepository,
    system: ActorSystem,
    ec: ExecutionContext
) {
  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")
  var redisInstances: Map[UUID, Redis] = Map()
  val log = LoggerFactory.getLogger(this.getClass());

  def create(dbRow: DatabaseRow, userId: UUID): Future[Int] = {
    val existingDb = Await.result(
      // databaseRepository.getDatabaseByNameAndUserId(dbRow.name, userId)
      databaseRepository.getDatabaseByName(dbRow.name),
      Duration.Inf
    ) match {
      case Some(db) => db
      case None     => null
    }

    if (existingDb != null && existingDb.userId == userId) {
      return start(existingDb)
    } else if (existingDb != null) {
      return Future.failed(new RuntimeException("Database already exists"))
    }

    val dbPath = redisDirPath + "/" + dbRow.name

    val redisDir = new File(redisDirPath)
    redisDir.mkdir()

    val db = new File(dbPath)
    db.mkdir()

    databaseRepository.addDatabase(dbRow)

    return start(dbRow);
  }

  def start(db: DatabaseRow): Future[Int] = {
    if (redisInstances.contains(db.id)) {
      return Future.successful(redisInstances(db.id).port)
    }
    val dbPath = redisDirPath + "/" + db.name

    var exitCode = 1
    var redisPort = -1
    var startPort = 49152
    do {

      redisPort = ConnectionUtils
        .findAvailablePort(redisHost, startPort, 65535)
        .getOrElse(-1)

      if (redisPort == -1) {
        return Future.failed(
          new RuntimeException("Redis port not found")
        ); // TODO: handle this
      }
      val startRedis =
        s"bash ./sh/start_redis.sh $redisPort $dbPath ${db.name}"
      val process = startRedis.run()
      exitCode = process.exitValue()

      if (exitCode != 0) {
        startPort = redisPort + 1
      }
    } while (exitCode != 0)

    val redisInstance = Redis(
      host = redisHost,
      port = redisPort
    )

    redisInstances = redisInstances + ((db.id, redisInstance))
    new Thread(new RedisInstanceManager(redisInstance)).start()
    return Future.successful(redisPort);
  }

  def deleteDatabasesByIds(
      databaseIds: Seq[UUID],
      userId: UUID
  ): Future[Unit] = {
    redisInstances.foreach({ case (id, instance) =>
      if (databaseIds.contains(id)) {
        // s"redis-cli -h ${instance.host} -p ${instance.port} shutdown".!
        instance.shutdown()
      }
    })
    redisInstances = redisInstances.removedAll(databaseIds)

    databaseRepository
      .getDatabaseByIdsIn(databaseIds, userId)
      .andThen {
        case Success(dbs) => {
          dbs.foreach(db => {
            val directoryPath = Path.of(redisDirPath, db.name)
            FileUtils.deleteDir(directoryPath)
          })
          databaseRepository.deleteDatabaseByIdsIn(databaseIds, userId)
        }
        case Failure(exception) => Future.failed(exception)
      }
      .collect({ case _ => () })
  }

  def dbExists(name: String): Boolean = {
    val directory = new File(redisDirPath)
    directory.listFiles.exists(_.getName.equals(name))
  }

  def getDb(id: UUID): Future[DatabaseResponse] = {

    databaseRepository
      .getDatabaseById(id)
      .recoverWith { case t => Future.failed(t) }
      .flatMap {
        case Some(db) =>
          if (redisInstances.contains(db.id)) {
            Future.successful(
              DatabaseResponse(
                id = db.id,
                name = db.name,
                port = redisInstances(db.id).port,
                createdAt = db.createdAt
              )
            )
          } else {
            start(db)
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
              .NotFound(s"There is no database with id: ${id.toString()}")
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

  Runtime.getRuntime.addShutdownHook(
    new Thread(() => redisInstances.foreach(_._2.shutdown()))
  )
}
