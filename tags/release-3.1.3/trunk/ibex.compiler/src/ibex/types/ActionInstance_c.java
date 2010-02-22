/*
 * Created on Mar 1, 2007
 */
/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package ibex.types;

import java.util.Iterator;
import java.util.List;

import polyglot.types.CodeInstance;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.Use_c;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;
import polyglot.util.TransformingList;

public class ActionInstance_c extends Use_c<ActionDef> implements ActionInstance {
    public ActionInstance_c(TypeSystem ts, Position pos, Ref<? extends ActionDef> def) {
        super(ts, pos, def);
    }

    public String toString() {
	return "() => " + returnType();
    }
    
    public Type returnType() {
        return Types.get(def().returnType());
    }

    public List<Type> throwTypes() {
        return new TransformingList<Ref<? extends Type>, Type>(def().throwTypes(), new polyglot.types.DerefTransform<Type>());
    }

    public boolean throwsSubset(ActionInstance p) {
        SubtypeSet s1 = new SubtypeSet(ts.Throwable());
        SubtypeSet s2 = new SubtypeSet(ts.Throwable());

        s1.addAll(this.throwTypes());
        s2.addAll(p.throwTypes());

        for (Iterator<Type> i = s1.iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            if (! ts.isUncheckedException(t) && ! s2.contains(t)) {
                return false;
            }
        }

        return true;
    }

    public boolean throwsSubset(CodeInstance<ActionDef> pi) {
        if (pi instanceof ActionInstance)
            return throwsSubset((ActionInstance) pi);
        return false;
    }
}
