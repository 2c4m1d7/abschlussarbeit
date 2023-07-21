package models.dtos

import play.api.libs.json.OFormat
import play.api.libs.json.Json
import java.util.UUID
import java.sql.Timestamp
import play.api.libs.json.Reads
import play.api.libs.json.Writes

final case class DatabaseResponse(
    id: UUID,
    name: String,
    port: Int,
    createdAt: Timestamp
)

object DatabaseResponse {
  implicit val timestampReads: Reads[Timestamp] =
    implicitly[Reads[Long]].map(new Timestamp(_))

  implicit val timestampWrites: Writes[Timestamp] =
    implicitly[Writes[Long]].contramap(_.getTime)

  implicit val format: OFormat[DatabaseResponse] = Json.format[DatabaseResponse]
}
