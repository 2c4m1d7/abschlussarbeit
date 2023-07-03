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

class RedisService @Inject() (implicit
    configuration: Configuration,
    system: ActorSystem
) {
  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")

  var redisInstances: Map[String, Redis] = Map()
  val log = LoggerFactory.getLogger(this.getClass());

  def create(dbName: String): Int = {
    val dbPath = redisDirPath + "/" + dbName

    val redisDir = new File(redisDirPath)
    redisDir.mkdir()

    val db = new File(dbPath)
    db.mkdir()

    return start(dbName);
  }

  def start(dbName: String): Int = {
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

    return redisPort;
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

  Runtime.getRuntime.addShutdownHook(
    new Thread(() => redisInstances.foreach(_._2.shutdown()))
  )
}
