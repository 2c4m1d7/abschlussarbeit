package monitors

import scala.sys.process._
import scredis._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.Configuration
import javax.inject.Inject
import scredis.exceptions.RedisIOException
import play.api.Logger
import repositories.DatabaseRepository
import models.RedisDatabase
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.concurrent.Future

class RedisInstanceMonitor @Inject() (
    redisInstance: Redis,
    redisDb: RedisDatabase
)(implicit
    configuration: Configuration,
    databaseRepository: DatabaseRepository,
    ec: ExecutionContext
) extends Runnable {

  private val logger = Logger(this.getClass)

  private val RedisHost = configuration.get[String]("redis_host")
  private val MonitoringThreadConnectionCount = 1
  private val SleepDurationMillis = 1000
  private val MaxTimeNotInUseSec = 3600

  private val stoppedPromise: Promise[Unit] = Promise[Unit]()
  var isStopped = false

  @volatile private var shouldContinue = true

  override def run(): Unit = {
    var timeNotInUse = 0

    while (shouldContinue) {
      Thread.sleep(SleepDurationMillis)

      try {
        val connectionQuantity =
          Await.result(redisInstance.clientList(), 5.seconds).size

        if (connectionQuantity < MonitoringThreadConnectionCount + 1) {
          timeNotInUse += 1
        } else {
          timeNotInUse = 0
        }

        if (timeNotInUse > MaxTimeNotInUseSec) {
          shouldContinue = false
        }
      } catch {
        case e: Exception =>
          logger.error("Error while checking Redis connections", e)
          instanceIsStopped
          shouldContinue = false
      }
    }

    try {
      redisInstance.shutdown()
      instanceIsStopped
      isStopped = true
      stoppedPromise.success(())
    } catch {
      case e: Exception =>
        logger.error("Failed to execute post-run operations", e)
        instanceIsStopped
    }
  }
  def whenStopped(): Future[Unit] = stoppedPromise.future
  def stop(): Unit = {
    shouldContinue = false
  }

  private def instanceIsStopped: Future[Unit] = {
    databaseRepository.updateDatabasePort(redisDb.id, None).recover {
      case ex: Exception => logger.error("Failed to update database port", ex)
    }
    Future {}
  }
}
