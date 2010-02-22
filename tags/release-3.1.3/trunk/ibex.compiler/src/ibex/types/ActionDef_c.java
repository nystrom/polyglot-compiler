/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package ibex.types;

import java.util.Collections;
import java.util.List;

import polyglot.types.CodeInstance;
import polyglot.types.Def_c;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.util.TypedList;

public class ActionDef_c extends Def_c implements ActionDef {

    protected Ref<? extends Type> returnType;
    protected List<Ref<? extends Type>> throwTypes;
    protected CodeInstance<?> asInstance;

    public ActionDef_c(TypeSystem ts, Position pos, Ref<? extends Type> returnType, List<Ref<? extends Type>> throwTypes) {
        super(ts, pos);
        this.returnType = returnType;
        this.throwTypes = TypedList.copyAndCheck(throwTypes, Ref.class, true);
    }

    protected boolean inferReturnType;

    public boolean inferReturnType() {
        return inferReturnType;
    }

    public void inferReturnType(boolean r) {
        this.inferReturnType = r;
    }

    public ActionInstance asInstance() {
        if (asInstance == null) {
            asInstance = new ActionInstance_c(ts, position(), Types.ref(this));
        }
        return (ActionInstance) asInstance;
    }

    public Ref<? extends Type> returnType() {
        return returnType;
    }

    public void setReturnType(Ref<? extends Type> returnType) {
        assert returnType != null;
        this.returnType = returnType;
    }

    public List<Ref<? extends Type>> throwTypes() {
        return Collections.unmodifiableList(throwTypes);
    }

    /**
     * @param throwTypes
     *            The throwTypes to set.
     */
    public void setThrowTypes(List<Ref<? extends Type>> throwTypes) {
        this.throwTypes = TypedList.copyAndCheck(throwTypes, Ref.class, true);
    }
    public String toString() {
        return "() => " + returnType();
    }
}
