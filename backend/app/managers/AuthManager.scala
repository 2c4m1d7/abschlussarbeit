package managers

import javax.inject.Inject
import play.api.Configuration
import services.UserService
import services.LdapService
import utils.TokenUtils
import scala.concurrent.ExecutionContext
import models.dtos.AuthResponse
import scala.concurrent.Future
import scala.util.Success
import services.exceptions.ServiceException
import models.User
import java.util.UUID
import auth.AuthContent
import play.api.libs.json.Json
import auth.AccessTokenContent
import pdi.jwt.JwtClaim

class AuthManager @Inject() (
    configuration: Configuration,
    userService: UserService,
    ldapService: LdapService,
    tokenUtils: TokenUtils
)(implicit
    ec: ExecutionContext
) {

  def signIn(username: String, password: String): Future[AuthResponse] = {
    for {
      ldapUser <- ldapService
        .authenticate(username, password)
        .recoverWith { case e: ServiceException =>
          serviceErrorMapper(e)
        } // TODO check exeptions handling
      user <- userService
        .findUserByUsername(username)
        .recoverWith {
          case _: UserService.Exceptions.NotFound =>
            userService.addUser(ldapUser)
          case e: ServiceException => serviceErrorMapper(e)
        }
    } yield {
      tokenUtils.generateTokens(user.id)
    }
  }

  def serviceErrorMapper(exception: ServiceException): Future[Nothing] = {
    // logger.error(exception.getMessage)
    exception match {
      case e: LdapService.Exceptions.AccessDenied =>
        Future.failed(AuthManager.Exceptions.AccessDenied(e.getMessage))
      case e: LdapService.Exceptions.EnvironmentError =>
        Future.failed(AuthManager.Exceptions.InternalError(e.getMessage))
      case e: LdapService.Exceptions.InternalError =>
        Future.failed(AuthManager.Exceptions.InternalError(e.getMessage))

      case _: Throwable => internalError("Uncaught exception")
    }
  }
  private def internalError(errorMessage: String): Future[Nothing] = {
    // logger.error(errorMessage)
    Future.failed(AuthManager.Exceptions.InternalError(errorMessage))
  }

  def verifyToken(token: String): Future[User] = {

    tokenUtils.getJwtClaims(token) match {
      case Some(decodedClaim: JwtClaim) =>
        val authContent = Json.parse(decodedClaim.content).as[AuthContent]
        authContent match {
          case AccessTokenContent(userId) =>
            userService
              .findUserById(userId)
              .recoverWith { case e: ServiceException => Future.failed(e) }
          case _ => Future.failed(AuthManager.Exceptions.InternalError()) // TODO 
        }
      case _ => Future.failed(AuthManager.Exceptions.InternalError())
    }
  }
}

object AuthManager {
  object Exceptions {
    sealed abstract class AuthManagerException(message: String)
        extends RuntimeException(message)

    final case class AccessDenied(message: String = "Access denied")
        extends AuthManagerException(message)
    final case class InternalError(message: String = "Internal error")
        extends AuthManagerException(message)
  }
}
