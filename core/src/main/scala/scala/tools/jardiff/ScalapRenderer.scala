/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scala.tools.jardiff

import java.nio.file.{Files, Path}

import scala.tools.scalap.scalax.rules.ScalaSigParserError

class ScalapRenderer(privates: Boolean) extends FileRenderer {
  def outFileExtension: String = ".scalap"
  override def render(in: Path, out: Path): Unit = {
    val classBytes = Files.readAllBytes(in)
    try {
      val main = new scala.tools.scalap.Main
      main.printPrivates = privates
      val decompiled = main.decompileScala(classBytes, in.getFileName.toString == "package.class")
      if (decompiled != "") {
        Files.createDirectories(out.getParent)
        Files.write(out, decompiled.getBytes("UTF-8"))
      }
    } catch {
      case err: ScalaSigParserError =>
        System.err.println("WARN: unable to invoke scalap on: " + in + ": " + err.getMessage)
    }
  }
}
