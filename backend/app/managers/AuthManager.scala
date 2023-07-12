package managers

import javax.inject.Inject
import play.api.Configuration
import services.UserService
import services.LdapService
import utils.TokenUtils
import scala.concurrent.ExecutionContext
import models.dtos.AuthResponse
import scala.concurrent.Future
import scala.util.Success

class AuthManager @Inject() (
    configuration: Configuration,
    userService: UserService,
    ldapService: LdapService,
//   postgreSQLEngine: PostgreSQLEngine,
    tokenUtils: TokenUtils
)(implicit
    ec: ExecutionContext
) {

  def signIn(username: String, password: String): Future[AuthResponse] = {
    for {
      user <- ldapService.authenticate(username, password)
    } yield {
      println(user)

      tokenUtils.generateTokens(user.id)
    }
  }
}
