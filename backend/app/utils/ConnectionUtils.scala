package utils

import java.net.{InetSocketAddress, Socket}
import views.html.defaultpages.error

object ConnectionUtils {

  def isPortOpen(host: String, port: Int): Boolean = {
      val socket = new Socket()
    try {
      socket.connect(new InetSocketAddress(host, port), 1000)
      false
    } catch {
      case e: Throwable => true
    } finally {
      socket.close()
    }
  }

  def findAvailablePort(
      host: String,
      startPort: Int,
      endPort: Int
  ): Option[Int] = {
    (startPort to endPort).find(isPortOpen(host, _))
  }

}
