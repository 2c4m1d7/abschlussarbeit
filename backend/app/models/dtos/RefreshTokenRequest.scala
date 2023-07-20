package models.dtos

import play.api.libs.json.OFormat
import play.api.libs.json.Json

final case class RefreshTokenRequest(refreshToken:String)

object RefreshTokenRequest {
    implicit val format: OFormat[RefreshTokenRequest] = Json.format[RefreshTokenRequest]
}