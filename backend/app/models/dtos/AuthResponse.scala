package models.dtos

import play.api.libs.json.OFormat
import play.api.libs.json.Json

final case class AuthResponse(accessToken: String, refreshToken: String)

object AuthResponse {
    implicit val format: OFormat[AuthResponse] = Json.format[AuthResponse]
}