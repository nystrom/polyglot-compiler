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
    
    f.f = delayedFuture[T] {
      
//      clock.next
//      next
      f.callable.call.asInstanceOf[T]
    }
	
//    f.start
    f
    
//    b += f
    
//    b
  }

  def make[T](c: polyglot.types.Ref.Callable[T]) = {
    //new Exception("making unclocked future").printStackTrace

    val f = new SettableFuture[T](null, c)
    f.f = delayedFuture[T] {
      f.callable.call.asInstanceOf[T]
    }
//	println("makeUnclocked: starting unclocked future" + f.f.toString)
//    f.start
    f
  }

  def finish(r: Runnable) = {
    funicular.finish(r.run)
  }

  def async(job: polyglot.frontend.Job) = {

    funicular.finish {
    
    var node:polyglot.ast.Node = null
      var node1: polyglot.ast.Node = null
    funicular.finish {
    	node = polyglot.Main.runJob1(job);
    }
    
      funicular.finish {

        //funicular.async {
    val ast = node.asInstanceOf[polyglot.ast.Node]
        node1 = polyglot.Main.runJob(job, ast)
        //}
      }
      
      funicular.finish {
    	  val astFinal = node1.asInstanceOf[polyglot.ast.Node]
    	  polyglot.Main.runJobFinal(job, astFinal)
    
      }
    
    }
  }

  def async(r: Runnable) = {
    funicular.async {
      r.run
    }
  }
}
