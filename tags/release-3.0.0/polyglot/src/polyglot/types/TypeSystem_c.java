/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.lang.reflect.Modifier;
import java.util.*;

import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLazyClassInitializer;
import polyglot.util.*;

/**
 * TypeSystem_c
 *
 * Overview:
 *    A TypeSystem_c is a universe of types, including all Java types.
 **/
public class TypeSystem_c implements TypeSystem
{
	protected SystemResolver systemResolver;
	protected TopLevelResolver loadedResolver;
	protected Map<String, Flags> flagsForName;
	protected ExtensionInfo extInfo;

	public TypeSystem_c() {}

	/**
	 * Initializes the type system and its internal constants (which depend on
	 * the resolver).
	 */
	public void initialize(TopLevelResolver loadedResolver, ExtensionInfo extInfo)
	throws SemanticException {

		if (Report.should_report(Report.types, 1))
			Report.report(1, "Initializing " + getClass().getName());

		this.extInfo = extInfo;

		// The loaded class resolver.  This resolver automatically loads types
		// from class files and from source files not mentioned on the command
		// line.
		this.loadedResolver = loadedResolver;

		// The system class resolver. The class resolver contains a map from
		// fully qualified names to instances of Named. A pass over a
		// compilation unit looks up classes first in its
		// import table and then in the system resolver.
		this.systemResolver = new SystemResolver(loadedResolver, extInfo);

		initEnums();
		initFlags();
		initTypes();
	}

	protected void initEnums() {
		// Ensure the enums in the type system are initialized and interned
		// before any deserialization occurs.

		// Just force the static initializers of ClassType and PrimitiveType
		// to run.
		Object o;
		o = ClassDef.TOP_LEVEL;
	}

	protected void initTypes() throws SemanticException {
		// FIXME: don't do this when rewriting a type system!

		// Prime the resolver cache so that we don't need to check
		// later if these are loaded.

		// We cache the most commonly used ones in fields.
		/* DISABLED CACHING OF COMMON CLASSES; CAUSES PROBLEMS IF
           COMPILING CORE CLASSES (e.g. java.lang package).
           TODO: Longer term fix. Maybe a flag to tell if we are compiling
                 core classes? XXX
        Object();
        Class();
        String();
        Throwable();

        systemResolver.find("java.lang.Error");
        systemResolver.find("java.lang.Exception");
        systemResolver.find("java.lang.RuntimeException");
        systemResolver.find("java.lang.Cloneable");
        systemResolver.find("java.io.Serializable");
        systemResolver.find("java.lang.NullPointerException");
        systemResolver.find("java.lang.ClassCastException");
        systemResolver.find("java.lang.ArrayIndexOutOfBoundsException");
        systemResolver.find("java.lang.ArrayStoreException");
        systemResolver.find("java.lang.ArithmeticException");
		 */
	}

	/** Return the language extension this type system is for. */
	public ExtensionInfo extensionInfo() {
		return extInfo;
	}

	public SystemResolver systemResolver() {
		return systemResolver;
	}

	/**
	 * Return the system resolver.  This used to return a different resolver.
	 * enclosed in the system resolver.
	 * @deprecated
	 */
	public CachingResolver parsedResolver() {
		return systemResolver;
	}

	public TopLevelResolver loadedResolver() {
		return loadedResolver;
	}

	public ClassFileLazyClassInitializer classFileLazyClassInitializer(ClassFile clazz) {
		return new ClassFileLazyClassInitializer(clazz, this);
	}

	public ImportTable importTable(String sourceName, Ref<? extends Package> pkg) {
		assert_(pkg);
		return new ImportTable(this, pkg, sourceName);
	}

	public ImportTable importTable(Ref<? extends Package> pkg) {
		assert_(pkg);
		return new ImportTable(this, pkg);
	}

	/**
	 * Returns true if the package named <code>name</code> exists.
	 */
	public boolean packageExists(QName name) {
		return systemResolver.packageExists(name);
	}

	protected void assert_(Collection<?> l) {
		for (Iterator<?> i = l.iterator(); i.hasNext(); ) {
			Object o = i.next();
			if (o instanceof TypeObject) {
				assert_((TypeObject) o);
			}
			else if (o instanceof Ref) {
				assert_((Ref<?>) o);
			}
		}
	}

	protected void assert_(Ref<?> ref) { }

	protected void assert_(TypeObject o) {
		if (o != null && o.typeSystem() != this) {
			throw new InternalCompilerError("we are " + this + " but " + o + " ("+o.getClass()+")" +
					" is from " + o.typeSystem());
		}
	}

	public String wrapperTypeString(Type t) {
		assert_(t);

		if (t.isBoolean())
		    return "java.lang.Boolean";
		if (t.isChar())
		    return "java.lang.Character";
		if (t.isByte())
		    return "java.lang.Byte";
		if (t.isShort())
		    return "java.lang.Short";
		if (t.isInt())
		    return "java.lang.Integer";
		if (t.isLong())
		    return "java.lang.Long";
		if (t.isFloat())
		    return "java.lang.Float";
		if (t.isDouble())
		    return "java.lang.Double";
		if (t.isVoid())
		    return "java.lang.Void";

		throw new InternalCompilerError("Unrecognized primitive type " + t);
	}

	public Context createContext() {
		return new Context_c(this);
	}

	/** @deprecated */
	public Resolver packageContextResolver(Resolver cr, Package p) {
		return packageContextResolver(p);
	}

	public AccessControlResolver createPackageContextResolver(Package p) {
		assert_(p);
		return new PackageContextResolver(this, p);
	}

	public Resolver packageContextResolver(Package p, ClassDef accessor) {
		if (accessor == null) {
			return p.resolver();
		}
		else {
			return new AccessControlWrapperResolver(createPackageContextResolver(p), accessor);
		}
	}

	public Resolver packageContextResolver(Package p) {
		assert_(p);
		return packageContextResolver(p, null);
	}

	public Resolver classContextResolver(final Type type, ClassDef accessor) {
		assert_(type);
		if (accessor == null) {
		        if (type instanceof ClassType) {
		            return ((ClassType) type).resolver();
		        }
		        return new Resolver() {
			    public Named find(Matcher<Named> matcher) throws SemanticException {
				throw new NoClassException(matcher.name().toString(), type);
			    }
		        };
		}
		else {
			return new AccessControlWrapperResolver(createClassContextResolver(type), accessor);
		}
	}

	public Resolver classContextResolver(Type type) {
		return classContextResolver(type, null);
	}

	public AccessControlResolver createClassContextResolver(Type type) {
		assert_(type);
		return new ClassContextResolver(this, type);
	}

	public FieldDef fieldDef(Position pos,
			Ref<? extends StructType> container, Flags flags,
			Ref<? extends Type> type, Name name) {
		assert_(container);
		assert_(type);
		return new FieldDef_c(this, pos, container, flags, type, name);
	}

	public LocalDef localDef(Position pos,
			Flags flags, Ref<? extends Type> type, Name name) {
		assert_(type);
		return new LocalDef_c(this, pos, flags, type, name);
	}

	public ConstructorDef defaultConstructor(Position pos,
			Ref<? extends ClassType> container) {
		assert_(container);

		// access for the default constructor is determined by the 
		// access of the containing class. See the JLS, 2nd Ed., 8.8.7.
		Flags access = Flags.NONE;
		Flags flags = container.get().flags();
		if (flags.isPrivate()) {
			access = access.Private();
		}
		if (flags.isProtected()) {
			access = access.Protected();            
		}
		if (flags.isPublic()) {
			access = access.Public();            
		}
		return constructorDef(pos, container,
				access, Collections.<Ref<? extends Type>>emptyList(),
				Collections.<Ref<? extends Type>>emptyList());
	}

	public ConstructorDef constructorDef(Position pos,
			Ref<? extends ClassType> container,
			Flags flags, List<Ref<? extends Type>> argTypes,
			List<Ref<? extends Type>> excTypes) {
		assert_(container);
		assert_(argTypes);
		assert_(excTypes);
		return new ConstructorDef_c(this, pos, container, flags,
				argTypes, excTypes);
	}

	public InitializerDef initializerDef(Position pos,
			Ref<? extends ClassType> container,
			Flags flags) {
		assert_(container);
		return new InitializerDef_c(this, pos, container, flags);
	}

	public MethodDef methodDef(Position pos,
			Ref<? extends StructType> container, Flags flags,
			Ref<? extends Type> returnType, Name name,
			List<Ref<? extends Type>> argTypes, List<Ref<? extends Type>> excTypes) {

		assert_(container);
		assert_(returnType);
		assert_(argTypes);
		assert_(excTypes);
		return new MethodDef_c(this, pos, container, flags,
		                       returnType, name, argTypes, excTypes);
	}

	/**
	 * Returns true iff child and ancestor are object types and child descends from or equals ancestor.
	 **/
	public boolean descendsFrom(Type child, Type ancestor) {
		assert_(child);
		assert_(ancestor);
		
		if (typeEquals(child, ancestor)) {
		    return true;
		}
		
		if (child instanceof ObjectType) {
		    ObjectType childRT = (ObjectType) child;
		    
		    if (typeEquals(ancestor, Object())) {
			return true;
		    }

		    if (typeEquals(childRT, Object())) {
			return false;
		    }

		    // Check subclass relation.
		    if (childRT.superClass() != null) {
			if (descendsFrom(childRT.superClass(), ancestor)) {
			    return true;
			}
		    }

		    // Next check interfaces.
		    for (Type parentType : childRT.interfaces()) {
			if (descendsFrom(parentType, ancestor)) {
			    return true;
			}
		    }

		    return false;
		}

		return false;
	}

