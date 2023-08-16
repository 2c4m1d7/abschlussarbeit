package utils

import scala.io.Source

object RedisConfigReader {
  def extractPassword(filePath: String): Option[String] = {
    val lines = Source.fromFile(filePath).getLines()

    for (line <- lines) {
      if (line.startsWith("requirepass")) {
        return Some(line.split(" ").last)
      }
    }
    None
  }
}
