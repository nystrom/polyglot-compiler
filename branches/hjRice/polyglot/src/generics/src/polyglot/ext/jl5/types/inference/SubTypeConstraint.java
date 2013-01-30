package polyglot.ext.jl5.types.inference;

import java.util.Collections;
import java.util.List;

import polyglot.types.Type;

public class SubTypeConstraint extends Constraint {

    public SubTypeConstraint(Type actual, Type formal, InferenceSolver solver) {
        super(actual, formal, solver);
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
        return actual + " <: " + formal;
    }

}
