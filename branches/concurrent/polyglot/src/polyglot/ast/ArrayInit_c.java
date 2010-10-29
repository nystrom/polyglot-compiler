/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Iterator;
import java.util.List;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * An <code>ArrayInit</code> is an immutable representation of
 * an array initializer, such as { 3, 1, { 4, 1, 5 } }.  Note that
 * the elements of these array may be expressions of any type (e.g.,
 * <code>Call</code>).
 */
public class ArrayInit_c extends Expr_c implements ArrayInit
{
    protected List<Expr> elements;

    public ArrayInit_c(Position pos, List<Expr> elements) {
	super(pos);
	assert(elements != null);
	this.elements = TypedList.copyAndCheck(elements, Expr.class, true);
    }

    /** Get the elements of the initializer. */
    public List<Expr> elements() {
	return this.elements;
    }

    /** Set the elements of the initializer. */
    public ArrayInit elements(List<Expr> elements) {
	ArrayInit_c n = (ArrayInit_c) copy();
	n.elements = TypedList.copyAndCheck(elements, Expr.class, true);
	return n;
    }

    /** Reconstruct the initializer. */
    protected ArrayInit_c reconstruct(List<Expr> elements) {
	if (! CollectionUtil.allEqual(elements, this.elements)) {
	    ArrayInit_c n = (ArrayInit_c) copy();
	    n.elements = TypedList.copyAndCheck(elements, Expr.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the initializer. */
    public Node visitChildren(NodeVisitor v) {
	List<Expr> elements = visitList(this.elements, v);
	return reconstruct(elements);
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
		if (elements.isEmpty()) {
            return child.type();
        }

        Type t = av.toType();

        if (! t.isArray()) {
            throw new InternalCompilerError("Type of array initializer must " +
                                            "be an array.", position());
        }

        t = t.toArray().base();

        TypeSystem ts = av.typeSystem();

	for (Iterator i = elements.iterator(); i.hasNext(); ) {
	    Expr e = (Expr) i.next();

            if (e == child) {
                if (ts.numericConversionValid(t, e.constantValue(), av.context())) {
                    return child.type();
                }
                else {
                    return t;
                }
            }
        }

        return child.type();
    }

    public String toString() {
	return "{ ... }";
    }

    /** Write the initializer to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	w.write("{ ");

	for (Iterator<Expr> i = elements.iterator(); i.hasNext(); ) {
	    Expr e = i.next();
	    
	    print(e, w, tr);

	    if (i.hasNext()) {
		w.write(",");
                w.allowBreak(0, " ");
	    }
	}

	w.write(" }");
    }

    public Term firstChild() {
        return listChild(elements, null);
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFGList(elements, this, EXIT);
        return succs;
    }
}
