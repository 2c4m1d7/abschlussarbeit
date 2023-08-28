package services

import org.scalatestplus.play._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import repositories.DatabaseRepository
import models.RedisDatabase
import models.User
import java.util.UUID
import scala.concurrent.Future
import java.sql.Timestamp
import java.time.Instant
import utils.RedisConfigGenerator
import play.api.Configuration
import repositories.UserRepository
import scala.concurrent.ExecutionContext
import akka.actor.CoordinatedShutdown
import akka.Done
import java.io.File
import utils.FileUtils
import java.nio.file.Path
import org.scalatest.BeforeAndAfter
import org.scalactic.source.Position
import scala.concurrent.duration._

class RedisServiceSpec extends PlaySpec with MockitoSugar with BeforeAndAfter {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockDatabaseRepo = mock[DatabaseRepository]
  val mockUserRepo = mock[UserRepository]
  val mockConfig = mock[Configuration]
  val mockCs = mock[CoordinatedShutdown]

  val redisService =
    new RedisService()(mockConfig, mockDatabaseRepo, mockUserRepo, ec, mockCs)

  val testUser = User(UUID.randomUUID(), "testUser", "", "", "", "")
  val testRedisDb = RedisDatabase(
    UUID.randomUUID(),
    testUser.id,
    "testDb",
    Timestamp.from(Instant.now()),
    None
  )
  val testRedisConf = RedisConfigGenerator.generateRedisConfig(
    "localhost",
    "yes",
    s"./data/test/${testUser.username}/testDb",
    testRedisDb.name,
    ""
  )

  before {
    reset(mockDatabaseRepo, mockConfig, mockUserRepo, mockCs)
  }

  after {
    FileUtils.deleteDir(Path.of("./testData"))
    reset(mockDatabaseRepo, mockConfig, mockUserRepo, mockCs)
  }

  "RedisService" should {

    "create a Redis database" when {

      "database with the same name already exists" in {
        when(mockConfig.get[String]("redis_host"))
          .thenReturn("localhost")

        when(mockConfig.get[String]("redis_directory"))
          .thenReturn("./testData")

        when(mockDatabaseRepo.getDatabaseByNameAndUserId(anyString, any()))
          .thenReturn(
            Future.successful(
              Some(
                testRedisDb
              )
            )
          )

        val exception = intercept[RuntimeException] {
          await(redisService.create(testRedisDb, testUser, testRedisConf))
        }

        exception.getMessage mustBe "Database already exists"
      }

      "create a new Redis database" in {
        val redisService =
          new RedisService()(
            mockConfig,
            mockDatabaseRepo,
            mockUserRepo,
            ec,
            mockCs
          )
        when(mockConfig.get[String]("redis_host"))
          .thenReturn("localhost")

        when(mockConfig.get[String]("redis_directory"))
          .thenReturn("./testData")

        when(mockDatabaseRepo.getDatabaseByNameAndUserId(anyString, any()))
          .thenReturn(Future.successful(None))
        when(mockDatabaseRepo.addDatabase(any()))
          .thenReturn(Future.successful(UUID.randomUUID()))
        when(mockDatabaseRepo.getPort(any()))
          .thenReturn(Future.successful(Some(49152)))

        val result =
          await(redisService.create(testRedisDb, testUser, testRedisConf))

        result mustBe 49152
      }
      "directory cannot be created" in {
        val redisService =
          new RedisService()(
            mockConfig,
            mockDatabaseRepo,
            mockUserRepo,
            ec,
            mockCs
          )

        when(mockDatabaseRepo.getDatabaseByNameAndUserId(anyString, any()))
          .thenReturn(Future.successful(None))
        when(mockConfig.get[String]("redis_host"))
          .thenReturn("localhost")
        when(mockConfig.get[String]("redis_directory"))
          .thenReturn("./testDb/t")

        val exception = intercept[RuntimeException] {
          await(redisService.create(testRedisDb, testUser, testRedisConf))
        }

        exception.getMessage must startWith("Failed to create directory")
      }
    }

    

  }

  def await[T](f: Future[T]): T =
    scala.concurrent.Await.result(f, 5.seconds)
}
