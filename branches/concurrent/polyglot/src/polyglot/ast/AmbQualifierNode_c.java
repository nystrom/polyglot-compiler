/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ExceptionCheckerContext;
import polyglot.visit.NodeVisitor;

/**
 * An <code>AmbQualifierNode</code> is an ambiguous AST node composed of
 * dot-separated list of identifiers that must resolve to a type qualifier.
 */
public class AmbQualifierNode_c extends Node_c implements AmbQualifierNode {
    protected Ref<Qualifier> qualifier;

    Node child;

    public AmbQualifierNode_c(Position pos, Node name) {
	super(pos);
	child = name;
    }

    public Ref<Qualifier> qualifierRef() {
	return this.qualifier;
    }

    public Qualifier qualifier() {
	return qualifierRef().get();
    }
    
    public AmbQualifierNode qualifier(Ref<Qualifier> q) {
	AmbQualifierNode_c n = (AmbQualifierNode_c) copy();
	n.qualifier = q;
	return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
	Node child = (Node) visitChild(this.child, v);
	return child(child);
    }

    public AmbQualifierNode_c child(Node qname2) {
	if (qname2 == this.child)
	    return this;
	AmbQualifierNode_c n = (AmbQualifierNode_c) copy();
	n.child = qname2;
	return n;
    }

    public Node child() {
	return child;
    }

    @Override
    public String toString() {
	return child.toString();
    }
}
