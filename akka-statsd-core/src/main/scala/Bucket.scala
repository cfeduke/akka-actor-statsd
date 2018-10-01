package akka.statsd

import scala.util.matching.Regex

case class Transformation(regexS: String, into: String) {
  val regex = new Regex(regexS)
  def apply(s: String): String = regex.replaceAllIn(s, into)
}

case class Bucket private(
  path: Seq[String],
  transformations: Seq[Transformation],
  transformUuid: Boolean
) {
  private def transform(part: String) =
    Bucket.transform(part, transformations, transformUuid)

  def /(part: String) =
    new Bucket(path :+ transform(part), transformations, transformUuid)

  def prepend(part: String) =
    new Bucket(transform(part) +: path, transformations, transformUuid)

  def render =
    transform(path.mkString(Bucket.delimiter))
}

object Bucket {

  def apply(path: String, ts: Seq[Transformation] = Seq.empty, transformUuid: Boolean = true): Bucket =
    new Bucket(Seq(transform(path, ts, transformUuid)), ts, transformUuid)

  private val delimiter = "."
  private def defaults(transformUuid: Boolean) = {
    val reservedSymbols = Transformation("""[:|@\\]""", "_")
    val partDelimiters = Transformation("""[/]""", delimiter)
    lazy val uuid = {
      val hex = """[a-fA-F\d]"""
      Transformation(s"""$hex{8}-$hex{4}-$hex{4}-$hex{4}-$hex{12}""", "[id]")
    }

    Seq(reservedSymbols, partDelimiters) ++ (if (transformUuid) Seq(uuid) else Seq.empty)
  }

  protected[statsd] def transform(path: String, ts: Seq[Transformation], transformUuid: Boolean): String =
    withoutHangingSeparators(
      (ts ++ defaults(transformUuid)).foldLeft(path){case (s, t) => t(s)}
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
