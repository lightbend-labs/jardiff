/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scala.tools.jardiff

import java.nio.file.{Files, Path, StandardCopyOption}

trait FileRenderer {
  def outFileExtension: String

  def render(in: Path, out: Path): Unit
}

object IdentityRenderer extends FileRenderer {
  def outFileExtension: String = ""

  override def render(in: Path, out: Path): Unit = {
    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING)
  }
}
