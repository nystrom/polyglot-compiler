package polyglot.ext.jl5.types;

import polyglot.types.ClassType;

public interface JL5ClassType extends ClassType {

    EnumInstance enumConstantNamed(String name);
}
