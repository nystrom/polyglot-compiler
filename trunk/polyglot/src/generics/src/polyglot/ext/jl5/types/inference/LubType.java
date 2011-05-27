package polyglot.ext.jl5.types.inference;

import java.util.List;

import polyglot.ext.jl5.types.IntersectionType;
import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;
import polyglot.types.Type;
/**
 * Type that represents lub(U1,U2...) as defined on page 463 of JLS
 * @author Milan
 *
 */
public interface LubType extends Type {
    IntersectionType calculateLub();
    List<Type> lubElements();
    List<Type> bounds();
    

}
