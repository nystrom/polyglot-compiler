package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.ProcedureDef;
import polyglot.types.ProcedureInstance;
import polyglot.types.Type;

public interface JL5ProcedureInstance<T extends ProcedureDef> extends ProcedureInstance<T> {
    
    List<TypeVariable> typeVariables();
    void addTypeVariable(TypeVariable type);
    boolean hasTypeVariable(Name name);
    TypeVariable getTypeVariable(Name name);
    void typeVariables(List<TypeVariable> vars);

    JL5ProcedureInstance<T> typeArguments(List<? extends Type> typeArgs);
    List<Type> typeArguments();
    
    JL5ProcedureInstance<T> erasure();
    
    /**
     * This should be used instead of hasFormals(List formals) because of generics
     */
    boolean hasFormals(List<Type> otherFormalTypes, List<TypeVariable> otherTypeVariables, List<Type> typeArguments, Context context);
    
    boolean isGeneric();
    boolean isVariableArrity();

    List<Type> formalTypes();
    
}
