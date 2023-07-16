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

@Singleton
class RedisController @Inject() (
    redisService: RedisService,
    secuderAction: SecuredAction,
    cc: ControllerComponents
)(implicit val ec: ExecutionContext) extends AbstractController(cc) {

  def addDB(dbName: String) = secuderAction.async { implicit request: Request[AnyContent] =>
    redisService.create(dbName)
    .map(port => Ok(Json.toJson(port)))
    .recoverWith({ case _ => Future.successful(InternalServerError) })
  }

  def deleteDB(dbName: String) = Action {
    implicit request: Request[AnyContent] =>
      redisService.delete(dbName)
      Ok("")
  }

  def exists(dbName:String) = Action { implicit request: Request[AnyContent] =>
    Ok(Json.toJson(redisService.dbExists(dbName)))
  }

}
