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

class RedisService @Inject() (implicit
    ws: WSClient,
    configuration: Configuration
) {
  val redisHost = configuration.get[String]("redis_host")
  val redisDir = configuration.get[String]("redis_directory")

  def createDB(dbName: String): WSResponse = {
    val request: WSRequest =
      ws.url(redisHost + "/create-redis.php")

    val futureResponse: Future[WSResponse] = request
      .addHttpHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post("name=" + dbName)

    val response =
      Await.result[WSResponse](futureResponse, Duration.apply(5, "s"))

    response
  }

  def createDB(): Int = {

    val mkdir =
      "mkdir " + redisDir + "  " + redisDir + "/testDB" // TODO: add db name to parameters
    mkdir.!
    Seq("")
    var exitCode = 1
    var availablePort = -1
    var startPort = 49152
    do {

      availablePort =
        Utils.findAvailablePort("127.0.0.1", startPort, 65535).getOrElse(-1)

      if (availablePort == -1) {
        return -1;
      }
      val startRedis =
        "bash ./php/start_redis.sh " + availablePort + " testDB"
      val process = startRedis.run()
      exitCode = process.exitValue()
      if (exitCode != 0) {
        startPort = availablePort + 1
      }
    } while (exitCode != 0)

    return availablePort;
  }

  def deleteDB(dbName: String, port: Long): Unit = {

    ws.url(redisHost + "/delete.php/" + dbName + "/" + port)
      .delete()
  }

}
