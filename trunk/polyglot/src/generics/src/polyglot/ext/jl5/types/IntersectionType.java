package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;
import polyglot.types.Type;

public interface IntersectionType extends Type {
    List<ClassType> bounds();
    
    void boundOf(TypeVariable tv);
    TypeVariable boundOf();
    
//    public static final Kind INTERSECTION = new Kind("intersection");
    
}
