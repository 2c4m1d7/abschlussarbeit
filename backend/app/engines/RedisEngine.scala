package engines

import scredis._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.Configuration
import javax.inject.Inject

class RedisEngine(redisInstance: Redis, configuration: Configuration)
    extends Runnable {

  val redisHost = configuration.get[String]("redis_host")
  override def run(): Unit = {

    var continue = true
    var timeNotInUse = 0
    try {
      while (continue) {

        Thread.sleep(1000)
        val connectionQuantity =
          Await.result(redisInstance.clientList(), 1.seconds).size

        if (connectionQuantity < 2) {
          timeNotInUse += 1
        } else {
          timeNotInUse = 0
        }
        if (timeNotInUse > 3600) {
          continue = false
        }
      }
      redisInstance.shutdown()
    } catch {
      case e: Exception => println(e.getMessage())
    }
  }

}
