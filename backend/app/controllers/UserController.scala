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
    cc: ControllerComponents,
    securedAction: SecuredAction,
)(implicit
    executionContext: ExecutionContext
) extends AbstractController(cc) {

  def findUser() = securedAction.async {
    implicit request: UserRequest[AnyContent] =>
      Future.successful(Ok(Json.toJson(request.user)))
  }

}
