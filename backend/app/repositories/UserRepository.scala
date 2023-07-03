package repositories

import play.api.db.slick.DatabaseConfigProvider
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import slick.jdbc.JdbcProfile
import models.User
import slick.lifted.ProvenShape
import java.util.UUID
import scala.concurrent.Future
import play.api.db.slick.HasDatabaseConfigProvider

@Singleton
class UserRepository @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider
)(implicit
    ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val users = TableQuery[UserTable]

  def addUser(user: User): Future[UUID] =
    db.run(users.returning(users.map(_.id)) += user)

  def getUserByUsername(username: String): Future[Option[User]] =
    db.run(
      users.filter(_.username === username).result.headOption
    )

  def getUserById(userId: UUID): Future[Option[User]] =
    db.run(
      users.filter(_.id === userId).result.headOption
    )
  def getUsers: Future[Seq[User]] = db.run(
    users.result
  )

  private class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[UUID]("id", O.PrimaryKey)
    def username = column[String]("username")
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def mail = column[String]("mail")
    def employeeType = column[String]("employee_type")
    override def * = (
      id,
      username,
      firstName,
      lastName,
      mail,
      employeeType
    ) <> ((User.apply _).tupled, User.unapply)
  }

}
