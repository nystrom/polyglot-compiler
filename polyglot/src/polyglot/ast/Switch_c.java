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

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.util.*;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Switch</code> is an immutable representation of a Java
 * <code>switch</code> statement.  Such a statement has an expression which
 * is evaluated to determine where to branch to, an a list of labels
 * and block statements which are conditionally evaluated.  One of the
 * labels, rather than having a constant expression, may be lablled
 * default.
 */
public class Switch_c extends Stmt_c implements Switch
{
    protected Expr expr;
    protected List<SwitchElement> elements;

    public Switch_c(Position pos, Expr expr, List<SwitchElement> elements) {
	super(pos);
	assert(expr != null && elements != null);
	this.expr = expr;
	this.elements = TypedList.copyAndCheck(elements, SwitchElement.class, true);
    }

    /** Get the expression to switch on. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression to switch on. */
    public Switch expr(Expr expr) {
	Switch_c n = (Switch_c) copy();
	n.expr = expr;
	return n;
    }

    /** Get the switch elements of the statement. */
    public List<SwitchElement> elements() {
	return Collections.unmodifiableList(this.elements);
    }

    /** Set the switch elements of the statement. */
    public Switch elements(List<SwitchElement> elements) {
	Switch_c n = (Switch_c) copy();
	n.elements = TypedList.copyAndCheck(elements, SwitchElement.class, true);
	return n;
    }

    /** Reconstruct the statement. */
    protected Switch_c reconstruct(Expr expr, List<SwitchElement> elements) {
	if (expr != this.expr || ! CollectionUtil.allEqual(elements, this.elements)) {
	    Switch_c n = (Switch_c) copy();
	    n.expr = expr;
	    n.elements = TypedList.copyAndCheck(elements, SwitchElement.class, true);
	    return n;
	}

	return this;
    }

    public Context enterChildScope(Node child, Context c) {
	if (child != this.expr) {
	    Name label = null;
	    c = c.pushBreakLabel(label);
	}
            
        return super.enterChildScope(child, c);
    }

    public Context enterScope(Context c) {
        return c.pushBlock();
    }

    /** Visit the children of the statement. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v);
	List<SwitchElement> elements = visitList(this.elements, v);
	return reconstruct(expr, elements);
    }

    public String toString() {
	return "switch (" + expr + ") { ... }";
    }

}
