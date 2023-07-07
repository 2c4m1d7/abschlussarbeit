package services

import scredis._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.Configuration
import javax.inject.Inject
import scredis.exceptions.RedisIOException

class RedisInstanceManager @Inject() (redisInstance: Redis)(implicit
    configuration: Configuration
) extends Runnable {

  val redisHost = configuration.get[String]("redis_host")
  override def run(): Unit = {

    var continue = true
    var timeNotInUse = 0

    while (continue) {

      Thread.sleep(1000)
      try {
        val connectionQuantity =
          Await.result(redisInstance.clientList(), 5.seconds).size

        if (connectionQuantity < 2) {
          timeNotInUse += 1
        } else {
          timeNotInUse = 0
        }
        if (timeNotInUse > 3600000) {
          continue = false
        }
      } catch {
        case e: Exception => {
          continue = false
        }
      }
    }
    redisInstance.shutdown()

  }

}
