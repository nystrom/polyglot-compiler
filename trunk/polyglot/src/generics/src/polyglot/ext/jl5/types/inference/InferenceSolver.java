package polyglot.ext.jl5.types.inference;

import java.util.List;

import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.Type;


public interface InferenceSolver {
    
    List<TypeVariable> typeVariables();
    boolean isTargetTypeVariable(Type t);
    
    List<Type> solve();
    List<Type> solve(Type expectedReturnType);
    
    JL5TypeSystem typeSystem();

}
