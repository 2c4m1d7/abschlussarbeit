package services

import javax.inject.Inject
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.Configuration
import scala.concurrent.Future
import play.api.libs.ws.WSResponse
import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

import sys.process._
import utils.ConnectionUtils
import engines.RedisInstanceManager
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

class RedisService @Inject() (implicit
    ws: WSClient,
    configuration: Configuration,
    system: ActorSystem
) {
  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")

  var redisInstances: Map[String, Redis] = Map()

  Runtime.getRuntime.addShutdownHook(
    new Thread(() => redisInstances.foreach(_._2.shutdown()))
  )

  def create(dbName: String): Int = {
    val dbPath = redisDirPath + "/" + dbName

    val redisDir = new File(redisDirPath)
    redisDir.mkdir()

    val db = new File(dbPath)
    db.mkdir()

    return start(dbName);
  }

  def start(dbName: String): Int = {
    // TODO: check if db exists
    if (redisInstances.contains(dbName)) {
      return redisInstances(dbName).port
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
        return -1; // TODO: handle this
      }
      val startRedis =
        "bash ./sh/start_redis.sh " + redisPort + " " + dbPath + " " + dbName
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

    new Thread(new RedisInstanceManager(redisInstance, configuration)).start()

    return redisPort;
  }

  def delete(dbName: String): Unit = {

    val instance = redisInstances(dbName)
    s"redis-cli -h ${instance.host} -p ${instance.port} shutdown".!
    redisInstances = redisInstances.removed(dbName)

    val directoryPath = Path.of(redisDirPath, dbName)
    FileUtils.deleteDir(directoryPath)
  }

  def dbNameExists(name: String): Boolean = {
    val directory = new File(redisDirPath)
    // directory.listFiles().foreach(x => println(x.getName))
    directory.listFiles.exists(_.getName == name)
  }

}
