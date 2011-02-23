package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import polyglot.ast.ArrayInit;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassLit;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NullLit;
import polyglot.ext.jl5.ast.AnnotationElem;
import polyglot.ext.jl5.ast.ElementValuePair;
import polyglot.ext.jl5.ast.JL5Field;
import polyglot.ext.jl5.ast.JL5ProcedureMatcher;
import polyglot.ext.jl5.ast.NormalAnnotationElem;
import polyglot.ext.jl5.types.inference.InferenceSolver;
import polyglot.ext.jl5.types.inference.InferenceSolver_c;
import polyglot.ext.jl5.types.inference.LubType;
import polyglot.ext.jl5.types.inference.LubType_c;
import polyglot.frontend.Source;
import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.ImportTable;
import polyglot.types.Matcher;
import polyglot.types.MemberDef;
import polyglot.types.MemberInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.NoMemberException;
import polyglot.types.ObjectType;
import polyglot.types.ParsedClassType;
import polyglot.types.PrimitiveType;
import polyglot.types.QName;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeEnv;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.Predicate2;

public class JL5TypeSystem_c extends TypeSystem_c implements JL5TypeSystem {
    // TODO: implement new methods in JL5TypeSystem.
    // TODO: override methods as needed from TypeSystem_c.

    protected ClassType ENUM_;

    protected ClassType ANNOTATION_;

    // this is for extended for
    protected ClassType ITERABLE_;

    protected ClassType ITERATOR_;

	// get a type representing Class<t>
    public ClassType Class(Type t) {
	JL5ParsedClassType raw = (JL5ParsedClassType) Class();
	ParameterizedType pt = parameterizedType(raw);
	List args = new LinkedList();
	args.add(t);
	pt.typeArguments(args);
	return pt;
    }

    public ClassType Enum() {
        if (ENUM_ != null) {
            return ENUM_;
        }
        else {
            return ENUM_ = load("java.lang.Enum");
        }
    }

    public ClassType Annotation() {
        if (ANNOTATION_ != null) {
            return ANNOTATION_;
        }
        else {
            return ANNOTATION_ = load("java.lang.annotation.Annotation");
        }
    }

    public ClassType Iterable() {
        if (ITERABLE_ != null) {
            return ITERABLE_;
        }
        else {
            return ITERABLE_ = load("java.lang.Iterable");
        }
    }

    public ClassType Iterator() {
        if (ITERATOR_ != null) {
            return ITERATOR_;
        }
        else {
            return ITERATOR_ = load("java.util.Iterator");
        }
    }

    protected ClassType INTEGER_WRAPPER;

    protected ClassType BYTE_WRAPPER;

    protected ClassType SHORT_WRAPPER;

    protected ClassType CHARACTER_WRAPPER;

    protected ClassType BOOLEAN_WRAPPER;

    protected ClassType LONG_WRAPPER;

    protected ClassType DOUBLE_WRAPPER;

    protected ClassType FLOAT_WRAPPER;

    public ClassType IntegerWrapper() {
        if (INTEGER_WRAPPER != null) {
            return INTEGER_WRAPPER;
        }
        else {
            return INTEGER_WRAPPER = load("java.lang.Integer");
        }
    }

    public ClassType ByteWrapper() {
        if (BYTE_WRAPPER != null) {
            return BYTE_WRAPPER;
        }
        else {
            return BYTE_WRAPPER = load("java.lang.Byte");
        }
    }

    public ClassType ShortWrapper() {
        if (SHORT_WRAPPER != null) {
            return SHORT_WRAPPER;
        }
        else {
            return SHORT_WRAPPER = load("java.lang.Short");
        }
    }

    public ClassType BooleanWrapper() {
        if (BOOLEAN_WRAPPER != null) {
            return BOOLEAN_WRAPPER;
        }
        else {
            return BOOLEAN_WRAPPER = load("java.lang.Boolean");
        }
    }

    public ClassType CharacterWrapper() {
        if (CHARACTER_WRAPPER != null) {
            return CHARACTER_WRAPPER;
        }
        else {
            return CHARACTER_WRAPPER = load("java.lang.Character");
        }
    }

    public ClassType LongWrapper() {
        if (LONG_WRAPPER != null) {
            return LONG_WRAPPER;
        }
        else {
            return LONG_WRAPPER = load("java.lang.Long");
        }
    }

    public ClassType DoubleWrapper() {
        if (DOUBLE_WRAPPER != null) {
            return DOUBLE_WRAPPER;
        }
        else {
            return DOUBLE_WRAPPER = load("java.lang.Double");
        }
    }

    public ClassType FloatWrapper() {
        if (FLOAT_WRAPPER != null) {
            return FLOAT_WRAPPER;
        }
        else {
            return FLOAT_WRAPPER = load("java.lang.Float");
        }
    }

    public ClassType classOf(Type t) {
        if (t.isClass())
            return (ClassType) t;
        if (equals(t, Float()))
            return FloatWrapper();
        if (equals(t, Double()))
            return DoubleWrapper();
        if (equals(t, Long()))
            return LongWrapper();
        if (equals(t, Int()))
            return IntegerWrapper();
        if (equals(t, Short()))
            return ShortWrapper();
        if (equals(t, Byte()))
            return ByteWrapper();
        if (equals(t, Char()))
            return CharacterWrapper();
        if (equals(t, Boolean()))
            return BooleanWrapper();
        return null;
    }


