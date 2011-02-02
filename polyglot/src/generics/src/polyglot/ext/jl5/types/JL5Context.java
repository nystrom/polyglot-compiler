package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.VarInstance;
public interface JL5Context extends Context {

    public VarInstance findVariableInThisScope(Name name);
    public VarInstance findVariableSilent(Name name);

    public JL5Context pushTypeVariable(TypeVariable iType);
    public TypeVariable findTypeVariableInThisScope(Name name);

    public boolean inTypeVariable();


    public JL5Context addTypeVariable(TypeVariable type);

    public JL5MethodInstance findJL5Method(Name name, List<Type> paramTypes, List<Type> explicitTypeArgTypes) throws SemanticException;
    public JL5TypeSystem typeSystem();
}
