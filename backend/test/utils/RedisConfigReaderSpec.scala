package utils

import org.scalatestplus.play._
import java.nio.file.{Files, Paths}
import java.io.PrintWriter

class RedisConfigReaderSpec extends PlaySpec {

  "RedisConfigReader" should {

    "correctly extract password from config file" in {

      val tempFile = Files.createTempFile("redis", ".conf").toFile
      val pw = new PrintWriter(tempFile)
      pw.write("some-setting 12345\n")
      pw.write("requirepass mysupersecretpassword\n")
      pw.write("some-other-setting 67890\n")
      pw.close()

      val extractedPassword = RedisConfigReader.extractPassword(tempFile.getAbsolutePath)


      extractedPassword mustBe Some("mysupersecretpassword")

      tempFile.delete()
    }

    "return None if requirepass is not in the config file" in {

      val tempFile = Files.createTempFile("redis", ".conf").toFile
      val pw = new PrintWriter(tempFile)
      pw.write("some-setting 12345\n")
      pw.write("some-other-setting 67890\n")
      pw.close()

      val extractedPassword = RedisConfigReader.extractPassword(tempFile.getAbsolutePath)

      extractedPassword mustBe None

      tempFile.delete()
    }

  }
}
