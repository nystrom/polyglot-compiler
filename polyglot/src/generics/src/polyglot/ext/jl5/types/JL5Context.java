package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.VarInstance;
public interface JL5Context extends Context {

    public VarInstance findVariableInThisScope(String name);
    public VarInstance findVariableSilent(String name);

    public JL5Context pushTypeVariable(TypeVariable iType);
    public TypeVariable findTypeVariableInThisScope(String name);

    public boolean inTypeVariable();


    public JL5Context addTypeVariable(TypeVariable type);

    public JL5MethodInstance findJL5Method(String name, List<Type> paramTypes, List<Type> explicitTypeArgTypes) throws SemanticException;
    public JL5TypeSystem typeSystem();
}
