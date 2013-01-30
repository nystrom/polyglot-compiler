package polyglot.ext.jl5.types.inference;

import java.util.Collections;
import java.util.List;

import polyglot.types.Type;

public class SuperTypeConstraint extends Constraint {

    public SuperTypeConstraint(Type actual, Type formal, InferenceSolver solver) {
        super(actual, formal, null);
    }

    @Override
    public List<Constraint> simplify() {
        return Collections.emptyList();
    }

    @Override
    public boolean canSimplify() {
        return !solver.isTargetTypeVariable(formal);
    }
    
    public String toString() {
        return actual + " :> " + formal;
    }

}
