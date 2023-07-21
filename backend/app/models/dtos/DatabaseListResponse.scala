package models.dtos

import java.util.UUID
import play.api.libs.json.OFormat
import play.api.libs.json.Json

 final case class DatabaseListInfo(
    id: UUID,
    name: String
)
object DatabaseListInfo {
  implicit val format: OFormat[DatabaseListInfo] = Json.format[DatabaseListInfo]
}

final case class DatabaseListResponse(
    databases: Seq[DatabaseListInfo]
) 

object DatabaseListResponse {
  implicit val format: OFormat[DatabaseListResponse] =
    Json.format[DatabaseListResponse]
}
