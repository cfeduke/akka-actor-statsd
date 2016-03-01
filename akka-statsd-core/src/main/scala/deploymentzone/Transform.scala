package deploymentzone

import scala.util.matching.Regex


case class Transform(pattern: Regex, into: String)
