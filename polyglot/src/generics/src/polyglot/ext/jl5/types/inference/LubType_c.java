package polyglot.ext.jl5.types.inference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.ext.jl5.types.AnySubType;
import polyglot.ext.jl5.types.AnySuperType;
import polyglot.ext.jl5.types.AnyType;
import polyglot.ext.jl5.types.GenericTypeRef;
import polyglot.ext.jl5.types.IntersectionType;
import polyglot.ext.jl5.types.IntersectionType_c;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.JL5TypeSystem_c;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.RawType;
import polyglot.ext.jl5.types.Wildcard;
import polyglot.types.ClassType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public class LubType_c extends IntersectionType_c implements LubType {

    protected JL5TypeSystem ts;
    protected List<ClassType> lubElems;
    public LubType_c(TypeSystem ts, List<ClassType> lubElems) {
        super(ts, null);
        this.lubElems = lubElems;
        this.ts = (JL5TypeSystem) ts;
    }

    public List<ClassType> lubElements() {
        return lubElems;
    }
    
    protected IntersectionType lubCalculated = null;
    public IntersectionType calculateLub() {
        if (lubCalculated == null) {
            lubCalculated = lub_force();
        }
        return lubCalculated;
    }
    
    @Override
    public List<ReferenceType> bounds() {
        return calculateLub().bounds();
    }

    @Override
    public Kind kind() {
        return LUB;
    }

    
    private Type lub(Type... a) {
        List<ClassType> l = new ArrayList<ClassType>();
        for (Type t : a) {
            l.add((ClassType) t);
        }
        return ts.lubType(l);
    }

    private IntersectionType lub_force() {
        Set<Type> st = new HashSet<Type>();
        Set<Type> est = null;
        for (Type u : lubElems) {
            List<Type> u_supers = new ArrayList<Type>(ts.allAncestorsOf((ReferenceType) u));
            st.addAll(u_supers);
            Set<Type> est_of_u = new HashSet<Type>();
            for (Type super_of_u : u_supers) {
                if (super_of_u instanceof GenericTypeRef) {
                    GenericTypeRef g = (GenericTypeRef) super_of_u;
                    est_of_u.add(g.baseType());
                }
                else est_of_u.add(super_of_u);
            }
            if (est == null) {
                est = new HashSet<Type>();
                est.addAll(est_of_u);
            }
            else {
                est.retainAll(est_of_u);
            }
        }
        Set<Type> mec = new HashSet<Type>(est);
        for (Type e1 : est) {
            for (Type e2 : est) {
                if (!ts.equals(e1,e2) && ts.isSubtype(e2, e1)) {
                    mec.remove(e1);
                    break;
                }
            }
        }
        List<ReferenceType> cand = new ArrayList<ReferenceType>();
        for (Type m : mec) {
            List<Type> inv = new ArrayList<Type>();
            for (Type t : st) {
                if ( ts.equals(m, t) || 
                    ( (t instanceof GenericTypeRef) && ((GenericTypeRef)t).baseType().equals(m) ) ) {
                    inv.add(t);
                }
            }
            cand.add((ReferenceType) lci(inv));
        }
        try {
            if (ts.checkIntersectionBounds(cand, true)) {
                return ts.intersectionType(cand);
            }
        } catch (SemanticException e) {
        }
        return ts.intersectionType(null);
    }

    private Type lci(List<Type> inv) {
        Type first = inv.get(0);
        if (inv.size() == 1 || first instanceof RawType) return first;
        ParameterizedType res = (ParameterizedType) first;
        for (int i = 1; i < inv.size(); i++) {
            Type next = inv.get(i);
            if (next instanceof RawType) return next;
            List<Type> lcta_args = new ArrayList<Type>();
            ParameterizedType nextp = (ParameterizedType) next;
            for (int argi = 0; argi < res.typeArguments().size(); argi++) {
                Type a1 = res.typeArguments().get(argi);
                Type a2 = nextp.typeArguments().get(argi);
                lcta_args.add(lcta(a1,a2));
            }
            res = ts.parameterizedType(res.baseType());
            res.typeArguments(lcta_args);
        }
        return res;
    }

    private Type lcta(Type a1, Type a2) {
        if (!(a1 instanceof Wildcard)) {
            if (!(a2 instanceof Wildcard)) {
                if (ts.equals(a1, a2)) return a1;
                else return ts.anySubType((ReferenceType) lub(a1,a2));
            }
            else if (a2 instanceof Wildcard) {
                Wildcard a2wc = (Wildcard) a2;
                if (a2wc instanceof AnyType) return a2wc;
                if (a2wc instanceof AnySubType) return ts.anySubType((ReferenceType) lub(a1,a2wc.bound()));
                if (a2wc instanceof AnySuperType) return ts.anySuperType((ReferenceType) glb(a1,a2wc.bound()));
            }
        }
        else {
            Wildcard a1wc = (Wildcard) a1;
            if (!(a2 instanceof Wildcard)) return lub(a1wc.bound(), a2);
            Wildcard a2wc = (Wildcard) a2;
            if ((a1wc instanceof AnyType) || (a2wc instanceof AnyType)) return ts.anyType();
            if ((a1wc instanceof AnySubType) && (a2wc instanceof AnySubType)) return ts.anySubType((ReferenceType) lub(a1wc.bound(), a2wc.bound()));
            if ((a1wc instanceof AnySuperType) && (a2wc instanceof AnySuperType)) return ts.anySuperType((ReferenceType) glb(a1wc.bound(), a2wc.bound()));
            if (ts.equals(a1wc.bound(),a2wc.bound())) return a1wc.bound();
            return ts.anyType();
        }
        return ts.anyType();
    }

    private Type glb(Type t1, Type t2) {
        List<ReferenceType> l = new ArrayList<ReferenceType>();
        l.add((ReferenceType) t1);
        l.add((ReferenceType) t2);
        try {
            if (!ts.checkIntersectionBounds(l, true)) {
                return ts.Object();
            }
            else {
                return ts.intersectionType(l);
            }
        } catch (SemanticException e) {
            return ts.Object();
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("lub(");
        sb.append(JL5TypeSystem_c.listToString(lubElems));
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean isCastValidImpl(Type toType) {
        for (Type elem : lubElements()) {
            if (!ts.isCastValid(elem, toType)) return false;
        }
        return true;
    }

    @Override
    public boolean isImplicitCastValidImpl(Type toType) {
        for (Type elem : lubElements()) {
            if (!ts.isImplicitCastValid(elem, toType)) return false;
        }
        return true;
    }

    @Override
    public boolean isSubtypeImpl(Type ancestor) {
        for (Type elem : lubElements()) {
            if (!ts.isSubtype(elem, ancestor)) return false;
        }
        return true;
    }

}
