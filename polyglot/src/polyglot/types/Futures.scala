package polyglot.types

import funicular._

object Futures {
  class SettableFuture[T](var f: Future[T], var callable: polyglot.types.Ref.Callable[T]) extends Future[T] {
    def force = f.force
    def forced = f.forced
    def start = f.start
    def started = f.started
    def clocks = f.clocks
    def setCallable(c: polyglot.types.Ref.Callable[T]) = {
      callable = c
    }
  }
  def makeClocked[T](clock: Clock, c: polyglot.types.Ref.Callable[T]) = {
    new Exception("registering new future on " + clock).printStackTrace

    val f = new SettableFuture[T](null, c)
    f.f = delayedFuture[T](clock) {
      println("done making new future on " + clock)
      clock.next
      println("done blocking new future on " + clock)
      f.callable.call.asInstanceOf[T]
    }
    f.start
    f
  }

  def make[T](c: polyglot.types.Ref.Callable[T]) = {
    new Exception("making unclocked future").printStackTrace

    val f = new SettableFuture[T](null, c)
    f.f = delayedFuture[T] {
      f.callable.call.asInstanceOf[T]
    }
    f.start
    f
  }

  def finish(r: Runnable) = {
    println("starting finish " + r)
    funicular.finish(r.run)
    println("done with finish " + r)
  }

  def async(job: polyglot.frontend.Job) = {
    println("running job " + job)
    funicular.async {
      println("during running job " + job)
      polyglot.Main.runJob(job)
    }
  }

  def async(r: Runnable) = {
    println("running runnable " + r)
    funicular.async {
      println("during running runnable " + r)
      r.run
    }
  }
}
