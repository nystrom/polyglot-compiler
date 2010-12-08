/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

/**
 * An <code>AmbTypeNode</code> is an ambiguous AST node composed of
 * dot-separated list of identifiers that must resolve to a type.
 */
public interface AmbTypeNode extends TypeNode
{
    Node child();
    AmbTypeNode child(Node child);
}
