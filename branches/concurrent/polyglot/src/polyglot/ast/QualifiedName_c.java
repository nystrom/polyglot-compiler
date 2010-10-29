/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ExceptionCheckerContext;
import polyglot.visit.NodeVisitor;

/**
 * An <code>AmbPrefix</code> is an ambiguous AST node composed of dot-separated
 * list of identifiers that must resolve to a prefix.
 */
public class QualifiedName_c extends Node_c implements QualifiedName
{
    protected Node prefix;
    protected Id name;

    public QualifiedName_c(Position pos, Node prefix, Id name) {
        super(pos);
        assert(name != null); 
        this.prefix = prefix;
        this.name = name;
    }
    
    /** Get the name of the prefix. */
    public Id name() {
        return this.name;
    }
    
    /** Set the name of the prefix. */
    public QualifiedName name(Id name) {
        QualifiedName_c n = (QualifiedName_c) copy();
        n.name = name;
        return n;
    }

    /** Get the prefix of the prefix. */
    public Node prefix() {
	return this.prefix;
    }

    /** Set the prefix of the prefix. */
    public QualifiedName prefix(Node prefix) {
	QualifiedName_c n = (QualifiedName_c) copy();
	n.prefix = prefix;
	return n;
    }

    /** Reconstruct the prefix. */
    protected QualifiedName_c reconstruct(Node prefix, Id name) {
	if (prefix != this.prefix || name != this.name) {
	    QualifiedName_c n = (QualifiedName_c) copy();
	    n.prefix = prefix;
            n.name = name;
	    return n;
	}

	return this;
    }

    /** Visit the children of the prefix. */
    public Node visitChildren(NodeVisitor v) {
	Node prefix = (Node) visitChild(this.prefix, v);
        Id name = (Id) visitChild(this.name, v);
        return reconstruct(prefix, name);
    }

    public String toString() {
	return (prefix == null
		? name.toString()
		: prefix.toString() + "." + name.toString()) + "{amb}";
    }
}
