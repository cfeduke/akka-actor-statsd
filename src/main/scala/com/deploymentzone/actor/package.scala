package com.deploymentzone

package object actor {

  /**
   * Packet sizes for limitations in batching messages.
   *
   * (These payload numbers take into account the maximum IP + UDP header sizes.)
   */
  /* derived from https://github.com/etsy/statsd/blob/master/docs/metric_types.md */
  object PacketSize {
    val FAST_ETHERNET = 1432
    val GIGABIT_ETHERNET = 8932
    val COMMODITY_INTERNET = 512
  }
}
