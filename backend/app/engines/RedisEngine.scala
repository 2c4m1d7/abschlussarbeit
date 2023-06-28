package engines

import scredis._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.Configuration
import javax.inject.Inject
import scredis.ShutdownModifier
class RedisEngine (redisInstance: Redis, configuration: Configuration) extends Runnable {

  val redisHost = configuration.get[String]("redis_host")
  override def run(): Unit = {

    var keepAlive = true

    while (keepAlive) {

      Thread.sleep(1 * 60000/4) // 1 hour
      val connectionQuantity = Await.result(redisInstance.clientList(), 10.seconds).size

      if (connectionQuantity < 2) {
        keepAlive = false
      }

    }
    redisInstance.shutdown()

  }

}
