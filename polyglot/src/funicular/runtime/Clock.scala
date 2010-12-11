package funicular.runtime

import funicular._
import funicular.ClockUseException

class Clock(name: String) extends funicular.Clock { self =>
  def this() = this("cluck")

  val debug = 0
  def dprintln(msg: => String) {
    if (debug > 0)
      println(msg)
  }
  def ddump(msg: => String) {
    if (debug > 1)
      new Exception(msg).printStackTrace
  }

  private val ph = new jsr166y.Phaser(0) {
    override def onAdvance(phase: Int, registered: Int) = {
      dprintln(self + " + advancing to " + phase + ", " + registered + " registered")
      false
    }
  }

  private val lock = new Lock
  private var registeredActivities = Set.empty[Activity]

  private def withExceptions[T](body: => T): T = {
    try {
      body
    }
    catch {
      case e: IllegalStateException => {
          e.printStackTrace
          throw new ClockUseException
        }
    }
  }
  
  def register: Unit = register(Runtime.activity)

  def register(a: Activity): Unit = {
    ddump("registering " + this + " with " + a)
    withExceptions {
      lock.withLock {
        if (! (registeredActivities contains a)) {
          ph.register
          registeredActivities += a
        }
      }
    }
  }

  // BUG: should return true if THIS activity is registered on the clock
  def registered = lock.withLock {
    registeredActivities contains Runtime.activity
  }

  def dropped = !registered

  def phase: Int = ph.getPhase

  def resume: Unit = {
    dprintln("resume " + this + " arrived=" + ph.getArrivedParties + "/" + ph.getRegisteredParties)
    ddump("resume " + this + " " + Runtime.activity)
    if (dropped)
      throw new ClockUseException
    withExceptions { ph.arrive }
    dprintln("done with resume " + this + " arrived=" + ph.getArrivedParties + "/" + ph.getRegisteredParties)
  }

  def next: Unit = {
    dprintln("next " + this + " arrived=" + ph.getArrivedParties + "/" + ph.getRegisteredParties)
    ddump("next " + this + " " + Runtime.activity)
    if (dropped)
      throw new ClockUseException
    withExceptions { ph.arriveAndAwaitAdvance }
    dprintln("done with next " + this + " arrived=" + ph.getArrivedParties + "/" + ph.getRegisteredParties)
    ddump("done with next " + this + " " + Runtime.activity)
  }

  def drop: Unit = {
    if (dropped)
      throw new ClockUseException
    doDrop
  }

  private def doDrop: Unit = {
    ddump("dropping " + this + " by " + Runtime.activity)
    withExceptions {
      lock.withLock {
        val a = Runtime.activity
        if (registeredActivities contains a) {
          ph.arriveAndDeregister
          registeredActivities -= a
        }
      }
    }
  }

  override def toString = "#" + name + "#arrived=" + ph.getArrivedParties + "/" + ph.getRegisteredParties
}
