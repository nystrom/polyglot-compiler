package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Ref;
import polyglot.types.Type;

public interface IntersectionType extends Type {
	List<Ref<? extends Type>> bounds();
    
    void boundOf(TypeVariable tv);
    TypeVariable boundOf();

	List<Type> boundsTypes();
    
//    public static final Kind INTERSECTION = new Kind("intersection");
    
}
