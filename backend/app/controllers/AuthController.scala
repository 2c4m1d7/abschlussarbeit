package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.ExecutionContext
import managers.AuthManager
import managers.AuthManagerExceptions
import models.dtos.{CredentialsRequest, RefreshTokenRequest}
import scala.concurrent.Future
import play.api.libs.json._
import utils.TokenUtils
import scala.collection.Seq
import services.LdapService

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
        errors =>
          Future.successful(
            BadRequest(Json.obj("message" -> "Invalid request body"))
          ),
        credentials => {
          authManager
            .signIn(credentials.username, credentials.password)
            .map(authResponse => Ok(Json.toJson(authResponse)))
            .recover { 
              case _: LdapService.Exceptions.AccessError => BadRequest("Access Denied")
              case _ =>
              InternalServerError(
                Json.obj("message" -> "An internal server error has occurred.")
              )
            }
        }
      )
  }

  def refreshToken() = Action(parse.json).async { implicit request =>
    request.body
      .validate[RefreshTokenRequest]
      .fold(
        errors =>
          Future.successful(
            BadRequest(Json.obj("message" -> "Invalid request body"))
          ),
        refreshTokenRequest => {
          tokenUtils
            .refreshAccessToken(refreshTokenRequest.refreshToken) match {
            case Some(accessToken) =>
              Future.successful(Ok(Json.toJson(accessToken)))
            case None =>
              Future.successful(
                BadRequest(
                  Json.obj("message" -> "Failed to refresh access token")
                )
              )
          }
        }
      )
  }

}
