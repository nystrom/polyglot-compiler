package polyglot.ext.jl5.types.inference;

import java.util.ArrayList;
import java.util.List;

import polyglot.ext.jl5.types.AnySubType;
import polyglot.ext.jl5.types.AnySuperType;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.types.Wildcard;
import polyglot.types.Type;

public class EqualConstraint extends Constraint {

    public EqualConstraint(Type actual, Type formal, InferenceSolver solver) {
        super(actual, formal, solver);
    }

    @Override
    public List<Constraint> simplify() {
        List<Constraint> r = new ArrayList<Constraint>();
        if (formal.isArray()) {
            if (actual.isArray() && actual.toArray().base().isReference()) {
                r.add(new EqualConstraint(actual.toArray().base(), formal.toArray().base(), solver));
            }
            else if (actual instanceof TypeVariable) {
                TypeVariable actual_tv = (TypeVariable) actual;
                for (Type b : actual_tv.bounds()) {
                    if (b.isArray() && b.toArray().base().isReference()) {
                        r.add(new EqualConstraint(b.toArray().base(), formal.toArray().base(), solver));
                    }
                }
            }
        }
        else if (formal instanceof ParameterizedType && actual instanceof ParameterizedType) {
            ParameterizedType formal_pt = (ParameterizedType) formal;
            ParameterizedType actual_pt = (ParameterizedType) actual;
            if (formal_pt.baseType().equals(actual_pt.baseType())) {
                int n = formal_pt.typeArguments().size();
                for (int i = 0; i < n; i++) {
                    Type formal_targ = formal_pt.typeArguments().get(i);
                    Type actual_targ = actual_pt.typeArguments().get(i);
                    if (!(formal_targ instanceof Wildcard) && !(actual_targ instanceof Wildcard)) {
                        r.add(new EqualConstraint(actual_targ, formal_targ, solver));
                    }
                    else if ( ((formal_targ instanceof AnySubType) && (actual_targ instanceof AnySubType)) ||
                              ((formal_targ instanceof AnySuperType) && (actual_targ instanceof AnySuperType)) ) {
                        Wildcard formal_targ_wc = (Wildcard) formal_targ;
                        Wildcard actual_targ_wc = (Wildcard) actual_targ;
                        r.add(new EqualConstraint(formal_targ_wc.bound(), actual_targ_wc.bound(), solver));
                    }
                }
            }
        }
        return r;
    }

    @Override
    public boolean canSimplify() {
        return !solver.isTargetTypeVariable(formal);
    }
    
    public String toString() {
        return actual + " = " + formal;
    }

}
