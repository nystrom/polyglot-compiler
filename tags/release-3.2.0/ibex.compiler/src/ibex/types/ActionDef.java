/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package ibex.types;

import java.util.List;

import polyglot.types.CodeDef;
import polyglot.types.Def;
import polyglot.types.Ref;
import polyglot.types.Type;

public interface ActionDef extends CodeDef, Def {
    
    ActionInstance asInstance();
    
    /** Set a flag indicating we should infer the return type. */
    boolean inferReturnType();
    void inferReturnType(boolean r);
    
    public Ref<? extends Type> returnType();
    public void setReturnType(Ref<? extends Type> type);
    
    public List<Ref<? extends Type>> throwTypes();
    public void setThrowTypes(List<Ref<? extends Type>> l);
}
