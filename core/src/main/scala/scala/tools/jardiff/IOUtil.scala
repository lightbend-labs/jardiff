/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbenc.com>
 */

package scala.tools.jardiff

import java.io.IOException
import java.net.URI
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util

object IOUtil {
  def rootPath(fileOrZip: Path): Path = {
    if (fileOrZip.getFileName.toString.endsWith(".jar")) {
      val uri = URI.create("jar:file:" + fileOrZip.toUri.getPath)
      FileSystems.newFileSystem(uri, new util.HashMap[String, Any]()).getPath("/")
    } else fileOrZip
  }

  def mapRecursive(source: java.nio.file.Path, target: java.nio.file.Path)(f: (Path, Path) => Unit) = {
    Files.walkFileTree(source, util.EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new FileVisitor[Path] {
      def preVisitDirectory(dir: Path, sourceBasic: BasicFileAttributes): FileVisitResult = {
        val relative = source.relativize(dir).toString
        if (!Files.exists(target.resolve(relative)))
          Files.createDirectory(target.resolve(relative))
        FileVisitResult.CONTINUE
      }

      def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val relative = source.relativize(file).toString
        f(file, target.resolve(relative))
        FileVisitResult.CONTINUE
      }

      def visitFileFailed(file: Path, e: IOException) = throw e

      def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
        if (e != null) throw e
        FileVisitResult.CONTINUE
      }
    })
  }

  def deleteRecursive(p: Path): Unit = {
    import java.io.IOException
    import java.nio.file.attribute.BasicFileAttributes
    import java.nio.file.{FileVisitResult, Files, SimpleFileVisitor}
    Files.walkFileTree(p, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }



}
