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
  //  globalClock.register

  def runJob1(job: Job): polyglot.ast.Node = {
	  
	val commandLineJob = Globals.Scheduler.commandLineJobs contains job
    if (!commandLineJob) {
      //globalClock.drop
    }
    Globals.Scheduler.currentJob = job

   
    val jobClock = Clock("Clock for " + job.toString)
    job.put("clock", jobClock)
    jobClock.register

    
    println("parsing " + job)
    val ast = parse(job) match { 
    	case Some(x) => 
    			x 
    	case None => 
    		return null 
    	}
    println("done parsing" + job)
//    next
    println("initing " + job)    

    val ast2 = initTypes(job, ast) match { case Some(x) => x case None => return null }
    println("done initing " + job)
    
    println("checking " + job)
    val ast3 = checkTypes(job, ast2) match { case Some(x) => x case None => return null }
    println("done checking " + job)
    
    ast3
  }

  def runJob(job: Job, ast: polyglot.ast.Node): polyglot.ast.Node = {
    //globalClock.register

    

    // Basic pattern:
    // All futures created in a phase are registered on jobClock.
    // All futures wait on the clock before doing any computation.
    // At the end of the phase, the clock is resumed, causing all
    // the pent up computation to run.
    //
    // Thus a future created in phase i will execute in phase i+1
    // Futures created in phase i are only allowed to depend on
    // futures created in phase i or earlier.

    //Globals.Scheduler.currentJob = job

//    val jobClock = Clock("Clock for " + job.toString)
//    job.put("clock", jobClock)
//    jobClock.register
//
//    println("parsing " + job)
//    val ast = parse(job) match { case Some(x) => x case None => return false }
//    next
//    //println("initing " + job)
//
//    val ast2 = initTypes(job, ast) match { case Some(x) => x case None => return false }
//    println("done initing " + job)
//    //    jobClock.next
//    //    next
//
//    println("checking " + job)
//    val ast3 = checkTypes(job, ast2) match { case Some(x) => x case None => return false }
//    println("done checking " + job)
//
//    //    jobClock.next
//    next
//	  *****
    val ast3 = ast
    println("conformanceChecking" + job)
    val ast4 = checkConformance(job, ast3) match { case Some(x) => x case None => return null }
    println("Done conformanceChecking" + job)
    next

    println("reachChecking" + job)
    val ast5 = reachChecked(job, ast4) match { case Some(x) => x case None => return null }
    println("Done reachChecking" + job)
    next

    println("exceptionsChecking" + job)
    val ast6 = exceptionsChecked(job, ast5) match { case Some(x) => x case None => return null }
    println("Done exceptionChecking" + job)

    ast6
//    println("exitPathsCheck" + job)
//    val ast7 = exitPathsChecked(job, ast6) match { case Some(x) => x case None => return false }
//    println("Done exitPathChecking" + job)
//    next

    
  }
  
  def runJobFinal(job: Job, ast: polyglot.ast.Node): Boolean = {
	  
    println("codeGenerated" + job)
    val ast8 = codeGenerate(job, ast)
    println("Done code generation" + job)

    ast8
	  
	  
  }

  def parse(job: Job): Option[polyglot.ast.Node] = {
    val eq = Globals.Compiler.errorQueue
    val source = job.source.asInstanceOf[polyglot.frontend.FileSource]

    try {
      val reader = source.open
      val p = job.extensionInfo.parser(reader, source, eq)

      val ast = p.parse

      

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

  def checkConformance(job: Job, ast: Node): Option[Node] = {

    val ts = job.extensionInfo.typeSystem
    val nf = job.extensionInfo.nodeFactory

    val ast2 = ast.visit(new polyglot.visit.NodeVisitor {
      override def `override`(n: Node) =
        {
          try {
            val m: Node =
              n.accept(new polyglot.dispatch.ConformanceChecker(job, ts, nf))
            m match {
              case _: polyglot.ast.SourceFile => m.accept(new polyglot.dispatch.ErrorReporter)
              case _ => ()
            }
          } catch {
            case x: polyglot.dispatch.PassthruError =>
              x.getCause match {
                case e: polyglot.types.SemanticException => Globals.Compiler.errorQueue.enqueue(polyglot.util.ErrorInfo.SEMANTIC_ERROR, e.getMessage, n.position)
                case _ => throw x
              }
          }

          null
        }
    })

    Some(ast2)

  }

  def reachChecked(job: Job, ast: Node): Option[Node] = {

    val ts = job.extensionInfo.typeSystem
    val nf = job.extensionInfo.nodeFactory

    val breaks = new java.util.IdentityHashMap[Node, polyglot.types.Ref[java.util.Collection[Name]]]();
    val continues = new java.util.IdentityHashMap[Node, polyglot.types.Ref[java.util.Collection[Name]]]();
    val exceptions = new java.util.IdentityHashMap[Node, polyglot.types.Ref[java.util.Collection[polyglot.types.Type]]]();
    val completes = new java.util.IdentityHashMap[Node, polyglot.types.Ref[java.lang.Boolean]]();

    val ast1 = ast.visit(new polyglot.visit.NodeVisitor {
      override def leave(parent: Node, old: Node, n: polyglot.ast.Node, v: polyglot.visit.NodeVisitor) =
        {
    		n.accept(new polyglot.dispatch.BreakContinueSetup(ts, breaks, continues), parent)
    		n
    	}
    })

    val ast2 = ast1.visit(new polyglot.visit.NodeVisitor {
      override def leave(parent: Node, old: Node, n: polyglot.ast.Node, v: polyglot.visit.NodeVisitor) =
        {
          n.accept(new polyglot.dispatch.ThrowSetup(ts, exceptions), parent)
          n
        }
    })

    val ast3 = ast2.visit(new polyglot.visit.NodeVisitor {
      override def leave(parent: Node, old: Node, n: polyglot.ast.Node, v: polyglot.visit.NodeVisitor) =
        {
          n.accept(new polyglot.dispatch.ReachSetup(ts, breaks, continues, exceptions,
            completes), parent);
          n
        }
    })

    val m: Node = ast3.accept(new polyglot.dispatch.ReachChecker(job, ts, nf, completes));
    m.visit(new polyglot.dispatch.dataflow.InitChecker());
    val m2: Node = m.accept(new polyglot.dispatch.FwdReferenceChecker(job, ts, nf), null)
    val ast4: Node = m2.accept(new polyglot.dispatch.ConstructorCallChecker(job, ts, nf));

    ast4.accept(new polyglot.dispatch.ErrorReporter());

    Some(ast4)
  }

  def exceptionsChecked(job: Job, ast: Node): Option[Node] = {
    val ts = job.extensionInfo.typeSystem
    val nf = job.extensionInfo.nodeFactory

    val exceptions = new java.util.IdentityHashMap[Node, polyglot.types.Ref[java.util.Collection[polyglot.types.Type]]]();

    val ast2 = ast.visit(new polyglot.visit.NodeVisitor {
      override def leave(parent: Node, old: Node, n: polyglot.ast.Node, v: polyglot.visit.NodeVisitor) =
        {
          n.accept(new polyglot.dispatch.ThrowSetup(ts, exceptions), parent)
          n
        }
    })

    val ast3 = ast2.visit(new polyglot.visit.NodeVisitor {
      override def `override`(n: Node) =
        {
          var m: Node = null
          m =
            n.accept(new polyglot.dispatch.ExceptionChecker(job, ts, nf),
              new polyglot.visit.ExceptionCheckerContext(job, ts, nf))
          m.accept(new polyglot.dispatch.ErrorReporter())
          m
        }
    })

    Some(ast3)

  }

  def exitPathsChecked(job: Job, ast: Node): Option[Node] = {
	
	//val ast2 = ast.visit(new polyglot.dispatch.dataflow.InitChecker())
    val ast2 = ast.visit(new polyglot.dispatch.dataflow.ExitChecker())
    job.ast(ast)
    Some(ast)
  }

  def codeGenerate(job: Job, ast: Node) = {
    val ts = job.extensionInfo.typeSystem
    val nf = job.extensionInfo.nodeFactory

    val n = new polyglot.frontend.OutputGoal(job, new polyglot.visit.Translator(job, ts, nf, Globals.Extension().targetFactory()), ast)
    n.runTask
  }

}
