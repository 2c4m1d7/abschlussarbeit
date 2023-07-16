package services

import javax.inject._
import repositories.UserRepository
import scala.concurrent.ExecutionContext
import models.User
import scala.concurrent.Future
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success
import scala.util.Failure
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class UserService @Inject() (
    userRepository: UserRepository
)(implicit ec: ExecutionContext) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def findUserById(userId: UUID): Future[User] =
    userRepository
      .getUserById(userId)
      .recoverWith { case t: Throwable => internalError(t.getMessage) }
      .flatMap {
        case None       => notFoundUserIdError(userId)
        case Some(user) => Future.successful(user)
      }

  def findUserByUsername(username: String): Future[User] = {
    userRepository
      .getUserByUsername(username)
      .recoverWith { case t: Throwable => internalError(t.getMessage) }
      .flatMap {
        case None       => notFoundUsernameError(username)
        case Some(user) => Future.successful(user)
      }
  }

  def addUser(user: User): Future[User] = {

    userRepository
      .addUser(user.copy(UUID.randomUUID()))
      .recoverWith { case t: Throwable =>
        internalError(t.getMessage)
      }
      .flatMap(findUserById(_))
  }




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

  private def internalError(errorMessage: String): Future[Nothing] = {
    logger.error(errorMessage)
    Future.failed(UserService.Exceptions.InternalError(errorMessage))
  }
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
