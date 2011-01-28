package polyglot.ext.jl5.types.inference;

import java.util.List;

import polyglot.ext.jl5.types.IntersectionType;
import polyglot.types.ClassType;
import polyglot.types.ReferenceType;
/**
 * Type that represents lub(U1,U2...) as defined on page 463 of JLS
 * @author Milan
 *
 */
public interface LubType extends ClassType {
    
    public static final Kind LUB = new Kind("lub");
    IntersectionType calculateLub();
    List<ClassType> lubElements();
    List<ReferenceType> bounds();
    

}
