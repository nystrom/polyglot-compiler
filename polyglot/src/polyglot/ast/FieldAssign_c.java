/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.dispatch.DispatchedTypeChecker;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>FieldAssign_c</code> represents a Java assignment expression to
 * a field.  For instance, <code>this.x = e</code>.
 * 
 * The class of the <code>Expr</code> returned by
 * <code>FieldAssign_c.left()</code>is guaranteed to be a <code>Field</code>.
 */
public class FieldAssign_c extends Assign_c implements FieldAssign
{
    boolean targetImplicit;
    Receiver target;
    Id name;
    protected Ref<FieldInstance> fi;

    public FieldAssign_c(Position pos, Receiver target, Id name, Operator op, Expr right) {
	super(pos, op, right);
	assert name != null;
	this.target = target;
	this.name = name;


	TypeSystem ts = Globals.TS();
	FieldInstance fi = ts.createFieldInstance(position(), new ErrorRef_c<FieldDef>(ts, position(), "Cannot get FieldDef before type-checking field access."));
	this.fi = Types.<FieldInstance>lazyRef(fi);
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
	FieldAssign_c n = (FieldAssign_c) super.buildTypes(tb);

	final Job job = tb.job();
	final TypeSystem ts = tb.typeSystem();
	final NodeFactory nf = tb.nodeFactory();

	((LazyRef<FieldInstance>) this.fi).setResolver(new Runnable() {
	    public void run() {
		new DispatchedTypeChecker(job, ts, nf).visit(FieldAssign_c.this);
	    } 
	});

	return n;
    }

    @Override
    public Assign visitLeft(NodeVisitor v) {
	Id name = (Id) visitChild(this.name, v);
	Receiver target = (Receiver) visitChild(this.target, v);
	return reconstruct(target, name);
    }

    protected Assign reconstruct(Receiver target, Id name) {
	if (name != this.name || target != this.target) {
	    FieldAssign_c n = (FieldAssign_c) copy();
	    n.target = target;
	    n.name = name;
	    return n;
	}
	return this;
    }

    public boolean targetImplicit() {
	return targetImplicit;
    }

    public FieldAssign targetImplicit(boolean f) {
	FieldAssign_c n = (FieldAssign_c) copy();
	n.targetImplicit = f;
	return n;
    }

    public Expr left(NodeFactory nf) {
	Field_c f = (Field_c) nf.Field(position(), target, name);
	f.fi = this.fi;
	f.typeRef = this.typeRef;
	f.targetImplicit = targetImplicit;
	return f;
    }

    public Type leftType() {
	if (fi == null) return null;
	return fieldInstance().type();
    }

    public Receiver target() {
	return target;
    }

    public FieldAssign target(Receiver target) {
	FieldAssign_c n = (FieldAssign_c) copy();
	n.target = target;
	return n;
    }

    public Id name() {
	return name;
    }

    public FieldAssign name(Id name) {
	FieldAssign_c n = (FieldAssign_c) copy();
	n.name = name;
	return n;
    }

    public FieldInstance fieldInstance() {
	return Types.get(fi);
    }  

    public FieldAssign fieldInstance(FieldInstance fi) {
	FieldAssign_c n = (FieldAssign_c) copy();
	n.fi.update(fi);
	return n;
    }

    public Term firstChild() {
	if (target instanceof Term)
	    return (Term) target;
	else
	    return right;
    }

    protected void acceptCFGAssign(CFGBuilder v) {
	//     o.f = e: visit o -> e -> (o.f = e)
	if (target instanceof Term)
	    v.visitCFG((Term) target, right(), ENTRY);              
	v.visitCFG(right(), this, EXIT);
    }
    protected void acceptCFGOpAssign(CFGBuilder v) {
	if (target instanceof Term)
	    v.visitCFG((Term) target, right(), ENTRY);              
	v.visitCFG(right(), this, EXIT);
    }

    public List<Type> throwTypes(TypeSystem ts) {
	List<Type> l = new ArrayList<Type>(super.throwTypes(ts));

	if (target instanceof Expr) {
	    l.add(ts.NullPointerException());
	}

	return l;
    }

}
