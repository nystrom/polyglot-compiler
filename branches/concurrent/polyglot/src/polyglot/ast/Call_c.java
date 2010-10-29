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
import polyglot.visit.ExceptionCheckerContext;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Call</code> is an immutable representation of a Java method call. It
 * consists of a method name and a list of arguments. It may also have either a
 * Type upon which the method is being called or an expression upon which the
 * method is being called.
 */
public class Call_c extends Expr_c implements Call {
    protected Receiver target;
    protected Id name;
    protected List<Expr> arguments;
    protected Ref<MethodInstance> mi;
    protected boolean targetImplicit;

    public Call_c(Position pos, Receiver target, Id name, List<Expr> arguments) {
	super(pos);
	assert (name != null && arguments != null); // target may be null
	this.target = target;
	this.name = name;
	this.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	this.targetImplicit = (target == null);
	this.mi = Types.<MethodInstance>lazyRef();
    }

    /** Get the target object or type of the call. */
    public Receiver target() {
	return this.target;
    }

    /** Set the target object or type of the call. */
    public Call target(Receiver target) {
	Call_c n = (Call_c) copy();
	n.target = target;
	return n;
    }

    /** Get the name of the call. */
    public Id name() {
	return this.name;
    }

    /** Set the name of the call. */
    public Call name(Id name) {
	Call_c n = (Call_c) copy();
	n.name = name;
	return n;
    }

    public ProcedureInstance procedureInstance() {
	return methodInstance();
    }

    /** Get the method instance of the call. */
    public MethodInstance methodInstance() {
	return Types.get(this.mi);
    }

    /** Set the method instance of the call. */
    public Call methodInstance(MethodInstance mi) {
	if (mi == this.mi)
	    return this;
	Call_c n = (Call_c) copy();
	n.mi.update(mi);
	return n;
    }

    public boolean isTargetImplicit() {
	return this.targetImplicit;
    }

    public Call targetImplicit(boolean targetImplicit) {
	if (targetImplicit == this.targetImplicit) {
	    return this;
	}

	Call_c n = (Call_c) copy();
	n.targetImplicit = targetImplicit;
	return n;
    }

    /** Get the actual arguments of the call. */
    public List<Expr> arguments() {
	return this.arguments;
    }

    /** Set the actual arguments of the call. */
    public ProcedureCall arguments(List<Expr> arguments) {
	Call_c n = (Call_c) copy();
	n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	return n;
    }

    /** Reconstruct the call. */
    protected Call_c reconstruct(Receiver target, Id name, List<Expr> arguments) {
	if (target != this.target || name != this.name || !CollectionUtil.allEqual(arguments, this.arguments)) {
	    Call_c n = (Call_c) copy();

	    // If the target changes, assume we want it to be an explicit
	    // target.
	    n.targetImplicit = n.targetImplicit && target == n.target;

	    n.target = target;
	    n.name = name;
	    n.arguments = TypedList.copyAndCheck(arguments, Expr.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the call. */
    public Node visitChildren(NodeVisitor v) {
	Receiver target = (Receiver) visitChild(this.target, v);
	Id name = (Id) visitChild(this.name, v);
	List<Expr> arguments = visitList(this.arguments, v);
	return reconstruct(target, name, arguments);
    }

    /**
     * Used to find the missing static target of a static method call. Should
     * return the container of the method instance.
     * 
     */
    public Type findContainer(TypeSystem ts, MethodInstance mi) {
	return mi.container();
    }

    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append(targetImplicit ? "" : target.toString() + ".");
	sb.append(name);
	sb.append("(");

	int count = 0;

	for (Iterator i = arguments.iterator(); i.hasNext();) {
	    if (count++ > 2) {
		sb.append("...");
		break;
	    }

	    Expr n = (Expr) i.next();
	    sb.append(n.toString());

	    if (i.hasNext()) {
		sb.append(", ");
	    }
	}

	sb.append(")");
	return sb.toString();
    }

    /** Dumps the AST. */
    public void dump(CodeWriter w) {
	super.dump(w);

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(targetImplicit " + targetImplicit + ")");
	w.end();

	if (mi != null) {
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(instance " + methodInstanceRef() + ")");
	    w.end();
	}

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(name " + name + ")");
	w.end();

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(arguments " + arguments + ")");
	w.end();
    }

    public Ref<MethodInstance> methodInstanceRef() {
	return mi;
    }
    
    public Call methodInstanceRef(Ref<MethodInstance> r) {
	Call_c n = (Call_c) copy();
	n.mi = r;
	return n;
    }

}
