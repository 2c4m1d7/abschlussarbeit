package services

import javax.inject.Inject
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest
import play.api.Configuration
import scala.concurrent.Future
import play.api.libs.ws.WSResponse
import scala.concurrent.ExecutionContext
import scala.concurrent.Await

import scala.concurrent.duration.Duration
import sys.process._
import utils.Utils
import engines.RedisEngine
import scredis.Client
import akka.actor.ActorSystem
import scredis.Redis
import scredis.RedisConfig
import java.io.File

class RedisService @Inject() (implicit
    ws: WSClient,
    configuration: Configuration
) {
  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")
  implicit val system = ActorSystem("my-actor-system")

  var redisInstances: List[Redis] = List()

    Runtime.getRuntime.addShutdownHook(new Thread(() => redisInstances.foreach(_.shutdown())))

  def createDB(dbName: String): Int = {
    val dbPath = redisDirPath + "/" + dbName

    val redisDir = new File(redisDirPath)
    redisDir.mkdir()

    val dbFolder = new File(dbPath)
    dbFolder.mkdir()

    return startDB(dbName);
  }

  def startDB(dbName: String): Int = {
    // TODO: check if db exists

    val dbPath = redisDirPath + "/" + dbName

    var exitCode = 1
    var redisPort = -1
    var startPort = 49152
    do {

      redisPort =
        Utils.findAvailablePort(redisHost, startPort, 65535).getOrElse(-1)

      if (redisPort == -1) {
        return -1; // TODO: handle this
      }
      val startRedis =
        "bash ./sh/start_redis.sh " + redisPort + " " + dbPath
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
    redisInstances = redisInstance :: redisInstances

    new Thread(new RedisEngine(redisInstance, configuration)).start()

    return redisPort;
  }

}