	/**
	 * Requires: all type arguments are canonical.  ToType is not a NullType.
	 *
	 * Returns true iff a cast from fromType to toType is valid; in other
	 * words, some non-null members of fromType are also members of toType.
	 **/
	public boolean isCastValid(Type fromType, Type toType) {
		assert_(fromType);
		assert_(toType);

		if (fromType instanceof NullType) {
			return toType.isNull() || toType.isReference();
		}
		
		if (fromType.isPrimitive() && toType.isPrimitive()) {
			if (fromType.isVoid() || toType.isVoid()) return false;
			if (typeEquals(fromType, toType)) return true;
			if (fromType.isNumeric() && toType.isNumeric()) return true;
			return false;
		}

		if (fromType instanceof ArrayType && toType instanceof ArrayType) {
			ArrayType fromAT = (ArrayType) fromType;
			ArrayType toAT = (ArrayType) toType;

			Type fromBase = fromAT.base();
			Type toBase = toAT.base();

			if (fromBase.isPrimitive()) return typeEquals(toBase, fromBase);
			if (toBase.isPrimitive()) return false;

			if (fromBase.isNull()) return false;
			if (toBase.isNull()) return false;

			// Both are reference types.
			return isCastValid(fromBase, toBase);
		}
		
		if (fromType instanceof ArrayType && toType instanceof ReferenceType) {
			// Ancestor is not an array, but child is.  Check if the array
			// is a subtype of the ancestor.  This happens when ancestor
			// is java.lang.Object.
			return isSubtype(fromType, toType);
		}

		if (fromType instanceof ClassType && toType instanceof ArrayType) {
			// From type is not an array, but to type is.  Check if the array
			// is a subtype of the from type.  This happens when from type
			// is java.lang.Object.
			return isSubtype(toType, fromType);
		}
		
		if (fromType instanceof ClassType && toType instanceof ClassType) {
			ClassType fromCT = (ClassType) fromType;
			ClassType toCT = (ClassType) toType;
			
			// From and to are neither primitive nor an array. They are distinct.
			boolean fromInterface = fromCT.flags().isInterface();
			boolean toInterface   = toCT.flags().isInterface();
			boolean fromFinal     = fromCT.flags().isFinal();
			boolean toFinal       = toCT.flags().isFinal();
			
			// This is taken from Section 5.5 of the JLS.
			if (fromInterface) {
			    // From is an interface
			    if (! toInterface && ! toFinal) {
				// To is a non-final class.
				return true;
			    }
			
			    if (toFinal) {
				// To is a final class.
				return isSubtype(toType, fromCT);
			    }
			
			    // To and From are both interfaces.
			    return true;
			}
			else {
			    // From is not an interface.
			    if (! toInterface) {
				// Nether from nor to is an interface.
				return isSubtype(fromCT, toType) || isSubtype(toType, fromCT);
			    }
			
			    if (fromFinal) {
				// From is a final class, and to is an interface
				return isSubtype(fromCT, toType);
			    }
			
			    // From is a non-final class, and to is an interface.
			    return true;
			}
		}

		if (fromType instanceof ReferenceType) {
			if (! toType.isReference()) return false;
			return isSubtype(fromType, toType) || isSubtype(toType, fromType);
		}

		return false;
	}

	/**
	 * Requires: all type arguments are canonical.
	 *
	 * Returns true iff an implicit cast from fromType to toType is valid;
	 * in other words, every member of fromType is member of toType.
	 *
	 * Returns true iff child and ancestor are non-primitive
	 * types, and a variable of type child may be legally assigned
	 * to a variable of type ancestor.
	 *
	 */
	public boolean isImplicitCastValid(Type fromType, Type toType) {
		assert_(fromType);
		assert_(toType);
		
		if (fromType.isPrimitive() && toType.isPrimitive()) {
			if (toType.isVoid()) return false;
			if (fromType.isVoid()) return false;
			
			if (typeEquals(toType, fromType)) return true;
			
			if (toType.isBoolean()) return fromType.isBoolean();
			if (fromType.isBoolean()) return false;
			
			if (! fromType.isNumeric() || ! toType.isNumeric()) return false;
			
			if (toType.isDouble()) return true;
			if (fromType.isDouble()) return false;
			
			if (toType.isFloat()) return true;
			if (fromType.isFloat()) return false;
			
			if (toType.isLong()) return true;
			if (fromType.isLong()) return false;
			
			if (toType.isInt()) return true;
			if (fromType.isInt()) return false;
			
			if (toType.isShort()) return fromType.isShort() || fromType.isByte();
			if (fromType.isShort()) return false;
			
			if (toType.isChar()) return fromType.isChar();
			if (fromType.isChar()) return false;
			
			if (toType.isByte()) return fromType.isByte();
			if (fromType.isByte()) return false;
			
			return false;
		}

		if (fromType instanceof ArrayType && toType instanceof ArrayType) {
			ArrayType fromAT = (ArrayType) fromType;
			Type fromBase = fromAT.base();
			ArrayType toAT = (ArrayType) toType;
			Type toBase = toAT.base();
			if (fromBase.isPrimitive() || toBase.isPrimitive()) {
				return typeEquals(fromBase, toBase);
			}
			else {
				return isImplicitCastValid(fromBase, toBase);
			}
		}

		if (fromType instanceof NullType) {
			return toType.isNull() || toType.isReference();
		}
		
		if (fromType instanceof ReferenceType) {
			// This handles classes and also coercions from Array to Object
			return isSubtype(fromType, toType);
		}
		
		return false;
	}

	/**
	 * Returns true iff type1 and type2 represent the same type object.
	 */
	public boolean equals(TypeObject type1, TypeObject type2) {
		assert_(type1);
		assert_(type2);
		if (type1 == type2) return true;
		if (type1 == null || type2 == null) return false;
		return type1.equalsImpl(type2);
	}

	public final void equals(Type type1, Type type2) {
		assert false;
	}

	public final void equals(Type type1, TypeObject type2) {
		assert false;
	}

	public final void equals(TypeObject type1, Type type2) {
		assert false;
	}

	/**
	 * Returns true iff type1 and type2 are equivalent.
	 */
	public boolean typeEquals(Type type1, Type type2) {
		assert_(type1);
		assert_(type2);
		
		if (type1 == type2)
			return true;
		if (type1 == null || type2 == null)
			return false;
		
		if (type1 instanceof ArrayType && type2 instanceof ArrayType) {
			ArrayType at1 = (ArrayType) type1;
			ArrayType at2 = (ArrayType) type2;
			return typeEquals(at1.base(), at2.base());
		}
		
		return equals((TypeObject) type1, (TypeObject) type2);
	}

	/**
	 * Returns true iff type1 and type2 are equivalent.
	 */
	public boolean packageEquals(Package type1, Package type2) {
		assert_(type1);
		assert_(type2);
		if (type1 == type2) return true;
		if (type1 == null || type2 == null) return false;
		return type1.packageEquals(type2);
	}

	/**
	 * Returns true if <code>value</code> can be implicitly cast to Primitive
	 * type <code>t</code>.
	 */
	public boolean numericConversionValid(Type t, Object value) {
		assert_(t);
		
		if (value instanceof Float || value instanceof Double)
		    return false;

		long v;

		if (value instanceof Number) {
		    v = ((Number) value).longValue();
		}
		else if (value instanceof Character) {
		    v = ((Character) value).charValue();
		}
		else {
		    return false;
		}

		if (t.isLong())
		    return true;
		if (t.isInt())
		    return Integer.MIN_VALUE <= v && v <= Integer.MAX_VALUE;
		if (t.isChar())
		    return Character.MIN_VALUE <= v && v <= Character.MAX_VALUE;
		if (t.isShort())
		    return Short.MIN_VALUE <= v && v <= Short.MAX_VALUE;
		if (t.isByte())
		    return Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE;
		
		return false;
	}

	/**
	 * Returns true if <code>value</code> can be implicitly cast to Primitive
	 * type <code>t</code>.  This method should be removed.  It is kept for
	 * backward compatibility.
	 */
	public boolean numericConversionValid(Type t, long value) {
		assert_(t);
		return numericConversionValid(t, Long.valueOf(value));
	}

	////
	// Functions for one-type checking and resolution.
	////

	/**
	 * Checks whether the member mi can be accessed from Context "context".
	 */
	public boolean isAccessible(MemberInstance<? extends MemberDef> mi, Context context) {
		return isAccessible(mi, context.currentClassDef());
	}

	/**
	 * Checks whether the member mi can be accessed from code that is
	 * declared in the class contextClass.
	 */
	public boolean isAccessible(MemberInstance<? extends MemberDef> mi, ClassDef contextClass) {
		assert_(mi);


		Type target = mi.container();
		Flags flags = mi.flags();

		if (! target.isClass()) {
			// public members of non-classes are accessible;
			// non-public members of non-classes are inaccessible
			return flags.isPublic();
		}

		if (contextClass == null) {
		    return flags.isPublic();
		}

		ClassType contextClassType = contextClass.asType();
		
		ClassType targetClass = target.toClass();

		if (! classAccessible(targetClass.def(), contextClass)) {
			return false;
		}

		if (equals(targetClass.def(), contextClass))
			return true;

		// If the current class and the target class are both in the
		// same class body, then protection doesn't matter, i.e.
		// protected and private members may be accessed. Do this by
		// working up through contextClass's containers.
		if (isEnclosed(contextClass, targetClass.def()) || isEnclosed(targetClass.def(), contextClass))
			return true;

		ClassDef cd = contextClass;
		while (!cd.isTopLevel()) {
			cd = cd.outer().get();
			if (isEnclosed(targetClass.def(), cd))
				return true;
		}

		// protected
		if (flags.isProtected()) {
			// If the current class is in a
			// class body that extends/implements the target class, then
			// protected members can be accessed. Do this by
			// working up through contextClass's containers.
			if (descendsFrom(contextClassType, targetClass)) {
				return true;
			}

			ClassType ct = contextClassType;
			while (!ct.isTopLevel()) {
				ct = ct.outer();
				if (descendsFrom(ct, targetClass)) {
					return true;
				}
			}
		}

		return accessibleFromPackage(flags, targetClass.package_(), contextClassType.package_());
	}

	/** True if the class targetClass accessible from the context. */
	public boolean classAccessible(ClassDef targetClass, Context context) {
		if (context.currentClass() == null) {
			return classAccessibleFromPackage(targetClass, context.package_());
		}
		else {
			return classAccessible(targetClass, context.currentClassDef());
		}
	}

