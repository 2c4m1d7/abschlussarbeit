package utils

import java.util.UUID
import models.dtos.AuthResponse
import javax.inject.Inject
import play.api.Configuration
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.JwtAlgorithm
import pdi.jwt.Jwt
import pdi.jwt.JwtClaim
import java.time.Clock
import play.api.libs.json.Json
import java.time.Instant

import auth._
import scala.util.Success
import scala.util.Failure

abstract class TokenUtils @Inject() (configuration: Configuration) {

  val SECRET_KEY: String = configuration.get[String]("jwt.secret_key")

  val hmacAlgorithm = JwtAlgorithm.HS256

  val accessExpirationTime: Long =
    configuration.get[Long]("jwt.access_expiration_time")

  val refreshExpirationTime: Long =
    configuration.get[Long]("jwt.refresh_expiration_time")

  def generateTokens(userId: UUID, currentTimestamp: Long): AuthResponse = {
    AuthResponse(
      accessToken = generateToken(
        AccessTokenContent(userId),
        currentTimestamp,
        accessExpirationTime
      ),
      refreshToken = generateToken(
        RefreshTokenContent(userId),
        currentTimestamp,
        refreshExpirationTime
      )
    )
  }

  def refreshAccessToken(refreshToken: String): Option[String] = {
    val currentTimestamp = Instant.now().getEpochSecond()
    Jwt.decode(refreshToken, SECRET_KEY, Seq(hmacAlgorithm)) match {
      case Success(decodedClaim) =>
        if (decodedClaim.expiration.exists(_ < currentTimestamp)) {
          return None
        }

        val authContent = Json.parse(decodedClaim.content).as[AuthContent]
        authContent match {
          case RefreshTokenContent(userId) =>
            Some(
              generateToken(
                AccessTokenContent(userId),
                currentTimestamp,
                refreshExpirationTime
              )
            )
          case _ => None
        }
      case Failure(_) => None
    }
  }

  private def generateToken(
      auth: AuthContent,
      currentTimestamp: Long,
      expirationTime: Long
  ): String = {
    val claim = JwtClaim(
      content = Json.stringify(Json.toJson(auth)),
      expiration = Some(expirationTime + currentTimestamp),
      issuedAt = Some(currentTimestamp)
    )
    Jwt.encode(
      claim,
      SECRET_KEY,
      hmacAlgorithm
    )
  }
}
