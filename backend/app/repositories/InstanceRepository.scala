package repositories

import javax.inject._
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.ExecutionContext
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import models.Instance
import java.util.UUID
import java.sql.Timestamp
import slick.sql.SqlProfile
import scala.concurrent.Future

@Singleton
class InstanceRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit
    ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val instances = TableQuery[InstanceTable]

 def getAllInstances: Future[Seq[Instance]] =
    dbConfig.db.run(instances.result)

  def getInstancesByUserId(userId: UUID): Future[Seq[Instance]] =
    dbConfig.db.run(
      instances.filter(_.userId === userId).result
    )

  def getInstancesByName(name: String): Future[Seq[Instance]] =
    dbConfig.db.run(
      instances.filter(_.name === name).result
    )

  def getInstanceById(instanceId: UUID): Future[Option[Instance]] =
    dbConfig.db.run(
      instances.filter(_.id === instanceId).result.headOption
    )

  def addInstance(instance: Instance): Future[UUID] =
    dbConfig.db.run(
      instances.returning(instances.map(_.id)) += instance
    )

  def deleteInstanceById(instanceId: UUID): Future[Int] =
    dbConfig.db.run(
      instances.filter(_.id === instanceId).delete
    )



  private class InstanceTable(tag: Tag)
      extends Table[Instance](tag, "instances") {

    def id = column[UUID]("id", O.PrimaryKey)
    def userId = column[UUID]("user_id")
    def name = column[String]("name")

    def createdAt = column[Timestamp](
      "created_at",
      SqlProfile.ColumnOption.SqlType(
        "timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"
      )
    )
    def * = (
      id,
      userId,
      name,
      createdAt
    ) <> ((Instance.apply _).tupled, Instance.unapply)
  }
}
