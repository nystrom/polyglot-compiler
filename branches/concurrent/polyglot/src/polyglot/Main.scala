package polyglot

import funicular._

object Main {
  import polyglot.types.QName
  import polyglot.types.Name
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.Queue

  import polyglot.types.Named
  import polyglot.frontend.Job
  import polyglot.frontend.Source
  import polyglot.ast.Node
  import polyglot.frontend.Globals

  val globalClock = Clock("Global clock")
  
  def runJob(job: Job): Boolean = {
    globalClock.register
    
    val commandLineJob = Globals.Scheduler.commandLineJobs contains job
    if (!commandLineJob) {
      globalClock.drop
    }

    // Basic pattern:
    // All futures created in a phase are registered on jobClock.
    // All futures wait on the clock before doing any computation.
    // At the end of the phase, the clock is resumed, causing all
    // the pent up computation to run.
    //
    // Thus a future created in phase i will execute in phase i+1
    // Futures created in phase i are only allowed to depend on
    // futures created in phase i or earlier.

    Globals.Scheduler.currentJob = job

    val jobClock = Clock("Clock for " + job.toString)
    job.put("clock", jobClock)
    jobClock.register

    println("parsing " + job)
    val ast = parse(job) match { case Some(x) => x case None => return false }

    println("initing " + job)
//    val ast2 = initTypes(job, ast) match { case Some(x) => x case None => return false }
//    println("done initing " + job)
//
//    // BUG: might create a future, but not register the clock until after next
//    // so it gets registered during check and blocks!
//    if (commandLineJob) {
//      globalClock.next
//    }
//
//    jobClock.next
//
//    println("checking " + job)
//    val ast3 = checkTypes(job, ast2) match { case Some(x) => x case None => return false }
//    println("done checking " + job)
   
    jobClock.next
    println("about to return from runJob for " + job)

    true
  }

  def parse(job: Job): Option[polyglot.ast.Node] = {
    val eq = Globals.Compiler.errorQueue
    val source = job.source.asInstanceOf[polyglot.frontend.FileSource]

    try {
      val reader = source.open
      val p = job.extensionInfo.parser(reader, source, eq)

      val ast = p.parse
      
      println("done parsing")

      source.close

      if (ast != null) {
        job.ast(ast)
        Some(ast)
      } else None
    } catch {
      case e: java.io.IOException => {
          eq.enqueue(polyglot.util.ErrorInfo.IO_ERROR, e.getMessage(),
            new polyglot.util.Position(source.path,
              source.name, 1, 1, 1, 1))
          None
        }
    }
  }

  def initTypes(job: Job, ast: Node): Option[Node] = {
    val ts = job.extensionInfo.typeSystem
    val nf = job.extensionInfo.nodeFactory

    val v2 = new polyglot.visit.InitImportsVisitor(job, ts, nf).begin
    val ast2 = ast.visit(v2)
    
    val ast3 = ast2.visit(new polyglot.visit.NodeVisitor {
      override def `override`(n: Node) =
        {
          val m: Node =
            n.accept(new polyglot.dispatch.TypeBuilder(job, ts, nf), new polyglot.visit.TypeBuilderContext(job, ts, nf))
          m
        }
    })

    val v3 = new polyglot.frontend.ContextSetter(job, ts, nf).begin
    val ast4 = ast3.visit(v3)

    Some(ast4)
  }

  def checkTypes(job: Job, ast: Node): Option[Node] = {
    val ts = job.extensionInfo.typeSystem
    val nf = job.extensionInfo.nodeFactory

    val ast2 = ast.visit(new polyglot.visit.NodeVisitor {
      override def leave(parent: Node, old: Node, n: Node, v: polyglot.visit.NodeVisitor) = {
        val m = n.checked()

        m match {
          case _: polyglot.ast.SourceFile => m.accept(new polyglot.dispatch.ErrorReporter)
          case _ => ()
        }

        m
      }
    })

    Some(ast2)
  }

}
