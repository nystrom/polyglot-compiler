package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassDef.Kind;
import polyglot.types.ClassType;
import polyglot.types.Type;
import polyglot.types.TypeObject;

public interface TypeVariable extends ClassType {

    public static final Kind TYPEVARIABLE = new Kind("type_variable");
    
    public static enum TVarDecl { CLASSTV, PROCEDURETV, SYNTHETICTV }
   
    TVarDecl declaredIn();
    void declaringProcedure(JL5ProcedureInstance pi);
    void declaringClass(ClassType ct);
    ClassType declaringClass();
    JL5ProcedureInstance declaringProcedure();
    
    List <Type>bounds();
    void bounds(List<Type> l);

    boolean isEquivalent(TypeObject arg2);

    boolean equivalentImpl(TypeObject arg2);

    Type erasureType();

    IntersectionType upperBound();
    void upperBound(IntersectionType b);
    
    
    
    /**
     * lower bound can only occur in the process of capture conversion
     * @return
     */
    Type lowerBound();
    void lowerBound(Type lowerBound); 
}
