package com.deploymentzone.actor.domain

import scala.collection.immutable
import com.deploymentzone.actor.PacketSize
import scala.annotation.tailrec
import java.nio.charset.Charset

/**
 * Logic for combining messages so they won't cross a predetermined packet size boundary.
 *
 * Takes UTF-8 byte size into account for messages.
 *
 * When a single message is larger than the provided [[packetSize]] that message is dropped
 * with a log warning.
 *
 * This class is not thread-safe.
 *
 * @param packetSize maximum byte size that is permitted for any given payload
 */
private[actor] class MultiMetricQueue(val packetSize: Int) {
  var queue = immutable.Queue[String]()

  /**
   * Enqueues a message for future dispatch.
   * @param message message to enqueue
   * @return this instance (for convenience chaining)
   */
  def enqueue(message: String) = {
    queue = queue.enqueue(message)
    this
  }

  /**
   * Creates a StatsD payload message from a list of messages up to the [[packetSize]] limit
   * in bytes taking UTF-8 size into account.
   *
   * Items that are added to the payload are also removed from the queue.
   *
   * @return Newline separated list of StatsD messages up to the maximum [[packetSize]]
   */
  def payload(): String = {
    @tailrec
    def recurse(acc: StringBuilder = new StringBuilder, utf8Length: Int = 0): String = {
      val utf8 = Charset.forName("utf-8")
      queue.isEmpty match {
        case true => acc.toString()
        case false =>
          val proposedAddition = queue.head.getBytes(utf8).length
          if (proposedAddition + utf8Length + 1 > (packetSize + 1))
            acc.toString()
          else {
            val (item, sq) = queue.dequeue
            queue = sq
            acc.append(item)
            acc.append("\n")
            recurse(acc, proposedAddition + utf8Length)
          }
      }
    }
    recurse().stripLineEnd
  }
}

object MultiMetricQueue {
  /**
   * Creates an instance of MultiMetricQueue.
   *
   * @param packetSize maximum packet size for a single aggregated message
   */
  def apply(packetSize: Int = PacketSize.GIGABIT_ETHERNET) = new MultiMetricQueue(packetSize)
}
