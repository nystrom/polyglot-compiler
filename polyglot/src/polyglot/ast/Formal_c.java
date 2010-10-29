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
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Formal</code> represents a formal parameter for a procedure
 * or catch block.  It consists of a type and a variable identifier.
 */
public class Formal_c extends Term_c implements Formal
{
    protected LocalDef li;
    protected FlagsNode flags;
    protected TypeNode type;
    protected Id name;
//    protected boolean reachable;

    public Formal_c(Position pos, FlagsNode flags, TypeNode type,
                    Id name)
    {
	super(pos);
	assert(flags != null && type != null && name != null);
        this.flags = flags;
        this.type = type;
        this.name = name;
    }

    public List<Def> defs() {
        return Collections.<Def>singletonList(li);
    }

    /** Get the type of the formal. */
    public Type declType() {
        return type.type();
    }

    /** Get the flags of the formal. */
    public FlagsNode flags() {
	return flags;
    }

    /** Set the flags of the formal. */
    public Formal flags(FlagsNode flags) {
	Formal_c n = (Formal_c) copy();
	n.flags = flags;
	return n;
    }

    /** Get the type node of the formal. */
    public TypeNode typeNode() {
	return type;
    }

    /** Set the type node of the formal. */
    public Formal typeNode(TypeNode type) {
	Formal_c n = (Formal_c) copy();
	n.type = type;
	return n;
    }
    
    /** Get the name of the formal. */
    public Id name() {
        return name;
    }
    
    /** Set the name of the formal. */
    public Formal name(Id name) {
        Formal_c n = (Formal_c) copy();
        n.name = name;
        return n;
    }

    /** Get the local instance of the formal. */
    public LocalDef localDef() {
        return li;
    }

    /** Set the local instance of the formal. */
    public Formal localDef(LocalDef li) {
        if (li == this.li) return this;
        Formal_c n = (Formal_c) copy();
	n.li = li;
	return n;
    }

    /** Reconstruct the formal. */
    protected Formal_c reconstruct(FlagsNode flags, TypeNode type, Id name) {
	if (flags != this.flags || this.type != type || name != this.name) {
	    Formal_c n = (Formal_c) copy();
	    n.flags = flags;
	    n.type = type;
            n.name = name;
	    return n;
	}

	return this;
    }

    /** Visit the children of the formal. */
    public Node visitChildren(NodeVisitor v) {
	FlagsNode flags = (FlagsNode) visitChild(this.flags, v);
	TypeNode type = (TypeNode) visitChild(this.type, v);
        Id name = (Id) visitChild(this.name, v);
	return reconstruct(flags, type, name);
    }

    public void addDecls(Context c) {
        c.addVariable(li.asInstance());
    }

    public void dump(CodeWriter w) {
	super.dump(w);

	if (li != null) {
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(instance " + li + ")");
	    w.end();
	}

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(name " + name + ")");
	w.end();
    }

    public String toString() {
        return flags.flags().translate() + type + " " + name;
    }

}
