package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;

public interface IntersectionType extends ClassType {
    List<ClassType> bounds();
    
    void boundOf(TypeVariable tv);
    TypeVariable boundOf();
    
    public static final Kind INTERSECTION = new Kind("intersection");
    
}
