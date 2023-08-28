package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import models.User
import java.util.UUID
import auth.UserRequest
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Headers
import play.api.http.HeaderNames
import repositories.DatabaseRepository
import repositories.UserRepository
import scala.concurrent.duration._
import scala.concurrent.Await
import utils.TokenUtils
import auth.AuthContent
import auth.AccessTokenContent
import java.sql.Timestamp
import java.time.Instant
import pdi.jwt.JwtClaim

class UserControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "UserController" should {

    "return user for findUser request" in {
      val databaseRepository = app.injector.instanceOf[UserRepository]
      val tokenUtils = app.injector.instanceOf[TokenUtils]
      val user = Await
        .result(databaseRepository.getUserByUsername("user01"), 5.seconds)
        .get
      val tokens = tokenUtils.generateTokens(user.id)

      val request = FakeRequest(GET, "/user")
        .withHeaders(
          HeaderNames.AUTHORIZATION -> s"Bearer ${tokens.accessToken}"
        )
        .withBody(AnyContentAsEmpty)

      val findUser = route(app, request).get

      status(findUser) mustBe OK
      contentAsJson(findUser) mustBe Json.toJson(user)
    }

    "return Unauthorized for findUser request with invalid access token" in {
      val invalidAccessToken = "invalid_access_token"

      val request = FakeRequest(GET, "/user")
        .withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $invalidAccessToken")
        .withBody(AnyContentAsEmpty)

      val findUser = route(app, request).get

      status(findUser) mustBe UNAUTHORIZED
      contentAsString(findUser) must include("Invalid access token")
    }

    "return Unauthorized for findUser request without access token" in {

      val request = FakeRequest(GET, "/user")
        .withBody(AnyContentAsEmpty)

      val findUser = route(app, request).get

      status(findUser) mustBe UNAUTHORIZED
      contentAsString(findUser) must include("Access token is missing")
    }

    "return Unauthorized for findUser request for valid user but invalid access token" in {
      val databaseRepository = app.injector.instanceOf[UserRepository]
      val tokenUtils = app.injector.instanceOf[TokenUtils]
      val user = Await
        .result(databaseRepository.getUserByUsername("user01"), 5.seconds)
        .get
      val token = tokenUtils.generateToken(AccessTokenContent(user.id), Instant.now().getEpochSecond(), -1000)
      
      val request = FakeRequest(GET, "/user")
        .withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${token}")
        .withBody(AnyContentAsEmpty)

      val findUser = route(app, request).get

      status(findUser) mustBe UNAUTHORIZED
      contentAsString(findUser) must include("Invalid access token")
    }

  }
}
