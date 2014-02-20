package com.deploymentzone.actor.domain

import com.deploymentzone.actor.Metric
import com.deploymentzone.actor.validation.StatsDBucketValidator

/**
 * Transforms the toString result value of a CounterMessage instance to include an
 * optional namespace.
 */
private[actor] class NamespaceTransformer(val namespace: String) extends ((Metric[_]) => String) {

  require(StatsDBucketValidator(namespace))

  override def apply(counter: Metric[_]): String = {
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
