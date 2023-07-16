package models.dtos

import play.api.libs.json.OFormat
import play.api.libs.json.Json

final case class CredentialsRequest(username: String, password: String)

object CredentialsRequest {
    implicit val format: OFormat[CredentialsRequest] = Json.format[CredentialsRequest]
}
