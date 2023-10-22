import org.eclipse.jgit.api.Git
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.LoggerFactory

final class JDK8SmokeTest extends AnyFlatSpec with Matchers {
  behavior of "Testing dependencies are compiled with JDK 1.8"

  it should "make sure slf4j/logback works" in {
    LoggerFactory.getLogger(getClass.getName)
  }

  it should "make sure JGit works" in {
    Git.init
  }
}
