/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.List;

import polyglot.util.Position;

/**
 * A <code>ConstructorInstance</code> contains type information for a
 * constructor.
 */
public class ConstructorDef_c extends ProcedureDef_c
                                implements ConstructorDef
{
    /** Used for deserializing types. */
    protected ConstructorDef_c() { }

    public ConstructorDef_c(TypeSystem ts, Position pos,
	                         Ref<? extends ClassType> container,
				 Flags flags, List<Ref<? extends Type>> formalTypes, List<Ref<? extends Type>> excTypes) {
        super(ts, pos, container, flags, formalTypes, excTypes);
    }
    
    public ConstructorInstance asReference() {
        return new ConstructorInstance_c(ts, position(), Ref_c.<ConstructorDef>ref(this));
    }

    public String toString() {
	return designator() + " " + flags.translate() + signature();
    }
    
    public String signature() {
        return container + "(" + TypeSystem_c.listToString(formalTypes) + ")";
    }

    public String designator() {
        return "constructor";
    }
}
