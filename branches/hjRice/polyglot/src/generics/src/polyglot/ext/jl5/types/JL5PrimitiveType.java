package polyglot.ext.jl5.types;

import polyglot.types.PrimitiveType;
import polyglot.types.TypeObject;

public interface JL5PrimitiveType extends PrimitiveType, JL5Type {

    boolean equivalentImpl(TypeObject arg2);

}
