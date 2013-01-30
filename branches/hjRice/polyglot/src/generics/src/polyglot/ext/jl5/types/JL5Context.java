package polyglot.ext.jl5.types;

import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.VarInstance;
public interface JL5Context extends Context {

    public VarInstance findVariableInThisScope(Name name);
    public VarInstance findVariableSilent(Name name);

    public JL5Context pushTypeVariable(TypeVariable iType);
    public TypeVariable findTypeVariableInThisScope(Name name);

    /**
     * @deprecated
     * @return
     */
    public boolean inTypeVariable();

    public JL5Context addTypeVariable(Name name, Ref<? extends Type> type);

    public JL5TypeSystem typeSystem();
}
