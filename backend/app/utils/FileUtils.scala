package utils

import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.FileVisitResult

object FileUtils {
  def deleteDir(pathToDir: Path): Unit = {
   if (Files.exists(pathToDir)) {
      Files.walkFileTree(
        pathToDir,
        new SimpleFileVisitor[Path] {
          override def visitFile(
              file: Path,
              attrs: BasicFileAttributes
          ): FileVisitResult = {
            Files.delete(file)
            FileVisitResult.CONTINUE
          }

          override def postVisitDirectory(
              dir: Path,
              exc: java.io.IOException
          ): FileVisitResult = {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          }
        }
      )
    }
  }
}
