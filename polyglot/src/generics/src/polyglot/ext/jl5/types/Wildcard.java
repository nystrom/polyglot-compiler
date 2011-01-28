package polyglot.ext.jl5.types;

import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface Wildcard extends Type {
    
    ReferenceType lowerBound();
    ReferenceType upperBound();

    ReferenceType bound(); //either lower or upper, depending on wildcard type
    void bound(ReferenceType bound);

}
