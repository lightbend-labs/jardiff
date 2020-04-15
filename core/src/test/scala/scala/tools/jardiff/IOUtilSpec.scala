package scala.tools.jardiff

import java.nio.file._
import java.util.zip.ZipOutputStream

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class IOUtilSpec extends AnyFlatSpec with Matchers {
  behavior of "IOUtil.rootPath"

  it should "handle jar path with spaces" in {
    val jar = Files.createTempDirectory("app support").resolve("best project.jar")
    Using.resource(new ZipOutputStream(Files.newOutputStream(jar)))(_.closeEntry()) // create jar
    IOUtil.rootPath(jar).toString shouldBe "/"
    IOUtil.rootPath(jar.resolve("foo/Bar.class")).toString shouldBe "/foo/Bar.class"
  }
}
