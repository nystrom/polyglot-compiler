/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * A <code>VarInstance</code> contains type information for a variable.  It may
 * be either a local or a field.
 */
public abstract class VarDef_c extends Def_c implements VarDef
{
    protected Flags flags;
    protected Ref<? extends Type> type;
    protected String name;
    protected Object constantValue;
    protected boolean isConstant;
    protected boolean constantValueSet;

    /** Used for deserializing types. */
    protected VarDef_c() { }

    public VarDef_c(TypeSystem ts, Position pos,
	                 Flags flags, Ref<? extends Type> type, String name) {
        super(ts, pos);
	this.flags = flags;
	this.type = type;
	this.name = name;
    }
    
    public boolean constantValueSet() {
        return constantValueSet;
    }
    
    public boolean isConstant() {
        if (! constantValueSet) {
            if (! flags.isFinal()) {
                setNotConstant();
                return isConstant;
            }
        }
        return isConstant;
    }

    public Object constantValue() {
        if (isConstant()) {
            return constantValue;
        }
        return null;
    }

    public Flags flags() {
        return flags;
    }
    
    public Ref<? extends Type> type() {
        return type;
    }

    public String name() {
        return name;
    }

    public int hashCode() {
        return flags.hashCode() + name.hashCode();
    }

    public void setType(Ref<? extends Type> type) {
        this.type = type;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }
    
    /** Destructive update of constant value. */
    public void setConstantValue(Object constantValue) {
        if (! (constantValue == null) &&
                ! (constantValue instanceof Boolean) &&
                ! (constantValue instanceof Number) &&
                ! (constantValue instanceof Character) &&
                ! (constantValue instanceof String)) {
            
            throw new InternalCompilerError(
            "Can only set constant value to a primitive or String.");
        }

        this.constantValue = constantValue;
        this.isConstant = true;
        this.constantValueSet = true;
    }
    
    public void setNotConstant() {
        this.constantValue = null;
        this.isConstant = false;
        this.constantValueSet = true;
    }
    
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
}
