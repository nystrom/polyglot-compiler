package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.ast.ArrayInit;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassLit;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Formal;
import polyglot.ast.Id;
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
import polyglot.ext.jl5.types.reflect.JL5ClassFileLazyClassInitializer;
import polyglot.frontend.Source;
import polyglot.types.ArrayType;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.ImportTable;
import polyglot.types.Matcher;
import polyglot.types.MemberDef;
import polyglot.types.MemberInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.NoMemberException;
import polyglot.types.ObjectType;
import polyglot.types.Package;
import polyglot.types.ParsedClassType;
import polyglot.types.PrimitiveType;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeEnv;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c;
import polyglot.types.Types;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLazyClassInitializer;
import polyglot.util.Copy;
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
		} else {
			return ENUM_ = load("java.lang.Enum");
		}
	}

	public ClassType Annotation() {
		if (ANNOTATION_ != null) {
			return ANNOTATION_;
		} else {
			return ANNOTATION_ = load("java.lang.annotation.Annotation");
		}
	}

	public ClassType Iterable() {
		if (ITERABLE_ != null) {
			return ITERABLE_;
		} else {
			return ITERABLE_ = load("java.lang.Iterable");
		}
	}

	public ClassType Iterator() {
		if (ITERATOR_ != null) {
			return ITERATOR_;
		} else {
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
		} else {
			return INTEGER_WRAPPER = load("java.lang.Integer");
		}
	}

	public ClassType ByteWrapper() {
		if (BYTE_WRAPPER != null) {
			return BYTE_WRAPPER;
		} else {
			return BYTE_WRAPPER = load("java.lang.Byte");
		}
	}

	public ClassType ShortWrapper() {
		if (SHORT_WRAPPER != null) {
			return SHORT_WRAPPER;
		} else {
			return SHORT_WRAPPER = load("java.lang.Short");
		}
	}

	public ClassType BooleanWrapper() {
		if (BOOLEAN_WRAPPER != null) {
			return BOOLEAN_WRAPPER;
		} else {
			return BOOLEAN_WRAPPER = load("java.lang.Boolean");
		}
	}

	public ClassType CharacterWrapper() {
		if (CHARACTER_WRAPPER != null) {
			return CHARACTER_WRAPPER;
		} else {
			return CHARACTER_WRAPPER = load("java.lang.Character");
		}
	}

	public ClassType LongWrapper() {
		if (LONG_WRAPPER != null) {
			return LONG_WRAPPER;
		} else {
			return LONG_WRAPPER = load("java.lang.Long");
		}
	}

	public ClassType DoubleWrapper() {
		if (DOUBLE_WRAPPER != null) {
			return DOUBLE_WRAPPER;
		} else {
			return DOUBLE_WRAPPER = load("java.lang.Double");
		}
	}

	public ClassType FloatWrapper() {
		if (FLOAT_WRAPPER != null) {
			return FLOAT_WRAPPER;
		} else {
			return FLOAT_WRAPPER = load("java.lang.Float");
		}
	}

	public ClassType classOf(Type t) {
		Context context = emptyContext();
		if (t.isClass())
			return (ClassType) t;
		if (typeEquals(t, Float(), context))
			return FloatWrapper();
		if (typeEquals(t, Double(), context))
			return DoubleWrapper();
		if (typeEquals(t, Long(), context))
			return LongWrapper();
		if (typeEquals(t, Int(), context))
			return IntegerWrapper();
		if (typeEquals(t, Short(), context))
			return ShortWrapper();
		if (typeEquals(t, Byte(), context))
			return ByteWrapper();
		if (typeEquals(t, Char(), context))
			return CharacterWrapper();
		if (typeEquals(t, Boolean(), context))
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

	@Override
	public MethodDef methodDef(Position pos,
			Ref<? extends StructType> container, Flags flags,
			Ref<? extends Type> returnType, Name name,
			List<Ref<? extends Type>> argTypes,
			List<Ref<? extends Type>> excTypes) {
		return methodDef(pos, container, flags, returnType, name, argTypes,
				excTypes, new ArrayList<Ref<? extends Type>>(), false);
	}

	@Override
	public ConstructorDef constructorDef(Position pos,
			Ref<? extends ClassType> container, Flags flags,
			List<Ref<? extends Type>> argTypes,
			List<Ref<? extends Type>> excTypes) {
		return constructorDef(pos, container, flags, argTypes, excTypes,
				new ArrayList<Ref<? extends Type>>());
	}

	public MethodDef methodDef(Position pos,
			Ref<? extends StructType> container, Flags flags,
			Ref<? extends Type> returnType, Name name,
			List<Ref<? extends Type>> argTypes,
			List<Ref<? extends Type>> excTypes,
			List<Ref<? extends Type>> tvTypes, boolean compilerGenerated) {

		assert_(container);
		assert_(returnType);
		assert_(argTypes);
		assert_(excTypes);
		assert_(tvTypes);
		return new JL5MethodDef_c(this, pos, container, flags, returnType,
				name, argTypes, excTypes, tvTypes, compilerGenerated);
	}

	public ConstructorDef constructorDef(Position pos,
			Ref<? extends ClassType> container, Flags flags,
			List<Ref<? extends Type>> argTypes,
			List<Ref<? extends Type>> excTypes,
			List<Ref<? extends Type>> tvTypes) {
		assert_(container);
		assert_(argTypes);
		assert_(excTypes);
		return new JL5ConstructorDef_c(this, pos, container, flags, argTypes,
				excTypes, tvTypes);
	}

	/**
	 * Called by the type builder when instantiating types
	 */
	public ParsedClassType createClassType(Position pos,
			Ref<? extends ClassDef> def) {
		return new JL5ParsedClassType_c(this, pos, def);
	}

	public MethodInstance createMethodInstance(Position position,
			Ref<? extends MethodDef> def) {
		return new JL5MethodInstance_c(this, position, def);
	}

	/**
	 * Creates an "erased" version of a method definition
	 */
	public MethodInstance erasureMethodInstance(MethodInstance mi) {
		List<Type> miErasureFormals = new ArrayList<Type>(mi.formalTypes()
				.size());
		for (Iterator<Type> it = mi.formalTypes().iterator(); it.hasNext();) {
			miErasureFormals.add(erasure(it.next()));
		}

		List<Type> miErasureExcTypes = new ArrayList<Type>(mi.throwTypes()
				.size());
		for (Iterator<Type> it = mi.formalTypes().iterator(); it.hasNext();) {
			// CHECK this for loop wasn't here before, assume that was a bug
			// (was just creating an empty list of size throwTypes().size())
			miErasureExcTypes.add(erasure(it.next()));
		}
		Type erasureRet = erasure(mi.returnType());

		mi = mi.formalTypes(miErasureFormals);
		mi = mi.throwTypes(miErasureExcTypes);
		mi = mi.returnType(erasureRet);

		return mi;
	}

	public ClassDef createClassDef(Source fromSource) {
		return new JL5ClassDef_c(this, fromSource);
	}

	protected final Flags TOP_LEVEL_CLASS_FLAGS = JL5Flags
			.setAnnotationModifier(JL5Flags
					.setEnumModifier(super.TOP_LEVEL_CLASS_FLAGS));

	protected final Flags MEMBER_CLASS_FLAGS = JL5Flags
			.setAnnotationModifier(JL5Flags
					.setEnumModifier(super.MEMBER_CLASS_FLAGS));

	public void checkTopLevelClassFlags(Flags f) throws SemanticException {
		if (!f.clear(TOP_LEVEL_CLASS_FLAGS).equals(JL5Flags.NONE)) {
			throw new SemanticException(
					"Cannot declare a top-level class with flag(s) "
							+ f.clear(TOP_LEVEL_CLASS_FLAGS) + ".");
		}

		if (f.isFinal() && f.isInterface()) {
			throw new SemanticException("Cannot declare a final interface.");
		}

		checkAccessFlags(f);
	}

	public void checkMemberClassFlags(Flags f) throws SemanticException {
		if (!f.clear(MEMBER_CLASS_FLAGS).equals(JL5Flags.NONE)) {
			throw new SemanticException(
					"Cannot declare a member class with flag(s) "
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

	public ConstructorDef defaultConstructor(Position pos,
			Ref<? extends ClassType> container) {
		assert_(container);

		Flags flags = ((ClassType)container.get()).flags();
		// CHECK do we really need to extend or does isEnum => access is private and delegating to super is ok ?
		if (JL5Flags.isEnumModifier(flags)) {
			Flags access = Flags.NONE;
			access = access.Private();
			return constructorDef(pos, container, access, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
		} else {
			return super.defaultConstructor(pos, container);
		}
	}

	public boolean typeExtendsAnnotation(Type t) {
		if (t instanceof ClassType) {
			ClassType ct = (ClassType) t;
			return ((ClassType) ct.superClass()).fullName().equals(
					QName.make("java.lang.annotation.Annotation"));
		}

		return false;
	}

	@Override
	public ClassFileLazyClassInitializer classFileLazyClassInitializer(
			ClassFile clazz) {
		return new JL5ClassFileLazyClassInitializer(clazz, this);
	}

	@Override
	protected PrimitiveType createPrimitive(Name name) {
		return new JL5PrimitiveType_c(this, name);
	}

	/**
	 * Support additional checks for enum
	 */
	@Override
	public void checkClassConformance(ClassType ct, Context context)
			throws SemanticException {

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
			for (Iterator<EnumInstance> it = enumConsts.iterator(); it
					.hasNext();) {
				EnumInstance ei = it.next();
				if (ei.anonType() != null) {
					allAnonNull = false;
					break;
				}
			}

			if (allAnonNull) {
				// That's the simplest case when enums are defined without
				// declaring any ClassBody
				super.checkClassConformance(ct, context);
			} else {
				// if enum type declares abstract method ensure
				// !!every!! enum constant decl declares anon body
				// and !!every!! body implements this abstract
				// method
				for (Iterator<MethodInstance> it = ct.methods().iterator(); it
						.hasNext();) {
					MethodInstance mi = it.next();
					if (!mi.flags().isAbstract())
						continue;
					for (Iterator<EnumInstance> jt = enumConsts.iterator(); jt
							.hasNext();) {
						EnumInstance ei = jt.next();
						// Enum doesn't declare any ClassBody so it cannot
						// implement the abstract method
						if (ei.anonType() == null) {
							throw new SemanticException(
									"Enum constant decl: "
											+ ei.name()
											+ " must declare an anonymous subclass of: "
											+ ct
											+ " and implement the abstract method: "
											+ mi, ei.position());
						} else {
							boolean implFound = false;
							// check if enum's anonType implements the abstract
							// method
							for (Iterator<MethodInstance> kt = ei.anonType()
									.methods().iterator(); kt.hasNext();) {
								MethodInstance mj = kt.next();
								if (canOverride(mj, mi, context)) {
									implFound = true;
								}
							}
							if (!implFound) {
								throw new SemanticException(
										"Enum constant decl anonymous subclass must implement method: "
												+ mi, ei.position());
							}
						}
					}
				}

				// still need to check superInterfaces to ensure this
				// class implements the methods except they can't be
				// abstract (previous checks ensure okay)
				List<Type> superInterfaces = abstractSuperInterfaces(ct);
				for (Iterator<Type> it = superInterfaces.iterator(); it
						.hasNext();) {
					Type t = (Type) it.next();
					if (t instanceof StructType) {
						StructType rt = (StructType) it;
						if (typeEquals(rt, ct, context))
							continue;
						for (Iterator<MethodInstance> jt = rt.methods()
								.iterator(); jt.hasNext();) {
							MethodInstance mi = jt.next();
							if (!mi.flags().isAbstract())
								continue;

							boolean implFound = false;
							// don't need to look in super classes as the only
							// one is java.lang.Enum so just look here and
							// there

							for (Iterator<MethodInstance> kt = ct.methods()
									.iterator(); kt.hasNext();) {
								MethodInstance mj = kt.next();
								if ((canOverride(mj, mi, context))) {
									implFound = true;
									break;
								}
							}
							for (Iterator<MethodInstance> kt = ct.superClass()
									.toClass().methods().iterator(); !implFound
									&& kt.hasNext();) {
								MethodInstance mj = kt.next();
								if ((canOverride(mj, mi, context))) {
									implFound = true;
									break;
								}
							}

							if (!implFound) {
								throw new SemanticException(
										ct.fullName()
												+ " should be declared abstract: it does not define: "
												+ mi.signature()
												+ ", which is declared in "
												+ rt.toClass().fullName(),
										ct.position());
							}
						}
					}
				}
			}
		} else {
			super.checkClassConformance(ct, context);
		}
	}

	/**
	 * Returns a new use of an enum def
	 */
	public EnumInstance enumInstance(Position pos, Ref<? extends FieldDef> def) {
		return new EnumInstance_c(this, pos, def);
	}

	public AnnotationElemInstance findAnnotation(Type container,
			AnnotationMatcher matcher) throws SemanticException {
		Collection<AnnotationElemInstance> annotations = findAnnotations(
				container, matcher);
		Name name = matcher.name();

		if (annotations.size() == 0) {
			throw new NoMemberException(JL5NoMemberException.ANNOTATION,
					"Annotation \"" + name + "\" not found in type \""
							+ container + "\".");
		}
		Iterator<AnnotationElemInstance> i = annotations.iterator();
		AnnotationElemInstance ai = i.next();

		if (i.hasNext()) {
			AnnotationElemInstance ai2 = (AnnotationElemInstance) i.next();

			throw new SemanticException("Annotation \"" + name
					+ "\" is ambiguous; it is defined in both "
					+ ai.container() + " and " + ai2.container() + ".");
		}

		Context context = matcher.context();
		if (context != null && !isAccessible(ai, context)) {
			throw new SemanticException("Cannot access " + ai + ".");
		}
		return ai;
	}

	/**
	 * Returns a set of annotations named <code>name</code> defined in type
	 * <code>container</code> or a supertype. The list returned may be empty.
	 */
	protected Set<AnnotationElemInstance> findAnnotations(Type container,
			AnnotationMatcher matcher) {
		Name name = matcher.name();

		Context context = matcher.context();
		assert_(container);

		if (container == null) {
			throw new InternalCompilerError("Cannot access annotation \""
					+ name + "\" within a null container type.");
		}

		if (container instanceof JL5ParsedClassType) {
			AnnotationElemInstance fi = ((JL5ParsedClassType) container)
					.annotationElemNamed(name);
			if (fi != null) {
				try {
					fi = matcher.instantiate(fi);
					if (fi != null)
						return Collections.singleton(fi);
				} catch (SemanticException e) {
				}
				return Collections.EMPTY_SET;
			}
		}
		// CHECK previous code wasn't recursing on super

		// if not found look in super classes
		Set<AnnotationElemInstance> annot = new HashSet<AnnotationElemInstance>();

		if (container instanceof ObjectType) {
			ObjectType ot = (ObjectType) container;
			if (ot.superClass() != null
					&& ot.superClass() instanceof StructType) {
				Set<AnnotationElemInstance> superFields = findAnnotations(
						(StructType) ot.superClass(), matcher);
				annot.addAll(superFields);
			}

			for (Type it : ot.interfaces()) {
				if (it instanceof StructType) {
					Set<AnnotationElemInstance> superFields = findAnnotations(
							(StructType) it, matcher);
					annot.addAll(superFields);
				}
			}
		}

		return annot;
	}

	public AnnotationElemInstance annotationElemInstance(Position pos,
			ClassType ct, Flags f, Type type, Id name, boolean hasDefault) {
		assert_(ct);
		assert_(type);
		return new AnnotationElemInstance_c(this, pos, ct, f, type, name.id(),
				hasDefault);
	}

	/**
	 * Rely on JL5Context to support additional JL5 features
	 */
	public Context createContext() {
		return new JL5Context_c(this);
	}

	public FieldInstance findFieldOrEnum(Type container,
			TypeSystem_c.FieldMatcher matcher) throws SemanticException {
		FieldInstance fi = null;
		try {
			fi = super.findField(container, matcher);
		} catch (NoMemberException e) {
			// nothing in fields, look for enums
			EnumMatcher enumMatcher = EnumMatcher(container, matcher.name(),
					matcher.context());
			fi = findEnumConstant(container, enumMatcher);
		}
		return fi;
	}

	/**
	 * find enum constant in a container matching matcher requisites
	 * 
	 * @param container
	 * @param matcher
	 * @return
	 * @throws SemanticException
	 */
	public EnumInstance findEnumConstant(Type container, EnumMatcher matcher)
			throws SemanticException {
		Context context = matcher.context();

		Collection<EnumInstance> enumConstants = findEnumConstants(container,
				matcher);

		if (enumConstants.size() == 0) {
			throw new NoMemberException(JL5NoMemberException.ENUM_CONSTANT,
					"Enum Constant " + matcher.signature()
							+ " not found in type \"" + container + "\".");
		}

		Iterator<EnumInstance> i = enumConstants.iterator();
		EnumInstance fi = i.next();

		if (i.hasNext()) {
			EnumInstance fi2 = i.next();

			throw new SemanticException("Enum Constant " + matcher.name()
					+ " is ambiguous; it is defined in both " + fi.container()
					+ " and " + fi2.container() + ".");
		}

		if (context != null && !isAccessible(fi, context)) {
			throw new SemanticException("Cannot access " + fi + ".");
		}

		return fi;
	}

	/**
	 * Returns a set of enum constant named <code>name</code> defined in type
	 * <code>container</code> or a supertype. The list returned may be empty.
	 */
	protected Set<EnumInstance> findEnumConstants(Type container,
			EnumMatcher matcher) {
		Name name = matcher.name();

		Context context = matcher.context();
		assert_(container);

		if (container == null) {
			throw new InternalCompilerError("Cannot access field \"" + name
					+ "\" within a null container type.");
		}

		if (container instanceof JL5ParsedClassType) {
			EnumInstance fi = ((JL5ParsedClassType) container)
					.enumConstantNamed(name);
			if (fi != null) {
				try {
					fi = matcher.instantiate(fi);
					if (fi != null)
						return Collections.singleton(fi);
				} catch (SemanticException e) {
				}
				return Collections.EMPTY_SET;
			}
		}
		// CHECK previous code wasn't recursing on super
		Set<EnumInstance> enums = new HashSet<EnumInstance>();

		if (container instanceof ObjectType) {
			ObjectType ot = (ObjectType) container;
			if (ot.superClass() != null
					&& ot.superClass() instanceof StructType) {
				Set<EnumInstance> superFields = findEnumConstants(
						(StructType) ot.superClass(), matcher);
				enums.addAll(superFields);
			}

			for (Type it : ot.interfaces()) {
				if (it instanceof StructType) {
					Set<EnumInstance> superFields = findEnumConstants(
							(StructType) it, matcher);
					enums.addAll(superFields);
				}
			}
		}

		return enums;
	}

	/**
	 * Called by Signature when doing in reflect when reading class from bytecode
	 */
	@Override
    public TypeVariable typeVariable(Position pos, String name, List bounds) {
        return new TypeVariable_c(this, pos, name, bounds);
    }

	public TypeVariable typeVariable(Position pos, String name,
			Ref<? extends ClassDef> def, List bounds) {
		return new TypeVariable_c(this, pos, Name.make(name), def, bounds);
	}

	public IntersectionType intersectionType(Ref<? extends ClassDef> def, List<ClassType> bounds) {
		// CHECK need to think about how we initialize this stuff
		return new IntersectionType_c(this, def.get().position(), def, bounds);
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
			if (((JL5ParsedClassType) t).fullName().equals(
					QName.make("java.lang.String")))
				return true;
			if (((JL5ParsedClassType) t).fullName().equals(
					QName.make("java.lang.Class")))
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
			return env(context).numericConversionValid(((ArrayType) t).base(),
					value);
		}
		return false;
	}

	public void checkDuplicateAnnotations(List annotations)
			throws SemanticException {
		// check no duplicate annotations used
		ArrayList l = new ArrayList(annotations);
		for (int i = 0; i < l.size(); i++) {
			AnnotationElem ai = (AnnotationElem) l.get(i);
			for (int j = i + 1; j < l.size(); j++) {
				AnnotationElem aj = (AnnotationElem) l.get(j);
				if (ai.typeName().type() == aj.typeName().type()) {
					throw new SemanticException("Duplicate annotation use: "
							+ aj.typeName(), aj.position());
				}
			}
		}
	}

	public void checkValueConstant(Expr value) throws SemanticException {
		if (value instanceof ArrayInit) {
			// check elements
			for (Iterator it = ((ArrayInit) value).elements().iterator(); it
					.hasNext();) {
				Expr next = (Expr) it.next();
				if ((!next.isConstant() || next == null || next instanceof NullLit)
						&& !(next instanceof ClassLit)) {
					throw new SemanticException(
							"Annotation attribute value must be constant",
							value.position());
				}
			}
		} else if ((!value.isConstant() || value == null || value instanceof NullLit)
				&& !(value instanceof ClassLit)) {
			// for purposes of annotation elems class lits are constants
			throw new SemanticException(
					"Annotation attribute value must be constant",
					value.position());
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
		List applAnnots = ((JL5ParsedClassType) annotation.typeName().type())
				.annotations();
		// if there are no annotations applied to this annotation type then
		// there is no need to check the target type of the annotation
		if (applAnnots != null) {

			for (Iterator it = applAnnots.iterator(); it.hasNext();) {
				AnnotationElem next = (AnnotationElem) it.next();
				if (((ClassType) next.typeName().type()).fullName().equals(
						QName.make("java.lang.annotation.Target"))) {
					if (next instanceof NormalAnnotationElem) {
						for (Iterator elems = ((NormalAnnotationElem) next)
								.elements().iterator(); elems.hasNext();) {
							ElementValuePair elemVal = (ElementValuePair) elems
									.next();
							if (elemVal.value() instanceof JL5Field) {
								Name check = ((JL5Field) elemVal.value())
										.name().id();
								appCheckValue(check, n);
							} else if (elemVal.value() instanceof ArrayInit) {
								ArrayInit val = (ArrayInit) elemVal.value();
								if (val.elements().isEmpty()) {
									// automatically throw exception
									// this annot cannot be applied anywhere
									throw new SemanticException(
											"Annotation type not applicable to this kind of declaration",
											n.position());
								} else {
									for (Iterator vals = val.elements()
											.iterator(); vals.hasNext();) {
										Object nextVal = vals.next();
										if (nextVal instanceof JL5Field) {
											Name valCheck = ((JL5Field) nextVal)
													.name().id();
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
		if (((ClassType) annotation.typeName().type()).fullName().equals(
				QName.make("java.lang.Override"))) {
			appCheckOverride(n);
		}
	}

	private void appCheckValue(Name val, Node n) throws SemanticException {
		if (val.equals(Name.make("ANNOTATION_TYPE"))) {
			if (!(n instanceof ClassDecl)
					|| !JL5Flags.isAnnotationModifier(((ClassDecl) n).flags()
							.flags())) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		} else if (val.equals(Name.make("CONSTRUCTOR"))) {
			if (!(n instanceof ConstructorDecl)) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		} else if (val.equals(Name.make("FIELD"))) {
			if (!(n instanceof FieldDecl)) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		} else if (val.equals(Name.make("LOCAL_VARIABLE"))) {
			if (!(n instanceof LocalDecl)) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		} else if (val.equals(Name.make("METHOD"))) {
			if (!(n instanceof MethodDecl)) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		} else if (val.equals(Name.make("PACKAGE"))) {
		} else if (val.equals(Name.make("PARAMETER"))) {
			if (!(n instanceof Formal)) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		} else if (val.equals(Name.make("TYPE"))) {
			if (!(n instanceof ClassDecl)) {
				throw new SemanticException(
						"Annotation type not applicable to this kind of declaration",
						n.position());
			}
		}
	}

	private void appCheckOverride(Node n) throws SemanticException {
		MethodDecl md = (MethodDecl) n; // the other check should
		// prevent anything else
		MethodDef def = md.methodDef();
		MethodInstance mi = def.asInstance();
		ClassType container = (ClassType) mi.container();
		ClassType sc = (ClassType) container.superClass();

		try {	
	        // CHECK explicitTypeArgs and context set to null
			findMethod(sc, new JL5MethodMatcher(sc, mi.name(), mi.formalTypes(), null, null));
		} catch (NoMemberException e) {
			throw new SemanticException(
					"method does not override a method from its superclass",
					md.position());
		}
	}

	public boolean equivalent(Type fromType, Type toType) {
		Context context = emptyContext();
		return env(context).equivalent(fromType, toType);
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

	/** These are from polyglot 2.4
	public ClassType findMemberClass(ClassType container, String name,
			ClassType currClass) throws SemanticException
			{
		assert_(container);
		
		//ClassType type, ClassType accessor
		Named n = classContextResolver(container, currClass).find(name);

		if (n instanceof ClassType) {
			return (ClassType) n;
		}

		throw new NoClassException(name, container);
			}

	public ClassType findMemberClass(ClassType container, String name)
	throws SemanticException {

		return findMemberClass(container, name, (ClassType) null);
	}
	**/

//	from context:
//	findInThisScope(ts.TypeMatcher(name))
//	findInThisScope(Matcher<Named> matcher)
//	
//	public ClassType findMemberClass(ClassType container, String name,
//			ClassType currClass) throws SemanticException {
//		if (container instanceof ParameterizedType) {
//			// How do we get a context to call findInThisScope ?
//			return container.findInThisScope(ts.TypeMatcher(name));
//			// Find a type object in the context of the class.
//			// find 'name' in the context of container and 
//			return super
//					.findMemberClass(
//							((ParameterizedType) container).baseType(), name,
//							currClass);
//		}
//		return super.findMemberClass(container, name, currClass);
//	}

	/**
	// This is related to static imports and code commented in JL5Import_c
	// CHECK should be replaced by a call to something like
	// context.findInThisScope(ts.TypeMatcher(name));
	public Set findMemberClasses(ClassType container, String name)
			throws SemanticException {
		ClassType mt = container.memberClassNamed(name);

		if (mt != null) {
			if (!mt.isMember()) {
				throw new InternalCompilerError("Class " + mt
						+ " is not a member class, " + " but is in "
						+ container + "\'s list of members.");
			}

			// CHECK diff from 1.3 returns Set<ClassType>
			// Think this should be extracted to a new TypeMatcher
			if ((mt.outer() != container)
					&& (mt.outer() instanceof TypeVariable && !((TypeVariable) mt
							.outer()).bounds().contains(container))) {

				throw new InternalCompilerError("Class " + mt
						+ " has outer class " + mt.outer()
						+ " but is a member of " + container);
			}
			// END diff

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
	**/

	@Override
	public ImportTable importTable(Ref<? extends Package> pkg) {
		assert_(pkg);
		return new JL5ImportTable(this, pkg);
	}
	
	@Override
	public ImportTable importTable(String sourceName, Ref<? extends Package> pkg) {
		assert_(pkg);
		return new JL5ImportTable(this, pkg, sourceName);
	}
	
	Map<Ref<? extends Type>, Type> varargsArrayTypeCache = new HashMap<Ref<? extends Type>, Type>();

	/**
	 * Factory method for ArrayTypes with varargs support. We maintain a
	 * separate cache for varargs array type.
	 */
	public ArrayType createArrayType(Position pos, Ref<? extends Type> type, boolean varargs) {
		if (varargs) {
			ArrayType t = (ArrayType) varargsArrayTypeCache.get(type);
			if (t == null) {
				t = createArrayTypeImpl(pos, type, varargs);
				varargsArrayTypeCache.put(type, t);
			}
			return t;
		} else {
			return super.arrayType(pos, type);
		}
	}

    protected ArrayType createArrayTypeImpl(Position pos, Ref<? extends Type> type, boolean varargs) {
    	return new JL5ArrayType_c(this, pos, type, varargs);
    }

	@Override
    protected ArrayType createArrayType(Position pos, Ref<? extends Type> type) {
    	return createArrayTypeImpl(pos, type, false);
    }

	public boolean isEquivalent(TypeObject arg1, TypeObject arg2) {
		if (arg1 instanceof ArrayType && arg2 instanceof ArrayType) {
			return isEquivalent(((ArrayType) arg1).base(),
					((ArrayType) arg2).base());
		}
		if (arg1 instanceof TypeVariable) {
			return ((TypeVariable) arg1).isEquivalent(arg2);
		} else if (arg2 instanceof TypeVariable) {
			return ((TypeVariable) arg2).isEquivalent(arg1);
		}
		return this.equals(arg1, arg2);
	}

	@Override
	public MethodInstance findMethod(Type container, MethodMatcher matcher)
			throws SemanticException {
		assert_(container);

		Name name = matcher.name();

		// Filters by method name (JLS 15.12.2.1)
		// Filters on parameter size (including variable arity) and
		// accessibility
		// For now we use the JL5MethodMatcher to filter names, however it
		// should
		// be able to take care of filter on additional properties

		List<JL5MethodInstance> acceptable = new ArrayList<JL5MethodInstance>(
				findMethodsNamed(container, matcher));
		acceptable = filterPotentiallyApplicable(acceptable, (JL5ProcedureMatcher) matcher);

		// JLS 15.12.2-4
		acceptable = identifyApplicableProcedures(acceptable, (JL5ProcedureMatcher) matcher);

		if (acceptable.size() > 0) {
			Collection<MethodInstance> maximal = findMostSpecificProcedures(
					(List) acceptable, (Matcher<MethodInstance>) matcher,
					matcher.context());

			if (maximal.size() > 1) {
				StringBuffer sb = new StringBuffer();
				for (Iterator<MethodInstance> i = maximal.iterator(); i
						.hasNext();) {
					MethodInstance ma = (MethodInstance) i.next();
					sb.append(ma.container());
					sb.append(".");
					sb.append(ma.signature());
					if (i.hasNext()) {
						if (maximal.size() == 2) {
							sb.append(" and ");
						} else {
							sb.append(", ");
						}
					}
				}

				throw new SemanticException("Reference to " + matcher.name()
						+ " is ambiguous, multiple methods match: "
						+ sb.toString());
			}
			JL5MethodInstance mi = (JL5MethodInstance) maximal.iterator()
					.next();
			return mi;
		} else {
			throw new SemanticException("No valid method call found for "
					+ name + "(" + listToString(((JL5MethodMatcher)matcher).getArgTypes())
					+ ") in " + container + ".");
		}
	}

	private boolean checkBoxingNeeded(JL5ProcedureInstance pi,
			List<Type> paramTypes) {
		int numFormals = pi.formalTypes().size();
		for (int i = 0; i < numFormals - 1; i++) {
			Type formal = (Type) pi.formalTypes().get(i);
			Type actual = paramTypes.get(i);
			if (formal.isPrimitive() ^ actual.isPrimitive())
				return true;
		}
		if (pi.isVariableArrity()) {
			Type lastParams = ((JL5ArrayType) pi.formalTypes().get(
					numFormals - 1)).base();
			for (int i = numFormals - 1; i < paramTypes.size() - 1; i++) {
				if (lastParams.isPrimitive() ^ paramTypes.get(i).isPrimitive())
					return true;
			}
		} else if (numFormals > 0) {
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
			if (!isAccessible((MemberInstance<? extends MemberDef>) pi,
					matcher.context())) {
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
			// capture conversion on argTypes!!!
			List<Type> capParamTypes = new ArrayList<Type>(argTypes);
			for (int i = 0; i < capParamTypes.size(); i++) {
				if (capParamTypes.get(i) instanceof ParameterizedType_c) {
					ParameterizedType_c pt = (ParameterizedType_c) capParamTypes
							.get(i);
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
								(GenericTypeRef) ((MemberInstance) actualCalledProc)
										.container(),
								((TypeVariable) actualCalledProc
										.typeVariables().get(i)).upperBound());
					} else {
						tvBound = ((TypeVariable) actualCalledProc
								.typeVariables().get(i)).upperBound();
					}
					if (!isSubtype(typeArgs.get(i), tvBound, matcher.context())) {
						badTypeArgs = true;
						break;
					}
				}
				if (badTypeArgs)
					continue; // check next procedure
			} else {
				actualCalledProc = pi;
			}

			if (callValid(actualCalledProc, matcher.container(), argTypes, matcher.context())) {
				if (!actualCalledProc.isVariableArrity() && !boxingNeeded) {
					phase1methods.add(actualCalledProc);
				} else if (!actualCalledProc.isVariableArrity()) {
					phase2methods.add(actualCalledProc);
				} else {
					phase3methods.add(actualCalledProc);
				}
			}
		}
		if (phase1methods.size() > 0) {
			return phase1methods;
		} else if (phase2methods.size() > 0) {
			return phase2methods;
		} else if (phase3methods.size() > 0) {
			return phase3methods;
		}
		return Collections.emptyList();
	}

	/**
	 * Returns methods named after matcher.name();
	 * 
	 * @param container
	 * @param matcher
	 * @return
	 */
	protected Set<JL5MethodInstance> findMethodsNamed(Type container,
			MethodMatcher matcher) {
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
				for (Iterator<MethodInstance> i = type.methodsNamed(mthName)
						.iterator(); i.hasNext();) {
					JL5MethodInstance mi = (JL5MethodInstance) i.next();
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

	public List<ObjectType> allAncestorsOf(Type t) {
		ObjectType rt = (ObjectType) t;
		Set<ObjectType> ancestors = new HashSet<ObjectType>();
		ancestors.add(rt);
		ObjectType superT = (ObjectType) rt.superClass();
		if (superT != null) {
			ancestors.add(superT);
			ancestors.addAll(allAncestorsOf(superT));
		}
		for (Iterator<Type> it = rt.interfaces().iterator(); it.hasNext();) {
			ObjectType inter = (ObjectType) it.next();
			ancestors.add(inter);
			ancestors.addAll(allAncestorsOf(inter));
		}
		return new ArrayList<ObjectType>(ancestors);
	}

	public ParameterizedType findGenericSupertype(ObjectType base,
			ObjectType actual_pt) {
		List<ObjectType> supers = allAncestorsOf(actual_pt);
		for (ObjectType t : supers) {
			if (t instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) t;
				if (typeEquals(pt.baseType(), base, emptyContext())) {
					return pt;
				}
			}
		}
		return null;
	}

	public void sortAnnotations(List annotations, List runtimeAnnotations,
			List classAnnotations, List sourceAnnotations) {
		for (Iterator it = annotations.iterator(); it.hasNext();) {
			AnnotationElem annot = (AnnotationElem) it.next();
			boolean sorted = false;
			List appliedAnnots = ((JL5ParsedClassType) annot.typeName().type())
					.annotations();
			if (appliedAnnots != null) {
				for (Iterator jt = appliedAnnots.iterator(); jt.hasNext();) {
					AnnotationElem next = (AnnotationElem) jt.next();
					if (((ClassType) next.typeName().type()).fullName().equals(
							QName.make("java.lang.annotation.Retention"))) {
						for (Iterator elems = ((NormalAnnotationElem) next)
								.elements().iterator(); elems.hasNext();) {
							ElementValuePair elem = (ElementValuePair) elems
									.next();
							if (elem.name().equals("value")) {
								if (elem.value() instanceof JL5Field) {
									Name val = ((JL5Field) elem.value()).name()
											.id();
									if (val.equals(Name.make("RUNTIME"))) {
										runtimeAnnotations.add(annot);
										sorted = true;
									} else if (val.equals(Name.make("SOURCE"))) {
										sourceAnnotations.add(annot);
										sorted = true;
									} else {
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
		return to.isPrimitive() && from.isClass();
	}

	public boolean needsBoxing(Type to, Type from) {
		return to.isClass() && from.isPrimitive();
	}

	public Type getSubstitution(GenericTypeRef orig, Type curr) {
		orig = orig.capture();

		if (curr == null || !orig.isGeneric())
			return curr;

		if (orig instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) orig;

			return applySubstitution(curr, pt.typeVariables(),
					pt.typeArguments());
		} else if (orig instanceof RawType) {
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


	@Override
	public <T extends Type> List<T> applySubstitution(List<T> listToSub,
			List<TypeVariable> orig, List<Type> typeArgs) {
		List<T> result = new ArrayList<T>();
		for (Type toBeSubed : listToSub) {
			result.add((T) applySubstitution(toBeSubed, orig, typeArgs));
		}
		return result;
	}

	// substitution for parameters
	public Type applySubstitution(Type toBeSubed, List<TypeVariable> orig,
			List<Type> sub) {
		if (toBeSubed instanceof TypeVariable) {
			for (int i = 0; i < orig.size(); i++) {
				if(typeEquals(orig.get(i), toBeSubed, emptyContext()))
					return (sub.get(i));
			}
		} else if (toBeSubed instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) toBeSubed;
			List<Type> newArgs = new ArrayList<Type>();
			for (Type t : pt.typeArguments())
				newArgs.add(applySubstitution(t, orig, sub));

			ParameterizedType newpt = parameterizedType(pt.baseType());
			newpt.typeArguments(newArgs);
			return newpt;
		} else if (toBeSubed instanceof ArrayType) {
			ArrayType at = (ArrayType) toBeSubed;
			return arrayOf(at.position(),
					applySubstitution(at.base(), orig, sub));
		} else if (toBeSubed instanceof Wildcard) {
			Wildcard wc = (Wildcard) toBeSubed;
			ReferenceType b = wc.bound();
			if (b != null) {
				Wildcard newwc = (Wildcard) wc.copy();
				newwc.bound((ReferenceType) applySubstitution(b, orig, sub));
				return newwc;
			}
		} else if (toBeSubed instanceof LubType) {
			LubType lt = (LubType) toBeSubed;
			LubType n = lubType(applySubstitution(lt.lubElements(), orig, sub));
			return n;
		} else if (toBeSubed instanceof IntersectionType) {
			IntersectionType it = (IntersectionType) toBeSubed;
			IntersectionType n = intersectionType(Types.ref(((IntersectionType) toBeSubed).def()), applySubstitution(
					it.bounds(), orig, sub));
			return n;
		}

		return toBeSubed;
	}

	// return the "raw type" version of a given type.
	// if the type is not generic, it is unchanged.
	public ClassType rawify(Type t) {
		// Already raw
		if (t == null || t instanceof RawType) {
			return (RawType) t;
		}
		// Not generic
		if (!((JL5ParsedClassType) t).isGeneric()) {
			return (JL5ParsedClassType) t;
		}
		// Parameterized
		JL5ParsedClassType bt;
		if (t instanceof ParameterizedType) {
			bt = (JL5ParsedClassType) ((ParameterizedType) t).baseType();			
		} else {
			bt = (JL5ParsedClassType) t;
		}
		
		// and nested
		if (bt.isNested()) {
			ClassDef outerRawDef = rawify(bt.outer()).def();
			ClassDef currentDefCopy = (ClassDef) bt.def().copy();
			currentDefCopy.outer(Types.ref(outerRawDef));
			bt = (JL5ParsedClassType) currentDefCopy.asType();
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
		} else if (t instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) t;
			return erasure(tv.upperBound());
		} else if (t instanceof IntersectionType) {
			IntersectionType it = (IntersectionType) t;
			return erasure(it.bounds().get(0));
		} else if (t instanceof ArrayType) {
			ArrayType at = (ArrayType) t;
			return arrayOf(null, erasure(at.base()));
		} else {
			return t;
		}
	}

	public Type capture(Type t) {
		if (t == null)
			return null;
		if (t instanceof GenericTypeRef) {
			GenericTypeRef gt = (GenericTypeRef) t;
			return gt.capture();
		} else {
			return t;
		}
	}

	/**
	 * JLS 4.5.1.1
	 */
	public boolean checkContains(ParameterizedType child,
			ParameterizedType ancestor) {
		Context ctx = emptyContext();
		if (!typeEquals(child.baseType(), ancestor.baseType(), ctx))
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
					if (!isSubtype(((AnySubType) argChild).bound(),
							((AnySubType) argAnc).bound(), ctx))
						return false;
				} else if (!isSubtype(argChild, ((AnySubType) argAnc).bound(), ctx))
					return false;
			} else if (argAnc instanceof AnySuperType) {
				if (argChild instanceof AnySuperType) {
					if (!isSubtype(((AnySuperType) argAnc).bound(),
							((AnySuperType) argChild).bound(), ctx))
						return false;
				} else if (!isSubtype(((AnySuperType) argAnc).bound(), argChild, ctx))
					return false;
			} else if (!typeEquals(argChild, argAnc, ctx))
				return false;
		}
		return true;
	}

	@Override
	public boolean descendsFrom(ClassDef child, ClassDef ancestor) {
		Context ctx = emptyContext();
		Type childT = child.asType();
		Type ancestorT = ancestor.asType();

		if(child instanceof RawType) {
	        if (super.descendsFrom(child, ancestor))
	            return true;
	        // if the ancestor's associated raw type is in the set
	        // then we allow it
	        if (ancestor instanceof ParameterizedType
		    || (ancestor instanceof JL5ParsedClassType && !(ancestor instanceof RawType) &&
			((JL5ParsedClassType)ancestor).isGeneric())) {
	            return this.isSubtype(childT, this.rawify(ancestorT), ctx);
	        }
	        return false;			
		}

		if(child instanceof ParameterizedType) {
	        if (super.descendsFrom(child, ancestor))
	            return true;
	        // if the ancestor is a raw type and some corresponding
	        // parameterized type is in the set then we allow it
	        if (ancestor instanceof RawType) {
	            if (this.isSubtype(this.rawify(childT), ancestorT, ctx)) {
	                return true;
	            }
	        }
	        else if ((ancestor instanceof ParameterizedType) && (!this.typeEquals(childT, ancestorT, ctx))) {
	            return this.checkContains((ParameterizedType) ((ParameterizedType)childT).capture(), (ParameterizedType) ancestorT);
	        }
	        return false;
	    }
	    
		if (ancestor instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) ancestorT;
			return super.descendsFrom(child, ancestor)
					|| isSubtype(childT, tv.lowerBound(), ctx);
		} else {
			return super.descendsFrom(child, ancestor);
		}
	}

	@Override
	public boolean isSubtype(Type t1, Type t2, Context ctx) {
		// CHECK: Need to double check correctness of this method

		// these rules come from each implementation
		// of Wildcard, LubType and IntersectionType
		if (t1 instanceof Wildcard) {
			return false;
		}

		if (t1 instanceof LubType) {
			LubType lubType = (LubType) t1;
			Type ancestor = t2;
			for (Type elem : lubType.lubElements()) {
				if (!isSubtype(elem, ancestor, ctx))
					return false;
			}
			return true;
		}

		if (t1 instanceof IntersectionType) {
			IntersectionType it = (IntersectionType) t1;
			Type ancestor = t2;
			for (Type b : it.bounds()) {
				if (isSubtype(b, ancestor, ctx))
					return true;
			}
			return false;
		}

		// these come from the old implementation of JL5TypeSystem
		if (t2 instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) t2;
			return super.isSubtype(t1, t2, ctx)
					|| super.isSubtype(t1, tv.lowerBound(), ctx);
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

	public ConstructorInstance findJL5Constructor(Type container,
			JL5ConstructorMatcher matcher) throws SemanticException {
		assert_(container);

		// Get a list of constructor
		// List<JL5ConstructorInstance> cs = ct.constructors();
		List<JL5ConstructorInstance> acceptable = (List) findAcceptableConstructors(
				container, matcher);
		acceptable = filterPotentiallyApplicable(acceptable, matcher);
		acceptable = identifyApplicableProcedures(acceptable, matcher);
		Collection<ConstructorInstance> maximal = findMostSpecificProcedures(
				(List) acceptable, matcher, matcher.context());

		if (maximal.size() > 1) {
			throw new NoMemberException(NoMemberException.CONSTRUCTOR,
					"Reference to " + container + " is ambiguous, multiple "
							+ "constructors match: " + maximal);
		}

		ConstructorInstance ci = maximal.iterator().next();
		return ci;
	}

	/**
	 * 
	 * @param bounds
	 * @throws SemanticException
	 */
	public boolean checkIntersectionBounds(List<ClassType> bounds, boolean quiet)
			throws SemanticException {
		Context ctx = emptyContext();
		/*
		 * if ((bounds == null) || (bounds.size() == 0)) { if (!quiet) throw new
		 * SemanticException("Intersection type can't be empty"); return false;
		 * }
		 */
		List<ClassType> concreteBounds = concreteBounds(bounds);
		if (concreteBounds.size() == 0) {
			if (!quiet)
				throw new SemanticException(
						"Invalid bounds in intersection type.");
			else
				return false;
		}
		for (int i = 0; i < concreteBounds.size(); i++)
			for (int j = i + 1; j < concreteBounds.size(); j++) {
				ClassType t1 = concreteBounds.get(i);
				ClassType t2 = concreteBounds.get(j);
				// for now, no checks if at least one is an array type
				if (!t1.isClass() || !t2.isClass()) {
					return true;
				}
				if (!t1.toClass().flags().isInterface()
						&& !t2.toClass().flags().isInterface()) {
					if ((!isSubtype(t1, t2, ctx)) && (!isSubtype(t2, t1, ctx))) {
						if (!quiet)
							throw new SemanticException(
									"Error in intersection type. Types " + t1
											+ " and " + t2
											+ " are not in subtype relation.");
						else
							return false;
					}
				}
				if (t1.toClass().flags().isInterface()
						&& t2.toClass().flags().isInterface()
						&& (t1 instanceof GenericTypeRef)
						&& (t2 instanceof GenericTypeRef)) {
					GenericTypeRef j5t1 = (GenericTypeRef) t1;
					GenericTypeRef j5t2 = (GenericTypeRef) t2;
					if (j5t1.isGeneric() && j5t2.isGeneric()
							&& typeEquals(j5t1.baseType(), j5t2.baseType(), ctx)) {
						if (!typeEquals(j5t1, j5t2, ctx)) {
							if (!quiet)
								throw new SemanticException(
										"Error in intersection type. Interfaces "
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

	@Override
	public List<ClassType> concreteBounds(
			List<ClassType> bounds) {

		Set<ClassType> included = new HashSet<ClassType>();
		Set<ClassType> visited = new HashSet<ClassType>();
		List<ClassType> queue = new ArrayList<ClassType>(bounds);
		while (!queue.isEmpty()) {
			ClassType t = queue.remove(0);
			if (visited.contains(t))
				continue;
			visited.add(t);
			if (t instanceof TypeVariable) {
				TypeVariable tv = (TypeVariable) t;
				queue.addAll(tv.upperBound().bounds());
			} else if (t instanceof IntersectionType) {
				IntersectionType it = (IntersectionType) t;
				queue.addAll(it.bounds());
			} else {
				included.add(t);
			}
		}
		return new ArrayList<ClassType>(included);
	}

	public void checkTVForwardReference(List<TypeVariable> list)
			throws SemanticException {
		for (int i = 0; i < list.size(); i++) {
			TypeVariable tv = list.get(i);
			for (Type b : tv.bounds()) {
				if (b instanceof TypeVariable) {
					TypeVariable other_tv = (TypeVariable) b;
					if (list.indexOf(other_tv) >= i) {
						throw new SemanticException(
								"Illegal forward reference.", tv.position());
					}
				}
			}
		}
	}

	public InferenceSolver inferenceSolver(JL5ProcedureInstance pi,
			List<Type> actuals) {
		return new InferenceSolver_c(pi, actuals, this);
	}

	public InferenceSolver inferenceSolver(List<TypeVariable> typeVars,
			List<Type> formals, List<Type> actuals) {
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

	public boolean typeVariableEquals(TypeVariable type1, TypeVariable type2,
			Context context) {
		IntersectionType bound1 = type1.upperBound();
		IntersectionType bound2 = type2.upperBound();
		TypeSystem ts = context.typeSystem();
		return ts.equals((TypeObject) bound1, (TypeObject) bound2);
	}

	public ConstructorMatcher JL5ConstructorMatcher(Type container,
			List<Type> argTypes, List<Type> explicitTypeArgs, Context context) {
		return new JL5ConstructorMatcher(container, argTypes, explicitTypeArgs,
				context);
	}

	public MethodMatcher JL5MethodMatcher(Type container, Name name,
			List<Type> argTypes, List<Type> explicitTypeArgs, Context context) {
		return new JL5MethodMatcher(container, name, argTypes,
				explicitTypeArgs, context);
	}

	public static class JL5MethodMatcher extends MethodMatcher implements
			JL5ProcedureMatcher {
		protected List<Type> explicitTypeArgs;

		protected JL5MethodMatcher(Type container, Name name,
				List<Type> argTypes, List<Type> explicitTypeArgs,
				Context context) {
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
			if (!mi.name().equals(name)) {
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

		@Override
		public Type container() {
			return container;
		}
	}

	public static class JL5ConstructorMatcher extends ConstructorMatcher
			implements JL5ProcedureMatcher {
		protected List<Type> explicitTypeArgs;

		protected JL5ConstructorMatcher(Type receiverType, List<Type> argTypes,
				List<Type> explicitTypeArgs, Context context) {
			super(receiverType, argTypes, context);
			this.explicitTypeArgs = explicitTypeArgs;
		}

		@Override
		public java.lang.String signature() {
			// TODO Auto-generated method stub
			return super.signature();
		}

		public ConstructorInstance instantiate(ConstructorInstance ci)
				throws SemanticException {
			TypeSystem ts = ci.typeSystem();

			return ci;
		}

		public List<Type> getArgTypes() {
			return this.argTypes;
		}

		public List<Type> getExplicitTypeArgs() {
			return explicitTypeArgs;
		}

		@Override
		public Type container() {
			return container;
		}
	}

	public EnumMatcher EnumMatcher(Type container, Name name, Context ctx) {
		return new EnumMatcher(container, name, ctx);
	}

	/**
	 * Enum matcher is a field matcher which we cast the return type to
	 * EnumInstance
	 * 
	 * @author vcave
	 * 
	 */
	public static class EnumMatcher extends FieldMatcher {

		protected EnumMatcher(Type container, Name name, Context context) {
			super(container, name, context);
		}

		public EnumInstance instantiate(EnumInstance mi)
				throws SemanticException {
			return (EnumInstance) super.instantiate(mi);
		}
	}

	public AnnotationMatcher AnnotationMatcher(Type container, Name name,
			Context ctx) {
		return new AnnotationMatcher(container, name, ctx);
	}

	public static class AnnotationMatcher implements Copy,
			Matcher<AnnotationElemInstance> {
		protected Type container;
		protected Name name;
		protected Context context;

		protected AnnotationMatcher(Type container, Name name, Context context) {
			super();
			this.container = container;
			this.name = name;
			this.context = context;
		}

		public Context context() {
			return context;
		}

		public AnnotationMatcher container(Type container) {
			AnnotationMatcher n = copy();
			n.container = container;
			return n;
		}

		public AnnotationMatcher copy() {
			try {
				return (AnnotationMatcher) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalCompilerError(e);
			}
		}

		public String signature() {
			return name.toString();
		}

		public Name name() {
			return name;
		}

		public AnnotationElemInstance instantiate(AnnotationElemInstance mi)
				throws SemanticException {
			if (!mi.name().equals(name)) {
				return null;
			}
			return mi;
		}

		public String toString() {
			return signature();
		}

		public Object key() {
			return null;
		}
	}

	//MARKERMARKERMARKERMARKERMARKERMARKERMARKERMARKERMARKERMARKER

	@Override
	public boolean isTypeExtendsAnnotation(Type t) {
        if (t instanceof ClassType) {
        	ClassType ct = (ClassType)t;
        	return ((ClassType) ct.superClass()).fullName().equals(QName.make("java.lang.annotation.Annotation"));
        }
		return false;
	}

}