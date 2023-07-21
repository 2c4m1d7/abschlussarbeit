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

class RedisService @Inject() (implicit
    configuration: Configuration,
    databaseRepository: DatabaseRepository,
    system: ActorSystem,
    ec: ExecutionContext
) {
  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")

  var redisInstances: Map[String, Redis] = Map()
  val log = LoggerFactory.getLogger(this.getClass());

  def create(dbRow: DatabaseRow): Future[Int] = {
    val dbPath = redisDirPath + "/" + dbRow.name

    val redisDir = new File(redisDirPath)
    redisDir.mkdir()

    val db = new File(dbPath)
    db.mkdir()

    databaseRepository.addDatabase(dbRow)

    return start(dbRow.name);
  }

  def start(dbName: String): Future[Int] = {
    if (redisInstances.contains(dbName)) {
      return Future.successful(redisInstances(dbName).port)
    }
    val dbPath = redisDirPath + "/" + dbName

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
        s"bash ./sh/start_redis.sh $redisPort $dbPath $dbName"
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

    redisInstances = redisInstances + ((dbName, redisInstance))

    new Thread(new RedisInstanceManager(redisInstance)).start()

    return Future.successful(redisPort);
  }

  def delete(dbName: String): Unit = {

    val instance = redisInstances.getOrElse(dbName, null)
    if (instance != null) {
      s"redis-cli -h ${instance.host} -p ${instance.port} shutdown".!
      redisInstances = redisInstances.removed(dbName)
    }

    val directoryPath = Path.of(redisDirPath, dbName)
    FileUtils.deleteDir(directoryPath)
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
          if (redisInstances.contains(db.name)) {
             Future.successful(
              DatabaseResponse(
                id = db.id,
                name = db.name,
                port = redisInstances(db.name).port,
                createdAt = db.createdAt
              )
            )
          } else {
            start(db.name)
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
