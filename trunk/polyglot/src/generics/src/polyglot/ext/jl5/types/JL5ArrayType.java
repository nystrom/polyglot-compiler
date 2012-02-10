package polyglot.ext.jl5.types;

import polyglot.types.ArrayType;

public interface JL5ArrayType extends ArrayType, JL5Type {
    boolean isVarargs();
}
