package auth

import play.api.mvc.BodyParser
import play.api.mvc.AnyContent
import utils.TokenUtils
import scala.concurrent.ExecutionContext
import play.api.mvc.ActionBuilderImpl
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.http.HeaderNames
import managers.AuthManager
import play.api.mvc.Results.Unauthorized
import play.api.mvc.Results.BadRequest
import javax.inject.Inject
import play.api.mvc.BodyParsers
import play.api.Logging
import models.User
import play.api.mvc.WrappedRequest
import play.api.mvc.ActionBuilder
import play.api.mvc.ActionTransformer
import play.api.mvc.ActionRefiner

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

class SecuredAction @Inject() (
    val parser: BodyParsers.Default,
    authManager: AuthManager
)(implicit
    ec: ExecutionContext
) extends ActionBuilder[UserRequest, AnyContent]
    with ActionRefiner[Request, UserRequest]
    with Logging {

  override protected def refine[A](
      request: Request[A]
  ): Future[Either[Result, UserRequest[A]]] = {
    request.headers.get(HeaderNames.AUTHORIZATION).flatMap { header =>
      val parts = header.split(" ")
      if (parts.length == 2 && parts(0) == "Bearer") Some(parts(1)) else None
    } match {
      case Some(token) =>
        authManager
          .verifyToken(token)
          .map(user => Right(new UserRequest(user, request)))
          .recover { case ex => 
            logger.warn(s"Failed to verify access token ${token}", ex)
            Left(Results.Unauthorized("Invalid access token")) 
          }
      case None =>
        logger.warn(s"Missing access token in the request headers: ${request}")
        Future.successful(Left(Results.Unauthorized("Access token is missing")))
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