	/** True if the class targetClass accessible from the body of class contextClass. */
	public boolean classAccessible(ClassDef targetClass, ClassDef contextClass) {
		assert_(targetClass);

		if (targetClass.isMember()) {
			return isAccessible(targetClass.asType(), contextClass);
		}

		ClassType contextClassType = contextClass.asType();

		// Local and anonymous classes are accessible if they can be named.
		// This method wouldn't be called if they weren't named.
		if (! targetClass.isTopLevel()) {
			return true;
		}

		// targetClass must be a top-level class

		// same class
		if (equals(targetClass, contextClass))
			return true;

		if (isEnclosed(contextClass, targetClass))
			return true;

		return classAccessibleFromPackage(targetClass, contextClassType.package_());
	}

	/** True if the class targetClass accessible from the package pkg. */
	public boolean classAccessibleFromPackage(ClassDef targetClass, Package pkg) {
		assert_(targetClass);

		// Local and anonymous classes are not accessible from the outermost
		// scope of a compilation unit.
		if (! targetClass.isTopLevel() && ! targetClass.isMember())
			return false;

		Flags flags = targetClass.flags();

		if (targetClass.isMember()) {
			if (! targetClass.container().get().isClass()) {
				// public members of non-classes are accessible
				return flags.isPublic();
			}

			if (! classAccessibleFromPackage(targetClass.container().get().toClass().def(), pkg)) {
				return false;
			}
		}
		
		return accessibleFromPackage(flags, Types.get(targetClass.package_()), pkg);
	}

	/**
	 * Return true if a member (in an accessible container) or a
	 * top-level class with access flags <code>flags</code>
	 * in package <code>pkg1</code> is accessible from package
	 * <code>pkg2</code>.
	 */
	protected boolean accessibleFromPackage(Flags flags, Package pkg1, Package pkg2) {
		// Check if public.
		if (flags.isPublic()) {
			return true;
		}

		// Check if same package.
		if (flags.isPackage() || flags.isProtected()) {
			if (pkg1 == null && pkg2 == null)
				return true;
			if (pkg1 == null || pkg2 == null)
				return false;
			if (pkg1.packageEquals(pkg2))
				return true;
		}

		// Otherwise private.
		return false;
	}

	public boolean isEnclosed(ClassDef inner, ClassDef outer) {
		return inner.asType().isEnclosed(outer.asType());
	}

	public boolean hasEnclosingInstance(ClassDef inner, ClassDef encl) {
		return inner.asType().hasEnclosingInstance(encl.asType());
	}

	public void checkCycles(Type goal) throws SemanticException {
		checkCycles(goal, goal);
	}

	protected void checkCycles(Type curr, Type goal) throws SemanticException {
		assert_(curr);
		assert_(goal);

		if (curr == null) {
		    return;
		}

		if (curr instanceof ObjectType) {
		    ObjectType ot = (ObjectType) curr;
		    Type superType = null;

		    if (ot.superClass() != null) {
			superType = ot.superClass();
		    }

		    if (goal == superType) {
			throw new SemanticException("Circular inheritance involving " + goal, 
			                            curr.position());
		    }

		    checkCycles(superType, goal);

		    for (Type si : ot.interfaces()) {
			if (si == goal) {
			    throw new SemanticException("Circular inheritance involving " + goal, 
			                                curr.position());
			}

			checkCycles(si, goal);
		    }    
		}

		if (curr.isClass()) {
		    checkCycles(curr.toClass().outer(), goal);
		}
	}

	////
	// Various one-type predicates.
	////

	/**
	 * Returns true iff the type t can be coerced to a String in the given
	 * Context. If a type can be coerced to a String then it can be
	 * concatenated with Strings, e.g. if o is of type T, then the code snippet
	 *         "" + o
	 * would be allowed.
	 */
	public boolean canCoerceToString(Type t, Context c) {
		// every Object can be coerced to a string, as can any primitive,
		// except void.
		return ! t.isVoid();
	}

	public boolean isNumeric(Type t) {
		return t.isLongOrLess() || t.isFloat() || t.isDouble();
	}

	public boolean isIntOrLess(Type t) {
		return t.isByte() || t.isShort() || t.isChar() || t.isInt();
	}

	public boolean isLongOrLess(Type t) {
		return t.isByte() || t.isShort() || t.isChar() || t.isInt() || t.isLong();
	}

	public boolean isVoid(Type t) {
	    return typeEquals(t, Void());
	}

	public boolean isBoolean(Type t) {
		return isSubtype(t, Boolean());
	}

	public boolean isChar(Type t) {
		return isSubtype(t, Char());
	}

	public boolean isByte(Type t) {
		return isSubtype(t, Byte());
	}

	public boolean isShort(Type t) {
		return isSubtype(t, Short());
	}

	public boolean isInt(Type t) {
		return isSubtype(t, Int());
	}

	public boolean isLong(Type t) {
		return isSubtype(t, Long());
	}

	public boolean isFloat(Type t) {
		return isSubtype(t, Float());
	}

	public boolean isDouble(Type t) {
		return isSubtype(t, Double());
	}

	/**
	 * Returns true iff an object of type <type> may be thrown.
	 **/
	public boolean isThrowable(Type type) {
		assert_(type);
		return isSubtype(type, Throwable());
	}

	/**
	 * Returns a true iff the type or a supertype is in the list
	 * returned by uncheckedExceptions().
	 */
	public boolean isUncheckedException(Type type) {
		assert_(type);

		if (type.isThrowable()) {
		    for (Type t : uncheckedExceptions()) {
		        if (isSubtype(type, t)) {
		            return true;
		        }
		    }
		}
		
		return false;
	}

	/**
	 * Returns a list of the Throwable types that need not be declared
	 * in method and constructor signatures.
	 */
	public Collection<Type> uncheckedExceptions() {
		List<Type> l = new ArrayList<Type>(2);
		l.add(Error());
		l.add(RuntimeException());
		return l;
	}

	public boolean isSubtype(Type t1, Type t2) {
		assert_(t1);
		assert_(t2);
		
		if (typeEquals(t1, t2))
		    return true;

		if (t1.isNull()) {
		    return t2.isNull() || t2.isReference();
		}

		if (t1.isReference() && t2.isNull()) {
		    return false;
		}

		Type child = t1;
		Type ancestor = t2;

		if (child instanceof ObjectType) {
		    ObjectType childRT = (ObjectType) child;

		    if (typeEquals(ancestor, Object())) {
			return true;
		    }

		    if (typeEquals(childRT, Object())) {
			return false;
		    }

		    // Check subclass relation.
		    if (childRT.superClass() != null) {
			if (isSubtype(childRT.superClass(), ancestor)) {
			    return true;
			}
		    }

		    // Next check interfaces.
		    for (Type parentType : childRT.interfaces()) {
			if (isSubtype(parentType, ancestor)) {
			    return true;
			}
		    }
		}
		
		return false;
	}

	////
	// Functions for type membership.
	////

	/**
	 * Returns the FieldInstance for the field <code>name</code> defined
	 * in type <code>container</code> or a supertype, and visible from
	 * <code>currClass</code>.  If no such field is found, a SemanticException
	 * is thrown.  <code>currClass</code> may be null.
	 **/
	public FieldInstance findField(Type container, TypeSystem_c.FieldMatcher matcher,
			ClassDef currClass) throws SemanticException {
		Collection<FieldInstance> fields = findFields(container, matcher);

		if (fields.size() == 0) {
			throw new NoMemberException(NoMemberException.FIELD,
					"Field " + matcher.signature() +
					" not found in type \"" +
					container + "\".");
		}

		Iterator<FieldInstance> i = fields.iterator();
		FieldInstance fi = i.next();

		if (i.hasNext()) {
			FieldInstance fi2 = i.next();

			throw new SemanticException("Field " + matcher.signature() +
					" is ambiguous; it is defined in both " +
					fi.container() + " and " +
					fi2.container() + "."); 
		}

		if (currClass != null && ! isAccessible(fi, currClass)) {
			throw new SemanticException("Cannot access " + fi + ".");
		}

		return fi;
	}

	/**
	 * Returns the FieldInstance for the field <code>name</code> defined
	 * in type <code>container</code> or a supertype.  If no such field is
	 * found, a SemanticException is thrown.
	 */
	public FieldInstance findField(Type container, TypeSystem_c.FieldMatcher matcher)
	throws SemanticException {

		return findField(container, matcher, (ClassDef) null);
	}


	/**
	 * Returns a set of fields named <code>name</code> defined
	 * in type <code>container</code> or a supertype.  The list
	 * returned may be empty.
	 */
	protected Set<FieldInstance> findFields(Type container, TypeSystem_c.FieldMatcher matcher) {
	    Name name = matcher.name();
	        
		assert_(container);

		if (container == null) {
			throw new InternalCompilerError("Cannot access field \"" + name +
			"\" within a null container type.");
		}
		
		if (container instanceof StructType) {
		    FieldInstance fi = ((StructType) container).fieldNamed(name);
		    if (fi != null) {
			try {
			    fi = matcher.instantiate(fi);
			    if (fi != null)
				return Collections.singleton(fi);
			}
			catch (SemanticException e) {
			}
			return Collections.EMPTY_SET;
		    }
		}
		
		Set<FieldInstance> fields = new HashSet<FieldInstance>();

		if (container instanceof ObjectType) {
		    ObjectType ot = (ObjectType) container;
		    if (ot.superClass() != null && ot.superClass() instanceof StructType) {
			Set<FieldInstance> superFields = findFields((StructType) ot.superClass(), matcher);
			fields.addAll(superFields);
		    }

		    for (Type it : ot.interfaces()) {
			if (it instanceof StructType) {
			    Set<FieldInstance> superFields = findFields((StructType) it, matcher);
			    fields.addAll(superFields);
			}
		    }
		}

		return fields;
	}

	/**
	 * @deprecated
	 */
	public Type findMemberClass(Type container, Name name,
			Context c) throws SemanticException {
		return findMemberType(container, name, c.currentClassDef());
	}

