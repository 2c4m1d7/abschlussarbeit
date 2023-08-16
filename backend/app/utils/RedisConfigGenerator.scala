package utils

import java.io.{File, PrintWriter}

object RedisConfigGenerator {

  def generateRedisConfig(
      bind: String,
      appendonly: String,
      dirPath: String,
      dbName: String,
      password: String
  ): String = {
    s"""
       |bind $bind
       |appendonly $appendonly
       |dir $dirPath
       |dbfilename $dbName
       |${if (password.isBlank()) '#' else ""}requirepass $password
       """.stripMargin
  }

  def saveToFile(filePath: String, content: String): Unit = {
    val writer = new PrintWriter(new File(filePath))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }
}
