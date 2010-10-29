/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import polyglot.ast.Node;
import polyglot.dispatch.NewTranslator;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;

/**
 * TypedTranslator extends Translator for type-directed code generation.
 * The base Translator uses types only to generate more readable code.
 * If an ambiguous or untyped AST node is encountered, code generation
 * continues. In contrast, with TypedTranslator, encountering an
 * ambiguous or untyped node is considered internal compiler error.
 * TypedTranslator should be used when the output AST is expected to be
 * (or required to be) type-checked before code generation.
 */
public class TypedTranslator extends NewTranslator {
    
    public TypedTranslator(Translator tr, CodeWriter w) {
	super(tr, w);
    }

    public void print(Node parent, Node child) {
        if (tr.context() == null) {
            throw new InternalCompilerError("Null context found during type-directed code generation.", child.position());
        }
        
        super.print(parent, child);
    }
}
