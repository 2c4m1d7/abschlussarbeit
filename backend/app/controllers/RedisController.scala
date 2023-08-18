package controllers


import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future
import services.RedisService
import auth.SecuredAction
import scala.concurrent.ExecutionContext
import java.util.UUID
import auth.UserRequest
import models.DatabaseRow
import java.sql.Timestamp
import models.dtos.CreateDatabaseRequest
import utils.RedisConfigGenerator
import play.api.{Configuration, Logger}

@Singleton
class RedisController @Inject() (
    redisService: RedisService,
    securedAction: SecuredAction,
    cc: ControllerComponents,
    configuration: Configuration
)(implicit val ec: ExecutionContext)
    extends AbstractController(cc) {

  private val logger = Logger(this.getClass)
  private val redisHost = configuration.get[String]("redis_host")
  private val redisDirPath = configuration.get[String]("redis_directory")

  def addDB() = securedAction.async { implicit request: UserRequest[AnyContent] =>
    request.body.asJson.fold(
      Future.successful(BadRequest("Invalid request body"))
    ){ jsonBody =>
      jsonBody.validate[CreateDatabaseRequest].fold(
        errors => {
          logger.warn(s"Invalid database request: $errors")
          Future.successful(BadRequest("Invalid database request"))
        },
        databaseRequest => {
          if (databaseRequest.dbName.trim.isEmpty) 
            Future.successful(BadRequest("Database name cannot be empty"))
          else {
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
              .recover { 
                case e: Exception => 
                  logger.error("Error creating database", e)
                  InternalServerError("Error creating database")
              }
          }
        }
      )
    }
  }

 def deleteDBs() = securedAction.async { implicit request: UserRequest[AnyContent] =>
    request.body.asJson.fold(
      Future.successful(BadRequest("Invalid request body"))
    ){ jsonBody =>
      jsonBody.validate[Seq[UUID]].fold(
        _ => Future.successful(BadRequest("Invalid UUID sequence")),
        databaseIds => {
          redisService
            .deleteDatabasesByIds(databaseIds, request.user)
            .map(_ => Ok("Databases deleted successfully"))
            .recover {
              case ex => 
                logger.error("Error deleting databases", ex)
                InternalServerError(s"Error deleting databases")
            }
        }
      )
    }
  }

def exists(dbName: String) = securedAction.async { implicit request: UserRequest[AnyContent] =>
    redisService
      .dbExists(dbName, request.user)
      .map(dbExists => Ok(Json.toJson(dbExists)))
      .recover {
        case ex => 
          logger.error(s"Error checking database existence for: $dbName", ex)
          InternalServerError(s"Error checking database existence")
      }
  }

def getDbDetails(id: String) = securedAction.async { implicit request: UserRequest[AnyContent] =>
    try {
      val databaseId = UUID.fromString(id)
      redisService
        .getDb(databaseId, request.user)
        .map(db => Ok(Json.toJson(db)))
        .recover {
          case ex => 
            logger.error(s"Error retrieving details for database ID: $id", ex)
            InternalServerError(s"Error retrieving database details")
        }
    } catch {
      case _: IllegalArgumentException => 
        logger.warn(s"Invalid UUID format: $id")
        Future.successful(BadRequest("Invalid UUID format"))
    }
  }

def getDatabases() = securedAction.async { implicit request: UserRequest[AnyContent] =>
    redisService
      .getDatabaseByUserId(request.user.id)
      .map(db => Ok(Json.toJson(db)))
      .recover {
        case ex => 
          logger.error(s"Error retrieving databases for user ID: ${request.user.id}", ex)
          InternalServerError(s"Error retrieving databases")
      }
  }


}
