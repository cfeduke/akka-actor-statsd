package deploymentzone.actor.validation

/**
 * Validates that a provided name won't cause downstream problems with statsd;
 * that is no reserved characters are present in the name.
 *
 * Periods (".") in the name are okay as these are statsd folder name separators.
 * Periods at the end or start of a name are not permitted.
 *
 * The reserved characters are colon (":"), pipe ("|"), at-symbol ("@") and backslash ("\").
 */
private[actor] object StatsDBucketValidator {
  val ReservedCharacters = Seq(":", "|", "@", "\\").mkString("\"", "\", \"", "\"")
  private val ReservedCharactersPattern = """[:|@\\]"""
  private val reserved = ReservedCharactersPattern.r

  /**
   * Validates that a string contains no special characters.
   */
  def apply(name: String): Boolean =
    name.isEmpty ||
    (reserved.findFirstIn(name).isEmpty &&
    !name.startsWith(".") &&
    !name.endsWith("."))
}
