package models

import java.util.UUID
import java.sql.Timestamp
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.json.OFormat
import play.api.libs.json.Json

object RedisDatabase {

  implicit val timestampReads: Reads[Timestamp] =
    implicitly[Reads[Long]].map(new Timestamp(_))

  implicit val timestampWrites: Writes[Timestamp] =
    implicitly[Writes[Long]].contramap(_.getTime)

  implicit lazy val instanceFormat: OFormat[RedisDatabase] = Json.format[RedisDatabase]
}

final case class RedisDatabase(
    id: UUID,
    userId: UUID,
    name: String,
    createdAt: Timestamp,
    port: Option[Int]
)