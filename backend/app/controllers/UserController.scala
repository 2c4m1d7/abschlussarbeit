package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import javax.inject.Inject
import services.UserService
import models.User
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import models.dtos.CredentialsRequest
import auth.SecuredAction
import play.api.http.HeaderNames
import utils.TokenUtils
import managers.AuthManager
import auth.UserRequest

@Singleton
class UserController @Inject() (
    userService: UserService,
    cc: ControllerComponents,
    securedAction: SecuredAction,
)(implicit
    executionContext: ExecutionContext
) extends AbstractController(cc) {

  def addUser() = Action.async(parse.json) { implicit request =>
    request.body
      .validate[User]
      .fold(
        errors => Future.successful(BadRequest(JsError.toJson(errors))),
        user =>
          userService
            .addUser(user)
            .map(addedUser => Ok(Json.toJson(addedUser)))
            .recoverWith { case e: RuntimeException => exceptionToResult(e) }
      )
  }

  def findUser() = securedAction.async {
    implicit request: UserRequest[AnyContent] =>
      Future.successful(Ok(Json.toJson(request.user)))
  }

  def exceptionToResult(e: RuntimeException): Future[Result] = e match {
    case _: UserService.Exceptions.NotFound =>
      Future.successful(NotFound(e.getMessage))
    case _: UserService.Exceptions.InternalError =>
      Future.successful(BadRequest(e.getMessage))
  }

}
