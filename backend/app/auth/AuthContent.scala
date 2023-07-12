package auth

import java.util.UUID
import play.api.libs.json.Json
import play.api.libs.json.OFormat

sealed trait AuthContent

object AuthContent {
  implicit val authContentFormat: OFormat[AuthContent] = Json.format[AuthContent]
}

final case class AccessTokenContent(userId: UUID) extends AuthContent

object AccessTokenContent {
  implicit val accessTokenContentFormat: OFormat[AccessTokenContent] = Json.format[AccessTokenContent]
}

final case class RefreshTokenContent(userId: UUID) extends AuthContent

object RefreshTokenContent {
  implicit val refreshTokenContentFormat: OFormat[RefreshTokenContent] = Json.format[RefreshTokenContent]
}