	public Type findMemberType(Type container, Name name,
			ClassDef currClass) throws SemanticException
			{
		assert_(container);

		Named n = classContextResolver(container, currClass).find(MemberTypeMatcher(container, name));

		if (n instanceof ClassType) {
			return (ClassType) n;
		}

		throw new NoClassException(name.toString(), container);
			}

	public Type findMemberType(Type container, Name name)
	throws SemanticException {

		return findMemberType(container, name, (ClassDef) null);
	}

	/**
	 * Returns the list of methods with the given name defined or inherited
	 * into container, checking if the methods are accessible from the
	 * body of currClass
	 */
	public boolean hasMethodNamed(Type container, Name name) {
		assert_(container);

		if (container == null) {
			throw new InternalCompilerError("Cannot access method \"" + name +
			"\" within a null container type.");
		}
		
		if (container instanceof StructType) {
		    if (! ((StructType) container).methodsNamed(name).isEmpty()) {
			return true;
		    }
		}
		
		if (container instanceof ObjectType) {
		    ObjectType ot = (ObjectType) container;
		    if (ot.superClass() != null && ot.superClass() instanceof StructType) {
			if (hasMethodNamed((StructType) ot.superClass(), name)) {
			    return true;
			}
		    }

		    for (Type it : ot.interfaces()) {
			if (it instanceof StructType) {
			    if (hasMethodNamed((StructType) it, name)) {
				return true;
			    }
			}
		    }
		}

		return false;
	}
	
	public static class ConstructorMatcher implements Matcher<ConstructorInstance> {
	    protected Type container;
	    protected List<Type> argTypes;
	    
	    protected ConstructorMatcher(Type receiverType, List<Type> argTypes) {
		super();
		this.container = receiverType;
		this.argTypes = argTypes;
	    }
	    
	    public Name name() {
		return Name.make("this");
	    }
	    
	    public String signature() {
		return container + argumentString();
	    }
	    
	    public ConstructorInstance instantiate(ConstructorInstance ci) throws SemanticException {
		TypeSystem ts = ci.typeSystem();
		if (ci.formalTypes().size() != argTypes.size()) {
		    return null;
		}
		if (! ts.callValid(ci, container, argTypes)) {
		    throw new SemanticException("Cannot invoke " + ci + " on " + signature() + ".");
		}
		return ci;
	    }
	    
	    public String argumentString() {
		return  "(" + CollectionUtil.listToString(argTypes) + ")";
	    }
	    
	    public String toString() {
		return signature();
	    }
	    
	    public Object key() {
		return null;
	    }
	}
	
	public static class MethodMatcher implements Copy, Matcher<MethodInstance> {
	    protected Type container;
	    protected Name name;
	    protected List<Type> argTypes;
	    
	    protected MethodMatcher(Type container, Name name, List<Type> argTypes) {
		super();
		this.container = container;
		this.name = name;
		this.argTypes = argTypes;
	    }
	    
	    public MethodMatcher container(Type container) {
		MethodMatcher n = copy();
		n.container = container;
		return n;
	    }
	    
	    public MethodMatcher copy() {
		try {
		    return (MethodMatcher) super.clone();
		}
		catch (CloneNotSupportedException e) {
		    throw new InternalCompilerError(e);
		}
	    }
	    
	    public String signature() {
		return name + argumentString();
	    }
	    
	    public Name name() {
		return name;
	    }
	    
	    public MethodInstance instantiate(MethodInstance mi) throws SemanticException {
		if (! mi.name().equals(name)) {
		    return null;
		}
		if (mi.formalTypes().size() != argTypes.size()) {
		    return null;
		}
		TypeSystem ts = mi.typeSystem();
		if (! ts.callValid(mi, container, argTypes)) {
		    throw new SemanticException("Cannot invoke " + mi + " on " + signature() + ".");
		}
		return mi;
	    }
	    
	    public String argumentString() {
		return  "(" + CollectionUtil.listToString(argTypes) + ")";
	    }
	    
	    public String toString() {
		return signature();
	    }
	    
	    public Object key() {
		return null;
	    }
	}
	
	public static class FieldMatcher implements Copy, Matcher<FieldInstance> {
	    protected Type container;
	    protected Name name;

	    protected FieldMatcher(Type container, Name name) {
		super();
		this.container = container;
		this.name = name;
	    }

	    public FieldMatcher container(Type container) {
		FieldMatcher n = copy();
		n.container = container;
		return n;
	    }

	    public FieldMatcher copy() {
		try {
		    return (FieldMatcher) super.clone();
		}
		catch (CloneNotSupportedException e) {
		    throw new InternalCompilerError(e);
		}
	    }

	    public String signature() {
		return name.toString();
	    }

	    public Name name() {
		return name;
	    }

