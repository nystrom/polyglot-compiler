package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface Wildcard extends Type {
    
	ClassType lowerBound();
    ClassType upperBound();

    ClassType bound(); //either lower or upper, depending on wildcard type
    void bound(ClassType bound);

}
