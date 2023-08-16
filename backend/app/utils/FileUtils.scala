package utils

import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult
import java.nio.file.DirectoryNotEmptyException
import scala.util.control.NonFatal

object FileUtils {
  // def deleteDir(pathToDir: Path): Unit = {
  //   deleteDirectory(pathToDir)
  // }

  def deleteDir(directory: Path): Unit = {
    try {
      if (Files.isDirectory(directory)) {
        val stream: java.util.stream.Stream[Path] = Files.list(directory)
        try {
          stream.forEach(deleteDir)
        } finally {
          stream.close()
        }
      }

      var deleted = false
      var retries = 0
      while (!deleted && retries < 5) {
        try {
          Files.delete(directory)
          deleted = true
        } catch {
          case _: DirectoryNotEmptyException =>
            retries += 1
            Thread.sleep(100)
          case NonFatal(ex) =>
            throw ex
        }
      }
    } catch {
      case ex: Throwable =>
        println(s"Failed to delete $directory: $ex")
    }
  }
}
