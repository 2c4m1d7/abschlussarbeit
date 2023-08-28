package services

import org.scalatestplus.play._
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import scala.concurrent.Future
import models.User
import play.api.Configuration
import org.apache.directory.ldap.client.api.LdapConnectionConfig
import org.apache.directory.ldap.client.api.LdapNetworkConnection
import org.apache.directory.api.ldap.model.entry.DefaultEntry
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.directory.api.ldap.model.cursor.EntryCursor
import play.api.ConfigLoader
import scala.concurrent.ExecutionContext
import play.api.Environment
import services.LdapService

class LdapServiceSpec extends PlaySpec {



  val config: Configuration = Configuration.load(Environment.simple())

  val ldapService = new LdapService(config)

  "LdapService" should {

    "authenticate a valid user" in {
      val testUser = "user01"  
      val testPassword = "password1"

      val result = await(ldapService.authenticate(testUser, testPassword))

      result.username mustBe testUser
      result.firstName mustBe "Undefined"
      result.lastName mustBe "Bar1"
    }

    def await[T](f: Future[T]): T =
      scala.concurrent.Await.result(f, scala.concurrent.duration.Duration.Inf)
  }
}
