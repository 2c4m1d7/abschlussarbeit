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
import models.dtos.CreateDatabaseRequest
import utils.RedisConfigGenerator
import play.api.Configuration

@Singleton
class RedisController @Inject() (
    redisService: RedisService,
    secuderAction: SecuredAction,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext, configuration: Configuration)
    extends AbstractController(cc) {

  val redisHost = configuration.get[String]("redis_host")
  val redisDirPath = configuration.get[String]("redis_directory")

  def addDB() = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      val databaseRequest: CreateDatabaseRequest =
        request.request.body.asJson.get.as[CreateDatabaseRequest]

      require(
        !databaseRequest.dbName.trim.isEmpty,
        "Database name cannot be empty"
      )

      val redisConf = RedisConfigGenerator.generateRedisConfig(
        redisHost,
        "yes",
        s"$redisDirPath/${request.user.username}/${databaseRequest.dbName}",
        databaseRequest.dbName,
        databaseRequest.password
      )

      redisService
        .create(
          DatabaseRow(
            UUID.randomUUID(),
            request.user.id,
            databaseRequest.dbName,
            new Timestamp(System.currentTimeMillis()),
            None
          ),
          request.user,
          redisConf
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
        .deleteDatabasesByIds(databaseIds, request.user)
        .map(_ => Ok(""))

  }

  def exists(dbName: String) = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      redisService
        .dbExists(dbName, request.user)
        .map(dbExists => Ok(Json.toJson(dbExists)))
    // Future.successful(Ok(Json.toJson(redisService.dbExists(dbName))))
  }

  def getDbDetails(id: String) = secuderAction.async {
    implicit request: UserRequest[AnyContent] =>
      val databaseId = UUID.fromString(id)
      redisService
        .getDb(databaseId, request.user)
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
