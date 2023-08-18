package utils

import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import scala.util.control.NonFatal
import play.api.Logging
import java.io.IOException

object FileUtils extends Logging {

  def deleteDir(directory: Path): Unit = {
    try {
      if (Files.exists(directory)) {
        Files.walkFileTree(directory, new SimpleFileVisitor[Path] {
          override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            tryDelete(file)
            FileVisitResult.CONTINUE
          }
          override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            tryDelete(dir)
            FileVisitResult.CONTINUE
          }
        })
      }
    } catch {
      case NonFatal(ex) =>
        logger.error(s"Failed to delete $directory", ex)
    }
  }

  private def tryDelete(path: Path): Unit = {
    var deleted = false
    var retries = 0
    while (!deleted && retries < 5) {
      try {
        Files.delete(path)
        deleted = true
      } catch {
        case _: DirectoryNotEmptyException =>
          retries += 1
          Thread.sleep(100)
        case NonFatal(ex) =>
          throw ex
      }
    }
  }
}
