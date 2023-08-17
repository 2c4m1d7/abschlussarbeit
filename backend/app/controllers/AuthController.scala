package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import managers.AuthManager
import models.dtos.{CredentialsRequest, RefreshTokenRequest}
import scala.concurrent.Future
import play.api.libs.json._
import utils.TokenUtils

@Singleton
class AuthController @Inject() (
    authManager: AuthManager,
    cc: ControllerComponents,
    tokenUtils: TokenUtils
)(implicit
    executionContext: ExecutionContext
) extends AbstractController(cc) {

  def signIn() = Action(parse.json).async { implicit request =>
  request.body
    .validate[CredentialsRequest]
    .fold(
      errors => {
        val errorMessage = errors.flatMap {
          case (path, validationErrors) =>
            validationErrors.map(e => s"${path.toJsonString} -> ${e.message}")
        }.mkString(", ")
        Future.successful(BadRequest(Json.obj("message" -> errorMessage)))
      },
      credentials => {
        authManager
          .signIn(credentials.username, credentials.password)
          .map(authResponse => Ok(Json.toJson(authResponse)))
          .recover {
            case e: Exception => InternalServerError(Json.obj("message" -> e.getMessage))
          }
      }
    )
}

def refreshToken() = Action(parse.json).async { implicit request =>
  request.body
    .validate[RefreshTokenRequest]
    .fold(
      errors => {
        val errorMessage = errors.flatMap {
          case (path, validationErrors) =>
            validationErrors.map(e => s"${path.toJsonString} -> ${e.message}")
        }.mkString(", ")
        Future.successful(BadRequest(Json.obj("message" -> errorMessage)))
      },
      refreshTokenRequest => {
        tokenUtils.refreshAccessToken(refreshTokenRequest.refreshToken) match {
          case Some(accessToken) => Future.successful(Ok(Json.toJson(accessToken)))
          case None => Future.successful(
            InternalServerError(Json.obj("message" -> "Failed to refresh access token"))
          )
        }
      }
    )
}

}
