package polyglot.ext.jl5.types.inference;

import java.util.ArrayList;
import java.util.List;

import polyglot.ext.jl5.types.AnySubType;
import polyglot.ext.jl5.types.AnySuperType;
import polyglot.ext.jl5.types.JL5PrimitiveType;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.types.Wildcard;
import polyglot.types.Type;

public class SubConversionConstraint extends Constraint {

    public SubConversionConstraint(Type actual, Type formal, InferenceSolver solver) {
        super(actual, formal, solver);
    }

    @Override
    public List<Constraint> simplify() {
        List<Constraint> r = new ArrayList<Constraint>();
        if (actual instanceof JL5PrimitiveType) {
            JL5PrimitiveType prim_actual = (JL5PrimitiveType) actual;
            r.add(new SubConversionConstraint(solver().typeSystem().classOf(prim_actual), formal, solver));
        }
        else if (solver().isTargetTypeVariable(formal)) {
            r.add(new SubTypeConstraint(actual, formal, solver));
        }
        else if (formal.isArray()) {
            if (actual.isArray() && actual.toArray().base().isReference()) {
                r.add(new SubConversionConstraint(actual.toArray().base(), formal.toArray().base(), solver));
            }
            else if (actual instanceof TypeVariable) {
                TypeVariable actual_tv = (TypeVariable) actual;
                for (Type b : actual_tv.bounds()) {
                    if (b.isArray() && b.toArray().base().isReference()) {
                        r.add(new SubConversionConstraint(b.toArray().base(), formal.toArray().base(), solver));
                    }
                }
            }
        }
        else if (formal instanceof ParameterizedType && actual instanceof ParameterizedType) {
            ParameterizedType formal_pt = (ParameterizedType) formal;
            ParameterizedType actual_pt = (ParameterizedType) actual;
            ParameterizedType s = solver.typeSystem().findGenericSupertype(formal_pt.baseType(), actual_pt);
            if (s != null) {
                int n = formal_pt.typeArguments().size();
                for (int i = 0; i < n; i++) {
                    Type formal_targ = formal_pt.typeArguments().get(i);
                    Type actual_targ = s.typeArguments().get(i);
                    if (!(formal_targ instanceof Wildcard) && !(actual_targ instanceof Wildcard)) {
                        r.add(new EqualConstraint(actual_targ, formal_targ, solver));
                    }
                    else if (formal_targ instanceof AnySubType) {
                        AnySubType formal_targ_wc = (AnySubType) formal_targ;
                        if (!(actual_targ instanceof Wildcard)) {
                            r.add(new SubConversionConstraint(actual_targ, formal_targ_wc.bound(), solver));
                        }
                        else if (actual_targ instanceof AnySubType) {
                            AnySubType actual_targ_wc = (AnySubType) actual_targ;
                            r.add(new SubConversionConstraint(actual_targ_wc.bound(), formal_targ_wc.bound(), solver));
                        }
                    }
                    else if (formal_targ instanceof AnySuperType) {
                        AnySuperType formal_targ_wc = (AnySuperType) formal_targ;
                        if (!(actual_targ instanceof Wildcard)) {
                            r.add(new SuperConversionConstraint(actual_targ, formal_targ_wc.bound(), solver));
                        }
                        else if (actual_targ instanceof AnySuperType) {
                            AnySuperType actual_targ_wc = (AnySuperType) actual_targ;
                            r.add(new SuperConversionConstraint(actual_targ_wc.bound(), formal_targ_wc.bound(), solver));
                        }
                    }
                }
            }
        }
        return r;
    }

    public boolean canSimplify() {
        return true;
    }
    
    public String toString() {
        return actual + " << " + formal;
    }

}
