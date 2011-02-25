package polyglot.ext.jl5.types;

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.types.VarInstance;
public interface JL5Context extends Context {

    public VarInstance findVariableInThisScope(Name name);
    public VarInstance findVariableSilent(Name name);

    public JL5Context pushTypeVariable(TypeVariable iType);
    public TypeVariable findTypeVariableInThisScope(Name name);

    public boolean inTypeVariable();


    public JL5Context addTypeVariable(TypeVariable type);

    public JL5TypeSystem typeSystem();
}
