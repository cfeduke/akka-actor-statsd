package com.deploymentzone.actor.util

/**
 * Validates that a provided name won't cause downstream problems with statsd; that is no reserved characters are
 * present in the name.
 *
 * Periods (".") in the name are okay as these are statsd folder name separators.
 *
 * The reserved characters are colon (":"), pipe ("|"), at-symbol ("@") and backslash ("\").
 */
object StatsDBucketValidator {
  val RESERVED_CHARACTERS = Seq(":", "|", "@", "\\").mkString("\"", "\", \"", "\"")
  private val RESERVED_CHARACTERS_PATTERN = """[:|@\\]"""
  private val reserved = RESERVED_CHARACTERS_PATTERN.r

  /**
   * Validates that a string contains no special characters.
   * @param name string to validate.
   */
  def apply(name: String): Boolean =
    name != null &&
    reserved.findFirstIn(name).fold(true)(_ => false) &&
    !name.startsWith(".") &&
    !name.endsWith(".")

}