	    public FieldInstance instantiate(FieldInstance mi) throws SemanticException {
		if (! mi.name().equals(name)) {
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
	
	public static class MemberTypeMatcher implements Matcher<Named> {
	    protected Type container;
	    protected Name name;
	    
	    protected MemberTypeMatcher(Type container, Name name) {
		super();
		this.container = container;
		this.name = name;
	    }
	    
	    public String signature() {
		return name.toString();
	    }
	    
	    public Name name() {
		return name;
	    }
	    
	    public Named instantiate(Named t) throws SemanticException {
		if (! t.name().equals(name)) {
		    return null;
		}
		return t;
	    }
	    
	    public String toString() {
		return signature();
	    }
	    
	    public Object key() {
		return name;
	    }
	}

	public Matcher<Named> MemberTypeMatcher(Type container, Name name) {
	    return new MemberTypeMatcher(container, name);
	}
	
	public Matcher<Named> TypeMatcher(Name name) {
	    return new TypeMatcher(name);
	}

	public static class TypeMatcher implements Matcher<Named> {
	    protected Name name;
	    
	    protected TypeMatcher(Name name) {
		super();
		this.name = name;
	    }
	    
	    public String signature() {
		return name.toString();
	    }
	    
	    public Name name() {
		return name;
	    }
	    
	    public Named instantiate(Named t) throws SemanticException {
		if (! t.name().equals(name)) {
		    return null;
		}
		return t;
	    }
	    
	    public String toString() {
		return signature();
	    }
	    
	    public Object key() {
		return name;
	    }
	}
	
	public MethodMatcher MethodMatcher(Type container, Name name, List<Type> argTypes) {
	    return new MethodMatcher(container, name, argTypes);
	}

	public ConstructorMatcher ConstructorMatcher(Type container, List<Type> argTypes) {
	    return new ConstructorMatcher(container, argTypes);
	}
	
	public FieldMatcher FieldMatcher(Type container, Name name) {
	    return new FieldMatcher(container, name);
	}
	
	public MethodInstance findMethod(Type container, MethodMatcher matcher, ClassDef currClass) 
		throws SemanticException {

			assert_(container);

			List<MethodInstance> acceptable = findAcceptableMethods(container, matcher, currClass);

			if (acceptable.size() == 0) {
				throw new NoMemberException(NoMemberException.METHOD,
						"No valid method call found for " + matcher.signature() +
						" in " +
						container + ".");
			}

			Collection<MethodInstance> maximal =
				findMostSpecificProcedures(acceptable, (Matcher<MethodInstance>) matcher);

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

			MethodInstance mi = maximal.iterator().next();
			return mi;
		}
	
	public ConstructorInstance findConstructor(Type container, ConstructorMatcher matcher, ClassDef currClass)
	throws SemanticException {

		assert_(container);

		List<ConstructorInstance> acceptable = findAcceptableConstructors(container, matcher, currClass);

		if (acceptable.size() == 0) {
			throw new NoMemberException(NoMemberException.CONSTRUCTOR,
					"No valid constructor found for " + matcher.signature() + ").");
		}

		Collection<ConstructorInstance> maximal = findMostSpecificProcedures(acceptable, matcher);

		if (maximal.size() > 1) {
			throw new NoMemberException(NoMemberException.CONSTRUCTOR,
					"Reference to " + container + " is ambiguous, multiple " +
					"constructors match: " + maximal);
		}

		ConstructorInstance ci = maximal.iterator().next();
		return ci;
	}

	public <S extends ProcedureDef, T extends ProcedureInstance<S>> Collection<T> findMostSpecificProcedures(List<T> acceptable, Matcher<T> matcher)
	throws SemanticException {

		// now, use JLS 15.11.2.2
		// First sort from most- to least-specific.
		Comparator<T> msc = mostSpecificComparator(matcher);
		acceptable = new ArrayList<T>(acceptable); // make into array list to sort
		Collections.<T>sort(acceptable, msc);

		List<T> maximal = new ArrayList<T>(acceptable.size());

		Iterator<T> i = acceptable.iterator();

		T first = i.next();
		maximal.add(first);

		// Now check to make sure that we have a maximal most-specific method.
		while (i.hasNext()) {
			T p = i.next();

			if (msc.compare(first, p) == 0) {
				maximal.add(p);
			}
		}

		if (maximal.size() > 1) {
			// If exactly one method is not abstract, it is the most specific.
			List<T> notAbstract = new ArrayList<T>(maximal.size());
			for (Iterator<T> j = maximal.iterator(); j.hasNext(); ) {
				T p = j.next();
				if (! (p instanceof MemberInstance) || ! ((MemberInstance) p).flags().isAbstract()) {
					notAbstract.add(p);
				}
			}

			if (notAbstract.size() == 1) {
				maximal = notAbstract;
			}
			else if (notAbstract.size() == 0) {
				// all are abstract; if all signatures match, any will do.
				Iterator<T> j = maximal.iterator();
				first = j.next();
				S firstDecl = first.def();
				List<Type> firstFormals = new TransformingList<Ref<? extends Type>,Type>(firstDecl.formalTypes(), new DerefTransform<Type>());
				while (j.hasNext()) {
					T p = j.next();

					// Use the declarations to compare formals.
					S pDecl = p.def();

					List<Type> pFormals = new TransformingList<Ref<? extends Type>,Type>(pDecl.formalTypes(), new DerefTransform<Type>());

					if (! CollectionUtil.allElementwise(firstFormals, pFormals, new TypeEquals())) {
						// not all signatures match; must be ambiguous
						return maximal;
					}
				}

				// all signatures match, just take the first
				maximal = Collections.<T>singletonList(first);
			}
		}

		return maximal;
	}

	protected <S extends ProcedureDef, T extends ProcedureInstance<S>> Comparator<T> mostSpecificComparator(Matcher<T> matcher) {
	    return new MostSpecificComparator<S,T>();
	}

	public static class TypeEquals implements Predicate2<Type> {
		public boolean isTrue(Type o, Type p) {
			return o.typeEquals(p);
		}
	}

	public static class ImplicitCastValid implements Predicate2<Type> {
		public boolean isTrue(Type o, Type p) {
			return o.isImplicitCastValid(p);
		}
	}

	public static class Subtype implements Predicate2<Type> {
		public boolean isTrue(Type o, Type p) {
			return o.isSubtype(p);
		}
	}

	/**
	 * Class to handle the comparisons; dispatches to moreSpecific method.
	 */
	protected static class MostSpecificComparator<S extends ProcedureDef, T extends ProcedureInstance<S>> implements Comparator<T> {
		public int compare(T p1, T p2) {
		    if (p1.moreSpecific(p2))
			return -1;
		    if (p2.moreSpecific(p1))
			return 1;
		    return 0;
		}
	}

	/**
	 * Populates the list acceptable with those MethodInstances which are
	 * Applicable and Accessible as defined by JLS 15.11.2.1
	 */
	public List<MethodInstance> findAcceptableMethods(Type container, MethodMatcher matcher, ClassDef currClass)
			throws SemanticException {

		assert_(container);

		SemanticException error = null;

		// The list of acceptable methods. These methods are accessible from
		// currClass, the method call is valid, and they are not overridden
		// by an unacceptable method (which can occur with protected methods
		// only).
		List<MethodInstance> acceptable = new ArrayList<MethodInstance>();

		// A list of unacceptable methods, where the method call is valid, but
		// the method is not accessible. This list is needed to make sure that
		// the acceptable methods are not overridden by an unacceptable method.
		List<MethodInstance> unacceptable = new ArrayList<MethodInstance>();

		Set<Type> visitedTypes = new HashSet<Type>();

		LinkedList<Type> typeQueue = new LinkedList<Type>();
		typeQueue.addLast(container);

		Q:
		while (! typeQueue.isEmpty()) {
			Type t = typeQueue.removeFirst();

			if (t instanceof StructType) {
			    StructType type = (StructType) t;

			    for (Type s : visitedTypes) {
				if (typeEquals(type, s))
				    continue Q;
			    }
		
			    if (visitedTypes.contains(type)) {
				continue;
			    }

			    visitedTypes.add(type);

			    if (Report.should_report(Report.types, 2))
				Report.report(2, "Searching type " + type + " for method " + matcher.signature());

			    for (Iterator<MethodInstance> i = type.methods().iterator(); i.hasNext(); ) {
				MethodInstance mi = i.next();

				if (Report.should_report(Report.types, 3))
				    Report.report(3, "Trying " + mi);

				try {
				    mi = matcher.instantiate(mi);

				    if (mi == null) {
					continue;
				    }

				    if (isAccessible(mi, currClass)) {
					if (Report.should_report(Report.types, 3)) {
					    Report.report(3, "->acceptable: " + mi + " in "
					                  + mi.container());
					}

					acceptable.add(mi);
				    }
				    else {
					// method call is valid, but the method is
					// unacceptable.
					unacceptable.add(mi);
					if (error == null) {
					    error = new NoMemberException(NoMemberException.METHOD,
					                                  "Method " + mi.signature() +
					                                  " in " + container +
					    " is inaccessible."); 
					}
				    }

				    continue;
				}
				catch (SemanticException e) {
				    // Treat any instantiation errors as call invalid errors.
				    if (error == null)
					error = new NoMemberException(NoMemberException.METHOD,
					                              "Method " + mi.signature() +
					                              " in " + container +
					                              " cannot be called with arguments " +
					                              matcher.argumentString() + "; " + e.getMessage()); 
				}

				if (error == null) {
				    error = new NoMemberException(NoMemberException.METHOD,
				                                  "Method " + mi.signature() +
				                                  " in " + container +
				                                  " cannot be called with arguments " +
				                                  matcher.argumentString() + "."); 
				}
			    }
			}

			if (t instanceof ObjectType) {
			    ObjectType ot = (ObjectType) t;

			    if (ot.superClass() != null) {
				typeQueue.addLast(ot.superClass());
			    }

			    typeQueue.addAll(ot.interfaces());
			}
		}

		if (error == null) {
			error = new NoMemberException(NoMemberException.METHOD,
					"No valid method call found for " + matcher.signature() +
					" in " +
					container + ".");
		}

		if (acceptable.size() == 0) {
			throw error;
		}

		// remove any method in acceptable that are overridden by an
		// unacceptable
		// method.
		for (Iterator<MethodInstance> i = unacceptable.iterator(); i.hasNext();) {
			MethodInstance mi = i.next();
			acceptable.removeAll(mi.overrides());
		}

		if (acceptable.size() == 0) {
			throw error;
		}

		return acceptable;
	}

	/**
	 * Populates the list acceptable with those MethodInstances which are
	 * Applicable and Accessible as defined by JLS 15.11.2.1
	 */
	public List<ConstructorInstance> findAcceptableConstructors(Type container, ConstructorMatcher matcher, ClassDef currClass) throws SemanticException {
	    assert_(container);

	    SemanticException error = null;

	    List<ConstructorInstance> acceptable = new ArrayList<ConstructorInstance>();

	    if (Report.should_report(Report.types, 2))
		Report.report(2, "Searching type " + container +
		              " for constructor " + matcher.signature());

	    if (!(container instanceof ClassType)) {
		return Collections.EMPTY_LIST;
	    }

	    for (ConstructorInstance ci : ((ClassType) container).constructors()) {
		if (Report.should_report(Report.types, 3))
		    Report.report(3, "Trying " + ci);

		try {
		    ci = matcher.instantiate(ci);

		    if (ci == null) {
			continue;
		    }

		    if (isAccessible(ci, currClass)) {
			if (Report.should_report(Report.types, 3))
			    Report.report(3, "->acceptable: " + ci);
			acceptable.add(ci);
		    }
		    else {
			if (error == null) {
			    error = new NoMemberException(NoMemberException.CONSTRUCTOR,
			                                  "Constructor " + ci.signature() +
			    " is inaccessible."); 
			}
		    }

		    continue;
		}
		catch (SemanticException e) {
		    // Treat any instantiation errors as call invalid errors.
		}

		if (error == null) {
		    error = new NoMemberException(NoMemberException.CONSTRUCTOR,
		                                  "Constructor " + ci.signature() +
		                                  " cannot be invoked with arguments " +
		                                  matcher.argumentString() + "."); 

		}
	    }

	    if (acceptable.size() == 0) {
		if (error == null) {
		    error = new NoMemberException(NoMemberException.CONSTRUCTOR,
		                                  "No valid constructor found for " + container +
		                                  matcher.signature() + ".");
		}

		throw error;
	    }

	    return acceptable;
	}

	/**
	 * Returns whether method 1 is <i>more specific</i> than method 2,
	 * where <i>more specific</i> is defined as JLS 15.11.2.2
	 */
	public <T extends ProcedureDef> boolean moreSpecific(ProcedureInstance<T> p1, ProcedureInstance<T> p2) {
		return p1.moreSpecific(p2);
	}

	/**
	 * Returns the supertype of type, or null if type has no supertype.
	 **/
	public Type superClass(Type type) {
		assert_(type);
		if (type instanceof ObjectType)
		    return ((ObjectType) type).superClass();
		return null;
	}

	/**
	 * Returns an immutable list of all the interface types which type
	 * implements.
	 **/
	public List<Type> interfaces(Type type) {
		assert_(type);
		if (type instanceof ObjectType)
		    return ((ObjectType) type).interfaces();
		return Collections.EMPTY_LIST;
	}

	/**
	 * Requires: all type arguments are canonical.
	 * Returns the least common ancestor of Type1 and Type2
	 **/
	public Type leastCommonAncestor(Type type1, Type type2)
	throws SemanticException
	{
		assert_(type1);
		assert_(type2);

		if (typeEquals(type1, type2)) return type1;

		if (type1.isNumeric() && type2.isNumeric()) {
			if (isImplicitCastValid(type1, type2)) {
				return type2;
			}

			if (isImplicitCastValid(type2, type1)) {
				return type1;
			}

			if (type1.isChar() && type2.isByte() ||
					type1.isByte() && type2.isChar()) {
				return Int();
			}

			if (type1.isChar() && type2.isShort() ||
					type1.isShort() && type2.isChar()) {
				return Int();
			}
		}

		if (type1.isArray() && type2.isArray()) {
		    // XYZ
		    assert false;
			return arrayOf(leastCommonAncestor(type1.toArray().base(), type2.toArray().base()));
		}

		if (type1.isReference() && type2.isNull()) return type1;
		if (type2.isReference() && type1.isNull()) return type2;

		// Don't consider interfaces.
		if (type1.isClass() && type1.toClass().flags().isInterface()) {
		    return Object();
		}

		if (type2.isClass() && type2.toClass().flags().isInterface()) {
		    return Object();
		}

		// Check against Object to ensure superType() is not null.
		if (typeEquals(type1, Object())) return type1;
		if (typeEquals(type2, Object())) return type2;
		
		if (isSubtype(type1, type2)) return type2;
		if (isSubtype(type2, type1)) return type1;

		if (type1 instanceof ObjectType && type2 instanceof ObjectType) {
			// Walk up the hierarchy
			Type t1 = leastCommonAncestor(((ObjectType) type1).superClass(), type2);
			Type t2 = leastCommonAncestor(((ObjectType) type2).superClass(), type1);

			if (typeEquals(t1, t2)) return t1;

			return Object();
		}

		throw new SemanticException(
				"No least common ancestor found for types \"" + type1 +
				"\" and \"" + type2 + "\".");
	}

	////
	// Functions for method testing.
	////

	/**
	 * Returns true iff <p1> throws fewer exceptions than <p2>.
	 */
	public <T extends ProcedureDef> boolean throwsSubset(ProcedureInstance<T> p1, ProcedureInstance<T> p2) {
		assert_(p1);
		assert_(p2);
		return p1.throwsSubset(p2);
	}

	/** Return true if t overrides mi */
	public boolean hasFormals(ProcedureInstance<? extends ProcedureDef> pi, List<Type> formalTypes) {
		assert_(pi);
		assert_(formalTypes);
		return pi.hasFormals(formalTypes);
	}

	/** Return true if t overrides mi */
	public boolean hasMethod(Type t, MethodInstance mi) {
		assert_(t);
		assert_(mi);
		return t instanceof StructType && ((StructType) t).hasMethod(mi);
	}

	public List<MethodInstance> overrides(MethodInstance mi) {
		return mi.overrides();
	}

	public List<MethodInstance> implemented(MethodInstance mi) {
		return mi.implemented(mi.container());
	}

	public boolean canOverride(MethodInstance mi, MethodInstance mj) {
		return mi.canOverride(mj);
	}

	public void checkOverride(MethodInstance mi, MethodInstance mj) throws SemanticException {
		mi.checkOverride(mj);
	}

	/**
	 * Returns true iff <m1> is the same method as <m2>
	 */
	public boolean isSameMethod(MethodInstance m1, MethodInstance m2) {
		assert_(m1);
		assert_(m2);
		return m1.isSameMethod(m2);
	}

	public boolean callValid(ProcedureInstance<? extends ProcedureDef> prototype, Type thisType, List<Type> argTypes) {
		assert_(prototype);
		assert_(argTypes);
		return prototype.callValid(thisType, argTypes);
	}

	////
	// Functions which yield particular types.
	////
	public NullType Null()         { return NULL_; }
	public Type Void()    { return VOID_; }
	public Type Boolean() { return BOOLEAN_; }
	public Type Char()    { return CHAR_; }
	public Type Byte()    { return BYTE_; }
	public Type Short()   { return SHORT_; }
	public Type Int()     { return INT_; }
	public Type Long()    { return LONG_; }
	public Type Float()   { return FLOAT_; }
	public Type Double()  { return DOUBLE_; }

	protected ClassType load(String name) {
		try {
			return (ClassType) typeForName(QName.make(name));
		}
		catch (SemanticException e) {
			throw new InternalCompilerError("Cannot find class \"" +
					name + "\"; " + e.getMessage(),
					e);
		}
	}

	public Named forName(QName name) throws SemanticException {
		try {
			return systemResolver.find(name);
		}
		catch (SemanticException e) {
		    if (name.qualifier() != null) {
				try {
					Named container = forName(name.qualifier());
					if (container instanceof ClassType) {
						return classContextResolver((ClassType) container).find(MemberTypeMatcher((ClassType) container, name.name()));
					}
				}
				catch (SemanticException e2) {
				}
			}

			// throw the original exception
			throw e;
		}
	}

	public Type typeForName(QName name) throws SemanticException {
		return (Type) forName(name);
	}

	protected Type OBJECT_;
	protected Type CLASS_;
	protected Type STRING_;
	protected Type THROWABLE_;

	public Type Object()  { if (OBJECT_ != null) return OBJECT_;
	return OBJECT_ = load("java.lang.Object"); }
	public Type Class()   { if (CLASS_ != null) return CLASS_;
	return CLASS_ = load("java.lang.Class"); }
	public Type String()  { if (STRING_ != null) return STRING_;
	return STRING_ = load("java.lang.String"); }
	public Type Throwable() { if (THROWABLE_ != null) return THROWABLE_;
	return THROWABLE_ = load("java.lang.Throwable"); }
	public Type Error() { return load("java.lang.Error"); }
	public Type Exception() { return load("java.lang.Exception"); }
	public Type RuntimeException() { return load("java.lang.RuntimeException"); }
	public Type Cloneable() { return load("java.lang.Cloneable"); }
	public Type Serializable() { return load("java.io.Serializable"); }
	public Type NullPointerException() { return load("java.lang.NullPointerException"); }
	public Type ClassCastException()   { return load("java.lang.ClassCastException"); }
	public Type OutOfBoundsException() { return load("java.lang.ArrayIndexOutOfBoundsException"); }
	public Type ArrayStoreException()  { return load("java.lang.ArrayStoreException"); }
	public Type ArithmeticException()  { return load("java.lang.ArithmeticException"); }

	protected NullType createNull() {
		return new NullType_c(this);
	}

	protected PrimitiveType createPrimitive(Name name) {
		return new PrimitiveType_c(this, name);
	}

	protected final NullType NULL_         = createNull();
	protected final PrimitiveType VOID_    = createPrimitive(Name.make("void"));
	protected final PrimitiveType BOOLEAN_ = createPrimitive(Name.make("boolean"));
	protected final PrimitiveType CHAR_    = createPrimitive(Name.make("char"));
	protected final PrimitiveType BYTE_    = createPrimitive(Name.make("byte"));
	protected final PrimitiveType SHORT_   = createPrimitive(Name.make("short"));
	protected final PrimitiveType INT_     = createPrimitive(Name.make("int"));
	protected final PrimitiveType LONG_    = createPrimitive(Name.make("long"));
	protected final PrimitiveType FLOAT_   = createPrimitive(Name.make("float"));
	protected final PrimitiveType DOUBLE_  = createPrimitive(Name.make("double"));

	public Object placeHolder(TypeObject o) {
		return placeHolder(o, Collections.EMPTY_SET);
	}

	public Object placeHolder(TypeObject o, Set roots) {
		assert_(o);

		if (o instanceof Ref_c) {
			Ref_c<?> ref = (Ref_c<?>) o;

			if (ref.get() instanceof ClassDef) {
				ClassDef ct = (ClassDef) ref.get();

				// This should never happen: anonymous and local types cannot
				// appear in signatures.
				if (ct.isLocal() || ct.isAnonymous()) {
					throw new InternalCompilerError("Cannot serialize " + o + ".");
				}

				// Use the transformed name so that member classes will
				// be sought in the correct class file.
				QName name = getTransformedClassName(ct);

				TypeSystem_c ts = this;
				LazyRef<ClassDef> sym = Types.lazyRef(unknownClassDef(), null);
				Goal resolver = Globals.Scheduler().LookupGlobalTypeDef(sym, name);
				resolver.update(Goal.Status.SUCCESS);
				sym.setResolver(resolver);
				return sym;
			}
		}

		return o;
	}

	public ClassDef unknownClassDef() {
		if (unknownClassDef == null) {
			unknownClassDef = new ClassDef_c(this, null);
			unknownClassDef.name(Name.make("<unknown class>"));
			unknownClassDef.kind(ClassDef.TOP_LEVEL);
		}
		return unknownClassDef;
	}

	protected ClassDef unknownClassDef = null;

	protected UnknownType unknownType = new UnknownType_c(this);
	protected UnknownPackage unknownPackage = new UnknownPackage_c(this);
	protected UnknownQualifier unknownQualifier = new UnknownQualifier_c(this);

	public UnknownType unknownType(Position pos) {
		return unknownType;
	}

	public UnknownPackage unknownPackage(Position pos) {
		return unknownPackage;
	}

	public UnknownQualifier unknownQualifier(Position pos) {
		return unknownQualifier;
	}

	public Package packageForName(Package prefix, Name name) throws SemanticException {
		return createPackage(prefix, name);
	}

	public Package packageForName(Ref<? extends Package> prefix, Name name) throws SemanticException {
		return createPackage(prefix, name);
	}

	public Package packageForName(QName name) throws SemanticException {
		if (name == null) {
			return null;
		}

		return packageForName(packageForName(name.qualifier()), name.name());
	}

	/** @deprecated */
	public Package createPackage(Package prefix, Name name) {
		return createPackage(prefix != null ? Types.ref(prefix) : null, name);
	}

	/** @deprecated */
	public Package createPackage(Ref<? extends Package> prefix, Name name) {
		assert_(prefix);
		return new Package_c(this, prefix, name);
	}

	/** @deprecated */
	public Package createPackage(QName name) {
		if (name == null) {
			return null;
		}

		return createPackage(createPackage(name.qualifier()), name.name());
	}

	/**
	 * Returns a type identical to <type>, but with <dims> more array
	 * dimensions.
	 */
	public Type arrayOf(Type type) {
		assert_(type);
		return arrayOf(type.position(), Types.ref(type));
	}

	public Type arrayOf(Ref<? extends Type> type) {
		assert_(type);
		return arrayOf(null, type);
	}

	public Type arrayOf(Position pos, Ref<? extends Type> type) {
		return arrayType(pos, type);
	}

	public Type arrayOf(Position pos, Type type) {
	    assert_(type);
		return arrayOf(pos, Types.ref(type));
	}

	public Type arrayOf(Type type, int dims) {
		return arrayOf(Types.ref(type), dims);
	}

	public Type arrayOf(Position pos, Type type, int dims) {
		return arrayOf(pos, Types.ref(type), dims);
	}

	Map<Ref<? extends Type>,Type> arrayTypeCache = new HashMap<Ref<? extends Type>,Type>();

	/**
	 * Factory method for ArrayTypes.
	 */
	 protected ArrayType arrayType(Position pos, Ref<? extends Type> type) {
		 ArrayType t = (ArrayType) arrayTypeCache.get(type);
		 if (t == null) {
			 t = createArrayType(pos, type);
			 arrayTypeCache.put(type, t);
		 }
		 return t;
	 }

	 protected ArrayType createArrayType(Position pos, Ref<? extends Type> type) {
		 return new ArrayType_c(this, pos, type);
	 }

	 public Type arrayOf(Ref<? extends Type> type, int dims) {
		 return arrayOf(null, type, dims);
	 }

	 public Type arrayOf(Position pos, Ref<? extends Type> type, int dims) {
		 if (dims > 1) {
			 return arrayOf(pos, arrayOf(pos, type, dims-1));
		 }
		 else if (dims == 1) {
			 return arrayOf(pos, type);
		 }
		 else {
			 throw new InternalCompilerError(
					 "Must call arrayOf(type, dims) with dims > 0");
		 }
	 }

	 /**
	  * Returns a canonical type corresponding to the Java Class object
	  * theClass.  Does not require that <theClass> have a JavaClass
	  * registered in this typeSystem.  Does not register the type in
	  * this TypeSystem.  For use only by JavaClass implementations.
	  **/
	 public Type typeForClass(Class<?> clazz) throws SemanticException
	 {
		 if (clazz == Void.TYPE)      return VOID_;
		 if (clazz == Boolean.TYPE)   return BOOLEAN_;
		 if (clazz == Byte.TYPE)      return BYTE_;
		 if (clazz == Character.TYPE) return CHAR_;
		 if (clazz == Short.TYPE)     return SHORT_;
		 if (clazz == Integer.TYPE)   return INT_;
		 if (clazz == Long.TYPE)      return LONG_;
		 if (clazz == Float.TYPE)     return FLOAT_;
		 if (clazz == Double.TYPE)    return DOUBLE_;

		 if (clazz.isArray()) {
			 return arrayOf(typeForClass(clazz.getComponentType()));
		 }

		 return (Type) systemResolver.find(QName.make(clazz.getName()));
	 }

	 /**
	  * Return the set of objects that should be serialized into the
	  * type information for the given TypeObject.
	  * Usually only the object itself should get encoded, and references
	  * to other classes should just have their name written out.
	  * If it makes sense for additional types to be fully encoded,
	  * (i.e., they're necessary to correctly reconstruct the given clazz,
	  * and the usual class resolvers can't otherwise find them) they
	  * should be returned in the set in addition to clazz.
	  */
	 public Set getTypeEncoderRootSet(TypeObject t) {
		 return Collections.singleton(t);
	 }

	 /**
	  * Get the transformed class name of a class.
	  * This utility method returns the "mangled" name of the given class,
	  * whereby all periods ('.') following the toplevel class name
	  * are replaced with dollar signs ('$'). If any of the containing
	  * classes is not a member class or a top level class, then null is
	  * returned.
	  * 
	  * Return null if the class is not globally accessible.
	  */
	 public QName getTransformedClassName(ClassDef ct) {
		 assert ct != null;
		 assert ct.fullName() != null;

		 if (!ct.isMember() && !ct.isTopLevel()) {
		     assert ! ct.asType().isGloballyAccessible();
			 return null;
		 }

		 StringBuilder sb = new StringBuilder();
		 
		 while (ct.isMember()) {
			 sb.insert(0, ct.name());
			 sb.insert(0, '$');
			 ct = ct.outer().get();
			 if (!ct.isMember() && !ct.isTopLevel()) {
			     assert ! ct.asType().isGloballyAccessible();
				 return null;
			 }
		 }
		 
		 assert ct.asType().isGloballyAccessible();
		 
		 if (sb.length() > 0)
		     return QName.make(ct.fullName(), Name.make(sb.toString()));
		 else
		     return QName.make(ct.fullName());
	 }

	 public String translatePackage(Resolver c, Package p) {
		 return p.translate(c);
	 }

	 public String translateArray(Resolver c, ArrayType t) {
		 return t.translate(c);
	 }

	 public String translateClass(Resolver c, ClassType t) {
		 return t.translate(c);
	 }

	 public String translatePrimitive(Resolver c, PrimitiveType t) {
		 return t.translate(c);
	 }

	 public Type primitiveForName(Name sname)
	 throws SemanticException {
	     String name = sname.toString();
		 if (name.equals("void")) return Void();
		 if (name.equals("boolean")) return Boolean();
		 if (name.equals("char")) return Char();
		 if (name.equals("byte")) return Byte();
		 if (name.equals("short")) return Short();
		 if (name.equals("int")) return Int();
		 if (name.equals("long")) return Long();
		 if (name.equals("float")) return Float();
		 if (name.equals("double")) return Double();

		 throw new SemanticException("Unrecognized primitive type \"" +
				 name + "\".");
	 }

	 public final ClassDef createClassDef() {
		 return createClassDef((Source) null);
	 }

	 public ClassDef createClassDef(Source fromSource) {
		 return new ClassDef_c(this, fromSource);
	 }

	 public ParsedClassType createClassType(Position pos, Ref<? extends ClassDef> def) {
		 return new ParsedClassType_c(this, pos, def);
	 }

	 public ConstructorInstance createConstructorInstance(Position pos, Ref<? extends ConstructorDef> def) {
		 return new ConstructorInstance_c(this, pos, def);
	 }

	 public MethodInstance createMethodInstance(Position pos, Ref<? extends MethodDef> def) {
		 return new MethodInstance_c(this, pos, def);
	 }

	 public FieldInstance createFieldInstance(Position pos, Ref<? extends FieldDef> def) {
		 return new FieldInstance_c(this, pos, def);
	 }

	 public LocalInstance createLocalInstance(Position pos, Ref<? extends LocalDef> def) {
		 return new LocalInstance_c(this, pos, def);
	 }

	 public InitializerInstance createInitializerInstance(Position pos, Ref<? extends InitializerDef> def) {
		 return new InitializerInstance_c(this, pos, def);
	 }

	 public List<QName> defaultOnDemandImports() {
		 List<QName> l = new ArrayList<QName>(1);
		 l.add(QName.make("java.lang"));
		 return l;
	 }

	 public Type promote(Type t1, Type t2) throws SemanticException {
		 if (! t1.isNumeric()) {
			 throw new SemanticException(
					 "Cannot promote non-numeric type " + t1);
		 }

		 if (! t2.isNumeric()) {
			 throw new SemanticException(
					 "Cannot promote non-numeric type " + t2);
		 }

		 if (t1.isDouble() || t2.isDouble()) {
			 return Double();
		 }

		 if (t1.isFloat() || t2.isFloat()) {
			 return Float();
		 }

		 if (t1.isLong() || t2.isLong()) {
			 return Long();
		 }
		 
		 return Int();
	 }

	 public Type promote(Type t) throws SemanticException {
		 if (! t.isNumeric()) {
			 throw new SemanticException(
					 "Cannot promote non-numeric type " + t);
		 }

		 if (t.isIntOrLess()) {
			 return Int();
		 }

		 return t;
	 }

	 /** All possible <i>access</i> flags. */
	 public Flags legalAccessFlags() {
		 return Public().Protected().Private();
	 }

	 protected final Flags ACCESS_FLAGS = legalAccessFlags();

	 /** All flags allowed for a local variable. */
	 public Flags legalLocalFlags() {
		 return Final();
	 }

	 protected final Flags LOCAL_FLAGS = legalLocalFlags();

	 /** All flags allowed for a field. */
	 public Flags legalFieldFlags() {
		 return legalAccessFlags().Static().Final().Transient().Volatile();
	 }

	 protected final Flags FIELD_FLAGS = legalFieldFlags();

	 /** All flags allowed for a constructor. */
	 public Flags legalConstructorFlags() {
		 return legalAccessFlags().Synchronized();
	 }

	 protected final Flags CONSTRUCTOR_FLAGS = legalConstructorFlags();

	 /** All flags allowed for an initializer block. */
	 public Flags legalInitializerFlags() {
		 return Static();
	 }

	 protected final Flags INITIALIZER_FLAGS = legalInitializerFlags();

	 /** All flags allowed for a method. */
	 public Flags legalMethodFlags() {
		 return legalAccessFlags().Abstract().Static().Final().Native().Synchronized().StrictFP();
	 }

	 protected final Flags METHOD_FLAGS = legalMethodFlags();

	 public Flags legalAbstractMethodFlags() {
		 return legalAccessFlags().clear(Private()).Abstract();
	 }

	 protected final Flags ABSTRACT_METHOD_FLAGS = legalAbstractMethodFlags();

	 /** All flags allowed for a top-level class. */
	 public Flags legalTopLevelClassFlags() {
		 return legalAccessFlags().clear(Private()).Abstract().Final().StrictFP().Interface();
	 }

	 protected final Flags TOP_LEVEL_CLASS_FLAGS = legalTopLevelClassFlags();

	 /** All flags allowed for an interface. */
	 public Flags legalInterfaceFlags() {
		 return legalAccessFlags().Abstract().Interface().Static();
	 }

	 protected final Flags INTERFACE_FLAGS = legalInterfaceFlags();

	 /** All flags allowed for a member class. */
	 public Flags legalMemberClassFlags() {
		 return legalAccessFlags().Static().Abstract().Final().StrictFP().Interface();
	 }

	 protected final Flags MEMBER_CLASS_FLAGS = legalMemberClassFlags();

	 /** All flags allowed for a local class. */
	 public Flags legalLocalClassFlags() {
		 return Abstract().Final().StrictFP().Interface();
	 }

	 protected final Flags LOCAL_CLASS_FLAGS = legalLocalClassFlags();

	 public void checkMethodFlags(Flags f) throws SemanticException {
		 if (! f.clear(METHOD_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare method with flags " +
					 f.clear(METHOD_FLAGS) + ".");
		 }

		 if (f.isAbstract() && ! f.clear(ABSTRACT_METHOD_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare abstract method with flags " +
					 f.clear(ABSTRACT_METHOD_FLAGS) + ".");
		 }

		 checkAccessFlags(f);
	 }

	 public void checkLocalFlags(Flags f) throws SemanticException {
		 if (! f.clear(LOCAL_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare local variable with flags " +
					 f.clear(LOCAL_FLAGS) + ".");
		 }
	 }

	 public void checkFieldFlags(Flags f) throws SemanticException {
		 if (! f.clear(FIELD_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare field with flags " +
					 f.clear(FIELD_FLAGS) + ".");
		 }

		 checkAccessFlags(f);
	 }

	 public void checkConstructorFlags(Flags f) throws SemanticException {
		 if (! f.clear(CONSTRUCTOR_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare constructor with flags " +
					 f.clear(CONSTRUCTOR_FLAGS) + ".");
		 }

		 checkAccessFlags(f);
	 }

	 public void checkInitializerFlags(Flags f) throws SemanticException {
		 if (! f.clear(INITIALIZER_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare initializer with flags " +
					 f.clear(INITIALIZER_FLAGS) + ".");
		 }
	 }

	 public void checkTopLevelClassFlags(Flags f) throws SemanticException {
		 if (! f.clear(TOP_LEVEL_CLASS_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare a top-level class with flag(s) " +
					 f.clear(TOP_LEVEL_CLASS_FLAGS) + ".");
		 }


		 if (f.isInterface() && ! f.clear(INTERFACE_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException("Cannot declare interface with flags " +
					 f.clear(INTERFACE_FLAGS) + ".");
		 }

		 checkAccessFlags(f);
	 }

	 public void checkMemberClassFlags(Flags f) throws SemanticException {
		 if (! f.clear(MEMBER_CLASS_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare a member class with flag(s) " +
					 f.clear(MEMBER_CLASS_FLAGS) + ".");
		 }

		 checkAccessFlags(f);
	 }

	 public void checkLocalClassFlags(Flags f) throws SemanticException {
		 if (f.isInterface()) {
			 throw new SemanticException("Cannot declare a local interface.");
		 }

		 if (! f.clear(LOCAL_CLASS_FLAGS).equals(Flags.NONE)) {
			 throw new SemanticException(
					 "Cannot declare a local class with flag(s) " +
					 f.clear(LOCAL_CLASS_FLAGS) + ".");
		 }

		 checkAccessFlags(f);
	 }

	 public void checkAccessFlags(Flags f) throws SemanticException {
		 int count = 0;
		 if (f.isPublic()) count++;
		 if (f.isProtected()) count++;
		 if (f.isPrivate()) count++;

		 if (count > 1) {
			 throw new SemanticException(
					 "Invalid access flags: " + f.retain(ACCESS_FLAGS) + ".");
		 }
	 }

	 /**
	  * Utility method to gather all the superclasses and interfaces of
	  * <code>ct</code> that may contain abstract methods that must be
	  * implemented by <code>ct</code>. The list returned also contains
	  * <code>rt</code>.
	  */
	 protected List<Type> abstractSuperInterfaces(ObjectType rt) {
		 List<Type> superInterfaces = new LinkedList<Type>();
		 superInterfaces.add(rt);

		 for (Type interf : rt.interfaces()) {
			 if (interf instanceof ObjectType) {
				 superInterfaces.addAll(abstractSuperInterfaces((ObjectType) interf));
			 }
		 }

		 if (rt.superClass() instanceof ClassType) {
			 ClassType c = (ClassType) rt.superClass();
			 if (c.flags().isAbstract()) {
				 // the superclass is abstract, so it may contain methods
				 // that must be implemented.
				 superInterfaces.addAll(abstractSuperInterfaces(c));
			 }
			 else {
				 // the superclass is not abstract, so it must implement
				 // all abstract methods of any interfaces it implements, and
				 // any superclasses it may have.
			 }
		 }
		 return superInterfaces;
	 }

	 /**
	  * Assert that <code>ct</code> implements all abstract methods required;
	  * that is, if it is a concrete class, then it must implement all
	  * interfaces and abstract methods that it or it's superclasses declare, and if 
	  * it is an abstract class then any methods that it overrides are overridden 
	  * correctly.
	  */
	 public void checkClassConformance(ClassType ct) throws SemanticException {
		 if (ct.flags().isAbstract()) {
			 // don't need to check interfaces or abstract classes           
			 return;
		 }

		 // build up a list of superclasses and interfaces that ct 
		 // extends/implements that may contain abstract methods that 
		 // ct must define.
		 List<Type> superInterfaces = abstractSuperInterfaces(ct);

		 // check each abstract method of the classes and interfaces in
		 // superInterfaces
		 for (Iterator<Type> i = superInterfaces.iterator(); i.hasNext(); ) {
		     Type it = i.next();
		     if (it instanceof StructType) {
			 StructType rt = (StructType) it;
			 for (Iterator<MethodInstance> j = rt.methods().iterator(); j.hasNext(); ) {
			     MethodInstance mi = j.next();
			     if (!mi.flags().isAbstract()) {
				 // the method isn't abstract, so ct doesn't have to
				 // implement it.
				 continue;
			     }

			     MethodInstance mj = findImplementingMethod(ct, mi);
			     if (mj == null) {
				 if (!ct.flags().isAbstract()) {
				     throw new SemanticException(ct.fullName() + " should be " +
				                                 "declared abstract; it does not define " +
				                                 mi.signature() + ", which is declared in " +
				                                 rt.toClass().fullName(), ct.position());
				 }
				 else { 
				     // no implementation, but that's ok, the class is abstract.
				 }
			     }
			     else if (!typeEquals(ct, mj.container()) && !typeEquals(ct, mi.container())) {
				 try {
				     // check that mj can override mi, which
				     // includes access protection checks.
				     checkOverride(mj, mi);
				 }
				 catch (SemanticException e) {
				     // change the position of the semantic
				     // exception to be the class that we
				     // are checking.
				     throw new SemanticException(e.getMessage(),
				                                 ct.position());
				 }
			     }
			     else {
				 // the method implementation mj or mi was
				 // declared in ct. So other checks will take
				 // care of access issues
			     }
			 }
		     }
		 }
	 }

	 public MethodInstance findImplementingMethod(ClassType ct, MethodInstance mi) {
	     return findImplementingMethod(ct, mi, false);
	 }
	 
	 public MethodInstance findImplementingMethod(ClassType ct, MethodInstance mi, boolean includeAbstract) {
	     StructType curr = ct;
		 while (curr != null) {
			 List<MethodInstance> possible = curr.methods(mi.name(), mi.formalTypes());
			 for (Iterator<MethodInstance> k = possible.iterator(); k.hasNext(); ) {
				 MethodInstance mj = k.next();
				 if ((includeAbstract || !mj.flags().isAbstract()) && 
						 ((isAccessible(mi, ct.def()) && isAccessible(mj, ct.def())) || 
								 isAccessible(mi, mj.container().toClass().def()))) {
					 // The method mj may be a suitable implementation of mi.
					 // mj is not abstract, and either mj's container 
					 // can access mi (thus mj can really override mi), or
					 // mi and mj are both accessible from ct (e.g.,
					 // mi is declared in an interface that ct implements,
					 // and mj is defined in a superclass of ct).
					 return mj;                    
				 }
			 }
			 if (curr.typeEquals(mi.container())) {
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

	 protected void initFlags() {
		 flagsForName = new HashMap();
		 flagsForName.put("public", Flags.PUBLIC);
		 flagsForName.put("private", Flags.PRIVATE);
		 flagsForName.put("protected", Flags.PROTECTED);
		 flagsForName.put("static", Flags.STATIC);
		 flagsForName.put("final", Flags.FINAL);
		 flagsForName.put("synchronized", Flags.SYNCHRONIZED);
		 flagsForName.put("transient", Flags.TRANSIENT);
		 flagsForName.put("native", Flags.NATIVE);
		 flagsForName.put("interface", Flags.INTERFACE);
		 flagsForName.put("abstract", Flags.ABSTRACT);
		 flagsForName.put("volatile", Flags.VOLATILE);
		 flagsForName.put("strictfp", Flags.STRICTFP);
	 }

	 public Flags createNewFlag(String name, Flags after) {
		 Flags f = Flags.createFlag(name, after);
		 flagsForName.put(name, f);
		 return f;
	 }

	 public Flags NoFlags()      { return Flags.NONE; }
	 public Flags Public()       { return Flags.PUBLIC; }
	 public Flags Private()      { return Flags.PRIVATE; }
	 public Flags Protected()    { return Flags.PROTECTED; }
	 public Flags Static()       { return Flags.STATIC; }
	 public Flags Final()        { return Flags.FINAL; }
	 public Flags Synchronized() { return Flags.SYNCHRONIZED; }
	 public Flags Transient()    { return Flags.TRANSIENT; }
	 public Flags Native()       { return Flags.NATIVE; }
	 public Flags Interface()    { return Flags.INTERFACE; }
	 public Flags Abstract()     { return Flags.ABSTRACT; }
	 public Flags Volatile()     { return Flags.VOLATILE; }
	 public Flags StrictFP()     { return Flags.STRICTFP; }

	 public Flags flagsForBits(int bits) {
		 Flags f = Flags.NONE;

		 if ((bits & Modifier.PUBLIC) != 0)       f = f.Public();
		 if ((bits & Modifier.PRIVATE) != 0)      f = f.Private();
		 if ((bits & Modifier.PROTECTED) != 0)    f = f.Protected();
		 if ((bits & Modifier.STATIC) != 0)       f = f.Static();
		 if ((bits & Modifier.FINAL) != 0)        f = f.Final();
		 if ((bits & Modifier.SYNCHRONIZED) != 0) f = f.Synchronized();
		 if ((bits & Modifier.TRANSIENT) != 0)    f = f.Transient();
		 if ((bits & Modifier.NATIVE) != 0)       f = f.Native();
		 if ((bits & Modifier.INTERFACE) != 0)    f = f.Interface();
		 if ((bits & Modifier.ABSTRACT) != 0)     f = f.Abstract();
		 if ((bits & Modifier.VOLATILE) != 0)     f = f.Volatile();
		 if ((bits & Modifier.STRICT) != 0)       f = f.StrictFP();

		 return f;
	 }

	 public Flags flagsForName(String name) {
		 Flags f = (Flags) flagsForName.get(name);
		 if (f == null) {
			 throw new InternalCompilerError("No flag named \"" + name + "\".");
		 }
		 return f;
	 }

	 public String toString() {
		 return StringUtil.getShortNameComponent(getClass().getName());
	 }

}
