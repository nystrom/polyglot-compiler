package polyglot.types

import funicular._
import funicular.runtime.Lock

class ClockedFuture[A](clock: Clock, future: Future[A]) extends Future[A] {
  def forced = deadLock.withLock { isDead || future.forced }

  def force = {
    deadLock.withLock {
      if (isDead)
        throw new RuntimeException("the future is dead")
      else {
        deadLock.withoutLock {
          clock.next
        }
        if (isDead)
          throw new RuntimeException("the future is dead")
      }
    }
    future.force
  }

  def start: Unit = future.start
  def started: Boolean = future.started

  private var isDead = false
  private val deadLock = new Lock

  def clocks = Nil

  def cancel = deadLock.withLock { isDead = true }
}