package polyglot.dispatch;

import java.util.*;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.*;
import polyglot.visit.*;

public class ExceptionChecker extends Visitor {

    Job job;
    TypeSystem ts;
    NodeFactory nf;

    public ExceptionChecker(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    public Node visit(Node_c n, Context context) throws SemanticException {
	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	return (Node_c) acceptChildren(n);
    }

    public Node visit(Call_c n, ExceptionCheckerContext ec) {
	acceptChildren(n, ec.push());

	if (n.methodInstance() == null) {
	    throw new InternalCompilerError(n.position(), "Null method instance after type check for " + n);
	}

	return visit((Node_c) n, ec);
    }

    public Node visit(ClassBody_c n, ExceptionCheckerContext ec) {
	ec = ec.push();
	Term t = (Term) ecNode(n, ec);
	//System.out.println("exceptions for " + t + " = " + ec.throwsSet());
	return t;
    }

    public Node visit(ConstructorDecl_c n, ExceptionCheckerContext ec) {
	ec = ec.push(new ExceptionCheckerContext.CodeTypeReporter("Constructor " + n.constructorDef().signature())).push(n.constructorDef().asInstance().throwTypes());
	Term t = (Term) ecNode(n, ec);
	//System.out.println("exceptions for " + t + " = " + ec.throwsSet());
	return t;
    }

    public Node visit(FieldDecl_c n, ExceptionCheckerContext ec) {
	ec = ec.push(new ExceptionCheckerContext.CodeTypeReporter("A field initializer"));
	Term t = (Term) ecNode(n, ec);
	//System.out.println("exceptions for " + t + " = " + ec.throwsSet());
	return t;
    }

    public ExceptionCheckerContext enter(Initializer_c n, ExceptionCheckerContext ec) {
	if (n.initializerDef().flags().isStatic()) {
	    return ec.push(new ExceptionCheckerContext.CodeTypeReporter("A static initializer block"));
	}

	if (!n.initializerDef().container().get().toClass().isAnonymous()) {
	    ec = ec.push(new ExceptionCheckerContext.CodeTypeReporter("An instance initializer block"));

	    // An instance initializer of a named class may not throw
	    // a checked exception unless that exception or one of its 
	    // superclasses is explicitly declared in the throws clause
	    // of each contructor or its class, and the class has at least
	    // one explicitly declared constructor.
	    SubtypeSet allowed = null;
	    Type throwable = ts.Throwable();
	    ClassType container = n.initializerDef().container().get().toClass();
	    for (ConstructorInstance ci : container.constructors()) {
		if (allowed == null) {
		    allowed = new SubtypeSet(throwable);
		    allowed.addAll(ci.throwTypes());
		}
		else {
		    // intersect allowed with ci.throwTypes()
		    SubtypeSet other = new SubtypeSet(throwable);
		    other.addAll(ci.throwTypes());
		    SubtypeSet inter = new SubtypeSet(throwable);
		    for (Type t : allowed) {
			if (other.contains(t)) {
			    // t or a supertype is thrown by other.
			    inter.add(t);
			}
		    }
		    for (Type t : other) {
			if (allowed.contains(t)) {
			    // t or a supertype is thrown by the allowed.
			    inter.add(t);
			}
		    }
		    allowed = inter;
		}
	    }
	    // allowed is now an intersection of the throw types of all
	    // constructors

	    ec = ec.push(allowed);


	    return ec;
	}

	return ec.push();
    }

    public Node visit(Initializer_c n, ExceptionCheckerContext ec) {
	ec = enter(n, ec);
	Term t = (Term) ecNode(n, ec);
	//System.out.println("exceptions for " + t + " = " + ec.throwsSet());
	return t;
    }

    public Node visit(MethodDecl_c n, ExceptionCheckerContext ec) {
	ec = ec.push(new ExceptionCheckerContext.CodeTypeReporter("Method " + n.methodDef().signature())).push(n.methodDef().asInstance().throwTypes());
	Term t = (Term) ecNode(n, ec);
	//System.out.println("exceptions for " + t + " = " + ec.throwsSet());
	return t;
    }

    public Node visit(New_c n, ExceptionCheckerContext ec) {
	// something didn't work in the type check phase, so just ignore it.
	if (n.constructorInstanceRef() == null) {
	    throw new InternalCompilerError(n.position(),
	    "Null constructor instance after type check.");
	}

	checkThrows(n, ec, n.constructorInstance().throwTypes());

	return ecNode(n, ec.push());
    }

    public Node visit(Node_c n, ExceptionCheckerContext ec) {
	return ecNode(n, ec.push());
    }

    public Node ecNode(Node_c n, ExceptionCheckerContext ec) {
	n = (Node_c) acceptChildren(n, ec);
	if (n instanceof Term) {
	    List<Type> l = new ArrayList<Type>(((Term) n).throwsRef().get());
	    return checkThrows(n, ec, l);
	}
	return n;
    }

    private Node checkThrows(Node_c n, ExceptionCheckerContext ec, List<Type> l) {
	for (Type t : l) {
	    try {
		ec.throwsException(t, n.position());
	    }
	    catch (SemanticException e) {
		Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), e.position());
	    }
	}
	return n;
    }

    public Node visit(Term_c n, ExceptionCheckerContext ec) {
	Term t = (Term) ecNode(n, ec.push());
	//System.out.println("exceptions for " + t + " = " + ec.throwsSet());
	return t;
    }

    public Node visit(Try_c n, ExceptionCheckerContext ec) {
	ExceptionCheckerContext ec1 = ec.push();

	TypeSystem ts = this.ts;
	ExceptionCheckerContext origEC = ec;

	if (n.finallyBlock() != null && !n.finallyBlock().reachable()) {
	    // the finally block cannot terminate normally.
	    // This implies that exceptions thrown in the try and catch
	    // blocks will not propogate upwards.
	    // Prevent exceptions from propagation upwards past the finally
	    // block. (The original exception checker will be used
	    // for checking the finally block).
	    ec = ec.pushCatchAllThrowable();
	}

	ExceptionCheckerContext newec = ec.push();
	for (ListIterator<Catch> i = n.catchBlocks().listIterator(n.catchBlocks().size()); i.hasPrevious(); ) {
	    Catch cb = i.previous();
	    Type catchType = cb.catchType();
	    newec = newec.push(catchType);
	}

	// Visit the try block.
	Block tryBlock = (Block) accept(n.tryBlock(), newec);

	SubtypeSet caught = new SubtypeSet(ts.Throwable());

	// Walk through our catch blocks, making sure that they each can 
	// catch something.
	for (Iterator<Catch> i = n.catchBlocks().iterator(); i.hasNext(); ) {
	    Catch cb = i.next();
	    Type catchType = cb.catchType();


	    // Check if the exception has already been caught.
	    if (caught.contains(catchType)) {
		Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
		                                        "The exception \"" +
		                                        catchType + "\" has been caught by an earlier catch block.",
		                                        cb.position()); 
	    }

	    caught.add(catchType);
	}


	// now visit the catch blocks, using the original exception checker
	List<Catch> catchBlocks = new ArrayList<Catch>(n.catchBlocks().size());

	for (Iterator<Catch> i = n.catchBlocks().iterator(); i.hasNext(); ) {
	    Catch cb = (Catch) i.next();

	    ec = ec.push();
	    cb = (Catch) accept(cb, ec);
	    catchBlocks.add(cb);
	    ec = ec.pop();
	}

	Block finallyBlock = null;

	if (n.finallyBlock() != null) {
	    ec = origEC;

	    finallyBlock = (Block) accept(n.finallyBlock(), ec);

	    if (!n.finallyBlock().reachable()) {
		// warn the user
		//              ###Don't warn, some versions of javac don't.              
		//              ec.errorQueue().enqueue(ErrorInfo.WARNING,
		                                        //              "The finally block cannot complete normally", 
		//              finallyBlock.position());
	    }

	    ec = ec.pop();
	}
	// now that all the exceptions have been added to the exception checker,
	// call the super method, which should set the exceptions field of 
	// Term_c.
	Try_c t = (Try_c)ecNode(n, ec);

	return t.tryBlock(tryBlock).catchBlocks(catchBlocks).finallyBlock(finallyBlock);
    }

}
