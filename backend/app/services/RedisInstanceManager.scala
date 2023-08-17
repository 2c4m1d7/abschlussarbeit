package services

import scredis._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.Configuration
import javax.inject.Inject
import scredis.exceptions.RedisIOException
import play.api.Logger
import repositories.DatabaseRepository
import models.DatabaseRow

class RedisInstanceManager @Inject() (redisInstance: Redis, dbRow: DatabaseRow)(implicit
    configuration: Configuration,
    databaseRepository: DatabaseRepository
) extends Runnable {

  private val logger = Logger(this.getClass)
  
  val redisHost = configuration.get[String]("redis_host")

  val MONITORING_THREAD_CONNECTION_COUNT = 1
  val SLEEP_DURATION_MILLIS = 1000
  val MAX_TIME_NOT_IN_USE_SEC = 3600

  override def run(): Unit = {

    var continue = true
    var timeNotInUse = 0

    while (continue) {

      Thread.sleep(SLEEP_DURATION_MILLIS)
      try {
        val connectionQuantity = Await.result(redisInstance.clientList(), 5.seconds).size

        if (connectionQuantity < MONITORING_THREAD_CONNECTION_COUNT + 1) {
          timeNotInUse += 1
        } else {
          timeNotInUse = 0
        }

        if (timeNotInUse > MAX_TIME_NOT_IN_USE_SEC) {
          continue = false
        }
      } catch {
        case e: Exception => {
          logger.error("Error while checking Redis connections", e)
          continue = false
        }
      }
    }
    redisInstance.shutdown()
    databaseRepository.updateDatabasePort(dbRow.id, None)

  }

}
