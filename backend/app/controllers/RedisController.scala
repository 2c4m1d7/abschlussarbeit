package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.Failure
import play.core.server.ProdServerStart
import services.RedisService
import auth.SecuredAction
import scala.concurrent.ExecutionContext
import java.util.UUID
import auth.UserRequest
import models.DatabaseRow
import java.sql.Timestamp

@Singleton
class RedisController @Inject() (
    redisService: RedisService,
    secuderAction: SecuredAction,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc) {

  def addDB(dbName: String) = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      require(!dbName.trim.isEmpty, "Database name cannot be empty")

      redisService
        .create(
          DatabaseRow(
            UUID.randomUUID(),
            request.user.id,
            dbName,
            new Timestamp(System.currentTimeMillis())
          ),
          request.user.id
        )
        .map(port => Ok(Json.toJson(port)))
        .recoverWith({ case _ => Future.successful(InternalServerError) })
  }

  // def deleteDB(id: String) = secuderAction.async {
  //   implicit request: UserRequest[AnyContent] =>
  //     redisService.delete(UUID.fromString(id))
  //     Future.successful(Ok(""))
  // }

  def deleteDBs() = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      val databaseIds = request.request.body.asJson.get.as[Seq[UUID]]
      redisService
        .deleteDatabasesByIds(databaseIds, request.user.id)
        .map(_ => Ok(""))

  }

  def exists(dbName: String) = Action.async {
    implicit request: Request[AnyContent] =>
      redisService.dbExists(dbName)
      .map(dbExists => Ok(Json.toJson(dbExists)))
      // Future.successful(Ok(Json.toJson(redisService.dbExists(dbName))))
  }

  def getDbDetails(id: String) = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      val databaseId = UUID.fromString(id)
      redisService
        .getDb(databaseId, request.user.id)
        .map(db => Ok(Json.toJson(db)))
        .recoverWith({ case e => Future.failed(e) })
  }

  def getDatabases() = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      redisService
        .getDatabaseByUserId(request.user.id)
        .map(db => Ok(Json.toJson(db)))
  }

}
