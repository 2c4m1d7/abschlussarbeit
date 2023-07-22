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

class UserRequest[A](val user: User, val request: Request[A])
    extends WrappedRequest[A](request)

class SecuredAction @Inject() (
    parser: BodyParsers.Default,
    authManager: AuthManager
)(implicit
    val ec: ExecutionContext
) extends ActionBuilder[UserRequest, AnyContent]
    with ActionRefiner[Request, UserRequest] 
    with Logging{

  override protected def refine[A](
      request: Request[A]
  ): Future[Either[Result, UserRequest[A]]] = {
    val maybeToken =
      request.headers.get(HeaderNames.AUTHORIZATION).map(_.split(" ").last)

    maybeToken match {
      case Some(token) =>
        authManager
          .verifyToken(token)
          .map(user => Right(new UserRequest(user, request)))
          .recover { case _ => Left(Results.Unauthorized("Invalid access token")) }
      case None =>
        Future.successful(Left(Results.Unauthorized("Access token is missing")))
    }
  }

  override protected def executionContext: ExecutionContext = ec

  override def parser: BodyParser[AnyContent] = this.parser

  // override def invokeBlock[A](
  //     request: Request[A],
  //     block: Request[A] => Future[Result]
  // ): Future[Result] = {
  //   val maybeToken =
  //     request.headers.get(HeaderNames.AUTHORIZATION).map(_.split(" ").last)

  //   maybeToken match {
  //     case Some(token) =>
  //       authManager
  //         .verifyToken(token)
  //         .flatMap(user => block(request))
  //         .recoverWith {
  //           case e: SecuredAction.Exceptions.SecuredActionException =>
  //             exceptionToResult(e)
  //         }
  //     case None =>
  //       // Missing authorization header, return unauthorized response
  //       exceptionToResult(
  //         SecuredAction.Exceptions.InvalidAccessTokenRejection()
  //       )
  //     // Future.successful(Results.Unauthorized("Missing authorization header"))
  //   }
  // }

  def exceptionToResult(
      error: SecuredAction.Exceptions.SecuredActionException
  ): Future[Result] = {
    logger.warn(error.getMessage)
    error match {
      case _: SecuredAction.Exceptions.MissingAccessTokenRejection =>
        Future.successful(Unauthorized(error.getMessage))
      case _: SecuredAction.Exceptions.InvalidAccessTokenRejection =>
        Future.successful(Unauthorized(error.getMessage))
      case _: SecuredAction.Exceptions.InvalidAuthContentRejection =>
        Future.successful(Unauthorized(error.getMessage))
      case _: SecuredAction.Exceptions.InternalError =>
        Future.successful(BadRequest(error.getMessage))
    }
  }
}

object SecuredAction {
  object Exceptions {
    sealed abstract class SecuredActionException(message: String)
        extends Exception(message)

    final case class MissingAccessTokenRejection(
        message: String = "Access token missing"
    ) extends SecuredActionException(message)
    final case class InvalidAccessTokenRejection(
        message: String = "Invalid access token"
    ) extends SecuredActionException(message)
    final case class InvalidAuthContentRejection(
        message: String = "Invalid claim"
    ) extends SecuredActionException(message)
    final case class InternalError(
        message: String = "Oops. Something went wrong :("
    ) extends SecuredActionException(message)
  }
}
