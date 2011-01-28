package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Type;

public interface ParameterizedType extends GenericTypeRef {

    List<Type> typeArguments();
    void typeArguments(List<Type> args);
    
}
