package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Flags;
import polyglot.types.MethodInstance;
import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface JL5MethodInstance extends JL5ProcedureInstance, MethodInstance {

    public boolean isCompilerGenerated();
    public JL5MethodInstance setCompilerGenerated(boolean val);
    
    JL5MethodInstance erasure();
    
    JL5MethodInstance formalTypes(List ars);
    JL5MethodInstance flags(Flags flags);
    JL5MethodInstance throwTypes(List l);
    JL5MethodInstance container(ReferenceType container);
   
}
