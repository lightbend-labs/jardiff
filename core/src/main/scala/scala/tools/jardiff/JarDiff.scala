/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbenc.com>
 */

package scala.tools.jardiff

import java.io.OutputStream
import java.nio.file._

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevCommit

import scala.tools.jardiff.JGitUtil._

final class JarDiff(file1: Path, file2: Path, config: JarDiff.Config, renderers: Map[String, FileRenderer]) {
  private val targetBase = config.gitRepo.getOrElse(Files.createTempDirectory("jardiff-"))

  def diff(): Unit = {
    import org.eclipse.jgit.api.Git
    val git: Git =
      Git.init.setDirectory(targetBase.toFile).call

    def renderAndCommit(f: Path): RevCommit = {
      git.rm().setCached(true).addFilepattern(".")
      renderFiles(IOUtil.rootPath(f))
      git.add().addFilepattern(".").call()
      git.commit().setMessage("jardiff textified output of: " + f).call()
    }

    val commit1 = renderAndCommit(file1)
    val commit2 = renderAndCommit(file2)
    printDiff(git, commit1, commit2)
    if (config.gitRepo.isEmpty)
      IOUtil.deleteRecursive(targetBase)
  }

  private def printDiff(git: Git, commit1: RevCommit, commit2: RevCommit): Unit = {
    val cmd = git.diff()
    val diffFormatter = new DiffFormatter(config.diffOutputStream)
    config.contextLines.foreach{lines => cmd.setContextLines(lines); diffFormatter.setContext(lines)}
    cmd.setOldTree(getCanonicalTreeParser(git, commit1))
    cmd.setNewTree(getCanonicalTreeParser(git, commit2))
    val diffEntries = cmd.call()
    diffFormatter.setRepository(git.getRepository)
    diffFormatter.format(diffEntries)
  }

  private def renderFiles(sourceBase: java.nio.file.Path) = {
    IOUtil.mapRecursive(sourceBase, targetBase) {
      (sourceFile, targetFile) =>
        val ix = sourceFile.getFileName.toString.lastIndexOf(".")
        val extension = if (ix >= 0) sourceFile.getFileName.toString.substring(ix + 1) else ""
        renderers.get(extension) match {
          case Some(renderer) =>
            val outPath = targetFile.resolveSibling(targetFile.getFileName + renderer.outFileExtension)
            renderer.render(sourceFile, outPath)
          case None =>
        }
    }
  }
}

object JarDiff {
  def apply(file1: Path, file2: Path, config: JarDiff.Config) = {
    val renderers = Map("class" -> new AsmTextifyRenderer(config.code)).withDefault(_ => IdentityRenderer)
    new JarDiff(file1, file2, config, renderers)
  }

  case class Config(gitRepo: Option[Path], code: Boolean, contextLines: Option[Int], diffOutputStream: OutputStream)

}
