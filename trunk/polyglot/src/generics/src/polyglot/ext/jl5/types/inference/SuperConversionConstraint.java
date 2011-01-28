package polyglot.ext.jl5.types.inference;

import java.util.ArrayList;
import java.util.List;

import polyglot.ext.jl5.types.AnySubType;
import polyglot.ext.jl5.types.AnySuperType;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.types.Wildcard;
import polyglot.types.Type;

public class SuperConversionConstraint extends Constraint {

    public SuperConversionConstraint(Type actual, Type formal, InferenceSolver solver) {
        super(actual, formal, solver);
    }

    @Override
    public List<Constraint> simplify() {
        List<Constraint> r = new ArrayList<Constraint>();
        if (solver().isTargetTypeVariable(formal)) {
            r.add(new SuperTypeConstraint(actual, formal, solver));
        }
        else if (formal.isArray()) {
            if (formal.isArray()) {
                if (actual.isArray() && actual.toArray().base().isReference()) {
                    r.add(new SuperConversionConstraint(actual.toArray().base(), formal.toArray().base(), solver));
                }
                else if (actual instanceof TypeVariable) {
                    TypeVariable actual_tv = (TypeVariable) actual;
                    for (Type b : actual_tv.bounds()) {
                        if (b.isArray() && b.toArray().base().isReference()) {
                            r.add(new SuperConversionConstraint(b.toArray().base(), formal.toArray().base(), solver));
                        }
                    }
                }
            }
        }
        else if ((formal instanceof ParameterizedType) && (actual instanceof ParameterizedType)) {
            ParameterizedType formal_pt = (ParameterizedType) formal;
            ParameterizedType actual_pt = (ParameterizedType) actual;
            ParameterizedType f = null;
            if (!actual_pt.baseType().equals(formal_pt.baseType())) {
                f = solver.typeSystem().findGenericSupertype(actual_pt.baseType(), formal_pt.baseType());
                if (f != null) {
                    solver.typeSystem().applySubstitution(f, formal_pt.baseType().typeVariables(), formal_pt.typeArguments());

                }
            }
            else
                f = formal_pt;
            if (f != null) {
                int n = formal_pt.typeArguments().size();
                for (int i = 0; i < n; i++) {
                    Type formal_targ = f.typeArguments().get(i);
                    Type actual_targ = actual_pt.typeArguments().get(i);
                    if (!(formal_targ instanceof Wildcard)) {
                        if (!(actual_targ instanceof Wildcard)) {
                            r.add(new EqualConstraint(actual_targ, formal_targ, solver));
                        }
                        else {
                            Wildcard actual_targ_wc = (Wildcard) actual_targ;
                            if (actual_targ_wc instanceof AnySubType) {
                                r.add(new SuperConversionConstraint(actual_targ_wc.bound(), formal_targ, solver));
                            }
                            else if (actual_targ_wc instanceof AnySuperType) {
                                r.add(new SubConversionConstraint(actual_targ_wc.bound(), formal_targ, solver));
                            }
                        }
                    }
                    else if (actual_targ instanceof Wildcard) {
                        Wildcard formal_targ_wc = (Wildcard) formal_targ;
                        Wildcard actual_targ_wc = (Wildcard) actual_targ;
                        if ((formal_targ_wc instanceof AnySubType) && (actual_targ_wc instanceof AnySubType)) {
                            r.add(new SuperConversionConstraint(actual_targ_wc.bound(), formal_targ_wc.bound(), solver));
                        }
                        else if ((formal_targ_wc instanceof AnySuperType)&& (actual_targ_wc instanceof AnySuperType)) {
                            r.add(new SubConversionConstraint(actual_targ_wc.bound(), formal_targ_wc.bound(), solver));
                        }
                    }
                }
            }
        }

        return r;
    }

    @Override
    public boolean canSimplify() {
        return true;
    }

    public String toString() {
        return actual + " >> " + formal;
    }

}
