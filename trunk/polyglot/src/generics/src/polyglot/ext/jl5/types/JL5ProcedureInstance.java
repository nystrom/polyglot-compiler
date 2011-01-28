package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ProcedureInstance;
import polyglot.types.Type;

public interface JL5ProcedureInstance extends ProcedureInstance {
    
    List<TypeVariable> typeVariables();
    void addTypeVariable(TypeVariable type);
    boolean hasTypeVariable(String name);
    TypeVariable getTypeVariable(String name);
    void typeVariables(List<TypeVariable> vars);

    JL5ProcedureInstance typeArguments(List<? extends Type> typeArgs);
    List<Type> typeArguments();
    
    JL5ProcedureInstance erasure();
    
    /**
     * This should be used instead of hasFormals(List formals) becuase of generics
     * 
     * @param other
     * @return
     */
    boolean hasSameFormals(JL5ProcedureInstance other);
    
    boolean isGeneric();
    boolean isVariableArrity();
/*    
    JL5ProcedureInstance formalTypes(List ars);
    JL5ProcedureInstance flags(Flags flags);
    JL5ProcedureInstance throwTypes(List l);
  */  
    List<Type> formalTypes();
    
}
