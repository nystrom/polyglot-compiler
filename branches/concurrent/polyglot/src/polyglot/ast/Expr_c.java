/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import polyglot.dispatch.ConstantValueVisitor;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.types.Ref.Handler;
import polyglot.util.CodeWriter;
import polyglot.util.Position;

/**
 * An <code>Expr</code> represents any Java expression.  All expressions
 * must be subtypes of Expr.
 */
public abstract class Expr_c extends Term_c implements Expr
{
    protected Ref<Type> typeRef;
    protected Ref<Object> constantValueRef;

    void updateType() {
	if (this.typeRef != null && ! this.typeRef.forced())
	     this.typeRef.setResolver(new Ref.Callable<Type>() {
		public Type call() {
		    Expr e = (Expr) Expr_c.this.checked();
		    return e.type();
		}
	    }, jobClock());
    }

    void updateCV() {
	if (this.constantValueRef != null && ! this.constantValueRef.forced())
	    this.constantValueRef.setResolver(new Ref.Callable<Object>() {
		public Object call() {
		    Expr_c e = Expr_c.this;
		    Job job = e.job;
		    TypeSystem ts = Globals.TS();
		    NodeFactory nf = Globals.NF();
		    Object v = e.accept(new ConstantValueVisitor(job, ts, nf));
		    return v;
		}
	    }, jobClock());
    }

    public Expr_c(Position pos) {
	super(pos);
	TypeSystem ts = Globals.TS();
	this.typeRef = Types.<Type>lazyRef(ts.unknownType(position()));
	this.constantValueRef = Types.lazyRef(ConstantValueVisitor.NOT_CONSTANT);
	updateType();
	updateCV();
	addCopyHook(new Handler<Node>() {
	    public void handle(Node t) {
		((Expr_c) t).updateType();
		((Expr_c) t).updateCV();
	    }
	});
    }

    /**
     * Get the type of the expression.  This may return an
     * <code>UnknownType</code> before type-checking, but should return the
     * correct type after type-checking.
     */
    public Ref<Type> typeRef() {
	return this.typeRef;
    }

    public Type type() {
	return Types.get(this.typeRef);
    }

    /** Set the type of the expression. */
    public Expr type(Type type) {
	this.typeRef.update(type);
	return this;
    }

    public void dump(CodeWriter w) {
	super.dump(w);

	if (typeRef != null) {
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(type " + typeRef + ")");
	    w.end();
	}
    }

    /** Get the precedence of the expression. */
    final public Precedence precedence() {
	return Precedence.UNKNOWN;
    }

    public boolean isConstant() {
	return constantValueRef.get() != ConstantValueVisitor.NOT_CONSTANT;
    }

    public Object constantValue() {
	Object v = constantValueRef.get();
	if (v == ConstantValueVisitor.NOT_CONSTANT)
	    return null;
	return v;
    }

    public String stringValue() {
	return (String) constantValue();
    }

    public boolean booleanValue() {
	return ((Boolean) constantValue()).booleanValue();
    }

    public byte byteValue() {
	return ((Byte) constantValue()).byteValue();
    }

    public short shortValue() {
	return ((Short) constantValue()).shortValue();
    }

    public char charValue() {
	return ((Character) constantValue()).charValue();
    }

    public int intValue() {
	return ((Integer) constantValue()).intValue();
    }

    public long longValue() {
	return ((Long) constantValue()).longValue();
    }

    public float floatValue() {
	return ((Float) constantValue()).floatValue();
    }

    public double doubleValue() {
	return ((Double) constantValue()).doubleValue();
    }
}
