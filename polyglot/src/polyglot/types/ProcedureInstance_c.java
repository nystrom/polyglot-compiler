package polyglot.types;

import java.util.Iterator;
import java.util.List;

import polyglot.util.*;

public class ProcedureInstance_c<T extends ProcedureDef> extends Use_c<T> implements ProcedureInstance<T> {
    ProcedureInstance_c(TypeSystem ts, Position pos, Ref<T> def) {
        super(ts, pos, def);
    }

    /**
     * Returns whether <code>this</code> is <i>more specific</i> than
     * <code>p</code>, where <i>more specific</i> is defined as JLS
     * 15.12.2.2.
     *<p>
     * <b>Note:</b> There is a fair amount of guesswork since the JLS
     * does not include any info regarding Java 1.2, so all inner class
     * rules are found empirically using jikes and javac.
     */
    public boolean moreSpecific(ProcedureInstance<T> p) {
        ProcedureInstance<T> p1 = this;
        ProcedureInstance<T> p2 = p;

        // rule 1:
        Type t1 = null;
        Type t2 = null;
        
        if (p1 instanceof MemberInstance) {
            t1 = ((MemberInstance<T>) p1).container();
        }
        if (p2 instanceof MemberInstance) {
            t2 = ((MemberInstance<T>) p2).container();
        }
        
        if (t1 != null && t2 != null) {
            if (t1.isClass() && t2.isClass()) {
                if (! t1.isSubtype(t2) &&
                        ! t1.toClass().isEnclosed(t2.toClass())) {
                    return false;
                }
            }
            else {
                if (! t1.isSubtype(t2)) {
                    return false;
                }
            }
        }

        // rule 2:
        return p2.callValid(p1.formalTypes());
    }

    /** Returns true if the procedure has the given formal parameter types. */
    public boolean hasFormals(List<Type> formalTypes) {
        List<Type> l1 = this.formalTypes();
        List<Type> l2 = formalTypes;

        Iterator<Type> i1 = l1.iterator();
        Iterator<Type> i2 = l2.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            Type t1 = (Type) i1.next();
            Type t2 = (Type) i2.next();

            if (! ts.equals(t1, t2)) {
                return false;
            }
        }

        return ! (i1.hasNext() || i2.hasNext());
    }

    /** Returns true iff <code>this</code> throws fewer exceptions than
     * <code>p</code>. */
    public boolean throwsSubset(ProcedureInstance<T> p) {
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

    /** Returns true if a call can be made with the given argument types. */
    public boolean callValid(List<Type> argTypes) {
        List<Type> l1 = this.formalTypes();
        List<Type> l2 = argTypes;

        Iterator<Type> i1 = l1.iterator();
        Iterator<Type> i2 = l2.iterator();

        while (i1.hasNext() && i2.hasNext()) {
            Type t1 = (Type) i1.next();
            Type t2 = (Type) i2.next();

            if (! ts.isImplicitCastValid(t2, t1)) {
                return false;
            }
        }

        return ! (i1.hasNext() || i2.hasNext());
    }

    public String designator() {
        return def().designator();
    }
    
    public String signature() {
        return def().signature();
    }

    static class Force<T extends TypeObject> implements Transformation<Ref<? extends T>,T> {
        public T transform(Ref<? extends T> o) {
            return TypeObject_c.get(o);
        }
    }

    public List<Type> formalTypes() {
        return new TransformingList<Ref<? extends Type>, Type>(def().formalTypes(), new Force<Type>());
    }

    public List<Type> throwTypes() {
        return new TransformingList<Ref<? extends Type>, Type>(def().throwTypes(), new Force<Type>());
    }

}
