package utils

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import java.util.UUID
import play.api.{Application, Configuration, Play}
import play.api.Environment
import java.io.File
import play.api.Mode

class TokenUtilsSpec extends PlaySpec with GuiceOneAppPerSuite {


  val configuration: Configuration = app.injector.instanceOf[Configuration]
  val tokenUtils = new TokenUtils(configuration)

  "TokenUtils" should {

    "generate valid tokens" in {
      val userId = UUID.randomUUID()

      val tokens = tokenUtils.generateTokens(userId)

      tokens.accessToken must not be empty
      tokens.refreshToken must not be empty
    }

    "generate different access and refresh tokens" in {
      val userId = UUID.randomUUID()

      val tokens = tokenUtils.generateTokens(userId)

      tokens.accessToken must not equal tokens.refreshToken
    }

    "refresh access token with valid refresh token" in {
      val userId = UUID.randomUUID()

      val tokens = tokenUtils.generateTokens(userId)
      Thread.sleep(1000)
      val newAccessToken = tokenUtils.refreshAccessToken(tokens.refreshToken)

      newAccessToken must be(Symbol("defined"))
      newAccessToken.get must not equal tokens.accessToken
    }

    "not refresh access token with invalid token" in {
      val invalidToken = "invalidToken"

      val newAccessToken = tokenUtils.refreshAccessToken(invalidToken)

      newAccessToken must not be (Symbol("defined"))
    }

    "retrieve correct Jwt claims for a given token" in {
      val userId = UUID.randomUUID()

      val tokens = tokenUtils.generateTokens(userId)
      val claims = tokenUtils.getJwtClaims(tokens.accessToken)

      claims must be(Symbol("defined"))
      claims.get.content must include(userId.toString)
    }
  }
}
