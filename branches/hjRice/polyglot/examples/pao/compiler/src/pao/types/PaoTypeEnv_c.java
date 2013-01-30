/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package pao.types;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.types.TypeSystem_c;
import polyglot.frontend.Source;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

/**
 * Implementation of the PAO type system interface. Also overrides some
 * methods of <code>TypeSystem_c</code>.
 */
public class PaoTypeEnv_c extends TypeEnv_c {
    public PaoTypeEnv_c(Context context) {
        super(context);
    }

    /**
     * Returns <code>true</code> if normal casting rules permit this cast, or
     * if <code>fromType</code> is <code>Object</code> and the 
     * <code>toType</code> is a primitive or vice-versa.
     * 
     * @see polyglot.types.ClassType_c#isCastValidImpl(Type)
     */
    public boolean isCastValid(Type fromType, Type toType) {
        if (toType.isPrimitive() && typeEquals(fromType, ts.Object()))
            return true;
        if (fromType.isPrimitive() && typeEquals(toType, ts.Object()))
            return true;
        return super.isCastValid(fromType, toType);
    }

    /**
     * Returns <code>true</code> if normal subtyping rules apply, or
     * if <code>child</code> is a primitive and <code>ancestor</code>
     * is <code>Object</code>.
     */
    public boolean isSubtype(Type child, Type ancestor) {
        if (child.isPrimitive() && typeEquals(ancestor, ts.Object()))
            return true;
        return super.isSubtype(child, ancestor);
    }

    /**
     * Returns <code>true</code> if the normal rules for implicit casts
     * hold, or if casting a primitive to <code>Object</code>.
     */
    public boolean isImplicitCastValid(Type fromType, Type toType) {
        if (fromType.isPrimitive() && typeEquals(toType, ts.Object()))
            return true;
        return super.isImplicitCastValid(fromType, toType);
    }
}
