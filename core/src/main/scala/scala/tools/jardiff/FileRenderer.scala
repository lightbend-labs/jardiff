/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbenc.com>
 */

package scala.tools.jardiff

import java.nio.file.{Files, Path}

trait FileRenderer {
  def outFileExtension: String

  def render(in: Path, out: Path): Unit
}

object IdentityRenderer extends FileRenderer {
  def outFileExtension: String = ""

  override def render(in: Path, out: Path): Unit = {
    Files.copy(in, out)
  }
}
