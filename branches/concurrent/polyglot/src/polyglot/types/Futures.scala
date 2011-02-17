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
   
  
  def makeClocked[T](clock: Clock, c: polyglot.types.Ref.Callable[T]) = { //,  = {
    //new Exception("registering new future on " + clock).printStackTrace
	var b : Set[SettableFuture[T]] = Set[SettableFuture[T]]()
    val f = new SettableFuture[T](null, c)
    //println("makeClocked: made new future on "  + clock  + " future: " + f.f.toString)
    
    f.f = delayedFuture[T] {
      println("makeClocked: done blocking future " + f.f.toString + " on " + clock)
      println("makeClocked: before calling next " )
      
//      clock.next
//      next
      f.callable.call.asInstanceOf[T]
    }
	
//	println("makeClocked: before starting future on " + clock + " f: " + f.f.toString)
//    f.start
    f
    
    
//    b += f
    
//    b
  }

  def make[T](c: polyglot.types.Ref.Callable[T]) = {
    //new Exception("making unclocked future").printStackTrace
	 println("makeUnclocked: before making unclocked future")

    val f = new SettableFuture[T](null, c)
    f.f = delayedFuture[T] {
      f.callable.call.asInstanceOf[T]
    }
//	println("makeUnclocked: starting unclocked future" + f.f.toString)
//    f.start
    f
  }

  def finish(r: Runnable) = {
    println("starting finish " + r)
    funicular.finish(r.run)
    println("done with finish " + r)
  }

  def async(job: polyglot.frontend.Job) = {
    println("running job " + job)
    
    var node:polyglot.ast.Node = null
    funicular.finish {
    	node = polyglot.Main.runJob1(job);
    }
    
    val ast = node.asInstanceOf[polyglot.ast.Node]
    
    
    funicular.async {
      println("during running job " + job + "I am: " + this)
     polyglot.Main.runJob(job, ast)
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
