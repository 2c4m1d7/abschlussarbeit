package services

import javax.inject._
import repositories.UserRepository
import scala.concurrent.ExecutionContext
import models.User
import scala.concurrent.Future
import java.util.UUID
import scala.util.Success
import scala.util.Failure
import play.api.Logging

@Singleton
class UserService @Inject() (
    userRepository: UserRepository
)(implicit ec: ExecutionContext) extends Logging {


  def findUserById(userId: UUID): Future[Option[User]] =
    userRepository
      .getUserById(userId)
      .recoverWith { 
        case t: Throwable => 
          logger.error(s"Error fetching user by ID: ${userId}", t)
          Future.failed(UserService.Exceptions.InternalError(t.getMessage))
      }

  def findUserByUsername(username: String): Future[Option[User]] =
    userRepository
      .getUserByUsername(username)
      .recoverWith { 
        case t: Throwable => 
          logger.error(s"Error fetching user by username: ${username}", t)
          Future.failed(UserService.Exceptions.InternalError(t.getMessage))
      }

  def addUser(user: User): Future[User] =
    userRepository
      .addUser(user.copy(UUID.randomUUID()))
      .recoverWith { 
        case t: Throwable => 
          logger.error("Error adding user", t)
          Future.failed(UserService.Exceptions.InternalError(t.getMessage))
      }
      .flatMap(findUserById(_).map(_.getOrElse(throw UserService.Exceptions.NotFound("User not found after adding"))))

  private def notFoundUserIdError(userId: UUID) =
    Future.failed(
      UserService.Exceptions.NotFound(
        s"There is no user with user_id: ${userId.toString()}"
      )
    )

  private def notFoundUsernameError(username: String) =
    Future.failed(
      UserService.Exceptions.NotFound(
        s"There is no user with username: ${username}"
      )
    )
}

object UserService {
  object Exceptions {
    final case class NotFound(message: String = "User not found")
        extends RuntimeException(message)
    final case class AccessDenied(
        message: String = "Access denied. You are not the instance owner"
    ) extends RuntimeException(message)
    final case class InternalError(message: String = "Internal error")
        extends RuntimeException(message)
  }
}
