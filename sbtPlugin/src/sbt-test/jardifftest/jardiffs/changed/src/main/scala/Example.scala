package org.example

object Example {
  def member = "public has method body"
  private[this] def privatethis = "privatethis has method body"
  private[this] val privatefield: Int = 7
}

class OtherExample {
  def othermember = "other public has method body"
}