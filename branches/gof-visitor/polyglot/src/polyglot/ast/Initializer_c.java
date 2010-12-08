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

import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.ExceptionCheckerContext;
import polyglot.visit.NodeVisitor;

/**
 * An <code>Initializer</code> is an immutable representation of an
 * initializer block in a Java class (which appears outside of any
 * method).  Such a block is executed before the code for any of the
 * constructors.  Such a block can optionally be static, in which case
 * it is executed when the class is loaded.
 */
public class Initializer_c extends Term_c implements Initializer
{
    protected FlagsNode flags;
    protected Block body;
    protected InitializerDef ii;

    public Initializer_c(Position pos, FlagsNode flags, Block body) {
	super(pos);
	assert(flags != null && body != null);
	this.flags = flags;
	this.body = body;
    }
    
    public List<Def> defs() {
        return Collections.<Def>singletonList(ii);
    }

    public MemberDef memberDef() {
        return ii;
    }

    /** Get the flags of the initializer. */
    public FlagsNode flags() {
	return this.flags;
    }

    /** Set the flags of the initializer. */
    public Initializer flags(FlagsNode flags) {
	Initializer_c n = (Initializer_c) copy();
	n.flags = flags;
	return n;
    }

    /** Get the initializer instance of the initializer. */
    public InitializerDef initializerDef() {
        return ii;
    }

    public CodeDef codeDef() {
	return initializerDef();
    }

    /** Set the initializer instance of the initializer. */
    public Initializer initializerDef(InitializerDef ii) {
        if (ii == this.ii) return this;
	Initializer_c n = (Initializer_c) copy();
	n.ii = ii;
	return n;
    }

    public Term codeBody() {
        return this.body;
    }
    
    /** Get the body of the initializer. */
    public Block body() {
	return this.body;
    }

    /** Set the body of the initializer. */
    public CodeBlock body(Block body) {
	Initializer_c n = (Initializer_c) copy();
	n.body = body;
	return n;
    }

    /** Reconstruct the initializer. */
    protected Initializer_c reconstruct(FlagsNode flags, Block body) {
	if (flags != this.flags || body != this.body) {
	    Initializer_c n = (Initializer_c) copy();
	    n.flags = flags;
	    n.body = body;
	    return n;
	}

	return this;
    }

    /** Visit the children of the initializer. */
    public Node visitChildren(NodeVisitor v) {
	FlagsNode flags = (FlagsNode) visitChild(this.flags, v);
	Block body = (Block) visitChild(this.body, v);
	return reconstruct(flags, body);
    }

    public Context enterScope(Context c) {
	return c.pushCode(ii);
    }

    public InitializerDef createInitializerDef(TypeSystem ts, ClassDef ct, Flags flags) {
	InitializerDef ii = ts.initializerDef(position(), Types.ref(ct.asType()), flags);
	return ii;
    }

    public Node visitSignature(NodeVisitor v) {
        return this;
    }

    public void dump(CodeWriter w) {
	super.dump(w);

	if (ii != null) {
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(instance " + ii + ")");
	    w.end();
	}
    }

    public String toString() {
	return flags.flags().translate() + "{ ... }";
    }
    

}
