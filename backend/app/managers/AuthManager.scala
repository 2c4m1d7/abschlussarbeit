package managers

import javax.inject.Inject
import play.api.Configuration
import services.UserService
import services.LdapService
import utils.TokenUtils
import scala.concurrent.{Future, ExecutionContext}
import models.dtos.AuthResponse

import models.User
import auth.AuthContent
import play.api.libs.json.Json
import auth.AccessTokenContent
import pdi.jwt.JwtClaim
import play.api.Logger

// class AuthManager @Inject() (
//     configuration: Configuration,
//     userService: UserService,
//     ldapService: LdapService,
//     tokenUtils: TokenUtils
// )(implicit
//     ec: ExecutionContext
// ) {
//   private val logger = Logger(this.getClass)
//   def signIn(username: String, password: String): Future[AuthResponse] = {
//     for {
//       ldapUser <- ldapService.authenticate(username, password).recoverWith {
//         case e: ServiceException => serviceErrorMapper(e)
//       }
//       userOpt <- userService.findUserByUsername(username)
//       user <- userOpt
//         .map(Future.successful)
//         .getOrElse(userService.addUser(ldapUser))
//     } yield {
//       tokenUtils.generateTokens(user.id)
//     }
//   }

//   def serviceErrorMapper(exception: ServiceException): Future[Nothing] = {
//     logger.error(exception.getMessage)
//     exception match {
//       case e: LdapService.Exceptions.AccessDenied =>
//         Future.failed(AuthManager.Exceptions.AccessDenied(e.getMessage))
//       case e: LdapService.Exceptions.EnvironmentError =>
//         Future.failed(AuthManager.Exceptions.InternalError(e.getMessage))
//       case e: LdapService.Exceptions.InternalError =>
//         Future.failed(AuthManager.Exceptions.InternalError(e.getMessage))
//       case _ => internalError("Uncaught exception")
//     }
//   }

//   private def internalError(errorMessage: String): Future[Nothing] = {
//     logger.error(errorMessage)
//     Future.failed(AuthManager.Exceptions.InternalError(errorMessage))
//   }

//   def verifyToken(token: String): Future[User] = {
//     tokenUtils.getJwtClaims(token) match {
//       case Some(decodedClaim: JwtClaim) =>
//         val authContent = Json.parse(decodedClaim.content).as[AuthContent]
//         authContent match {
//           case AccessTokenContent(userId) =>
//             userService.findUserById(userId).flatMap {
//               case Some(user) => Future.successful(user)
//               case None =>
//                 Future.failed(
//                   UserService.Exceptions
//                     .NotFound(s"User with ID: $userId not found")
//                 )
//             }
//           case _ => Future.failed(LdapService.Exceptions.AccessDenied())
//         }
//       case _ => Future.failed(LdapService.Exceptions.AccessDenied())
//     }
//   }

// }

// object AuthManager {
//   object Exceptions {
//     sealed abstract class AuthManagerException(message: String)
//         extends RuntimeException(message)
        
//     final case class AccessDenied(message: String = "Access denied")
//         extends AuthManagerException(message)
//     final case class InternalError(message: String = "Internal error")
//         extends AuthManagerException(message)
//   }
// }

class AuthManager @Inject() (
    configuration: Configuration,
    userService: UserService,
    ldapService: LdapService,
    tokenUtils: TokenUtils
)(implicit
    ec: ExecutionContext
) {
  private val logger = Logger(this.getClass)
  
  def signIn(username: String, password: String): Future[AuthResponse] = {
    for {
      ldapUser <- ldapService.authenticate(username, password)
      userOpt <- userService.findUserByUsername(username)
      user <- userOpt.map(Future.successful).getOrElse(userService.addUser(ldapUser))
    } yield {
      tokenUtils.generateTokens(user.id)
    }
  }

  def verifyToken(token: String): Future[User] = {
    tokenUtils.getJwtClaims(token) match {
      case Some(decodedClaim: JwtClaim) =>
        val authContent = Json.parse(decodedClaim.content).as[AuthContent]
        authContent match {
          case AccessTokenContent(userId) => userService.findUserById(userId).flatMap {
            case Some(user) => Future.successful(user)
            case None => Future.failed(UserNotFoundException(s"User with ID: $userId not found"))
          }
          case _ => Future.failed(AccessDeniedException("Access Denied"))
        }
      case _ => Future.failed(AccessDeniedException("Access Denied"))
    }
  }


  case class UserNotFoundException(message: String) extends RuntimeException(message)
  case class AccessDeniedException(message: String) extends RuntimeException(message)
  case class AuthenticationException(message: String) extends RuntimeException(message)

}

