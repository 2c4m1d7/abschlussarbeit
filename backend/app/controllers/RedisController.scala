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

      redisService
        .create(DatabaseRow(UUID.randomUUID(), request.user.id, dbName, new Timestamp(System.currentTimeMillis())))
        .map(port => Ok(Json.toJson(port)))
        .recoverWith({ case _ => Future.successful(InternalServerError) })
  }

  def deleteDB(dbName: String) = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      redisService.delete(dbName)
      Future.successful(Ok(""))
  }

  def exists(dbName: String) = secuderAction.async { implicit request: UserRequest[AnyContent] =>
     Future.successful(Ok(Json.toJson(redisService.dbExists(dbName))))
  }

  def getDbDetails(id: String) = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>

      val databaseId = UUID.fromString(id)
      redisService
        .getDb(databaseId)
        .map(db => Ok(Json.toJson(db)))
        .recoverWith({ case e => Future.successful(Ok(Json.toJson(e.getMessage()))) })
  }

  def getDatabases() = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      redisService
        .getDatabaseByUserId(request.user.id)
        .map(db => Ok(Json.toJson(db)))
  }

}
