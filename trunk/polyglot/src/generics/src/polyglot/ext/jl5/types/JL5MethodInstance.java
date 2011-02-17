package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Flags;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface JL5MethodInstance extends JL5ProcedureInstance<MethodDef>, MethodInstance {

    public boolean isCompilerGenerated();
    public JL5MethodInstance setCompilerGenerated(boolean val);
    
    JL5MethodInstance erasure();
    
    JL5MethodInstance formalTypes(List<Type> ars);
    JL5MethodInstance flags(Flags flags);
    JL5MethodInstance throwTypes(List<Type> l);
    JL5MethodInstance container(ReferenceType container);
   
}
