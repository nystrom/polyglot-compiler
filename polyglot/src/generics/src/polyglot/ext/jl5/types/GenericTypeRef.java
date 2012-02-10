package polyglot.ext.jl5.types;

import polyglot.types.TypeObject;


/* a use of a class type */
public interface GenericTypeRef extends JL5ParsedClassType, JL5Type {

    JL5ParsedClassType baseType();
    boolean equivalentImpl(TypeObject t);
    /*
     * Capture conversion
     */
    GenericTypeRef capture();
    
}
