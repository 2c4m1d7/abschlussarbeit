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

@Singleton
class UserController @Inject() (
    userService: UserService,
    cc: ControllerComponents
)(implicit
    executionContext: ExecutionContext
) extends AbstractController(cc) {

  def addUser() = Action.async { implicit request: Request[AnyContent] =>
    val user = User(
      id = UUID.randomUUID(),
      username = request.body.asJson.get("username").as[String],
      firstName = request.body.asJson.get("firstName").as[String],
      lastName = request.body.asJson.get("lastName").as[String],
      mail = request.body.asJson.get("mail").as[String],
      employeeType = request.body.asJson.get("employeeType").as[String]
    )

    userService
      .addUser(user)
      .map(user => Ok(Json.toJson(user)))
      .recoverWith { case e: RuntimeException => exceptionToResult(e) }

  }



  def exceptionToResult(e: RuntimeException): Future[Result] = e match {
    case _: UserService.Exceptions.NotFound =>
      Future.successful(NotFound(e.getMessage))
    case _: UserService.Exceptions.AccessDenied =>
      Future.successful(Forbidden(e.getMessage))
    case _: UserService.Exceptions.InternalError =>
      Future.successful(BadRequest(e.getMessage))
  }

}

