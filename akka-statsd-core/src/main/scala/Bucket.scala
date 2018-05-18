package akka.statsd

import scala.util.matching.Regex

case class Transformation(regexS: String, into: String) {
  val regex = new Regex(regexS)
  def apply(s: String): String = regex.replaceAllIn(s, into)
}

case class Bucket private(
  path: Seq[String],
  transformations: Seq[Transformation]
) {
  private def transform(part: String) =
    Bucket.transform(part, transformations)

  def /(part: String) =
    new Bucket(path :+ transform(part), transformations)

  def prepend(part: String) =
    new Bucket(transform(part) +: path, transformations)

  def render =
    transform(path.mkString(Bucket.delimiter))
}

object Bucket {

  def apply(path: String, ts: Seq[Transformation] = Seq.empty): Bucket =
    new Bucket(Seq(transform(path, ts)), ts)


  private val delimiter = "."
  private val defaults = {
    val reservedSymbols = Transformation("""[:|@\\]""", "_")
    val partDelimiters = Transformation("""[/]""", delimiter)
    val uuid = {
      val hex = """[a-fA-F\d]"""
      Transformation(s"""$hex{8}-$hex{4}-$hex{4}-$hex{4}-$hex{12}""", "[id]")
    }

    Seq(reservedSymbols, partDelimiters, uuid)
  }

  protected[statsd] def transform(path: String, ts: Seq[Transformation]): String =
    withoutHangingSeparators(
      (ts ++ defaults).foldLeft(path){case (s, t) => t(s)}
    )

  @annotation.tailrec
  protected[statsd] def withoutHangingSeparators(s: String): String = {
    if (s.startsWith("."))
      withoutHangingSeparators(s.substring(1))
    else if (s.endsWith("."))
      withoutHangingSeparators(s.substring(0, s.length - 1))
    else
      s
  }

}
