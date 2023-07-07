package models

import java.util.UUID
import java.sql.Timestamp
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.OFormat
import play.api.libs.json.Json

final case class Instance(
    id: UUID,
    userId: UUID,
    name: String,
    createdAt: Timestamp
)

object Instance {

  implicit val timestampReads: Reads[Timestamp] =
    implicitly[Reads[Long]].map(new Timestamp(_))

  implicit val timestampWrites: Writes[Timestamp] =
    implicitly[Writes[Long]].contramap(_.getTime)

  implicit lazy val instanceFormat: OFormat[Instance] = Json.format[Instance]
}