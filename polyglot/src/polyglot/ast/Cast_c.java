/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Cast</code> is an immutable representation of a casting
 * operation.  It consists of an <code>Expr</code> being cast and a
 * <code>TypeNode</code> being cast to.
 */ 
public class Cast_c extends Expr_c implements Cast
{
    protected TypeNode castType;
    protected Expr expr;

    public Cast_c(Position pos, TypeNode castType, Expr expr) {
	super(pos);
	assert(castType != null && expr != null);
	this.castType = castType;
	this.expr = expr;
    }

    /** Get the cast type of the expression. */
    public TypeNode castType() {
	return this.castType;
    }

    /** Set the cast type of the expression. */
    public Cast castType(TypeNode castType) {
	Cast_c n = (Cast_c) copy();
	n.castType = castType;
	return n;
    }

    /** Get the expression being cast. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression being cast. */
    public Cast expr(Expr expr) {
	Cast_c n = (Cast_c) copy();
	n.expr = expr;
	return n;
    }

    /** Reconstruct the expression. */
    protected Cast_c reconstruct(TypeNode castType, Expr expr) {
	if (castType != this.castType || expr != this.expr) {
	    Cast_c n = (Cast_c) copy();
	    n.castType = castType;
	    n.expr = expr;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	TypeNode castType = (TypeNode) visitChild(this.castType, v);
	Expr expr = (Expr) visitChild(this.expr, v);
	return reconstruct(castType, expr);
    }

    public String toString() {
	return "(" + castType + ") " + expr;
    }
}
