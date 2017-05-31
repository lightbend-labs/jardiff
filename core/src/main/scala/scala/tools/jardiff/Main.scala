/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbenc.com>
 */

package scala.tools.jardiff

import java.io.{ByteArrayOutputStream, PrintWriter}
import java.nio.file._

import org.apache.commons.cli
import org.apache.commons.cli.{CommandLine, DefaultParser, HelpFormatter, Options}

import scala.util.Try
import scala.util.control.NonFatal

object Main {
  def main(args: Array[String]): Unit = {
    run(args) match {
      case ShowUsage(msg) => System.err.println(msg); sys.exit(-1)
      case Error(err) => err.printStackTrace(System.err); sys.exit(-1)
      case Success =>
    }
  }

  private object Opts {
    val Help = new cli.Option("h", "help", false, "Display this message")
    val Git = new cli.Option("g", "git", true, "Directory to output a git repository containing the diff")
    Git.setArgName("dir")
    val NoCode = new cli.Option("c", "suppress-code", false, "Suppress method bodies")
    val ContextLines = new cli.Option("U", "--unified", true, "Suppress method bodies")
    ContextLines.setArgName("n")
    def apply(): Options = {
      new cli.Options().addOption(Help).addOption(Git).addOption(ContextLines).addOption(NoCode)
    }
  }
  private implicit class RichCommandLine(val self: CommandLine) {
    def has(o: cli.Option): Boolean = self.hasOption(o.getOpt)
    def get(o: cli.Option): String = self.getOptionValue(o.getOpt)
    def getOptInt(o: cli.Option): Option[Int] = Option(self.getOptionValue(o.getOpt)).map(x => Try(x.toInt).getOrElse(throw new cli.ParseException("--" + o.getLongOpt + " requires an integer")))
  }

  private def helpText: String = {
    val formatter = new HelpFormatter
    val baos = new ByteArrayOutputStream()
    val writer = new PrintWriter(baos)
    try {
      formatter.printHelp(writer, 80, "jardiff", "", Opts(), HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, "", true)
      writer.flush()
      baos.toString().replaceFirst("\n", " <jar/directory/url> <jar/directory/url>\n")
    } finally {
      writer.close()
    }
  }

  def run(args: Array[String]): RunResult = {
    val parser = new DefaultParser

    try {
      val line = parser.parse(Opts(), args)
      val trailingArgs = line.getArgList
      if (line.has(Opts.Help) || trailingArgs.size() != 2) {
        System.out.println(helpText)
        ShowUsage(helpText)
      } else {
        val gitRepo = if (line.has(Opts.Git)) Some(Paths.get(line.get(Opts.Git))) else None
        val config = JarDiff.Config(gitRepo, !line.has(Opts.NoCode), line.getOptInt(Opts.ContextLines), System.out)
        val jarDiff = JarDiff(Paths.get(trailingArgs.get(0)), Paths.get(trailingArgs.get(1)), config)
        jarDiff.diff()
        Success
      }
    } catch {
      case exp: cli.ParseException => ShowUsage(helpText)
      case NonFatal(t) => Error(t)
    }
  }
}

sealed abstract class RunResult
case class ShowUsage(msg: String) extends RunResult
case class Error(err: Throwable) extends RunResult
object Success extends RunResult
