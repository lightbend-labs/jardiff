package scala.tools.jardiff

import java.nio.file._
import java.util.zip.ZipOutputStream

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class IOUtilSpec extends AnyFlatSpec with Matchers {
  behavior of "IOUtil.rootPath"

  it should "handle jar path with spaces" in {
    val jar = Files.createTempDirectory("app support").resolve("best project.jar")
    val stream = new ZipOutputStream(Files.newOutputStream(jar))
    try {
      IOUtil.rootPath(jar).toString shouldBe "/"
      IOUtil.rootPath(jar.resolve("foo/Bar.class")).toString shouldBe "/foo/Bar.class"
    }
    finally {
      stream.closeEntry()
    }
  }
}
