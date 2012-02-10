package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.Ref;
import polyglot.types.Type;

public interface Wildcard extends Type {
    
	Ref<ClassType> lowerBound();
	Ref<ClassType> upperBound();

    ClassType bound(); //either lower or upper, depending on wildcard type
    Ref<ClassType> boundRef(); //either lower or upper, depending on wildcard type
    void bound(Ref<ClassType> bound);
}
