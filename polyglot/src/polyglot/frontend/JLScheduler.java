/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

/*
 * Scheduler.java
 * 
 * Author: nystrom
 * Creation date: Feb 6, 2005
 */
package polyglot.frontend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.ast.Term;
import polyglot.bytecode.BytecodeTranslator;
import polyglot.dispatch.BreakContinueSetup;
import polyglot.dispatch.ConformanceChecker;
import polyglot.dispatch.ConstructorCallChecker;
import polyglot.dispatch.ErrorReporter;
import polyglot.dispatch.ExceptionChecker;
import polyglot.dispatch.FwdReferenceChecker;
import polyglot.dispatch.PassthruError;
import polyglot.dispatch.ReachChecker;
import polyglot.dispatch.ReachSetup;
import polyglot.dispatch.ThrowSetup;
import polyglot.dispatch.dataflow.InitChecker;
import polyglot.interp.BytecodeCache;
import polyglot.types.ClassType;
import polyglot.types.Name;
import polyglot.types.Named;
import polyglot.types.ParsedClassType_c;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.visit.ExceptionCheckerContext;
import polyglot.visit.InitImportsVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.Translator;

/**
 * Comment for <code>Scheduler</code>
 *
 * @author nystrom
 */
public class JLScheduler extends Scheduler {

    /**
     * @param extInfo
     */
    public JLScheduler(ExtensionInfo extInfo) {
	super(extInfo);
    }

    public List<Goal> goals(Job job) {
	List<Goal> goals = new ArrayList<Goal>();

	goals.add(CompileGoal(job));

	if (Globals.Options().interpret || ! Globals.Options().output_source) {
	    goals.add(BytecodeCached(job));
	}
	else if (Globals.Options().output_source) {
	    goals.add(CodeGenerated(job));
	}

	goals.add(End(job));

	return goals;
    }
    
    public Goal CompileGoal(final Job job) {
	return new SourceGoal_c("scala", job) {
	    
	    @Override
	    public boolean runTask() {
		return polyglot.Main$.MODULE$.runJob(job);
	    }
	}.intern(this);
    }

    public Goal Parsed(Job job) {
	return new ParserGoal(extInfo.compiler(), job).intern(this);
    }    

    public Goal ImportTableInitialized(Job job) {
	TypeSystem ts = job.extensionInfo().typeSystem();
	NodeFactory nf = job.extensionInfo().nodeFactory();
	Goal g = new VisitorGoal("ImportTableInitialized", job, new InitImportsVisitor(job, ts, nf));
	Goal g2 = g.intern(this);
	//        if (g == g2) {
	//            g.addPrereq(TypesInitializedForCommandLine());
	//        }
	return g2;
    }

    public Goal TypesInitialized(Job job) {
	// For this one, the goal is stored in the job.  This is called a lot from the system resolver and interning is expensive.
	return job.TypesInitialized(this);
    }

    public Goal TypesInitializedForCommandLine() {
	TypeSystem ts = extInfo.typeSystem();
	NodeFactory nf = extInfo.nodeFactory();
	return new BarrierGoal("TypesInitializedForCommandLine", commandLineJobs()) {
	    public Goal prereqForJob(Job job) {
		return PreTypeCheck(job);
	    }
	}.intern(this);
    }

    public Goal PreTypeCheck(Job job) {
	TypeSystem ts = job.extensionInfo().typeSystem();
	NodeFactory nf = job.extensionInfo().nodeFactory();
	return new VisitorGoal("PreTypeCheck", job, new ContextSetter(job, ts, nf)).intern(this);
    }

    public Goal PreTypeCheck2(final Job job) {
	final TypeSystem ts = job.extensionInfo().typeSystem();

	final Map<Node, Ref<Collection<Name>>> breaks = new IdentityHashMap<Node, Ref<Collection<Name>>>();
	final Map<Node, Ref<Collection<Name>>> continues = new IdentityHashMap<Node, Ref<Collection<Name>>>();
	final Map<Node, Ref<Collection<Type>>> exceptions = new IdentityHashMap<Node, Ref<Collection<Type>>>();
	final Map<Node, Ref<Boolean>> completes = new IdentityHashMap<Node, Ref<Boolean>>();

	job.put("completes", completes);

	return new VisitorGoal("PreTypeCheck2", job, new NodeVisitor() {
	    @Override
	    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {


		//		    /** True if the term is may complete normally. */
		//		    public Ref<Boolean> completesRef();
		//		    
		//		    /** Labels that may break out of this term.  Result may include null for unlabeled break. */
		//		    public Ref<Collection<Name>> breaksRef();
		//		    /** Labels that may continue out of this term.  Result may include null for unlabeled continue. */
		//		    public Ref<Collection<Name>> continuesRef();

		n.accept(new BreakContinueSetup(ts, breaks, continues), parent);
		n.accept(new ThrowSetup(ts, exceptions), parent);
		n.accept(new ReachSetup(ts, breaks, continues, exceptions, completes), parent);
		
		if (n instanceof SourceFile) {
		    n.visit(new NodeVisitor() {
			public Node leave(Node old, Node n, NodeVisitor v) {
			    if (n instanceof Term) {
				Term t = (Term) n;
				Ref<Collection<Type>> r = exceptions.get(n);
				if (r != null)
				    r.start();
				Ref<Collection<Name>> r2 = breaks.get(n);
				if (r2 != null)
				    r2.start();
				Ref<Collection<Name>> r3 = continues.get(n);
				if (r3 != null)
				    r3.start();
				Ref<Boolean> r4 = completes.get(n);
				if (r4 != null)
				    r4.start();
				t.reachableRef().start();
			    }
			    return n;
			}
		    });
		}
		
		return n;
	    }  
	}).intern(this);
    }