    @Override
    public boolean isByte(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, ByteWrapper(), context)) {
    		return true;
    	}
    	return super.isByte(t);
    }
    
    @Override
    public boolean isBoolean(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, BooleanWrapper(), context)) {
    		return true;
    	}
    	return super.isBoolean(t);
    }
    
    @Override
    public boolean isChar(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, CharacterWrapper(), context)) {
    		return true;
    	}
    	return super.isChar(t);
    }
    
    @Override
    public boolean isShort(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, ShortWrapper(), context)) {
    		return true;
    	}
    	return super.isShort(t);
    }
    
    @Override
    public boolean isInt(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, IntegerWrapper(), context)) {
    		return true;
    	}
    	return super.isInt(t);
    }
    
    @Override
    public boolean isLong(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, LongWrapper(), context)) {
    		return true;
    	}
    	return super.isLong(t);
    }

    @Override
    public boolean isFloat(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, FloatWrapper(), context)) {
    		return true;
    	}
    	return super.isFloat(t);
    }

    @Override
    public boolean isDouble(Type t) {
    	Context context = emptyContext();
    	if (typeEquals(t, DoubleWrapper(), context)) {
    		return true;
    	}
    	return super.isDouble(t);
    }
	
    
    protected final Flags TOP_LEVEL_CLASS_FLAGS = JL5Flags.setAnnotationModifier(JL5Flags.setEnumModifier(super.TOP_LEVEL_CLASS_FLAGS));

    protected final Flags MEMBER_CLASS_FLAGS = JL5Flags.setAnnotationModifier(JL5Flags.setEnumModifier(super.MEMBER_CLASS_FLAGS));

    public void checkTopLevelClassFlags(Flags f) throws SemanticException {
        if (!f.clear(TOP_LEVEL_CLASS_FLAGS).equals(JL5Flags.NONE)) {
            throw new SemanticException("Cannot declare a top-level class with flag(s) "
                    + f.clear(TOP_LEVEL_CLASS_FLAGS) + ".");
        }

        if (f.isFinal() && f.isInterface()) {
            throw new SemanticException("Cannot declare a final interface.");
        }

        checkAccessFlags(f);
    }

    public void checkMemberClassFlags(Flags f) throws SemanticException {
        if (!f.clear(MEMBER_CLASS_FLAGS).equals(JL5Flags.NONE)) {
            throw new SemanticException("Cannot declare a member class with flag(s) "
                    + f.clear(MEMBER_CLASS_FLAGS) + ".");
        }

        if (f.isStrictFP() && f.isInterface()) {
            throw new SemanticException("Cannot declare a strictfp interface.");
        }

        if (f.isFinal() && f.isInterface()) {
            throw new SemanticException("Cannot declare a final interface.");
        }

        checkAccessFlags(f);
    }

    public ConstructorInstance defaultConstructor(Position pos, ClassType container) {
        assert_(container);

        Flags access = Flags.NONE;

        if (container.flags().isPrivate() || JL5Flags.isEnumModifier(container.flags())) {
            access = access.Private();
        }
        if (container.flags().isProtected()) {
            access = access.Protected();
        }
        if (container.flags().isPublic() && !JL5Flags.isEnumModifier(container.flags())) {
            access = access.Public();
        }
        return constructorInstance(pos, container, access, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

    }

    public boolean typeExtendsAnnotation(Type t) {
		if (t instanceof ClassType) {
			ClassType ct = (ClassType) t;
			return ((ClassType)ct.superClass()).fullName().equals(QName.make("java.lang.annotation.Annotation"));
		}
	
		return false;
    }
    
    public LazyClassInitializer defaultClassInitializer() {
        if (defaultClassInit == null) {
            defaultClassInit = new JL5LazyClassInitializer_c(this);
        }
        return defaultClassInit;
    }

    public ParsedClassType createClassType(LazyClassInitializer init, Source fromSource) {
        return new JL5ParsedClassType_c(this, init, fromSource);
    }

    protected PrimitiveType createPrimitive(PrimitiveType.Kind kind) {
        return new JL5PrimitiveType_c(this, kind);
    }

    public void checkClassConformance(ClassType ct, Context context) throws SemanticException {

    	// JLS 8.9 Enums
    	// The optional class body of an enum constant implicitly defines an 
    	// anonymous class declaration (¤15.9.5) that extends the immediately 
    	// enclosing enum type
    	if (JL5Flags.isEnumModifier(ct.flags())) {
    		// check enums elsewhere - have to do something special with
    		// abstract methods and anon enum element bodies
    		// return;
    		JL5ParsedClassType pct = (JL5ParsedClassType) ct;
    		List<EnumInstance> enumConsts = pct.enumConstants();
    		boolean allAnonNull = true;
    		for (Iterator<EnumInstance> it = enumConsts.iterator(); it.hasNext();) {
    			EnumInstance ei = it.next();
    			if (ei.anonType() != null) {
    				allAnonNull = false;
    				break;
    			}
    		}

    		if (allAnonNull) {
    			// That's the simplest case when enums are defined without declaring any ClassBody
    			super.checkClassConformance(ct, context);
    		} else {
    			// if enum type declares abstract method ensure
    			// !!every!! enum constant decl declares anon body
    			// and !!every!! body implements this abstract
    			// method
    			for (Iterator<MethodInstance> it = ct.methods().iterator(); it.hasNext();) {
    				MethodInstance mi = it.next();
    				if (!mi.flags().isAbstract())
    					continue;
    				for (Iterator<EnumInstance> jt = enumConsts.iterator(); jt.hasNext();) {
    					EnumInstance ei = jt.next();
    					// Enum doesn't declare any ClassBody so it cannot implement the abstract method
    					if (ei.anonType() == null) {
    						throw new SemanticException("Enum constant decl: " + ei.name()
    								+ " must declare an anonymous subclass of: " + ct
    								+ " and implement the abstract method: " + mi, ei.position());
    					}
    					else {
    						boolean implFound = false;
    						// check if enum's anonType implements the abstract method
    						for (Iterator<MethodInstance> kt = ei.anonType().methods().iterator(); kt.hasNext();) {
    							MethodInstance mj = kt.next();
    							if (canOverride(mj, mi, context)) {
    								implFound = true;
    							}
    						}
    						if (!implFound) {
    							throw new SemanticException("Enum constant decl anonymous subclass must implement method: "
    									+ mi, ei.position());
    						}
    					}
    				}
    			}

    			// still need to check superInterfaces to ensure this
    			// class implements the methods except they can't be
    			// abstract (previous checks ensure okay)
    			List<Type> superInterfaces = abstractSuperInterfaces(ct);
    			for (Iterator<Type> it = superInterfaces.iterator(); it.hasNext();) {
    				Type t = (Type) it.next();
    				if (t instanceof StructType) {
    					StructType rt = (StructType) it;
    					if (typeEquals(rt, ct, context))
    						continue;
    					for (Iterator<MethodInstance> jt = rt.methods().iterator(); jt.hasNext();) {
    						MethodInstance mi = jt.next();
    						if (!mi.flags().isAbstract())
    							continue;

    						boolean implFound = false;
    						// don't need to look in super classes as the only
    						// one is java.lang.Enum so just look here and
    						// there
    						
    						for (Iterator<MethodInstance>  kt = ct.methods().iterator(); kt.hasNext();) {
    							MethodInstance mj = kt.next();
    							if ((canOverride(mj, mi, context))) {
    								implFound = true;
    								break;
    							}
    						}
    						for (Iterator<MethodInstance>  kt = ct.superClass().toClass().methods().iterator(); !implFound && kt.hasNext();) {
    							MethodInstance mj = kt.next();
    							if ((canOverride(mj, mi, context))) {
    								implFound = true;
    								break;
    							}
    						}

    						if (!implFound) {
    							throw new SemanticException(ct.fullName()
    									+ " should be declared abstract: it does not define: "
    									+ mi.signature() + ", which is declared in "
    									+ rt.toClass().fullName(), ct.position());
    						}
    					}
    				}
    			}
    		}
    	}
    	else {
    		super.checkClassConformance(ct, context);
    	}
    }


    @Override
    /**
     * This is a copy paste from TypeSystem_c.
     * If we modify the StructType to declaire methods(MethodInstance), we can use super implementation
     */
    public MethodInstance findImplementingMethod(ClassType ct, MethodInstance mi, boolean includeAbstract, Context context) {
    	StructType curr = ct;
    	while (curr != null) {
    		// methods returns all the mi that match the name and hasFormals
    		// that are compatible as defined in JL5ProcedureInstance_c
    		// Note that we could use the super implementation if StructType defined
    		// a methods(MethodInstance)
    		List<MethodInstance> possible = 
    			((JL5ParsedClassType)curr).methods((JL5MethodInstance) mi);
    		for (Iterator<MethodInstance> k = possible.iterator(); k.hasNext(); ) {
    			MethodInstance mj = k.next();
    			if ((includeAbstract || !mj.flags().isAbstract()) && 
    					((isAccessible(mi, context) && isAccessible(mj, context)) || 
    							isAccessible(mi, context))) {
    				// The method mj may be a suitable implementation of mi.
    				// mj is not abstract, and either mj's container 
    				// can access mi (thus mj can really override mi), or
    				// mi and mj are both accessible from ct (e.g.,
    				// mi is declared in an interface that ct implements,
    				// and mj is defined in a superclass of ct).
    				return mj;                    
    			}
    		}
    		if (curr.typeEquals(mi.container(), context)) {
    			// we've reached the definition of the abstract 
    			// method. We don't want to look higher in the 
    			// hierarchy; this is not an optimization, but is 
    			// required for correctness. 
    			break;
    		}

    		if (curr instanceof ObjectType) {
    			ObjectType ot = (ObjectType) curr;
    			if (ot.superClass() instanceof StructType) {
    				curr = (StructType) ot.superClass();
    			}
    			else {
    				curr = null;
    			}
    		}
    		else {
    			curr = null;
    		}
    	}
    	return null;
    }
    
    public EnumInstance findEnumConstant(ReferenceType container, String name, Context c)
            throws SemanticException {
        ClassType ct = null;
        if (c != null)
            ct = c.currentClass();
        return findEnumConstant(container, name, ct);
    }

    public EnumInstance findEnumConstant(ReferenceType container, String name, ClassType currClass)
            throws SemanticException {
        Collection enumConstants = findEnumConstants(container, name);
        if (enumConstants.size() == 0) {
            throw new NoMemberException(JL5NoMemberException.ENUM_CONSTANT, "Enum Constant: \""
                    + name + "\" not found in type \"" + container + "\".");
        }
        Iterator i = enumConstants.iterator();
        EnumInstance ei = (EnumInstance) i.next();

        if (i.hasNext()) {
            EnumInstance ei2 = (EnumInstance) i.next();

            throw new SemanticException("Enum Constant \"" + name
                    + "\" is ambiguous; it is defined in both " + ei.container() + " and "
                    + ei2.container() + ".");
        }

        if (currClass != null && !isAccessible(ei, currClass)) {
            throw new SemanticException("Cannot access " + ei + ".");
        }

        return ei;
    }

    public AnnotationElemInstance findAnnotation(ReferenceType container, String name,
            ClassType currClass) throws SemanticException {
        Collection annotations = findAnnotations(container, name);
        if (annotations.size() == 0) {
            throw new NoMemberException(JL5NoMemberException.ANNOTATION, "Annotation: \"" + name
                    + "\" not found in type \"" + container + "\".");
        }
        Iterator i = annotations.iterator();
        AnnotationElemInstance ai = (AnnotationElemInstance) i.next();

        if (i.hasNext()) {
            AnnotationElemInstance ai2 = (AnnotationElemInstance) i.next();

            throw new SemanticException("Annotation \"" + name
                    + "\" is ambiguous; it is defined in both " + ai.container() + " and "
                    + ai2.container() + ".");
        }

        if (currClass != null && !isAccessible(ai, currClass)) {
            throw new SemanticException("Cannot access " + ai + ".");
        }
        return ai;
    }

    public EnumInstance findEnumConstant(ReferenceType container, String name)
            throws SemanticException {
        return findEnumConstant(container, name, (ClassType) null);
    }

    public Set findEnumConstants(ReferenceType container, String name) {
        assert_(container);
        if (container == null) {
            throw new InternalCompilerError("Cannot access enum constant \"" + name
                    + "\" within a null container type.");
        }
        EnumInstance ei = null;

        if (container instanceof JL5ParsedClassType) {
            ei = ((JL5ParsedClassType) container).enumConstantNamed(name);
        }

        if (ei != null) {
            return Collections.singleton(ei);
        }

        Set enumConstants = new HashSet();

        return enumConstants;
    }

    public Set findAnnotations(ReferenceType container, String name) {
        assert_(container);
        if (container == null) {
            throw new InternalCompilerError("Cannot access annotation \"" + name
                    + "\" within a null container type.");
        }
        AnnotationElemInstance ai = ((JL5ParsedClassType) container).annotationElemNamed(name);

        if (ai != null) {
            return Collections.singleton(ai);
        }

        Set annotations = new HashSet();

        return annotations;
    }

    public EnumInstance enumInstance(Position pos, ClassType ct, Flags f, String name,
            ParsedClassType anonType) {
        assert_(ct);
        return new EnumInstance_c(this, pos, ct, f, name, anonType);
    }

    public AnnotationElemInstance annotationElemInstance(Position pos, ClassType ct, Flags f,
            Type type, String name, boolean hasDefault) {
        assert_(ct);
        assert_(type);
        return new AnnotationElemInstance_c(this, pos, ct, f, type, name, hasDefault);
    }

    public Context createContext() {
        return new JL5Context_c(this);
    }

    public FieldInstance findFieldOrEnum(ReferenceType container, String name, ClassType currClass)
            throws SemanticException {

        FieldInstance fi = null;

        try {
            fi = findField(container, name, currClass);
        } catch (NoMemberException e) {
            fi = findEnumConstant(container, name, currClass);
        }

        return fi;
    }

    public MethodInstance methodInstance(Position pos, ReferenceType container, Flags flags,
            Type returnType, String name, List argTypes, List excTypes) {

        assert_(container);
        assert_(returnType);
        assert_(argTypes);
        assert_(excTypes);
        return new JL5MethodInstance_c(this, pos, container, flags, returnType, name, argTypes, excTypes);
    }

    public ConstructorInstance constructorInstance(Position pos, ClassType container, Flags flags,
            List argTypes, List excTypes) {

        assert_(container);
        assert_(argTypes);
        assert_(excTypes);
        return new JL5ConstructorInstance_c(this, pos, container, flags, argTypes, excTypes);
    }

    public TypeVariable typeVariable(Position pos, String name, List bounds) {
        return new TypeVariable_c(this, pos, name, bounds);
    }

    public IntersectionType intersectionType(List<ReferenceType> bounds) {
        return new IntersectionType_c(this, bounds);
    }

    public ParameterizedType parameterizedType(JL5ParsedClassType ct) {

        return new ParameterizedType_c(ct);
    }

    public RawType rawType(JL5ParsedClassType ct) {

        return new RawType_c(ct);
    }

    public boolean isValidAnnotationValueType(Type t) {
        // must be one of primitive, String, Class, enum, annotation or
        // array of one of these
        if (t.isPrimitive())
            return true;
        if (t instanceof JL5ParsedClassType) {
            if (JL5Flags.isEnumModifier(((JL5ParsedClassType) t).flags()))
                return true;
            if (JL5Flags.isAnnotationModifier(((JL5ParsedClassType) t).flags()))
                return true;
            if (((JL5ParsedClassType) t).fullName().equals(QName.make("java.lang.String")))
                return true;
            if (((JL5ParsedClassType) t).fullName().equals(QName.make("java.lang.Class")))
                return true;
        }
        if (t.isArray()) {
            return isValidAnnotationValueType(((ArrayType) t).base());
        }
        return false;
    }

    public boolean isBaseCastValid(Type fromType, Type toType, Context context) {
    	assert_(fromType);
    	assert_(toType);
    	if (toType.isArray()) {
            Type base = ((ArrayType) toType).base();
            assert_(base);
    		return env(context).isImplicitCastValid(fromType, base);
        }
        return false;
    }

    public boolean numericConversionBaseValid(Type t, Object value, Context context) {
    	assert_(t);
        if (t.isArray()) {
        	return env(context).numericConversionValid(((ArrayType) t).base(), value);
        }
        return false;
    }

    public void checkDuplicateAnnotations(List annotations) throws SemanticException {
        // check no duplicate annotations used
        ArrayList l = new ArrayList(annotations);
        for (int i = 0; i < l.size(); i++) {
            AnnotationElem ai = (AnnotationElem) l.get(i);
            for (int j = i + 1; j < l.size(); j++) {
                AnnotationElem aj = (AnnotationElem) l.get(j);
                if (ai.typeName().type() == aj.typeName().type()) {
                    throw new SemanticException("Duplicate annotation use: " + aj.typeName(), aj.position());
                }
            }
        }
    }

    public void checkValueConstant(Expr value) throws SemanticException {
        if (value instanceof ArrayInit) {
            // check elements
            for (Iterator it = ((ArrayInit) value).elements().iterator(); it.hasNext();) {
                Expr next = (Expr) it.next();
                if ((!next.isConstant() || next == null || next instanceof NullLit)
                        && !(next instanceof ClassLit)) {
                    throw new SemanticException("Annotation attribute value must be constant", value.position());
                }
            }
        }
        else if ((!value.isConstant() || value == null || value instanceof NullLit)
                && !(value instanceof ClassLit)) {
            // for purposes of annotation elems class lits are constants
            throw new SemanticException("Annotation attribute value must be constant", value.position());
        }
    }

    public Flags flagsForBits(int bits) {
        Flags f = super.flagsForBits(bits);
        if ((bits & JL5Flags.ANNOTATION_MOD) != 0)
            f = JL5Flags.setAnnotationModifier(f);
        if ((bits & JL5Flags.ENUM_MOD) != 0) {
            f = JL5Flags.setEnumModifier(f);
        }
        return f;
    }

    public void checkAnnotationApplicability(AnnotationElem annotation, Node n)
            throws SemanticException {
        List applAnnots = ((JL5ParsedClassType) annotation.typeName().type()).annotations();
        // if there are no annotations applied to this annotation type then
        // there is no need to check the target type of the annotation
        if (applAnnots != null) {

            for (Iterator it = applAnnots.iterator(); it.hasNext();) {
                AnnotationElem next = (AnnotationElem) it.next();
                if (((ClassType) next.typeName().type()).fullName().equals(QName.make("java.lang.annotation.Target"))) {
                    if (next instanceof NormalAnnotationElem) {
                        for (Iterator elems = ((NormalAnnotationElem) next).elements().iterator(); elems.hasNext();) {
                            ElementValuePair elemVal = (ElementValuePair) elems.next();
                            if (elemVal.value() instanceof JL5Field) {
                                Name check = ((JL5Field) elemVal.value()).name().id();
                                appCheckValue(check, n);
                            }
                            else if (elemVal.value() instanceof ArrayInit) {
                                ArrayInit val = (ArrayInit) elemVal.value();
                                if (val.elements().isEmpty()) {
                                    // automatically throw exception
                                    // this annot cannot be applied anywhere
                                    throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
                                }
                                else {
                                    for (Iterator vals = val.elements().iterator(); vals.hasNext();) {
                                        Object nextVal = vals.next();
                                        if (nextVal instanceof JL5Field) {
                                            Name valCheck = ((JL5Field) nextVal).name().id();
                                            appCheckValue(valCheck, n);
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (((ClassType) annotation.typeName().type()).fullName().equals("java.lang.Override")) {
            appCheckOverride(n);
        }
    }

    private void appCheckValue(Name val, Node n) throws SemanticException {
        if (val.equals(Name.make("ANNOTATION_TYPE"))) {
            if (!(n instanceof ClassDecl)
                    || !JL5Flags.isAnnotationModifier(((ClassDecl) n).flags().flags())) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
        else if (val.equals(Name.make("CONSTRUCTOR"))) {
            if (!(n instanceof ConstructorDecl)) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
        else if (val.equals(Name.make("FIELD"))) {
            if (!(n instanceof FieldDecl)) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
        else if (val.equals(Name.make("LOCAL_VARIABLE"))) {
            if (!(n instanceof LocalDecl)) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
        else if (val.equals(Name.make("METHOD"))) {
            if (!(n instanceof MethodDecl)) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
        else if (val.equals(Name.make("PACKAGE"))) {
        }
        else if (val.equals(Name.make("PARAMETER"))) {
            if (!(n instanceof Formal)) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
        else if (val.equals(Name.make("TYPE"))) {
            if (!(n instanceof ClassDecl)) {
                throw new SemanticException("Annotation type not applicable to this kind of declaration", n.position());
            }
        }
    }

    private void appCheckOverride(Node n) throws SemanticException {
        MethodDecl md = (MethodDecl) n; // the other check should
        // prevent anything else
        JL5ParsedClassType mdClass = (JL5ParsedClassType) md.methodInstance().container();
        try {
            MethodInstance mi = findMethod((ReferenceType) mdClass.superType(), md.name(), md.methodInstance().formalTypes(), (ClassType) mdClass.superType());
        } catch (NoMemberException e) {
            throw new SemanticException("method does not override a method from its superclass", md.position());
        }
    }

    public boolean equivalent(Type fromType, Type toType) {
    	Context context = emptyContext();
    	return ((JL5Context)env(context)).equivalent(fromType, toType);
    }

    public AnyType anyType() {
        return new AnyType_c(this);
    }

    public AnySuperType anySuperType(ReferenceType t) {
        return new AnySuperType_c(this, t);
    }

    public AnySubType anySubType(ReferenceType t) {
        return new AnySubType_c(this, t);
    }

    public ClassType findMemberClass(ClassType container, String name, ClassType currClass)
            throws SemanticException {
        if (container instanceof ParameterizedType) {
            return super.findMemberClass(((ParameterizedType) container).baseType(), name, currClass);
        }
        return super.findMemberClass(container, name, currClass);
    }

    public Set findMemberClasses(ClassType container, String name) throws SemanticException {
        ClassType mt = container.memberClassNamed(name);

        if (mt != null) {
            if (!mt.isMember()) {
                throw new InternalCompilerError("Class " + mt + " is not a member class, "
                        + " but is in " + container + "\'s list of members.");
            }

            if ((mt.outer() != container)
                    && (mt.outer() instanceof TypeVariable && !((TypeVariable) mt.outer()).bounds().contains(container))) {

                throw new InternalCompilerError("Class " + mt + " has outer class " + mt.outer()
                        + " but is a member of " + container);
            }

            return Collections.singleton(mt);
        }
        Set memberClasses = new HashSet();

        if (container.superType() != null) {
            Set s = findMemberClasses(container.superType().toClass(), name);
            memberClasses.addAll(s);
        }

        for (Iterator i = container.interfaces().iterator(); i.hasNext();) {
            Type it = (Type) i.next();

            Set s = findMemberClasses(it.toClass(), name);
            memberClasses.addAll(s);
        }

        return memberClasses;

    }

    public ImportTable importTable(String sourceName, polyglot.types.Package pkg) {
        assert_(pkg);
        return new JL5ImportTable(this, systemResolver, pkg, sourceName);
    }

    public ImportTable importTable(polyglot.types.Package pkg) {
        assert_(pkg);
        return new JL5ImportTable(this, systemResolver, pkg);
    }

    public ArrayType arrayType(Position pos, Type type) {
        return new JL5ArrayType_c(this, pos, type);
    }

    public boolean isEquivalent(TypeObject arg1, TypeObject arg2) {
        if (arg1 instanceof ArrayType && arg2 instanceof ArrayType) {
            return isEquivalent(((ArrayType) arg1).base(), ((ArrayType) arg2).base());
        }
        if (arg1 instanceof TypeVariable) {
            return ((TypeVariable) arg1).isEquivalent(arg2);
        }
        else if (arg2 instanceof TypeVariable) {
            return ((TypeVariable) arg2).isEquivalent(arg1);
        }
        return this.equals(arg1, arg2);
    }
    

    public MethodInstance findJL5Method(Type container, JL5MethodMatcher matcher) throws SemanticException {
    	assert_(container); 

    	Name name = matcher.name(); 

    	// Filters by method name (JLS 15.12.2.1)
    	// Filters on parameter size (including variable arity) and accessibility
    	// For now we use the JL5MethodMatcher to filter names, however it should
    	// be able to take care of filter on additional properties

    	List<JL5MethodInstance> acceptable = 
    		new ArrayList<JL5MethodInstance>(findMethodsNamed(container, matcher));
    	acceptable = filterPotentiallyApplicable(acceptable, matcher);

    	// JLS 15.12.2-4
    	acceptable = identifyApplicableProcedures(acceptable, matcher);

    	if (acceptable.size() > 0) {
    		Collection<MethodInstance> maximal =
    			findMostSpecificProcedures((List) acceptable, (Matcher<MethodInstance>)matcher, matcher.context());

    		if (maximal.size() > 1) {
    			StringBuffer sb = new StringBuffer();
    			for (Iterator<MethodInstance> i = maximal.iterator(); i.hasNext();) {
    				MethodInstance ma = (MethodInstance) i.next();
    				sb.append(ma.container());
    				sb.append(".");
    				sb.append(ma.signature());
    				if (i.hasNext()) {
    					if (maximal.size() == 2) {
    						sb.append(" and ");
    					}
    					else {
    						sb.append(", ");
    					}
    				}
    			}

    			throw new SemanticException("Reference to " + matcher.name() +
    					" is ambiguous, multiple methods match: "
    					+ sb.toString());
    		}
    		JL5MethodInstance mi = (JL5MethodInstance) maximal.iterator().next();
    		return mi;
    	} else {
    		throw new SemanticException("No valid method call found for " + name + "("
    				+ listToString(matcher.getArgTypes()) + ") in " + container + ".");
    	}
    }
    
    private boolean checkBoxingNeeded(JL5ProcedureInstance pi, List<Type> paramTypes) {
        int numFormals = pi.formalTypes().size();
        for (int i = 0; i < numFormals - 1; i++) {
            Type formal = (Type) pi.formalTypes().get(i);
            Type actual = paramTypes.get(i);
            if (formal.isPrimitive() ^ actual.isPrimitive())
                return true;
        }
        if (pi.isVariableArrity()) {
            Type lastParams = ((JL5ArrayType) pi.formalTypes().get(numFormals - 1)).base();
            for (int i = numFormals - 1; i < paramTypes.size() - 1; i++) {
                if (lastParams.isPrimitive() ^ paramTypes.get(i).isPrimitive())
                    return true;
            }
        }
        else if (numFormals > 0) {
            Type formal = (Type) pi.formalTypes().get(numFormals - 1);
            Type actual = paramTypes.get(numFormals - 1);
            if (formal.isPrimitive() ^ actual.isPrimitive())
                return true;
        }
        return false;
    }

    /**
     * JLS 15.12.2.1
     * 
     * @param allProcedures
     * @param paramTypes
     * @param explicitTypeArgs
     * @param currentClass
     * @return
     */    
    protected <T extends JL5ProcedureInstance> List<T> filterPotentiallyApplicable(
    		List<T> allProcedures, JL5ProcedureMatcher matcher) {
    	List<T> potApplicable = new ArrayList<T>();

    	int numActuals = matcher.getArgTypes().size();
    	List<Type> explicitTypeArgs = matcher.getExplicitTypeArgs();

    	for (T pi : allProcedures) {
    		int numFormals = pi.formalTypes().size();
    		if (!pi.isVariableArrity()) {
    			if (numFormals != numActuals) { 
    				continue;
    			}
    		} else {
    			if (numActuals < numFormals - 1) {
    				continue;
    			}
    		}
    		if (explicitTypeArgs != null && pi.isGeneric()) {
    			if (pi.typeVariables().size() != explicitTypeArgs.size()) {
    				continue;
    			}
    		}
    		if (!isAccessible((MemberInstance<? extends MemberDef>) pi, matcher.context())) {
    			continue;
    		}
    		potApplicable.add(pi);
    	}

    	return potApplicable;
    }

    /**
     * JLS 15.12.2-4
     * 
     * @param <T>
     * @param potApplicable
     * @param paramTypes
     * @param explicitTypeArgs
     * @param currentClass
     * @return
     */
    protected <T extends JL5ProcedureInstance> List<T> identifyApplicableProcedures(
            List<T> potApplicable, JL5ProcedureMatcher matcher) {

        List<T> phase1methods = new ArrayList<T>();
        List<T> phase2methods = new ArrayList<T>();
        List<T> phase3methods = new ArrayList<T>();

        List<Type> argTypes = matcher.getArgTypes();
        List<Type> explicitTypeArgs = matcher.getExplicitTypeArgs();
        
        for (T pi : potApplicable) {
            List<Type> formals = pi.formalTypes();
            List<Type> typeArgs = null;
            boolean boxingNeeded = checkBoxingNeeded(pi, argTypes);
            //capture conversion on argTypes!!!
            List<Type> capParamTypes = new ArrayList<Type>(argTypes);
            for (int i = 0; i < capParamTypes.size(); i++) {
                if (capParamTypes.get(i) instanceof ParameterizedType_c) {
                    ParameterizedType_c pt = (ParameterizedType_c) capParamTypes.get(i);
                    capParamTypes.set(i, pt.capture());
                }
            }
            argTypes = capParamTypes;

            T actualCalledProc;
            if (pi.isGeneric()) {
                if (explicitTypeArgs != null) {
                    typeArgs = explicitTypeArgs;
                } else {
                    typeArgs = inferenceSolver(pi, argTypes).solve();
                }
                boolean badTypeArgs = false;

                actualCalledProc = (T) pi.typeArguments(typeArgs);

                for (int i = 0; i < actualCalledProc.typeVariables().size(); i++) {
                    Type tvBound;
                    if (((MemberInstance) actualCalledProc).container() instanceof GenericTypeRef) {
                        tvBound = getSubstitution(
                        		(GenericTypeRef) ((MemberInstance) actualCalledProc).container(), 
                        		((TypeVariable)actualCalledProc.typeVariables().get(i)).upperBound());
                    } else {
                        tvBound = ((TypeVariable)actualCalledProc.typeVariables().get(i)).upperBound();
                    }
                    if (!isSubtype(typeArgs.get(i), tvBound)) {
                        badTypeArgs = true;
                        break;
                    }
                }
                if (badTypeArgs)
                    continue; //check next procedure
            }
            else {
                actualCalledProc = pi;
            }
            
            //public boolean callValid(ProcedureInstance<? extends ProcedureDef> prototype, Type thisType, List<Type> argTypes, Context context) {
            if (callValid(actualCalledProc, argTypes)) {
                if (!actualCalledProc.isVariableArrity() && !boxingNeeded) {
                    phase1methods.add(actualCalledProc);
                }
                else if (!actualCalledProc.isVariableArrity()) {
                    phase2methods.add(actualCalledProc);
                }
                else {
                    phase3methods.add(actualCalledProc);
                }
            }
        }
        if (phase1methods.size() > 0) {
            return phase1methods;
        }
        else if (phase2methods.size() > 0) {
            return phase2methods;
        }
        else if (phase3methods.size() > 0) {
            return phase3methods;
        }
        return Collections.emptyList();
    }

	/**
	 * Returns methods named after matcher.name();
	 * @param container
	 * @param matcher
	 * @return
	 */
    protected Set<JL5MethodInstance> findMethodsNamed(Type container, MethodMatcher matcher) {
    	assert_(container);

    	Set<JL5MethodInstance> result = new HashSet<JL5MethodInstance>();
    	Set<Type> visitedTypes = new HashSet<Type>();
    	LinkedList<Type> typeQueue = new LinkedList<Type>();
    	typeQueue.addLast(container);
    	Name mthName = matcher.name();
    	while (!typeQueue.isEmpty()) {
    		Type t = (Type) typeQueue.removeFirst();
    		// get methods matching the name
    		if (t instanceof StructType) {
    			StructType type = (StructType) t;
    			if (visitedTypes.contains(type)) {
    				continue;
    			}
    			visitedTypes.add(type);
    			for (Iterator<MethodInstance> i = type.methodsNamed(mthName).iterator(); i.hasNext(); ) {
    				JL5MethodInstance mi = (JL5MethodInstance)i.next();
    				if (mi.name().equals(mthName)) {
    					result.add(mi);
    				}
    			}
    		}
    		// build closure
    		if (t instanceof ObjectType) {
    			ObjectType ot = (ObjectType) t;

    			if (ot.superClass() != null) {
    				typeQueue.addLast(ot.superClass());
    			}

    			typeQueue.addAll(ot.interfaces());
    		}
    	}
    	return result;
    }

    public List<ReferenceType> allAncestorsOf(ReferenceType rt) {
        Set<ReferenceType> ancestors = new HashSet<ReferenceType>();
        ancestors.add(rt);
        ReferenceType superT = (ReferenceType) rt.superType();
        if (superT != null) {
            ancestors.add(superT);
            ancestors.addAll(allAncestorsOf(superT));
        }
        for (Iterator it = rt.interfaces().iterator(); it.hasNext();) {
            ReferenceType inter = (ReferenceType) it.next();
            ancestors.add(inter);
            ancestors.addAll(allAncestorsOf(inter));
        }
        return new ArrayList<ReferenceType>(ancestors);
    }

    public ParameterizedType findGenericSupertype(ReferenceType base, ReferenceType actual_pt) {
        List<ReferenceType> supers = allAncestorsOf(actual_pt);
        for (ReferenceType t : supers) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                if (equals(pt.baseType(), base)) {
                    return pt;
                }
            }
        }
        return null;
    }

    public void sortAnnotations(List annotations, List runtimeAnnotations, List classAnnotations,
            List sourceAnnotations) {
        for (Iterator it = annotations.iterator(); it.hasNext();) {
            AnnotationElem annot = (AnnotationElem) it.next();
            boolean sorted = false;
            List appliedAnnots = ((JL5ParsedClassType) annot.typeName().type()).annotations();
            if (appliedAnnots != null) {
                for (Iterator jt = appliedAnnots.iterator(); jt.hasNext();) {
                    AnnotationElem next = (AnnotationElem) jt.next();
                    if (((ClassType) next.typeName().type()).fullName().equals(QName.make("java.lang.annotation.Retention"))) {
                        for (Iterator elems = ((NormalAnnotationElem) next).elements().iterator(); elems.hasNext();) {
                            ElementValuePair elem = (ElementValuePair) elems.next();
                            if (elem.name().equals("value")) {
                                if (elem.value() instanceof JL5Field) {
                                    Name val = ((JL5Field) elem.value()).name().id();
                                    if (val.equals(Name.make("RUNTIME"))) {
                                        runtimeAnnotations.add(annot);
                                        sorted = true;
                                    }
                                    else if (val.equals(Name.make("SOURCE"))) {
                                        sourceAnnotations.add(annot);
                                        sorted = true;
                                    }
                                    else {
                                        classAnnotations.add(annot);
                                        sorted = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!sorted) {
                classAnnotations.add(annot);
            }
        }
    }

    public boolean needsUnboxing(Type to, Type from) {
        if (to.isPrimitive() && from.isClass())
            return true;
        return false;
    }

    public boolean needsBoxing(Type to, Type from) {
        if (to.isClass() && from.isPrimitive())
            return true;
        return false;
    }

    public Type getSubstitution(GenericTypeRef orig, Type curr) {
        orig = orig.capture();

        if (curr == null || !orig.isGeneric())
            return curr;

        if (orig instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) orig;

            return applySubstitution(curr, pt.typeVariables(), pt.typeArguments());
        }
        else if (orig instanceof RawType) {
            RawType rt = (RawType) orig;
            List<Type> newTypeArgs = new LinkedList<Type>();
            List<TypeVariable> tyvars = rt.typeVariables();
            for (TypeVariable it : tyvars) {
                newTypeArgs.add(it.erasureType());
            }
            return applySubstitution(curr, tyvars, newTypeArgs);
        }
        return curr;
    }

    public static String listToString(List l) {
        StringBuffer sb = new StringBuffer();

        for (Iterator i = l.iterator(); i.hasNext();) {
            Object o = i.next();
            sb.append(o.toString());

            if (i.hasNext()) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    public <T extends Type> List<T> applySubstitution(List<T> listToSub, List<TypeVariable> orig,
            List<Type> typeArgs) {
        List<T> result = new ArrayList<T>();
        for (Type toBeSubed : listToSub) {
            result.add((T) applySubstitution(toBeSubed, orig, typeArgs));
        }
        return result;
    }

    // substitution for parameters
    public Type applySubstitution(Type toBeSubed, List<TypeVariable> orig, List<Type> sub) {
        if (toBeSubed instanceof TypeVariable) {
            for (int i = 0; i < orig.size(); i++) {
                if (orig.get(i).equals(toBeSubed))
                    return (sub.get(i));
            }
        }
        else if (toBeSubed instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) toBeSubed;
            List<Type> newArgs = new ArrayList<Type>();
            for (Type t : pt.typeArguments())
                newArgs.add(applySubstitution(t, orig, sub));

            ParameterizedType newpt = parameterizedType(pt.baseType());
            newpt.typeArguments(newArgs);
            return newpt;
        }
        else if (toBeSubed instanceof ArrayType) {
            ArrayType at = (ArrayType) toBeSubed;
            return arrayType(at.position(), applySubstitution(at.base(), orig, sub));
        }
        else if (toBeSubed instanceof Wildcard) {
            Wildcard wc = (Wildcard) toBeSubed;
            ReferenceType b = wc.bound();
            if (b != null) {
                Wildcard newwc = (Wildcard) wc.copy();
                newwc.bound((ReferenceType) applySubstitution(b, orig, sub));
                return newwc;
            }
        }
        else if (toBeSubed instanceof LubType) {
            LubType lt = (LubType) toBeSubed;
            LubType n = lubType(applySubstitution(lt.lubElements(), orig, sub));
            return n;
        }
        else if (toBeSubed instanceof IntersectionType) {
            IntersectionType it = (IntersectionType) toBeSubed;
            IntersectionType n = intersectionType(applySubstitution(it.bounds(), orig, sub));
            return n;
        }

        return toBeSubed;
    }

    // return the "raw type" version of a given type.
    // if the type is not generic, it is unchanged.
    public ReferenceType rawify(Type t) {
        if (t == null || t instanceof RawType)
            return (RawType) t;
        if (!((JL5ParsedClassType) t).isGeneric())
            return (JL5ParsedClassType) t;
        JL5ParsedClassType bt;
        if (t instanceof ParameterizedType)
            bt = (JL5ParsedClassType) ((ParameterizedType) t).baseType();
        else
            bt = (JL5ParsedClassType) t;
        if (bt.isNested()) {
            bt.outer((ClassType) rawify(bt.outer()));
        }
        return rawType(bt);
    }

	// turn bare occurences of a generic type into a raw type
    public Type rawifyBareGenericType(Type t) {
	if (!(t instanceof JL5ParsedClassType))
	    return t;
	JL5ParsedClassType pt = (JL5ParsedClassType) t;
	if (pt.isGeneric() && (!(pt instanceof ParameterizedType)))
	    return rawType(pt);
	else
	    return pt;
    }

    public List rawifyBareGenericTypeList(List l) {
	List newL = new ArrayList();
	for (Object o : l) {
	    newL.add(rawifyBareGenericType((Type) o));
	}
	return newL;
    }

    public Type erasure(Type t) {
        if (t == null)
            return null;
        if (t instanceof JL5ParsedClassType) {
            JL5ParsedClassType pct = (JL5ParsedClassType) t;
            return rawify(pct);
        }
        else if (t instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) t;
            return erasure(tv.upperBound());
        }
        else if (t instanceof IntersectionType) {
            IntersectionType it = (IntersectionType) t;
            return erasure(it.bounds().get(0));
        }
        else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            return arrayType(null, erasure(at.base()));
        }
        else {
            return t;
        }
    }
    
    public Type capture(Type t) {
        if (t == null) 
            return null;
        if (t instanceof GenericTypeRef) {
            GenericTypeRef gt = (GenericTypeRef) t;
            return gt.capture();
        }
        else {
            return t;
        }
    }

    /**
     * JLS 4.5.1.1
     */
    public boolean checkContains(ParameterizedType child, ParameterizedType ancestor) {
        if (!equals(child.baseType(), ancestor.baseType()))
            return false;
        Iterator<Type> itChild = child.typeArguments().iterator();
        Iterator<Type> itAnc = ancestor.typeArguments().iterator();
        while (itChild.hasNext()) {
            Type argChild = itChild.next();
            Type argAnc = itAnc.next();
            if (argAnc instanceof AnyType)
                continue; // ? contains everything
            if (argAnc instanceof AnySubType) {
                if (argChild instanceof AnySubType) {
                    if (!isSubtype(((AnySubType) argChild).bound(), ((AnySubType) argAnc).bound()))
                        return false;
                }
                else if (!isSubtype(argChild, ((AnySubType) argAnc).bound()))
                    return false;
            }
            else if (argAnc instanceof AnySuperType) {
                if (argChild instanceof AnySuperType) {
                    if (!isSubtype(((AnySuperType) argAnc).bound(), ((AnySuperType) argChild).bound()))
                        return false;
                }
                else if (!isSubtype(((AnySuperType) argAnc).bound(), argChild))
                    return false;
            }
            else if (!equals(argChild, argAnc))
                return false;
        }
        return true;
    }

    @Override
    public boolean descendsFrom(Type child, Type ancestor) {
        if (ancestor instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) ancestor;
            return super.descendsFrom(child, ancestor) || isSubtype(child, tv.lowerBound());
        }
        else {
            return super.descendsFrom(child, ancestor);
        }
    }

    @Override
    public boolean isSubtype(Type t1, Type t2, Context ctx) {
    	//CHECK: Need to double check correctness of this method 

    	// these rules come from each implementation
    	// of Wildcard, LubType and IntersectionType
    	if (t1 instanceof Wildcard) {
    		return false;
    	}

    	if (t1 instanceof LubType) {
    		LubType lubType = (LubType) t1;
    		Type ancestor = t2;
    		for (Type elem : lubType.lubElements()) {
    			if (!isSubtype(elem, ancestor, ctx)) return false;
    		}
    		return true;    		
    	}

    	if (t1 instanceof IntersectionType) {
    		IntersectionType it = (IntersectionType) t1;
    		Type ancestor = t2;
    		for (Type b : it.bounds()) {
    			if (isSubtype(b, ancestor, ctx)) return true;
    		}
    		return false;
    	}

    	// these come from the old implementation of JL5TypeSystem
    	if (t2 instanceof TypeVariable) {
    		TypeVariable tv = (TypeVariable) t2;
    		return super.isSubtype(t1, t2, ctx) || super.isSubtype(t1, tv.lowerBound(), ctx);
    	} else if (t2 instanceof LubType) {
    		LubType lt = (LubType) t2;
    		for (Type e : lt.lubElements()) {
    			if (isSubtype(t1, e, ctx))
    				return true;
    		}
    		return isSubtype(t1, lt.calculateLub(), ctx);
    	} else if (t2 instanceof IntersectionType) {
    		IntersectionType it = (IntersectionType) t2;
    		for (Type b : it.bounds()) {
    			if (!isSubtype(t1, b, ctx))
    				return false;
    		}
    		return true;
    	} else {
    		return super.isSubtype(t1, t2, ctx);
    	}
    }

    public ConstructorInstance findJL5Constructor(Type container, JL5ConstructorMatcher matcher) throws SemanticException {
    	assert_(container);

    	// Get a list of constructor 
    	//        List<JL5ConstructorInstance> cs = ct.constructors();
    	List<JL5ConstructorInstance> acceptable = (List) findAcceptableConstructors(container, matcher);
    	acceptable = filterPotentiallyApplicable(acceptable, matcher);
    	acceptable = identifyApplicableProcedures(acceptable, matcher);
    	Collection<ConstructorInstance> maximal = findMostSpecificProcedures((List)acceptable, matcher, matcher.context());

    	if (maximal.size() > 1) {
    		throw new NoMemberException(NoMemberException.CONSTRUCTOR,
    				"Reference to " + container + " is ambiguous, multiple " +
    				"constructors match: " + maximal);
    	}

    	ConstructorInstance ci = maximal.iterator().next();
    	return ci;
    }

    /**
     * 
     * @param bounds
     * @throws SemanticException
     */
    public boolean checkIntersectionBounds(List<? extends ReferenceType> bounds, boolean quiet)
            throws SemanticException {
        /*        if ((bounds == null) || (bounds.size() == 0)) {
         if (!quiet)
         throw new SemanticException("Intersection type can't be empty");
         return false;
         }*/
        List<ReferenceType> concreteBounds = concreteBounds(bounds);
        if (concreteBounds.size() == 0) {
            if (!quiet)
                throw new SemanticException("Invalid bounds in intersection type.");
            else
                return false;
        }
        for (int i = 0; i < concreteBounds.size(); i++)
            for (int j = i + 1; j < concreteBounds.size(); j++) {
                ReferenceType t1 = concreteBounds.get(i);
                ReferenceType t2 = concreteBounds.get(j);
		    // for now, no checks if at least one is an array type
		if (!t1.isClass() || !t2.isClass()) {
		    return true;
		}
                if (!t1.toClass().flags().isInterface() && !t2.toClass().flags().isInterface()) {
                    if ((!isSubtype(t1, t2)) && (!isSubtype(t2, t1))) {
                        if (!quiet)
                            throw new SemanticException("Error in intersection type. Types " + t1
                                    + " and " + t2 + " are not in subtype relation.");
                        else
                            return false;
                    }
                }
                if (t1.toClass().flags().isInterface() && t2.toClass().flags().isInterface()
                        && (t1 instanceof GenericTypeRef) && (t2 instanceof GenericTypeRef)) {
                    GenericTypeRef j5t1 = (GenericTypeRef) t1;
                    GenericTypeRef j5t2 = (GenericTypeRef) t2;
                    if (j5t1.isGeneric() && j5t2.isGeneric()
                            && j5t1.baseType().equals(j5t2.baseType())) {
                        if (!j5t1.equals(j5t2)) {
                            if (!quiet)
                                throw new SemanticException("Error in intersection type. Interfaces "
                                        + j5t1
                                        + " and "
                                        + j5t2
                                        + "are instantinations of the same generic interface but with different type arguments");
                            else
                                return false;
                        }
                    }
                }
            }
        return true;
    }

    public List<ReferenceType> concreteBounds(List<? extends ReferenceType> bounds) {

        Set<ReferenceType> included = new HashSet<ReferenceType>();
        Set<ReferenceType> visited = new HashSet<ReferenceType>();
        List<ReferenceType> queue = new ArrayList<ReferenceType>(bounds);
        while (!queue.isEmpty()) {
            ReferenceType t = queue.remove(0);
            if (visited.contains(t))
                continue;
            visited.add(t);
            if (t instanceof TypeVariable) {
                TypeVariable tv = (TypeVariable) t;
                queue.addAll(tv.upperBound().bounds());
            }
            else if (t instanceof IntersectionType) {
                IntersectionType it = (IntersectionType) t;
                queue.addAll(it.bounds());
            }
            else {
                included.add(t);
            }
        }
        return new ArrayList<ReferenceType>(included);
    }

    public void checkTVForwardReference(List<TypeVariable> list) throws SemanticException {
        for (int i = 0; i < list.size(); i++) {
            TypeVariable tv = list.get(i);
            for (Type b : tv.bounds()) {
                if (b instanceof TypeVariable) {
                    TypeVariable other_tv = (TypeVariable) b;
                    if (list.indexOf(other_tv) >= i) {
                        throw new SemanticException("Illegal forward reference.", tv.position());
                    }
                }
            }
        }
    }

    public InferenceSolver inferenceSolver(JL5ProcedureInstance pi, List<Type> actuals) {
        return new InferenceSolver_c(pi, actuals, this);
    }
    
    public InferenceSolver inferenceSolver(List<TypeVariable> typeVars, List<Type> formals, List<Type> actuals) {
        return new InferenceSolver_c(typeVars, formals, actuals, this);
    }
    

    public LubType lubType(List<ClassType> lst) {
        return new LubType_c(this, lst);
    }
    
    public TypeEnv env(Context context) {
    	return new JL5TypeEnv_c(context);
        }
    
    public static class TypeVariableEquals implements Predicate2<TypeVariable> {
    	Context context;
    	public TypeVariableEquals(Context context) {
    		this.context = context;
    	}
    	public boolean isTrue(TypeVariable o, TypeVariable p) {
    		JL5TypeSystem ts = (JL5TypeSystem) context.typeSystem();
    		return ts.typeVariableEquals(o, p, context);
    	}
    }
    
    public boolean typeVariableEquals(TypeVariable type1, TypeVariable type2, Context context) {
        IntersectionType bound1 = type1.upperBound();
        IntersectionType bound2 = type2.upperBound();
        TypeSystem ts = context.typeSystem();
        return ts.equals((TypeObject)bound1, (TypeObject)bound2);
    }

    public ConstructorMatcher JL5ConstructorMatcher(Type container, List<Type> argTypes, List<Type> explicitTypeArgs, Context context) {
    	return new JL5ConstructorMatcher(container, argTypes, explicitTypeArgs, context);
    }

    
    public JL5MethodMatcher JL5MethodMatcher(Type container, Name name, List<Type> argTypes, List<Type> explicitTypeArgs, Context context) {
    	return new JL5MethodMatcher(container, name, argTypes, explicitTypeArgs, context);
    }

    
    public static class JL5MethodMatcher extends MethodMatcher implements JL5ProcedureMatcher {
    	protected List<Type> explicitTypeArgs;
    	
    	protected JL5MethodMatcher(Type container, Name name, List<Type> argTypes, List<Type> explicitTypeArgs, Context context) {
    		super(container, name, argTypes, context);
    		this.explicitTypeArgs = explicitTypeArgs;
		}
    	
    	@Override
    	public java.lang.String signature() {
    		// TODO Auto-generated method stub
    		return super.signature();
    	}
    	
    	@Override
    	public MethodInstance instantiate(MethodInstance mi)
    			throws SemanticException {
    		if (! mi.name().equals(name)) {
    			return null;
    		}
    		return mi;
    	}

    	@Override
    	public java.lang.String argumentString() {
    		// TODO Auto-generated method stub
    		return super.argumentString();
    	}
    	
    	public List<Type> getArgTypes() {
    		return this.argTypes;
    	}

    	public List<Type> getExplicitTypeArgs() {
    		return explicitTypeArgs;
    	}
    }
    
    public static class JL5ConstructorMatcher extends ConstructorMatcher implements JL5ProcedureMatcher {
    	protected List<Type> explicitTypeArgs;
    	protected JL5ConstructorMatcher(Type receiverType, List<Type> argTypes, List<Type> explicitTypeArgs, Context context) {
    		super(receiverType, argTypes, context);
    		this.explicitTypeArgs = explicitTypeArgs;
    	}

    	@Override
    	public java.lang.String signature() {
    		// TODO Auto-generated method stub
    		return super.signature();
    	}

    	public ConstructorInstance instantiate(ConstructorInstance ci) throws SemanticException {
    		TypeSystem ts = ci.typeSystem();

    		return ci;
    	}
    	public List<Type> getArgTypes() {
    		return this.argTypes;
    	}

    	public List<Type> getExplicitTypeArgs() {
    		return explicitTypeArgs;
    	}
    }
}