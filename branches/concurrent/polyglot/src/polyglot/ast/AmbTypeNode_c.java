/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * An <code>AmbTypeNode</code> is an ambiguous AST node composed of
 * dot-separated list of identifiers that must resolve to a type.
 */
public class AmbTypeNode_c extends TypeNode_c implements AmbTypeNode {
    protected Node child;

    public AmbTypeNode_c(Position pos, Node child) {
      super(pos);
      assert(child != null);
      this.child = child;
    }
    
    public Node child() {
        return this.child;
    }
    
    public AmbTypeNode child(Node child) {
	AmbTypeNode_c n = (AmbTypeNode_c) copy();
        n.child = child;
        return n;
    }

    /** Reconstruct the expression. */
    protected AmbTypeNode_c reconstruct(Node child) {
        if (child != this.child) {
            AmbTypeNode_c n = (AmbTypeNode_c) copy();
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
}
