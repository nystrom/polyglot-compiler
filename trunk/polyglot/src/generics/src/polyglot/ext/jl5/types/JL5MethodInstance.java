package polyglot.ext.jl5.types;

import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;

public interface JL5MethodInstance extends JL5FunctionInstance<MethodDef>, MethodInstance {
    JL5MethodInstance erasure();
}
