/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.frontend.Globals;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.NodeVisitor;

/**
 * A <code>ConstructorCall_c</code> represents a direct call to a constructor.
 * For instance, <code>super(...)</code> or <code>this(...)</code>.
 */
public class ConstructorCall_c extends Stmt_c implements ConstructorCall
{
    protected Kind kind;
    protected Expr qualifier;
    protected List<Expr> arguments;
    private Ref<ConstructorInstance> ci;

    public ConstructorCall_c(Position pos, Kind kind, Expr qualifier, List arguments) {
	super(pos);
	assert(kind != null && arguments != null); // qualifier may be null
	this.kind = kind;
	this.qualifier = qualifier;
	this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	this.ci = Types.<ConstructorInstance>lazyRef();
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
	return Types.get(constructorInstanceRef());
    }

    /** Set the constructor we are calling. */
    public ConstructorCall constructorInstance(ConstructorInstance ci) {
	if (ci == this.constructorInstanceRef()) return this;
	ConstructorCall_c n = (ConstructorCall_c) copy();
	n.constructorInstanceRef().update(ci);
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

    public String toString() {
	switch (kind) {
	case SUPER:
	    return (qualifier != null ? qualifier + "." : "") + "super" + "(...)";
	case THIS:
	    return (qualifier != null ? qualifier + "." : "") + "this" + "(...)";
	default:
	    throw new InternalCompilerError("Unknown constructor call kind.", position());
	}
    }

    public Ref<ConstructorInstance> constructorInstanceRef() {
	return ci;
    }
}