    public Goal TypeChecked(final Job job) {
	final TypeSystem ts = job.extensionInfo().typeSystem();
	final NodeFactory nf = job.extensionInfo().nodeFactory();
	return new VisitorGoal("TypeChecked", job, new NodeVisitor() {
	    @Override
	    //    	    public Node override(Node parent, Node n) {
	    //    		// FIXME: order very important here -- need to check cycles for supertypes BEFORE calling isSubtype
	    //    		// so need to visit ClassDecl before anything else
	    //    		
	    //    		Node m = n.checked();
	    //    		assert m != null;
	    //    		
	    //    		if (m instanceof SourceFile) {
	    //    		    m.accept(new ErrorReporter());
	    //    		}
	    //    		
	    //    		return m;
	    //    	    }
	    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
		Node m = n.checked();
		assert m != null;

		if (m instanceof SourceFile) {
		    m.accept(new ErrorReporter());
		}

		return m;
	    }
	}).intern(this);
    }

    public Goal EnsureNoErrors(final Job job) {
	return new VisitorGoal("EnsureNoErrors", job, new NodeVisitor() {
	    @Override
	    public Node override(Node parent, Node n) {
		n.accept(new ErrorReporter());
		return n;
	    }
	}).intern(this);
    }

    public Goal ConformanceChecked(final Job job) {
	final TypeSystem ts = job.extensionInfo().typeSystem();
	final NodeFactory nf = job.extensionInfo().nodeFactory();
	return new VisitorGoal("ConformanceChecked", job, new NodeVisitor() {
	    @Override
	    public Node override(Node n) {
		try {
		    Node m = n.accept(new ConformanceChecker(job, ts, nf));
		    m.accept(new ErrorReporter());
		}
		catch (PassthruError x) {
		    if (x.getCause() instanceof SemanticException) {
			SemanticException e = (SemanticException) x.getCause();
			Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), n.position());
		    }
		    else throw x;
		}
		return null;
	    }   
	}).intern(this);
    }

    public Goal ExceptionsChecked(final Job job) {
	final TypeSystem ts = job.extensionInfo().typeSystem();
	final NodeFactory nf = job.extensionInfo().nodeFactory();
	return new VisitorGoal("ExceptionsChecked", job, new NodeVisitor() {
	    @Override
	    public Node override(Node n) {
		Node m = n.accept(new ExceptionChecker(job, ts, nf), new ExceptionCheckerContext(job, ts, nf));
		m.accept(new ErrorReporter());
		return m;
	    }   
	}).intern(this);
    }
    public Goal ReachChecked(final Job job) {
	final TypeSystem ts = job.extensionInfo().typeSystem();
	final NodeFactory nf = job.extensionInfo().nodeFactory();
	final Map<Node, Ref<Boolean>> completes = (Map<Node, Ref<Boolean>>) job.get("completes");
	job.put("completes", null);
	
	return new VisitorGoal("ReachChecked", job, new NodeVisitor() {
	    @Override
	    public Node override(Node n) {
		Node m = n.accept(new ReachChecker(job, ts, nf, completes));
		m.visit(new InitChecker());
		Node m2 = m.accept(new FwdReferenceChecker(job, ts, nf), (Object) null);
		Node m3 = m2.accept(new ConstructorCallChecker(job, ts, nf));
		m3.accept(new ErrorReporter());
		return m3;
	    }   
	}).intern(this);
    }

    public Goal CodeGenerated(Job job) {
	TypeSystem ts = extInfo.typeSystem();
	NodeFactory nf = extInfo.nodeFactory();
	return new OutputGoal(job, new Translator(job, ts, nf, extInfo.targetFactory()));
    }

    // TODO: symbol cleanup
    // Don't have direct pointers from FieldInstance to FieldDef (and all other Defs)
    // Instead, indirect through symbol table.
    // Don't copy data into FieldInstance.  FI is _always_ lazily constructed from an FD.
    // FI is subst(sigma, FD).
    // Then, can only modify defs.

    // Should be able to do InnerClassRemover in just one pass over the AST with the inner class.
    // Should not have to update users of the class.

    // TODO: implement InnerClassRemover to be compatible with javac generated code.
    // Problem is the InnerClasses attribute.  Do as annotations?

    public Goal BytecodeCached(final QName className) {
	final TypeSystem ts = extInfo.typeSystem();
	final NodeFactory nf = extInfo.nodeFactory();

	Goal g = new AbstractGoal_c("BytecodeCachedByName") {
	    public String toString() {
		return name + "(" + className + ")";
	    }

	    public boolean runTask() {
		try {
		    Named n = ts.systemResolver().find(className);
		    if (n instanceof ClassType) {
			ClassType ct = (ClassType) n;
			Job job = ((ParsedClassType_c) ct).job();
			if (job != null) {
			    BytecodeCached(job).get();
			    return true;
			}
		    }
		}
		catch (SemanticException e) {
		    Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), e.position());
		}
		return false;
	    }
	};

	g = g.intern(this);
	return g;
    }

    public Goal BytecodeCached(final Job job) {
	final TypeSystem ts = extInfo.typeSystem();
	final NodeFactory nf = extInfo.nodeFactory();
	final BytecodeCache bc = extInfo.bytecodeCache();

	return new VisitorGoal("BytecodeTranslator", job, new NodeVisitor() {
	    @Override
	    public Node override(Node n) {
		if (n instanceof SourceFile) {
		    new BytecodeTranslator(job, ts, nf, bc).visit((SourceFile) job.ast());
		    return n;
		}
		return null;
	    }   
	}).intern(this);

    }
}
