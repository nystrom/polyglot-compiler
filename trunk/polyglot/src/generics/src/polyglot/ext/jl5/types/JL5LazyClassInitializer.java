package polyglot.ext.jl5.types;

import polyglot.types.LazyClassInitializer;

public interface JL5LazyClassInitializer extends LazyClassInitializer{

    public void initEnumConstants(JL5ParsedClassType ct);
    public void initAnnotations(JL5ParsedClassType ct);
}
