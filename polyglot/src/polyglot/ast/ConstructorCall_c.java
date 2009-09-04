/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.dispatch.TypeChecker;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>ConstructorCall_c</code> represents a direct call to a constructor.
 * For instance, <code>super(...)</code> or <code>this(...)</code>.
 */
public class ConstructorCall_c extends Stmt_c implements ConstructorCall
{
    protected Kind kind;
    protected Expr qualifier;
    protected List<Expr> arguments;
    protected Ref<ConstructorInstance> ci;

    public ConstructorCall_c(Position pos, Kind kind, Expr qualifier, List arguments) {
	super(pos);
	assert(kind != null && arguments != null); // qualifier may be null
	this.kind = kind;
	this.qualifier = qualifier;
	this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);

	TypeSystem ts = Globals.TS();
	ConstructorInstance ci = ts.createConstructorInstance(position(), new ErrorRef_c<ConstructorDef>(ts, position(), "Cannot get ConstructorDef before type-checking constructor call."));
	this.ci = Types.<ConstructorInstance>lazyRef(ci);
    }

    /** Get the qualifier of the constructor call. */
    public Expr qualifier() {
	return this.qualifier;
    }

    /** Set the qualifier of the constructor call. */
    public ConstructorCall qualifier(Expr qualifier) {
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.qualifier = qualifier;
	return n;
    }

    /** Get the kind of the constructor call. */
    public Kind kind() {
	return this.kind;
    }

    /** Set the kind of the constructor call. */
    public ConstructorCall kind(Kind kind) {
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.kind = kind;
	return n;
    }

    /** Get the actual arguments of the constructor call. */
    public List<Expr> arguments() {
	return Collections.unmodifiableList(this.arguments);
    }

    /** Set the actual arguments of the constructor call. */
    public ProcedureCall arguments(List<Expr> arguments) {
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	return n;
    }

    public ProcedureInstance procedureInstance() {
	return constructorInstance();
    }

    /** Get the constructor we are calling. */
    public ConstructorInstance constructorInstance() {
	return Types.get(ci);
    }

    /** Set the constructor we are calling. */
    public ConstructorCall constructorInstance(ConstructorInstance ci) {
	if (ci == this.ci) return this;
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.ci.update(ci);
	return n;
    }

    /**
     * An explicit constructor call is a static context. We need to record
     * this.
     */
    public Context enterScope(Context c) {
	return c.pushStatic();
    }

    /** Reconstruct the constructor call. */
    protected ConstructorCall_c reconstruct(Expr qualifier, List arguments) {
	if (qualifier != this.qualifier || ! CollectionUtil.allEqual(arguments, this.arguments)) {
	    ConstructorCall_c n = (ConstructorCall_c) copy();
	    n.qualifier = qualifier;
	    n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the call. */
    public Node visitChildren(NodeVisitor v) {
	Expr qualifier = (Expr) visitChild(this.qualifier, v);
	List arguments = visitList(this.arguments, v);
	return reconstruct(qualifier, arguments);
    }


    public Node buildTypes(TypeBuilder tb) throws SemanticException {
	final Job job = tb.job();
	final TypeSystem ts = tb.typeSystem();
	final NodeFactory nf = tb.nodeFactory();

	// Remove super() calls for java.lang.Object.
	if (kind == SUPER && tb.currentClass() == ts.Object()) {
	    return nf.Empty(position());
	}

	ConstructorCall_c n = (ConstructorCall_c) super.buildTypes(tb);

	((LazyRef<ConstructorInstance>) n.ci).setResolver(new Runnable() {
	    public void run() {
		new TypeChecker(job, ts, nf).visit(ConstructorCall_c.this);
	    } 
	});

	return n;
}

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
	TypeSystem ts = av.typeSystem();

	if (child == qualifier) {
	    // FIXME: Can be more specific
	    return ts.Object();
	}

	Iterator i = this.arguments.iterator();
	Iterator j = constructorInstance().formalTypes().iterator();

	while (i.hasNext() && j.hasNext()) {
	    Expr e = (Expr) i.next();
	    Type t = (Type) j.next();

	    if (e == child) {
		return t;
	    }
	}

	return child.type();
    }

    public String toString() {
	return (qualifier != null ? qualifier + "." : "") + kind + "(...)";
    }

    /** Write the call to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	if (qualifier != null) {
	    print(qualifier, w, tr);
	    w.write(".");
	} 

	w.write(kind + "(");

	w.begin(0);

	for (Iterator i = arguments.iterator(); i.hasNext(); ) {
	    Expr e = (Expr) i.next();
	    print(e, w, tr);

	    if (i.hasNext()) {
		w.write(",");
		w.allowBreak(0);
	    }
	}

	w.end();

	w.write(");");
    }

    public Term firstChild() {
	if (qualifier != null) {
	    return qualifier;
	} else {
	    return listChild(arguments, null);
	}
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
	if (qualifier != null) {
	    if (!arguments.isEmpty()) {
		v.visitCFG(qualifier, listChild(arguments, null), ENTRY);
		v.visitCFGList(arguments, this, EXIT);
	    } else {
		v.visitCFG(qualifier, this, EXIT);
	    }
	} else {
	    if (!arguments.isEmpty()) {
		v.visitCFGList(arguments, this, EXIT);
	    }
	}

	return succs;
    }

    public List<Type> throwTypes(TypeSystem ts) {
	List<Type> l = new ArrayList<Type>();
	l.addAll(constructorInstance().throwTypes());
	l.addAll(ts.uncheckedExceptions());
	return l;
    }
}
