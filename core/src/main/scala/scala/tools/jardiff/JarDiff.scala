/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scala.tools.jardiff

import java.io.{File, OutputStream}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.revwalk.RevCommit

import scala.tools.jardiff.JGitUtil._
import scala.collection.JavaConverters._

final class JarDiff(files: List[List[Path]], config: JarDiff.Config, renderers: String => List[FileRenderer]) {
  private val targetBase = config.gitRepo.getOrElse(Files.createTempDirectory("jardiff-"))

  def diff(): Boolean = {
    var differenceFound = false
    import org.eclipse.jgit.api.Git
    val git: Git = Git.init.setDirectory(targetBase.toFile).call()
    try {
      val excluded = targetBase.resolve(".git").resolve("info").resolve("exclude")
      Files.createDirectories(excluded.getParent)
      Files.write(excluded, config.ignore.asJava)

      def renderAndCommit(fs: List[Path]): RevCommit = {
        IOUtil.deleteRecursive(targetBase)

        for (f <- fs) {
          val root = IOUtil.rootPath(f)
          if (Files.isDirectory(root))
            renderFiles(root)
          else
            renderFile(root, targetBase.resolve(f.getFileName))
        }
        val status = git.status().call()
        val ignored = status.getIgnoredNotInIndex
        ignored.forEach(p => IOUtil.deleteRecursive(targetBase.resolve(p)))
        val msg = s"jardiff textified output of: ${fs.mkString(File.pathSeparator)}"
        git.add().addFilepattern(".").call()
        git.commit().setAllowEmpty(true).setAll(true).setMessage(msg).setSign(false).call()
      }

      files match {
        case head :: Nil =>
          val commit = renderAndCommit(head)
          printInitialDiff(git, commit)
        case _ =>
          val commits = files.iterator.map(renderAndCommit)
          commits.sliding(2).foreach {
            case Seq(commit1, commit2) =>
              differenceFound ||= printDiff(git, commit1, commit2)
          }
      }
    } finally {
      git.getRepository.close()
      RepositoryCache.clear()
      if (config.gitRepo.isEmpty)
        IOUtil.deleteRecursive(targetBase)
    }

    differenceFound
  }

  private def printDiff(git: Git, commit1: RevCommit, commit2: RevCommit): Boolean = {
    val cmd = git.diff()
    val diffFormatter = new DiffFormatter(config.diffOutputStream)
    config.contextLines.foreach{lines => cmd.setContextLines(lines); diffFormatter.setContext(lines)}
    cmd.setOldTree(getCanonicalTreeParser(git, commit1))
    cmd.setNewTree(getCanonicalTreeParser(git, commit2))
    val diffEntries = cmd.call()
    diffFormatter.setRepository(git.getRepository)
    diffFormatter.format(diffEntries)
    diffEntries.size() > 0
  }
  private def printInitialDiff(git: Git, initialCommit: RevCommit): Unit = {
    class PrintingWalker extends SimpleFileVisitor[Path] {

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
        if (dir.getFileName.toString == ".git") FileVisitResult.SKIP_SUBTREE else super.preVisitDirectory(dir, attrs)

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        config.diffOutputStream.write(("\n+++ " + targetBase.relativize(file).toString + "\n").getBytes)
        config.diffOutputStream.write(Files.readAllBytes(file))
        super.visitFile(file, attrs)
      }
    }

    Files.walkFileTree(targetBase, new PrintingWalker)
  }

  private def renderFiles(sourceBase: java.nio.file.Path) = {
    IOUtil.mapRecursive(sourceBase, targetBase)(renderFile)
  }

  private def renderFile(sourceFile: Path, targetFile: Path) = {
    val ix = sourceFile.getFileName.toString.lastIndexOf(".")
    val extension = if (ix >= 0) sourceFile.getFileName.toString.substring(ix + 1) else ""
    if (!Files.isSymbolicLink(sourceFile)) {
      for (renderer <- renderers(extension)) {
        val outPath = targetFile.resolveSibling(targetFile.getFileName + renderer.outFileExtension)
        renderer.render(sourceFile, outPath)
      }
    }
  }
}

object JarDiff {
  def expandClassPath(f: String) = {
    val path = Paths.get(f)
    if (Files.exists(path)) List(path)
    else if (f.indexOf(java.io.File.pathSeparatorChar) != -1)
      f.split(java.io.File.pathSeparatorChar).toList.map(s => Paths.get(s))
    else List(path)
  }
  def apply(files: List[List[Path]], config: JarDiff.Config): JarDiff = {
    val renderers = Map(
      "class" -> List(new AsmTextifyRenderer(config.code, config.raw, config.privates), new ScalapRenderer(config.privates)),
      "sig" -> List(new ScalapSigRenderer(config.privates))
    ).withDefault(_ => List(IdentityRenderer))
    new JarDiff(files, config, renderers)
  }

  case class Config(gitRepo: Option[Path], code: Boolean, raw: Boolean, privates: Boolean, contextLines: Option[Int], diffOutputStream: OutputStream, ignore: List[String])

}
