/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * An <code>AmbReceiver</code> is an ambiguous AST node composed of
 * dot-separated list of identifiers that must resolve to a receiver.
 */
public class AmbReceiver_c extends Term_c implements AmbReceiver
{
    protected Node child;

    public AmbReceiver_c(Position pos, Node child) {
      super(pos);
      assert(child != null);
      this.child = child;
    }
    
    public Node child() {
        return this.child;
    }
    
    public AmbReceiver child(Node child) {
	AmbReceiver_c n = (AmbReceiver_c) copy();
        n.child = child;
        return n;
    }

    /** Reconstruct the expression. */
    protected AmbReceiver_c reconstruct(Node child) {
        if (child != this.child) {
            AmbReceiver_c n = (AmbReceiver_c) copy();
            n.child = child;
            return n;
        }
        return this;
    }
    
    /** Visit the children of the constructor. */
    public Node visitChildren(NodeVisitor v) {
        Node child = (Node) visitChild(this.child, v);
        return reconstruct(child);
    }

    public String toString() {
      return child.toString() + "{amb}";
    }
    
    Type type;

    public Type type() {
	if (child instanceof Typed) {
	    return ((Typed) child).type();
	}
	return this.type;
    }

    public AmbReceiver type(Type type) {
	AmbReceiver_c n = (AmbReceiver_c) copy();
	n.type = type;
	return n;
    }
}
