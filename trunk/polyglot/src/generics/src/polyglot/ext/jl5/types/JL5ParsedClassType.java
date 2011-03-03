package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.ParsedClassType;
import polyglot.types.TypeObject;

/* The type information for a class declaration */

public interface JL5ParsedClassType extends ParsedClassType {
    void addEnumConstant(EnumInstance ei);
    List<EnumInstance> enumConstants();
    EnumInstance enumConstantNamed(Name name);
    
    void addAnnotationElem(AnnotationElemInstance ai);
    List<AnnotationElemInstance> annotationElems();
    AnnotationElemInstance annotationElemNamed(Name name);

    void annotations(List annotations);
    List annotations();

    List<TypeVariable> typeVariables();

    void addTypeVariable(TypeVariable type);

    void typeVariables(List<TypeVariable> vars);
    
    boolean hasTypeVariable(Name name);
    TypeVariable getTypeVariable(Name name);

    boolean isGeneric();

	// find methods with compatible name and formals as the given one
    List<MethodInstance> methods(JL5MethodInstance mi);

    boolean equivalentImpl(TypeObject arg2);
    /*List typeArguments();
    void typeArguments(List args);

    boolean isParameterized();*/

}