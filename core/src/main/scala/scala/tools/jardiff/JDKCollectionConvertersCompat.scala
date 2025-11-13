/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package scala.tools.jardiff

import scala.annotation.nowarn

/** Magic to get cross-compiling access to `scala.jdk.CollectionConverters`
 *  with a fallback on `scala.collection.JavaConverters`, without deprecation
 *  warning in any Scala version.
 *
 *  @see https://github.com/scala/scala-collection-compat/issues/208
 */
@nowarn("msg=Unused import")
private[jardiff] object JDKCollectionConvertersCompat {
  object Scope1 {
    object jdk {
      type CollectionConverters = Int
    }
  }
  import Scope1._

  object Scope2 {
    import scala.collection.{JavaConverters => CollectionConverters}

    object Inner {
      import scala._
      import jdk.CollectionConverters
      val Converters = CollectionConverters
    }
  }

  val Converters = Scope2.Inner.Converters
}
