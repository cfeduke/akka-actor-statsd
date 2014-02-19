package com.deploymentzone.actor.util

import com.deploymentzone.actor.CounterMessage
import com.deploymentzone.actor.validation.StatsDBucketValidator

/**
 * Transforms the toString result value of a CounterMessage instance to include an
 * optional namespace.
 */
private[actor] class NamespaceTransformer(val namespace: String) extends ((CounterMessage[_]) => String) {

  require(StatsDBucketValidator(namespace))

  override def apply(counter: CounterMessage[_]): String = {
    require(counter != null)

    val intermediate = counter.toString
    namespace match {
      case null => intermediate
      case "" => intermediate
      case _ => s"$namespace.$intermediate"
    }
  }
}

private[actor] object NamespaceTransformer {
  def apply(namespace: String) = new NamespaceTransformer(namespace)
}
