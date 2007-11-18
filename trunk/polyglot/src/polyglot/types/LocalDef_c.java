/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.Position;

/**
 * A <code>LocalInstance</code> contains type information for a local variable.
 */
public class LocalDef_c extends VarDef_c implements LocalDef
{
    /** Used for deserializing types. */
    protected LocalDef_c() { }

    public LocalDef_c(TypeSystem ts, Position pos,
	  		   Flags flags, Ref<? extends Type> type, String name) {
        super(ts, pos, flags, type, name);
    }
    
    public LocalType asType() {
        return new LocalType_c(ts, position(), Ref_c.<LocalDef>ref(this));
    }

    public boolean equalsImpl(TypeObject o) {
        return this == o;
    }

    public String toString() {
        return "local " + flags.translate() + type + " " + name +
	    (constantValue != null ? (" = " + constantValue) : "");
    }
}
