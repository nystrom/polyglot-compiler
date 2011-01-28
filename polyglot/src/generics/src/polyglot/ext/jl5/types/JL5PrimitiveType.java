package polyglot.ext.jl5.types;

import polyglot.types.PrimitiveType;
import polyglot.types.TypeObject;

public interface JL5PrimitiveType extends PrimitiveType {

    boolean equivalentImpl(TypeObject arg2);

}
