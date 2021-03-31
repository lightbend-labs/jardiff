package scala.tools.jardiff

package sbtjardiff

import sbt._
import Keys._
import sbt.librarymanagement.ModuleFilter

object SbtJardiff extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport {
    val jardiff =
      taskKey[Boolean]("run jardiff, returning true if there is a difference")
    val jardiffGetPreviousVersion =
      taskKey[File]("Resolves and downloads the version set in jardiffPreviousVersionCoordinates")

    val jardiffSettings = taskKey[JarDiff.Config]("jardiff settings")
    val jardiffLeaveRepoAt = settingKey[Option[String]](
      "Directory to output a git repository containing the diff"
    )
    val jardiffMethodBodies = settingKey[Boolean](
      "Whether or not method bodies should be included in the output"
    )
    val jardiffRaw = settingKey[Boolean]("unsorted and unfiltered")
    val jardiffIncludePrivates = settingKey[Boolean]("include private members")
    val jardiffUnifiedDiffContext = settingKey[Option[Int]](
      "Create a unified diff with this number of context lines"
    )
    val jardiffOutputStream =
      taskKey[java.io.OutputStream]("Stream to write the diff to. Defaults to stdout")
    val jardiffIgnore = settingKey[List[String]](
      "list of file patterns to ignore, using .gitignore syntax"
    )
    val jardiffReferenceVersionCoordinates =
      settingKey[ModuleID]("reference to jardiff against, in libraryDependencies format")

    val jardiffReferenceVersion = settingKey[String](
      "version number of version to compare against, resolved with current organization and name." +
        " If those changed, set jardiffReferenceVersionCoordinates instead, and leave this unset"
    )
  }

  import autoImport._
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    jardiffMethodBodies := true,
    jardiffLeaveRepoAt := None,
    jardiffRaw := false,
    jardiffIncludePrivates := true,
    jardiffOutputStream := System.out,
    jardiffUnifiedDiffContext := None,
    jardiffIgnore := Nil
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    jardiffSettings := {
      val repo = jardiffLeaveRepoAt.value.map(java.nio.file.Paths.get(_))
      val code = jardiffMethodBodies.value
      val raw = jardiffRaw.value
      val privates = jardiffIncludePrivates.value
      val contextLines = jardiffUnifiedDiffContext.value
      val output = jardiffOutputStream.value
      val ignore = jardiffIgnore.value
      JarDiff.Config(repo, code, raw, privates, contextLines, output, ignore)
    },
    jardiffGetPreviousVersion := {
      //Adapted from a demonstration by Anton Sviridov
      val logger = streams.value.log
      val scalaV = scalaBinaryVersion.value
      val moduleId = jardiffReferenceVersionCoordinates.?.value
        .orElse {
          jardiffReferenceVersion.?.value.map(v => organization.value %% name.value % v)
        }
        .getOrElse {
          logger.error(
            "sbt-jardiff: set jardiffReferenceVersion to the version to compare to"
          )
          throw new Exception("failed to resolve jardiff previous version")
        }
      val resolver = dependencyResolution.in(update).value
      val updateConfiguration_ = updateConfiguration.in(update).value
      val unresolvedWarningConfiguration_ = unresolvedWarningConfiguration.in(update).value
      val descriptor = resolver.wrapDependencyInModule(moduleId)

      val mfilter: ModuleFilter = module => {
        val orgResult = module.organization == moduleId.organization
        val nameResult = module.name == moduleId.name
        val nameResultAlt = module.name == s"${moduleId.name}_$scalaV"
        val revisionResult = module.revision == moduleId.revision
        val result = orgResult && (nameResult || nameResultAlt) && revisionResult

        if (result)
          logger.debug(
            s"module $module matched search for $moduleId, matched by matching name ${module.name} against ${moduleId.name}"
          )
        else {
          logger.debug(s"module $module is not our dreamed module $moduleId")
          if (!orgResult)
            logger.debug(
              s"the organization doesn't match: ${module.organization} vs ${moduleId.organization}"
            )
          else if (!nameResult)
            logger.debug(
              s"the name doesn't match: ${module.name} vs ${moduleId.name} or ${moduleId.name}_$scalaV"
            )
          else if (!revisionResult)
            logger.debug(s"the revision doesn't match:  ${module.revision} vs ${moduleId.revision}")
        }
        result
      }

      resolver
        .update(descriptor, updateConfiguration_, unresolvedWarningConfiguration_, logger)
        .fold(
          uw => throw uw.resolveException,
          x =>
            x.select(mfilter) match {
              case Vector(head) => head
              case Vector(head, tail @ _*) => {
                logger.warn(
                  s"sbt-jardiff expected single file for moduleId $moduleId, but got more than that. Arbitrarily selecting $head and discarding ${tail
                    .mkString(", ")}"
                )
                head
              }
              case v => {
                logger.error(s"no file found for module $moduleId. This is a bug in sbt-jardiff")
                v.head
              }
            }
        )
    },
    jardiff := {
      //register the call to value on compile to make it a dependency of this
      //task so it gets executed and completes before this task gets executed
      //TODO: figure out what to do when compilation fails and how to
      //how to communicate that back upstream
      val _ = (Compile / compile).value

      //we assume the previous version can be compared to whatever
      //is in the classDirectory directory after compilation.
      //It's a somewhat daring assumption that underlines our swashbuckling
      //and freebooting disposition.

      //As a mild justification: this is the default location and we're just
      //going to have to hope that the project using this plugin don't change
      //that. It's unlikely that they'll have: in order to do that, they would
      //have had to modified one of the upstreams of classDirectory:
      //productDirectories, bloopGenerate, compileIncremental,
      //manipulateBytecode or compile itself.
      //Modifying these is to the best of my knowledge rare enough that assuming
      //they're unchanged. If they are changed, this will break, but the user
      //will probably know they are doing something unconventional.

      //That there aren't further steps that happen before the artifact ends up
      //is harder to justify, which we do only through furious handwaving and
      //victim blaming if it doesn't work, to hide my own incompetence in
      //figuring out where to get the exact compilation artifacts for some
      //project.
      val classDir = (Compile / classDirectory).value
      val currentPath = JarDiff.expandClassPath(classDir.getAbsolutePath())

      val previousJar = jardiffGetPreviousVersion.value
      val previousPath = JarDiff.expandClassPath(previousJar.getAbsolutePath())

      val config = jardiffSettings.value
      val differ = JarDiff.apply(List(previousPath, currentPath), config)

      differ.diff()
    }
  )
}
