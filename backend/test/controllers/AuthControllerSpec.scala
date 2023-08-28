package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import models.dtos.{CredentialsRequest, RefreshTokenRequest}
import utils.TokenUtils
import java.util.UUID
import javax.inject.Inject
import play.shaded.ahc.org.asynchttpclient.netty.handler.intercept.Unauthorized401Interceptor

class AuthControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "AuthController" should {

    "return BadRequest for invalid signIn request" in {
      val request = FakeRequest(POST, "/signin").withJsonBody(Json.obj())
      val signIn = route(app, request).get

      status(signIn) mustBe BAD_REQUEST
      contentAsString(signIn) must include("Invalid request body")
    }

    "sign in with valid credentials" in {

      val credentials = CredentialsRequest("user01", "password1")
      val request = FakeRequest(POST, "/signin")
        .withJsonBody(Json.toJson(credentials))

      val signIn = route(app, request).get

      status(signIn) mustBe OK
      contentAsString(signIn) must (include("accessToken") and include(
        "refreshToken"
      ))
    }

    "return BadRequest for invalid refreshToken request" in {
      val request = FakeRequest(POST, "/token/refresh").withJsonBody(Json.obj())
      val refreshToken = route(app, request).get

      status(refreshToken) mustBe BAD_REQUEST
      contentAsString(refreshToken) must include("Invalid request body")
    }

    "return OK for valid refreshToken request" in {
      val tokenUtils = app.injector.instanceOf[TokenUtils]
      val tokens = tokenUtils.generateTokens(UUID.randomUUID())
      val request = FakeRequest(POST, "/token/refresh").withJsonBody(
        Json.obj("refreshToken" -> tokens.refreshToken)
      )
      val result = route(app, request).get

      status(result) mustBe OK
    }

    "not sign in with invalid credentials" in {

      val credentials = CredentialsRequest("invalidUsername", "invalidPassword")
      val request = FakeRequest(POST, "/signin")
        .withJsonBody(Json.toJson(credentials))

      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

  }
}
