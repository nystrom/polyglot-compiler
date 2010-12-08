/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.lang.reflect.Array;

import polyglot.frontend.Globals;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * An <code>Assign</code> represents a Java assignment expression.
 */
public class Assign_c extends Expr_c implements Assign
{
    protected Expr left;
    protected Operator op;
    protected Expr right;

    public Assign_c(Position pos, Expr left, Operator op, Expr right) {
	super(pos);
	assert(left != null && op != null && right != null);
	this.left = left;
	this.op = op;
	this.right = right;
    }

    /** Get the left operand of the expression. */
    public Expr left() {
	return this.left;
    }

    /** Set the left operand of the expression. */
    public Assign left(Expr left) {
	assert left != null;
	if (left == this.left) return this;
	Assign_c n = (Assign_c) copy();
	n.left = left;
	return n;
    }

    /** Get the operator of the expression. */
    public Operator operator() {
	return this.op;
    }

    /** Set the operator of the expression. */
    public Assign operator(Operator op) {
	Assign_c n = (Assign_c) copy();
	n.op = op;
	return n;
    }

    /** Get the right operand of the expression. */
    public Expr right() {
	return this.right;
    }

    /** Set the right operand of the expression. */
    public Assign right(Expr right) {
	assert right != null;
	if (right == this.right) return this;
	Assign_c n = (Assign_c) copy();
	n.right = right;
	return n;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	Expr left = (Expr) visitChild(this.left, v);
	Expr right = (Expr) visitChild(this.right, v);
	return left(left).right(right);
    }

    public String toString() {
	return left + " " + op + " " + right;
    }

    /** Dumps the AST. */
    public void dump(CodeWriter w) {
	super.dump(w);
	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(operator " + op + ")");
	w.end();
    }
}
