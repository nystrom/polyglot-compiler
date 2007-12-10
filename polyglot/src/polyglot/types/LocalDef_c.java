/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.main.Report;
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

    protected transient LocalInstance asInstance;

    public LocalInstance asInstance() {
        if (Report.should_report("asi", 1)) asInstance = null;
        if (asInstance == null) {
            asInstance = ts.createLocalInstance(position(), Types.ref(this));
        }
        return asInstance;
    }

    public String toString() {
        return "local " + flags.translate() + type + " " + name +
	    (constantValue != null ? (" = " + constantValue) : "");
    }
}
