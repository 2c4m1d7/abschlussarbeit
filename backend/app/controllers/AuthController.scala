package controllers


import javax.inject._
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import play.api.mvc.AbstractController
import managers.AuthManager
import models.dtos.CredentialsRequest
import scala.concurrent.Future
import play.api.libs.json.Json

@Singleton
class AuthController @Inject() (
    authManager: AuthManager,
    cc: ControllerComponents
)(implicit
    executionContext: ExecutionContext
) extends AbstractController(cc) {

  def signIn() = Action(parse.json).async { implicit request =>
    request.body
      .validate[CredentialsRequest]
      .fold(
        errors => {
          Future.successful(
            BadRequest(Json.obj("message" -> "Invalid request body"))
          )
        },
        credentials => {
          authManager
            .signIn(credentials.username, credentials.password)
            .map(authResponse => {
              Ok(Json.toJson(authResponse))
            })
            .recoverWith { case _ => Future.successful(InternalServerError) }
          //   Future.successful(Ok(Json.toJson(tokens)))
        }
      )

  }
}
