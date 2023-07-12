package services

import scala.concurrent.Future
import models.User
import javax.inject.Inject
import play.api.Configuration
import scala.concurrent.ExecutionContext
import org.apache.directory.ldap.client.api.LdapConnectionConfig
import services.exceptions.ServiceException
import LdapService.Exceptions._
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.apache.directory.api.ldap.model.exception.LdapException
import org.apache.directory.api.ldap.model.cursor.CursorException
import org.apache.directory.api.ldap.model.message.SearchScope
import org.apache.directory.api.ldap.model.entry.Entry
import java.util.UUID

class LdapService @Inject() (config: Configuration)(implicit ec: ExecutionContext)  {

 def authenticate(username: String, password: String): Future[User] =
    for {
      ldapConnectionConfig <- getConnectionConfig(username, password)
      ldapConnection <- getLdapConnection(ldapConnectionConfig)
      entry <- searchForEntry(ldapConnection, username)
      user <- getUserFromEntry(entry, username)

    } yield {
      print(ldapConnectionConfig)
      print(ldapConnection)
      print(entry)
      print(user)
      user
    }

  def getConnectionConfig(username: String, password: String): Future[LdapConnectionConfig] = {
    val ldapConnectionConfig = new LdapConnectionConfig()

    val ldapHostJob = Future {
      config.getOptional[String]("ldap.host")
    } flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(EnvironmentError())
    }

    val ldapPortJob = Future {
      config.getOptional[Int]("ldap.port")
    } flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(EnvironmentError())
    }

    val ldapStartTlsJob = Future {
      config.getOptional[Boolean]("ldap.startTls")
    } flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(EnvironmentError())
    }

    val ldapUseSslJob = Future {
      config.getOptional[Boolean]("ldap.useSsl")
    } flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(EnvironmentError())
    }

    val ldapUserRootJob = Future {
      config.getOptional[String]("ldap.userRoot")
    } flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(EnvironmentError())
    }

    val ldapNameJob = Future {
      config.getOptional[String]("ldap.name")
    } flatMap {
      case Some(value) => Future.successful(value)
      case None        => Future.failed(EnvironmentError())
    }

    for {
      ldapHost <- ldapHostJob
      ldapPort <- ldapPortJob
      ldapStartTls <- ldapStartTlsJob
      ldapUseSsl <- ldapUseSslJob
      ldapUserRoot <- ldapUserRootJob
      ldapName <- ldapNameJob
    } yield {
      ldapConnectionConfig.setLdapHost(ldapHost)
      ldapConnectionConfig.setLdapPort(ldapPort)
      ldapConnectionConfig.setUseTls(ldapStartTls)
      ldapConnectionConfig.setUseSsl(ldapUseSsl)
      ldapConnectionConfig.setName(
        ldapName
          .replace("%USER%", username)
          .replace("%USER_ROOT%", ldapUserRoot)
      )
      ldapConnectionConfig.setCredentials(password)
      ldapConnectionConfig
    }
  }

  def getLdapConnection(ldapConnectionConfig: LdapConnectionConfig): Future[LdapNetworkConnection] =
    Future {
      val ldapConnection = new LdapNetworkConnection(ldapConnectionConfig)
      ldapConnection.bind()
      ldapConnection

    }.recoverWith {
      case _: LdapException => Future.failed(AccessDenied())
      case t: Throwable     => internalError(t.getMessage)
    }

  def searchForEntry(ldapNetworkConnection: LdapNetworkConnection, username: String): Future[Entry] =
    Future {
      val ldapUserRoot = config.get[String]("ldap.userRoot")
      val filter = s"(cn=$username)"
      val entryCursor = ldapNetworkConnection.search(ldapUserRoot, filter, SearchScope.ONELEVEL, "*")
      entryCursor.next()
      entryCursor.get();
    }.recoverWith {
      case e: LdapException   => Future.failed(AccessDenied(e.getMessage))
      case e: CursorException => Future.failed(AccessDenied(e.getMessage))
      case t: Throwable       => internalError(t.getMessage)
    }

  def getUserFromEntry(entry: Entry, username: String): Future[User] =
    Future {
      val firstName = getStringWithFallBack(entry, "givenName", "Undefined")
      val lastName = getStringWithFallBack(entry, "sn", "Undefined")
      val mail = getStringWithFallBack(entry, "mail", "")
      val employeeType = getStringWithFallBack(entry, "employeetype", "Uncategorized")
      User(UUID.randomUUID(), username, firstName, lastName, mail, employeeType)
    }

  private def getStringWithFallBack(entry: Entry, key: String, fallBack: String): String =
    try entry.get(key).getString
    catch {
      case _: Throwable => fallBack
    }

  private def internalError(errorMessage: String): Future[Nothing] = {
    // logger.error(errorMessage)
    Future.failed(LdapService.Exceptions.InternalError(errorMessage))
  }

}

object LdapService {
  object Exceptions {
    sealed abstract class LdapServiceException(message: String) extends ServiceException(message)

    final case class EnvironmentError(message: String = "Could not load ldap environment")
        extends LdapServiceException(message)
    final case class AccessDenied(message: String = "Access denied") extends LdapServiceException(message)
    final case class InternalError(message: String = "Internal error") extends LdapServiceException(message)
  }
}

