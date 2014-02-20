package com.deploymentzone.actor.domain

import scala.collection.immutable
import com.deploymentzone.actor.PacketSize
import scala.annotation.tailrec
import java.nio.charset.Charset
import akka.event.Logging
import akka.actor.ActorSystem

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
private[actor] class MultiMetricQueue(val packetSize: Int)(implicit system: ActorSystem) {
  val logger = Logging(system.eventStream, classOf[MultiMetricQueue])

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
   * The remaining number of messages in the queue.
   *
   * Primarily provided for instrumentation and testing.
   *
   * @return number of messages remaining in the queue.
   */
  def size: Int = {
    queue.size
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
          if (proposedAddition > packetSize) {
            if (logger.isWarningEnabled) {
              val DISCARD_MSG_MAX_LENGTH = 25
              val discardMsgLength = queue.head.length
              val ellipsis = discardMsgLength match {
                case n if n > DISCARD_MSG_MAX_LENGTH => "..."
                case _ => ""
              }
              val discardMsg = queue.head.substring(0, Math.min(DISCARD_MSG_MAX_LENGTH, discardMsgLength)) + ellipsis
              logger.warning(s"""Message "$discardMsg" discarded because its size ($discardMsgLength) was larger than the permitted maximum packet size ($packetSize)""")
            }
            val (_, sq) = queue.dequeue
            queue = sq
            recurse(acc, utf8Length)
          } else if (proposedAddition + utf8Length + 1 > (packetSize + 1)) {
            acc.toString()
          } else {
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
  def apply(packetSize: Int = PacketSize.GIGABIT_ETHERNET)(implicit system: ActorSystem) =
    new MultiMetricQueue(packetSize)(system)
}
