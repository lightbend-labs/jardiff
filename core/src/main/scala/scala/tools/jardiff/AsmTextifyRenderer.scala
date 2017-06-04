/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbenc.com>
 */

package scala.tools.jardiff

import java.io.PrintWriter
import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._
import scala.tools.asm.ClassReader
import scala.tools.asm.tree.ClassNode
import scala.tools.asm.util.TraceClassVisitor

class AsmTextifyRenderer(code: Boolean) extends FileRenderer {
  def outFileExtension: String = ".asm"
  override def render(in: Path, out: Path): Unit = {
    val classBytes = Files.readAllBytes(in)
    val node = zapScalaClassAttrs(sortClassMembers(classFromBytes(classBytes)))
    if (!code)
      node.methods.forEach(_.instructions.clear())
    Files.createDirectories(out.getParent)
    val pw = new PrintWriter(Files.newBufferedWriter(out))
    try {
      val trace = new TraceClassVisitor(pw)
      node.accept(trace)
    } finally {
      pw.close()
    }
  }

  def sortClassMembers(node: ClassNode): node.type = {
    node.fields.sort(_.name compareTo _.name)
    node.methods.sort(_.name compareTo _.name)
    node
  }

  private def isScalaSigAnnot(desc: String) =
    List("Lscala/reflect/ScalaSignature", "Lscala/reflect/ScalaLongSignature").exists(desc.contains)

  // drop ScalaSig annotation and class attributes
  private def zapScalaClassAttrs(node: ClassNode): node.type = {
    if (node.visibleAnnotations != null)
      node.visibleAnnotations = node.visibleAnnotations.asScala.filterNot(a => a == null || isScalaSigAnnot(a.desc)).asJava

    node.attrs = null
    node
  }

  private def classFromBytes(bytes: Array[Byte]): ClassNode = {
    val node = new ClassNode()
    new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)

    node
  }

}
