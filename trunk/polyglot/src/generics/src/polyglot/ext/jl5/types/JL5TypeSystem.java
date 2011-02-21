package polyglot.ext.jl5.types;


import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ext.jl5.ast.AnnotationElem;
import polyglot.ext.jl5.types.JL5TypeSystem_c.JL5ConstructorMatcher;
import polyglot.ext.jl5.types.JL5TypeSystem_c.JL5MethodMatcher;
import polyglot.ext.jl5.types.inference.InferenceSolver;
import polyglot.ext.jl5.types.inference.LubType;
import polyglot.frontend.Source;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.ParsedClassType;
import polyglot.types.PrimitiveType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c.ConstructorMatcher;
import polyglot.types.TypeSystem_c.MethodMatcher;
import polyglot.util.Position;

public interface JL5TypeSystem extends TypeSystem {
    // TODO: declare any new methods needed
    //polyglot.ext.jl5.types.LazyClassInitializer defaultClassInitializer();
    ParsedClassType createClassType(LazyClassInitializer init, Source fromSource);

    ParsedClassType createClassType(Source fromSource);

    boolean isTypeExtendsAnnotation(Type t);
    
    ClassType Class(Type t);

    ClassType Enum();

    ClassType Annotation();

    ClassType Iterable();

    ClassType Iterator();

    ClassType IntegerWrapper();

    ClassType ByteWrapper();

    ClassType ShortWrapper();

    ClassType BooleanWrapper();

    ClassType CharacterWrapper();

    ClassType LongWrapper();

    ClassType DoubleWrapper();

    ClassType FloatWrapper();

    ClassType classOf(Type t);

    boolean equivalent(TypeObject t1, TypeObject t2);

    Type erasure(Type t);

    EnumInstance enumInstance(Position pos, ClassType ct, Flags f, String name,
            ParsedClassType anonType);

    AnnotationElemInstance annotationElemInstance(Position pos, ClassType ct, Flags f, Type type,
            String name, boolean hasDefault);

    Context createContext();

    EnumInstance findEnumConstant(ReferenceType container, Name name, ClassType currClass)
            throws SemanticException;

    AnnotationElemInstance findAnnotation(ReferenceType container, String name, ClassType currClass)
            throws SemanticException;

    EnumInstance findEnumConstant(ReferenceType container, String name, Context c)
            throws SemanticException;

    EnumInstance findEnumConstant(ReferenceType container, String name) throws SemanticException;

    FieldInstance findFieldOrEnum(ReferenceType container, String name, ClassType currClass)
            throws SemanticException;

    boolean isValidAnnotationValueType(Type t);

    boolean numericConversionBaseValid(Type t, Object value);

    boolean isBaseCastValid(Type from, Type to);

    void checkDuplicateAnnotations(List annotations) throws SemanticException;

    void checkValueConstant(Expr value) throws SemanticException;

    Flags flagsForBits(int bits);

    void checkAnnotationApplicability(AnnotationElem annotation, Node n) throws SemanticException;

    TypeVariable typeVariable(Position pos, String name, List bounds);

    ParameterizedType parameterizedType(JL5ParsedClassType type);

    RawType rawType(JL5ParsedClassType ct);

    ArrayType arrayType(Position pos, Type base);

    /* void handleTypeRestrictions(List typeVariables, List typeArguments) throws SemanticException;
     void resetTypeRestrictions(List typeVariables, List typeArguments) throws SemanticException;*/

    //      Type findRequiredType(TypeVariable iType, ParameterizedType pType);
    boolean equals(TypeObject arg1, TypeObject arg2);

    AnyType anyType();

    AnySuperType anySuperType(ReferenceType t);

    AnySubType anySubType(ReferenceType t);

    boolean isEquivalent(TypeObject arg1, TypeObject arg2);

    List<ReferenceType> allAncestorsOf(ReferenceType rt);
    ParameterizedType findGenericSupertype(ReferenceType base, ReferenceType t);


    IntersectionType intersectionType(List<ReferenceType> elems);
    LubType lubType(List<ClassType> lst);

    Type getSubstitution(GenericTypeRef orig, Type curr);

    void sortAnnotations(List annots, List runtimeAnnots, List classAnnots, List sourceAnnots);

    boolean needsUnboxing(Type to, Type from);

    boolean needsBoxing(Type to, Type from);

    //  Set<ReferenceType> superTypesOf(ReferenceType t);

    boolean checkIntersectionBounds(List<? extends ReferenceType> bounds, boolean quiet)
            throws SemanticException;

    List<ReferenceType> concreteBounds(List<? extends ReferenceType> bounds);

    Type applySubstitution(Type toBeSubed, List<TypeVariable> orig, List<Type> sub);

    <T extends Type> List<T> applySubstitution(List<T> listToSub, List<TypeVariable> orig,
            List<Type> typeArgs);

    Type rawify(Type t);

    Type rawifyBareGenericType(Type t);

    List rawifyBareGenericTypeList(List l);

    Type capture(Type t);

    boolean checkContains(ParameterizedType desc, ParameterizedType ancestor);

    MethodInstance findJL5Method(Type container, JL5MethodMatcher matcher)  throws SemanticException;

    ConstructorInstance findJL5Constructor(Type container, JL5ConstructorMatcher context) throws SemanticException;

    void checkTVForwardReference(List<TypeVariable> name) throws SemanticException;
    
    InferenceSolver inferenceSolver(JL5ProcedureInstance pi, List<Type> actuals);
    InferenceSolver inferenceSolver(List<TypeVariable> typeVars, List<Type> formals, List<Type> actuals);
    
    boolean typeVariableEquals(TypeVariable type1, TypeVariable type2, Context context);

	MethodMatcher JL5MethodMatcher(Type targetType, Name id,
			List<Type> paramTypes, List<Type> explicitTypeArgs, JL5Context c);

	ConstructorMatcher JL5ConstructorMatcher(Type targetType,
			List<Type> paramTypes, List<Type> explicitTypeArgs, JL5Context c);

}
