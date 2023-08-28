package services

import org.scalatestplus.play._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import repositories.UserRepository
import scala.concurrent.ExecutionContext
import java.util.UUID
import scala.concurrent.Future
import models.User

class UserServiceSpec extends PlaySpec with MockitoSugar {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val mockUserRepo = mock[UserRepository]
  val userService = new UserService(mockUserRepo)

  "UserService" should {

    "return None if user by ID is not found" in {
      val testId = UUID.randomUUID()

      when(mockUserRepo.getUserById(testId)).thenReturn(Future.successful(None))

      val result = await(userService.findUserById(testId))
      result mustBe None
    }

    "return None if user by username is not found" in {
      val testUsername = "nonExistentUsername"

      when(mockUserRepo.getUserByUsername(testUsername))
        .thenReturn(Future.successful(None))

      val result = await(userService.findUserByUsername(testUsername))
      result mustBe None
    }

    "throw an internal error if there's an error fetching user by ID" in {
      val testId = UUID.randomUUID()

      when(mockUserRepo.getUserById(testId))
        .thenReturn(Future.failed(new RuntimeException("DB error")))

      val result = intercept[UserService.Exceptions.InternalError] {
        await(userService.findUserById(testId))
      }
      result.getMessage mustBe "DB error"
    }

    "throw an internal error if there's an error fetching user by username" in {
      val testUsername = "testUsername"

      when(mockUserRepo.getUserByUsername(testUsername))
        .thenReturn(Future.failed(new RuntimeException("DB error")))

      val result = intercept[UserService.Exceptions.InternalError] {
        await(userService.findUserByUsername(testUsername))
      }
      result.getMessage mustBe "DB error"
    }

    "throw an internal error if there's an error adding a user" in {
      val testUser =
        User(
          UUID.randomUUID(),
          "testUsername",
          "testFirstName",
          "testLastName",
          "testEmail",
          "testEmployeeType"
        )

      when(mockUserRepo.addUser(any[User]))
        .thenReturn(Future.failed(new RuntimeException("DB error")))

      val result = intercept[UserService.Exceptions.InternalError] {
        await(userService.addUser(testUser))
      }
      result.getMessage mustBe "DB error"
    }

    "throw not found error if a user is not found after adding" in {
      val testUser =
        User(
          UUID.randomUUID(),
          "testUsername",
          "testFirstName",
          "testLastName",
          "testEmail",
          "testEmployeeType"
        )

      when(mockUserRepo.addUser(any[User]))
        .thenReturn(Future.successful(testUser.id))
      when(mockUserRepo.getUserById(testUser.id))
        .thenReturn(Future.successful(None))

      val result = intercept[UserService.Exceptions.NotFound] {
        await(userService.addUser(testUser))
      }
      result.getMessage mustBe "User not found after adding"
    }
    "successfully add a user" in {
      val testUser =
        User(
          UUID.randomUUID(),
          "testUsername",
          "testFirstName",
          "testLastName",
          "testEmail",
          "testEmployeeType"
        )

      when(mockUserRepo.addUser(any[User]))
        .thenReturn(Future.successful(testUser.id))
      when(mockUserRepo.getUserById(testUser.id))
        .thenReturn(Future.successful(Some(testUser)))

      val result = await(userService.addUser(testUser))
      result mustBe testUser
    }

    def await[T](f: Future[T]): T =
      scala.concurrent.Await.result(f, scala.concurrent.duration.Duration.Inf)
  }

}
