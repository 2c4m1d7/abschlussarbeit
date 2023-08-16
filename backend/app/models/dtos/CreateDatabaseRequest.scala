package models.dtos

import play.api.libs.json._

final case class CreateDatabaseRequest(dbName: String, password: String)

object CreateDatabaseRequest {
  implicit val createDatabaseRequestFormat: Format[CreateDatabaseRequest] =
    Json.format[CreateDatabaseRequest]
}
