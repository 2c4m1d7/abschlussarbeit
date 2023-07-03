package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.Failure
import play.core.server.ProdServerStart
import services.RedisService

@Singleton
class RedisController @Inject() (
    redisService: RedisService,
    cc: ControllerComponents
) extends AbstractController(cc) {

  def addDB(dbName: String) = Action { implicit request: Request[AnyContent] =>
    Ok(Json.toJson(redisService.create(dbName)))
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
