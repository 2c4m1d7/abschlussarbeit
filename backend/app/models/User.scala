package models

import java.util.UUID
import slick.model.Table
import play.api.libs.json.OFormat
import play.api.libs.json.Json

final case class User(
    id: UUID,
    username: String,
    firstName: String,
    lastName: String,
    mail: String,
    employeeType: String
)

object User {
  implicit val userFormat: OFormat[User] = Json.format[User]
}
