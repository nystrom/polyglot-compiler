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
    protected Map flagsForName;
    protected ExtensionInfo extInfo;
    protected SymbolTable symbolTable;

    public SymbolTable symbolTable() {
        if (symbolTable == null) {
            symbolTable = new SymbolTable_c();
        }
        return symbolTable;
    }
    
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
        o = PrimitiveType.VOID;
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
    
    public SystemResolver saveSystemResolver() {
        SystemResolver r = this.systemResolver;
        this.systemResolver = (SystemResolver) r.copy();
        return r;
    }
    
    public void restoreSystemResolver(SystemResolver r) {
        if (r != this.systemResolver.previous()) {
            throw new InternalCompilerError("Inconsistent systemResolver.previous");
        }
        this.systemResolver = r;
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
    public boolean packageExists(String name) {
        return systemResolver.packageExists(name);
    }

    protected void assert_(Collection l) {
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof TypeObject) {
                assert_((TypeObject) o);
            }
            else if (o instanceof Ref) {
                assert_((Ref) o);
            }
        }
    }

    void assert_(Ref ref) { }

    protected void assert_(TypeObject o) {
        if (o != null && o.typeSystem() != this) {
            throw new InternalCompilerError("we are " + this + " but " + o + " ("+o.getClass()+")" +
                                            " is from " + o.typeSystem());
        }
    }

    public String wrapperTypeString(PrimitiveType t) {
        assert_(t);

	if (t.kind() == PrimitiveType.BOOLEAN) {
	    return "java.lang.Boolean";
	}
	if (t.kind() == PrimitiveType.CHAR) {
	    return "java.lang.Character";
	}
	if (t.kind() == PrimitiveType.BYTE) {
	    return "java.lang.Byte";
	}
	if (t.kind() == PrimitiveType.SHORT) {
	    return "java.lang.Short";
	}
	if (t.kind() == PrimitiveType.INT) {
	    return "java.lang.Integer";
	}
	if (t.kind() == PrimitiveType.LONG) {
	    return "java.lang.Long";
	}
	if (t.kind() == PrimitiveType.FLOAT) {
	    return "java.lang.Float";
	}
	if (t.kind() == PrimitiveType.DOUBLE) {
	    return "java.lang.Double";
	}
	if (t.kind() == PrimitiveType.VOID) {
	    return "java.lang.Void";
	}

	throw new InternalCompilerError("Unrecognized primitive type.");
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

    public Resolver classContextResolver(ClassType type, ClassDef accessor) {
        assert_(type);
        if (accessor == null) {
            return type.resolver();
        }
        else {
            return new AccessControlWrapperResolver(createClassContextResolver(type), accessor);
        }
    }
    
    public Resolver classContextResolver(ClassType type) {
        return classContextResolver(type, null);
    }

    public AccessControlResolver createClassContextResolver(ClassType type) {
        assert_(type);
	return new ClassContextResolver(this, type);
    }

    public FieldDef fieldInstance(Position pos,
	                               Ref<? extends ReferenceType> container, Flags flags,
	                               Ref<? extends Type> type, String name) {
        assert_(container);
        assert_(type);
	return new FieldDef_c(this, pos, container, flags, type, name);
    }

    public LocalDef localInstance(Position pos,
	                               Flags flags, Ref<? extends Type> type, String name) {
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
        return constructorInstance(pos, container,
                                   access, Collections.EMPTY_LIST,
                                   Collections.EMPTY_LIST);
    }

    public ConstructorDef constructorInstance(Position pos,
	                                           Ref<? extends ClassType> container,
						   Flags flags, List<Ref<? extends Type>> argTypes,
						   List<Ref<? extends Type>> excTypes) {
        assert_(container);
        assert_(argTypes);
        assert_(excTypes);
	return new ConstructorDef_c(this, pos, container, flags,
	                                 argTypes, excTypes);
    }

    public InitializerDef initializerInstance(Position pos,
            Ref<? extends ClassType> container,
						   Flags flags) {
        assert_(container);
	return new InitializerDef_c(this, pos, container, flags);
    }

    public MethodDef methodInstance(Position pos,
            Ref<? extends ReferenceType> container, Flags flags,
	                                 Ref<? extends Type> returnType, String name,
					 List<Ref<? extends Type>> argTypes, List<Ref<? extends Type>> excTypes) {

        assert_(container);
        assert_(returnType);
        assert_(argTypes);
        assert_(excTypes);
	return new MethodDef_c(this, pos, container, flags,
				    returnType, name, argTypes, excTypes);
    }

    /**
     * Returns true iff child and ancestor are distinct
     * reference types, and child descends from ancestor.
     **/
    public boolean descendsFrom(Type child, Type ancestor) {
        assert_(child);
        assert_(ancestor);
        return child.descendsFrom(ancestor);
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
        return fromType.isCastValid(toType);
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
        return fromType.isImplicitCastValid(toType);
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

    /**
     * Returns true iff type1 and type2 are equivalent.
     */
    public boolean typeEquals(Type type1, Type type2) {
        assert_(type1);
        assert_(type2);
        return type1.typeEquals(type2);
    }
    
    /**
     * Returns true iff type1 and type2 are equivalent.
     */
    public boolean packageEquals(Package type1, Package type2) {
        assert_(type1);
        assert_(type2);
        return type1.packageEquals(type2);
    }

    /**
     * Returns true if <code>value</code> can be implicitly cast to Primitive
     * type <code>t</code>.
     */
    public boolean numericConversionValid(Type t, Object value) {
        assert_(t);
        return t.numericConversionValid(value);
    }

    /**
     * Returns true if <code>value</code> can be implicitly cast to Primitive
     * type <code>t</code>.  This method should be removed.  It is kept for
     * backward compatibility.
     */
    public boolean numericConversionValid(Type t, long value) {
        assert_(t);
        return t.numericConversionValid(value);
    }

    ////
    // Functions for one-type checking and resolution.
    ////

    /**
     * Checks whether the member mi can be accessed from Context "context".
     */
    public boolean isAccessible(MemberInstance<? extends MemberDef> mi, Context context) {
        return isAccessible(mi, context.currentClassScope());
    }

    /**
     * Checks whether the member mi can be accessed from code that is
     * declared in the class contextClass.
     */
    public boolean isAccessible(MemberInstance<? extends MemberDef> mi, ClassDef contextClass) {
        assert_(mi);
        
        ClassType contextClassType = new ParsedClassType_c(this, contextClass.position(), Ref_c.<ClassDef>ref(contextClass));

        ReferenceType target = mi.container();
	Flags flags = mi.flags();

        if (! target.isClass()) {
            // public members of non-classes are accessible;
            // non-public members of non-classes are inaccessible
            return flags.isPublic();
        }

        ClassType targetClass = target.toClass();

        if (! classAccessible(targetClass, contextClass)) {
            return false;
        }

        if (equals(targetClass, contextClass))
            return true;

        // If the current class and the target class are both in the
        // same class body, then protection doesn't matter, i.e.
        // protected and private members may be accessed. Do this by
        // working up through contextClass's containers.
        if (isEnclosed(contextClassType, targetClass) || isEnclosed(targetClass, contextClassType))
            return true;

        ClassType ct = contextClassType;
        while (!ct.isTopLevel()) {
            ct = ct.outer();
            if (isEnclosed(targetClass, ct))
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

            ct = contextClassType;
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
    public boolean classAccessible(ClassType targetClass, Context context) {
        if (context.currentClass() == null) {
            return classAccessibleFromPackage(targetClass, TypeObject_c.get(context.importTable().package_()));
        }
        else {
            return classAccessible(targetClass, context.currentClassScope());
        }
    }

    /** True if the class targetClass accessible from the body of class contextClass. */
    public boolean classAccessible(ClassType targetClass, ClassDef contextClass) {
        assert_(targetClass);

        if (targetClass.isMember()) {
            return isAccessible(targetClass, contextClass);
        }

        ClassType contextClassType = new ParsedClassType_c(this, contextClass.position(), Ref_c.<ClassDef>ref(contextClass));

        // Local and anonymous classes are accessible if they can be named.
        // This method wouldn't be called if they weren't named.
        if (! targetClass.isTopLevel()) {
            return true;
        }

        // targetClass must be a top-level class
        
        // same class
	if (equals(targetClass, contextClass))
            return true;

        if (isEnclosed(contextClassType, targetClass))
            return true;
        
        return classAccessibleFromPackage(targetClass, contextClassType.package_());
    }

    /** True if the class targetClass accessible from the package pkg. */
    public boolean classAccessibleFromPackage(ClassType targetClass, Package pkg) {
        assert_(targetClass);

        // Local and anonymous classes are not accessible from the outermost
        // scope of a compilation unit.
        if (! targetClass.isTopLevel() && ! targetClass.isMember())
            return false;

	Flags flags = targetClass.flags();

        if (targetClass.isMember()) {
            if (! targetClass.container().isClass()) {
                // public members of non-classes are accessible
                return flags.isPublic();
            }

            if (! classAccessibleFromPackage(targetClass.container().toClass(), pkg)) {
                return false;
            }
        }

        return accessibleFromPackage(flags, targetClass.package_(), pkg);
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
            if (pkg1 != null && pkg1.equals(pkg2))
                return true;
	}

        // Otherwise private.
	return false;
    }

    public boolean isEnclosed(ClassType inner, ClassType outer) {
        return inner.isEnclosed(outer);
    }

    public boolean hasEnclosingInstance(ClassType inner, ClassType encl) {
        return inner.hasEnclosingInstance(encl);
    }

    public void checkCycles(ReferenceType goal) throws SemanticException {
	checkCycles(goal, goal);
    }

    protected void checkCycles(ReferenceType curr, ReferenceType goal)
	throws SemanticException {

        assert_(curr);
        assert_(goal);

	if (curr == null) {
	    return;
	}

	ReferenceType superType = null;

	if (curr.superType() != null) {
	    superType = curr.superType().toReference();
	}

	if (goal == superType) {
	    throw new SemanticException("Circular inheritance involving " + goal, 
                                        curr.position());
	}

	checkCycles(superType, goal);

	for (Iterator i = curr.interfaces().iterator(); i.hasNext(); ) {
	    Type si = (Type) i.next();

	    if (si == goal) {
                throw new SemanticException("Circular inheritance involving " + goal, 
                                            curr.position());
	    }

	    checkCycles(si.toReference(), goal);
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

    /**
     * Returns true iff an object of type <type> may be thrown.
     **/
    public boolean isThrowable(Type type) {
        assert_(type);
        return type.isThrowable();
    }

    /**
     * Returns a true iff the type or a supertype is in the list
     * returned by uncheckedExceptions().
     */
    public boolean isUncheckedException(Type type) {
        assert_(type);
        return type.isUncheckedException();
    }

    /**
     * Returns a list of the Throwable types that need not be declared
     * in method and constructor signatures.
     */
    public Collection<Type> uncheckedExceptions() {
        List l = new ArrayList(2);
	l.add(Error());
	l.add(RuntimeException());
	return l;
    }

    public boolean isSubtype(Type t1, Type t2) {
        assert_(t1);
        assert_(t2);
        return t1.isSubtype(t2);
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
    public FieldInstance findField(ReferenceType container, String name,
	                           ClassDef currClass) throws SemanticException {
	Collection<FieldInstance> fields = findFields(container, name);
	
	if (fields.size() == 0) {
	    throw new NoMemberException(NoMemberException.FIELD,
					"Field \"" + name +
					"\" not found in type \"" +
					container + "\".");
	}
	
	Iterator<FieldInstance> i = fields.iterator();
	FieldInstance fi = i.next();
	
	if (i.hasNext()) {
	    FieldInstance fi2 = i.next();
	    
	    throw new SemanticException("Field \"" + name +
					"\" is ambiguous; it is defined in both " +
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
    public FieldInstance findField(ReferenceType container, String name)
	throws SemanticException {
	
	return findField(container, name, (ClassDef) null);
    }
	    
    
    /**
     * Returns a set of fields named <code>name</code> defined
     * in type <code>container</code> or a supertype.  The list
     * returned may be empty.
     */
    protected Set<FieldInstance> findFields(ReferenceType container, String name) {
        assert_(container);

        if (container == null) {
            throw new InternalCompilerError("Cannot access field \"" + name +
                "\" within a null container type.");
        }

	FieldInstance fi = container.fieldNamed(name);
	
	if (fi != null) {
	    return Collections.singleton(fi);
	}

	Set<FieldInstance> fields = new HashSet<FieldInstance>();

	if (container.superType() != null && container.superType().isReference()) {
	    Set<FieldInstance> superFields = findFields(container.superType().toReference(), name);
	    fields.addAll(superFields);
	}

	if (container.isClass()) {
	    // Need to check interfaces for static fields.
	    ClassType ct = container.toClass();
	
	    for (Iterator<Type> i = ct.interfaces().iterator(); i.hasNext(); ) {
		Type it = i.next();
		Set superFields = findFields(it.toReference(), name);
		fields.addAll(superFields);
	    }
	}
	
	return fields;
    }

    /**
     * @deprecated
     */
    public ClassType findMemberClass(ClassType container, String name,
                                     Context c) throws SemanticException {
        return findMemberClass(container, name, c.currentClassScope());
    }
    
    public ClassType findMemberClass(ClassType container, String name,
                                     ClassDef currClass) throws SemanticException
    {
	assert_(container);
        
        Named n = classContextResolver(container, currClass).find(name);
        
        if (n instanceof ClassType) {
            return (ClassType) n;
        }
        
        throw new NoClassException(name, container);
    }
    
    public ClassType findMemberClass(ClassType container, String name)
        throws SemanticException {

	return findMemberClass(container, name, (ClassDef) null);
    }
    
    protected static String listToString(List l) {
	StringBuffer sb = new StringBuffer();

	for (Iterator i = l.iterator(); i.hasNext(); ) {
	    Object o = i.next();
            sb.append(o.toString());

	    if (i.hasNext()) {
                sb.append(", ");
	    }
	}

	return sb.toString();
    }

    /**
     * @deprecated
     */
    public MethodInstance findMethod(ReferenceType container,
                                 String name, List argTypes, Context c)
    throws SemanticException {
        return findMethod(container, name, argTypes, c.currentClassScope());
    }

    /**
     * Returns the list of methods with the given name defined or inherited
     * into container, checking if the methods are accessible from the
     * body of currClass
     */
    public boolean hasMethodNamed(ReferenceType container, String name) {
        assert_(container);

        if (container == null) {
            throw new InternalCompilerError("Cannot access method \"" + name +
                "\" within a null container type.");
        }

	if (! container.methodsNamed(name).isEmpty()) {
            return true;
	}

	if (container.superType() != null && container.superType().isReference()) {
            if (hasMethodNamed(container.superType().toReference(), name)) {
                return true;
            }
	}

	if (container.isClass()) {
	    ClassType ct = container.toClass();
	
	    for (Iterator i = ct.interfaces().iterator(); i.hasNext(); ) {
		Type it = (Type) i.next();
                if (hasMethodNamed(it.toReference(), name)) {
                    return true;
                }
	    }
	}
	
        return false;
    }

    /**
     * Requires: all type arguments are canonical.
     *
     * Returns the MethodInstance named 'name' defined on 'type' visible in
     * context.  If no such field may be found, returns a fieldmatch
     * with an error explaining why.  Access flags are considered.
     **/
    public MethodInstance findMethod(ReferenceType container,
	                             String name, List<Type> argTypes, ClassDef currClass)
	throws SemanticException {

        assert_(container);
        assert_(argTypes);
        
        List<MethodInstance> acceptable = findAcceptableMethods(container, name, argTypes, currClass);
        
        if (acceptable.size() == 0) {
            throw new NoMemberException(NoMemberException.METHOD,
                                        "No valid method call found for " + name +
                                        "(" + listToString(argTypes) + ")" +
                                        " in " +
                                        container + ".");
        }
    
        Collection<MethodInstance> maximal =
            findMostSpecificProcedures(acceptable);
    
	if (maximal.size() > 1) {
	    StringBuffer sb = new StringBuffer();
            for (Iterator i = maximal.iterator(); i.hasNext();) {
                MethodDef ma = (MethodDef) i.next();
                sb.append(ma.returnType());
                sb.append(" ");
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
        
	    throw new SemanticException("Reference to " + name +
					" is ambiguous, multiple methods match: "
					+ sb.toString());
	}
		
	MethodInstance mi = maximal.iterator().next();
	return mi;
    }

    /**
     * @deprecated
     */
    public ConstructorInstance findConstructor(ClassType container,
                                 List argTypes, Context c)
    throws SemanticException {
        return findConstructor(container, argTypes, c.currentClassScope());
    }

    public ConstructorInstance findConstructor(ClassType container,
                           List<Type> argTypes, ClassDef currClass)
	throws SemanticException {

        assert_(container);
        assert_(argTypes);

	List<ConstructorInstance> acceptable = findAcceptableConstructors(container, argTypes, currClass);

	if (acceptable.size() == 0) {
	    throw new NoMemberException(NoMemberException.CONSTRUCTOR,
                                        "No valid constructor found for " +
                                        container + "(" + listToString(argTypes) + ").");
	}

	Collection<ConstructorInstance> maximal = findMostSpecificProcedures(acceptable);

	if (maximal.size() > 1) {
	    throw new NoMemberException(NoMemberException.CONSTRUCTOR,
		"Reference to " + container + " is ambiguous, multiple " +
		"constructors match: " + maximal);
	}

	ConstructorInstance ci = maximal.iterator().next();
	return ci;
    }

    protected <T extends ProcedureInstance> T findProcedure(List<T> acceptable,
	                                      ReferenceType container,
					      List<Type> argTypes,
					      ClassDef currClass)
    throws SemanticException {
        Collection<T> maximal = findMostSpecificProcedures(acceptable);
        
       
        if (maximal.size() == 1) {
            return maximal.iterator().next();
        }
        
        return null;
    }
    
    protected <S extends ProcedureDef, T extends ProcedureInstance<S>> Collection<T> findMostSpecificProcedures(List<T> acceptable)
	throws SemanticException {

	// now, use JLS 15.11.2.2
	// First sort from most- to least-specific.
	MostSpecificComparator<S,T> msc = new MostSpecificComparator<S,T>();
	acceptable = new ArrayList<T>(acceptable); // make into array list to sort
	Collections.<T>sort(acceptable, msc);

	List<T> maximal = new ArrayList<T>(acceptable.size());

	Iterator<T> i = acceptable.iterator();
    
	T first = i.next();
	maximal.add(first);

	// Now check to make sure that we have a maximal most-specific method.
	while (i.hasNext()) {
	    T p = i.next();

	    if (msc.compare(first, p) >= 0) {
	        maximal.add(p);
	    }
	}
	
	if (maximal.size() > 1) {
	    // If exactly one method is not abstract, it is the most specific.
	    List<T> notAbstract = new ArrayList<T>(maximal.size());
	    for (Iterator<T> j = maximal.iterator(); j.hasNext(); ) {
	        T p = j.next();
	        if (! (p instanceof MemberDef) || ! ((MemberDef) p).flags().isAbstract()) {
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
	        List<Type> firstFormals = new TransformingList(firstDecl.formalTypes(), new SymbolTransform());
	        while (j.hasNext()) {
	            T p = j.next();
	            
                    // Use the declarations to compare formals.
	            S pDecl = p.def();

	            List<Type> pFormals = new TransformingList(pDecl.formalTypes(), new SymbolTransform());
                    if (! firstFormals.equals(pFormals)) {
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

    /**
     * Class to handle the comparisons; dispatches to moreSpecific method.
     */
    protected static class MostSpecificComparator<S extends ProcedureDef, T extends ProcedureInstance<S>> implements Comparator<T> {
	public int compare(T p1, T p2) {
	    if (p1.moreSpecific(p2)) return -1;
	    if (p2.moreSpecific(p1)) return 1;
	    return 0;
	}
    }
    
    /**
     * Populates the list acceptable with those MethodInstances which are
     * Applicable and Accessible as defined by JLS 15.11.2.1
     */
    protected List<MethodInstance> findAcceptableMethods(ReferenceType container, String name,
                                     List<Type> argTypes, ClassDef currClass)
	throws SemanticException {

        assert_(container);
        assert_(argTypes);
        
        SemanticException error = null;
        
        // The list of acceptable methods. These methods are accessible from
        // currClass, the method call is valid, and they are not overridden
        // by an unacceptable method (which can occur with protected methods
        // only).
        List<MethodInstance> acceptable = new ArrayList();

        // A list of unacceptable methods, where the method call is valid, but
        // the method is not accessible. This list is needed to make sure that
        // the acceptable methods are not overridden by an unacceptable method.
        List<MethodInstance> unacceptable = new ArrayList();
        
	Set<Type> visitedTypes = new HashSet();

	LinkedList<Type> typeQueue = new LinkedList<Type>();
	typeQueue.addLast(container);

	while (! typeQueue.isEmpty()) {
	    Type t = typeQueue.removeFirst();

	    if (! (t instanceof ReferenceType)) {
	        continue;
	    }

	    ReferenceType type = (ReferenceType) t;
	    
	    if (visitedTypes.contains(type)) {
		continue;
	    }

	    visitedTypes.add(type);

            if (Report.should_report(Report.types, 2))
		Report.report(2, "Searching type " + type + " for method " +
                              name + "(" + listToString(argTypes) + ")");

            for (Iterator<MethodInstance> i = type.methods().iterator(); i.hasNext(); ) {
	        MethodInstance mi = i.next();

		if (Report.should_report(Report.types, 3))
		    Report.report(3, "Trying " + mi);
        
		if (! mi.name().equals(name)) {
		    continue;
		}

                if (methodCallValid(mi, name, argTypes)) {
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
		}
                else {
                    if (error == null) {
                        error = new NoMemberException(NoMemberException.METHOD,
                                                      "Method " + mi.signature() +
                                                      " in " + container +
                                                      " cannot be called with arguments " +
                                                      "(" + listToString(argTypes) + ")."); 
                    }
                }
            }
            if (type.superType() != null) {
                typeQueue.addLast((ReferenceType) type.superType());
            }

            typeQueue.addAll(type.interfaces());
        }

	if (error == null) {
	    error = new NoMemberException(NoMemberException.METHOD,
	                                  "No valid method call found for " + name +
	                                  "(" + listToString(argTypes) + ")" +
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
    protected List<ConstructorInstance> findAcceptableConstructors(ClassType container,
                                              List<Type> argTypes,
                                              ClassDef currClass)
        throws SemanticException
    {
        assert_(container);
        assert_(argTypes);
        
        SemanticException error = null;

	List<ConstructorInstance> acceptable = new ArrayList<ConstructorInstance>();

	if (Report.should_report(Report.types, 2))
	    Report.report(2, "Searching type " + container +
                          " for constructor " + container + "(" +
                          listToString(argTypes) + ")");

	for (Iterator<ConstructorInstance> i = container.constructors().iterator(); i.hasNext(); ) {
	    ConstructorInstance ci = i.next();

	    if (Report.should_report(Report.types, 3))
		Report.report(3, "Trying " + ci);
	    
	    if (callValid(ci, argTypes)) {
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
	    }
	    else {
	        if (error == null) {
                    error = new NoMemberException(NoMemberException.CONSTRUCTOR,
                                                  "Constructor " + ci.signature() +
                                                  " cannot be invoked with arguments " +
                                                  "(" + listToString(argTypes) + ")."); 
      
	        }
	    }
	}

	if (acceptable.size() == 0) {
	    if (error == null) {
	        error = new NoMemberException(NoMemberException.CONSTRUCTOR,
	                                      "No valid constructor found for " + container +
	                                      "(" + listToString(argTypes) + ").");
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
    public Type superType(ReferenceType type) {
        assert_(type);
	return type.superType();
    }

    /**
     * Returns an immutable list of all the interface types which type
     * implements.
     **/
    public List<Type> interfaces(ReferenceType type) {
        assert_(type);
	return type.interfaces();
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
	    return arrayOf(leastCommonAncestor(type1.toArray().base(),
					       type2.toArray().base()));
	}

	if (type1.isReference() && type2.isNull()) return type1;
	if (type2.isReference() && type1.isNull()) return type2;

	if (type1.isReference() && type2.isReference()) {
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

	    // Walk up the hierarchy
	    Type t1 = leastCommonAncestor(type1.toReference().superType(),
		                          type2);
	    Type t2 = leastCommonAncestor(type2.toReference().superType(),
					  type1);

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
    public boolean hasMethod(ReferenceType t, MethodInstance mi) {
        assert_(t);
        assert_(mi);
        return t.hasMethod(mi);
    }

    public List<MethodInstance> overrides(MethodInstance mi) {
        return mi.overrides();
    }

    public List<MethodInstance> implemented(MethodInstance mi) {
	return mi.implemented(mi.container());
    }

    public boolean canOverride(MethodInstance mi, MethodInstance mj) {
        try {
            return mi.canOverride(mj, true);
        }
        catch (SemanticException e) {
            // this is the exception thrown by the canOverrideImpl check.
            // It should never be thrown if the quiet argument of
            // canOverrideImpl is true.
            throw new InternalCompilerError(e);
        }
    }

    public void checkOverride(MethodInstance mi, MethodInstance mj) throws SemanticException {
        mi.canOverride(mj, false);
    }

    /**
     * Returns true iff <m1> is the same method as <m2>
     */
    public boolean isSameMethod(MethodInstance m1, MethodInstance m2) {
        assert_(m1);
        assert_(m2);
        return m1.isSameMethod(m2);
    }

    public boolean methodCallValid(MethodInstance prototype,
				   String name, List<Type> argTypes) {
        assert_(prototype);
        assert_(argTypes);
	return prototype.methodCallValid(name, argTypes);
    }

    public boolean callValid(ProcedureInstance<? extends ProcedureDef> prototype, List<Type> argTypes) {
        assert_(prototype);
        assert_(argTypes);
        return prototype.callValid(argTypes);
    }

    ////
    // Functions which yield particular types.
    ////
    public NullType Null()         { return NULL_; }
    public PrimitiveType Void()    { return VOID_; }
    public PrimitiveType Boolean() { return BOOLEAN_; }
    public PrimitiveType Char()    { return CHAR_; }
    public PrimitiveType Byte()    { return BYTE_; }
    public PrimitiveType Short()   { return SHORT_; }
    public PrimitiveType Int()     { return INT_; }
    public PrimitiveType Long()    { return LONG_; }
    public PrimitiveType Float()   { return FLOAT_; }
    public PrimitiveType Double()  { return DOUBLE_; }

    protected ClassType load(String name) {
      try {
          return (ClassType) typeForName(name);
      }
      catch (SemanticException e) {
          throw new InternalCompilerError("Cannot find class \"" +
                                          name + "\"; " + e.getMessage(),
                                          e);
      }
    }
    
    public Named forName(String name) throws SemanticException {
        try {
            return systemResolver.find(name);
        }
        catch (SemanticException e) {
            if (! StringUtil.isNameShort(name)) {
                String containerName = StringUtil.getPackageComponent(name);
                String shortName = StringUtil.getShortNameComponent(name);
                
                try {
                    Named container = forName(containerName);
		    if (container instanceof ClassType) {
			return classContextResolver((ClassType) container).find(shortName);
		    }
                }
                catch (SemanticException e2) {
                }
            }
	    
            // throw the original exception
            throw e;
        }
    }

    public Type typeForName(String name) throws SemanticException {
	return (Type) forName(name);
    }

    protected ClassType OBJECT_;
    protected ClassType CLASS_;
    protected ClassType STRING_;
    protected ClassType THROWABLE_;

    public ClassType Object()  { if (OBJECT_ != null) return OBJECT_;
                                 return OBJECT_ = load("java.lang.Object"); }
    public ClassType Class()   { if (CLASS_ != null) return CLASS_;
                                 return CLASS_ = load("java.lang.Class"); }
    public ClassType String()  { if (STRING_ != null) return STRING_;
                                 return STRING_ = load("java.lang.String"); }
    public ClassType Throwable() { if (THROWABLE_ != null) return THROWABLE_;
                                   return THROWABLE_ = load("java.lang.Throwable"); }
    public ClassType Error() { return load("java.lang.Error"); }
    public ClassType Exception() { return load("java.lang.Exception"); }
    public ClassType RuntimeException() { return load("java.lang.RuntimeException"); }
    public ClassType Cloneable() { return load("java.lang.Cloneable"); }
    public ClassType Serializable() { return load("java.io.Serializable"); }
    public ClassType NullPointerException() { return load("java.lang.NullPointerException"); }
    public ClassType ClassCastException()   { return load("java.lang.ClassCastException"); }
    public ClassType OutOfBoundsException() { return load("java.lang.ArrayIndexOutOfBoundsException"); }
    public ClassType ArrayStoreException()  { return load("java.lang.ArrayStoreException"); }
    public ClassType ArithmeticException()  { return load("java.lang.ArithmeticException"); }

    protected NullType createNull() {
        return new NullType_c(this);
    }

    protected PrimitiveType createPrimitive(PrimitiveType.Kind kind) {
        return new PrimitiveType_c(this, kind);
    }

    protected final NullType NULL_         = createNull();
    protected final PrimitiveType VOID_    = createPrimitive(PrimitiveType.VOID);
    protected final PrimitiveType BOOLEAN_ = createPrimitive(PrimitiveType.BOOLEAN);
    protected final PrimitiveType CHAR_    = createPrimitive(PrimitiveType.CHAR);
    protected final PrimitiveType BYTE_    = createPrimitive(PrimitiveType.BYTE);
    protected final PrimitiveType SHORT_   = createPrimitive(PrimitiveType.SHORT);
    protected final PrimitiveType INT_     = createPrimitive(PrimitiveType.INT);
    protected final PrimitiveType LONG_    = createPrimitive(PrimitiveType.LONG);
    protected final PrimitiveType FLOAT_   = createPrimitive(PrimitiveType.FLOAT);
    protected final PrimitiveType DOUBLE_  = createPrimitive(PrimitiveType.DOUBLE);
    
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
                String name = getTransformedClassName(ct);

                TypeSystem_c ts = this;
                TypeRef<ClassDef> sym = ts.symbolTable().typeRef();
                Goal resolver = Globals.Scheduler().LookupGlobalTypeDef(sym, name);
                Globals.Scheduler().markReached(resolver);
                sym.setResolver(resolver);
                return sym;
            }
        }

	return o;
    }

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

    public Package packageForName(Package prefix, String name) throws SemanticException {
        return createPackage(prefix, name);
    }
    
    public Package packageForName(Ref<? extends Package> prefix, String name) throws SemanticException {
        return createPackage(prefix, name);
    }

    public Package packageForName(String name) throws SemanticException {
        if (name == null || name.equals("")) {
	    return null;
	}

	String s = StringUtil.getShortNameComponent(name);
	String p = StringUtil.getPackageComponent(name);

	return packageForName(packageForName(p), s);
    }

    /** @deprecated */
    public Package createPackage(Package prefix, String name) {
        return createPackage(prefix != null ? Ref_c.ref(prefix) : null, name);
    }
    
    /** @deprecated */
    public Package createPackage(Ref<? extends Package> prefix, String name) {
        assert_(prefix);
	return new Package_c(this, prefix, name);
    }

    /** @deprecated */
    public Package createPackage(String name) {
        if (name == null || name.equals("")) {
	    return null;
	}

	String s = StringUtil.getShortNameComponent(name);
	String p = StringUtil.getPackageComponent(name);

	return createPackage(createPackage(p), s);
    }

    /**
     * Returns a type identical to <type>, but with <dims> more array
     * dimensions.
     */
    public ArrayType arrayOf(Type type) {
        assert_(type);
        return arrayOf(type.position(), Ref_c.ref(type));
    }
    
    public ArrayType arrayOf(Ref<? extends Type> type) {
        assert_(type);
        return arrayOf(null, type);
    }

    public ArrayType arrayOf(Position pos, Ref<? extends Type> type) {
        return arrayType(pos, type);
    }
    
    public ArrayType arrayOf(Position pos, Type type) {
        assert_(type);
	return arrayType(pos, Ref_c.ref(type));
    }

    public ArrayType arrayOf(Type type, int dims) {
        return arrayOf(Ref_c.ref(type), dims);
    }

    public ArrayType arrayOf(Position pos, Type type, int dims) {
        return arrayOf(pos, Ref_c.ref(type), dims);
    }

    Map arrayTypeCache = new HashMap();

    /**
     * Factory method for ArrayTypes.
     */
    protected ArrayType arrayType(Position pos, Ref<? extends Type> type) {
        ArrayType t = (ArrayType) arrayTypeCache.get(type);
        if (t == null) {
            t = new ArrayType_c(this, pos, type);
            arrayTypeCache.put(type, t);
        }
        return t;
    }

    public ArrayType arrayOf(Ref<? extends Type> type, int dims) {
        return arrayOf(null, type, dims);
    }
    
    public ArrayType arrayOf(Position pos, Ref<? extends Type> type, int dims) {
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
    public Type typeForClass(Class clazz) throws SemanticException
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

	return (Type) systemResolver.find(clazz.getName());
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
     */
    public String getTransformedClassName(ClassDef ct) {
        StringBuffer sb = new StringBuffer(ct.fullName().length());
        if (!ct.isMember() && !ct.isTopLevel()) {
            return null;
        }
        while (ct.isMember()) {
            sb.insert(0, ct.name());
            sb.insert(0, '$');
            ct = ct.outer().get();
            if (!ct.isMember() && !ct.isTopLevel()) {
                return null;
            }
        }

        sb.insert(0, ct.fullName());
        return sb.toString();
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

    public PrimitiveType primitiveForName(String name)
	throws SemanticException {

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
    
    public final ClassDef createClassDef(Source fromSource) {
        return new ClassDef_c(this, fromSource);
    }

    public ParsedClassType createClassType(ClassDef def) {
        return new ParsedClassType_c(this, def.position(), Ref_c.<ClassDef>ref(def));
    }

    public List<Ref<Package>> defaultPackageImports() {
	List l = new ArrayList(1);
	l.add("java.lang");
	return l;
    }

    public PrimitiveType promote(Type t1, Type t2) throws SemanticException {
	if (! t1.isNumeric()) {
	    throw new SemanticException(
		"Cannot promote non-numeric type " + t1);
	}

	if (! t2.isNumeric()) {
	    throw new SemanticException(
		"Cannot promote non-numeric type " + t2);
	}

	return promoteNumeric(t1.toPrimitive(), t2.toPrimitive());
    }

    protected PrimitiveType promoteNumeric(PrimitiveType t1, PrimitiveType t2) {
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

    public PrimitiveType promote(Type t) throws SemanticException {
	if (! t.isNumeric()) {
	    throw new SemanticException(
		"Cannot promote non-numeric type " + t);
	}

	return promoteNumeric(t.toPrimitive());
    }

    protected PrimitiveType promoteNumeric(PrimitiveType t) {
	if (t.isByte() || t.isShort() || t.isChar()) {
	    return Int();
	}

	return t.toPrimitive();
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
        return legalAccessFlags().Synchronized().Native();
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
    protected List<Type> abstractSuperInterfaces(ReferenceType rt) {
        List<Type> superInterfaces = new LinkedList<Type>();
        superInterfaces.add(rt);

        for (Iterator<Type> iter = rt.interfaces().iterator(); iter.hasNext(); ) {
            ClassType interf = (ClassType) iter.next();
            superInterfaces.addAll(abstractSuperInterfaces(interf));
        }

        if (rt.superType() != null) {
            ClassType c = rt.superType().toClass();
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
            ReferenceType rt = (ReferenceType)i.next();
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
                else if (!equals(ct, mj.container()) && !equals(ct, mi.container())) {
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
    
    public MethodInstance findImplementingMethod(ClassType ct, MethodInstance mi) {
        ReferenceType curr = ct;
        while (curr != null) {
            List<MethodInstance> possible = curr.methods(mi.name(), mi.formalTypes());
            for (Iterator<MethodInstance> k = possible.iterator(); k.hasNext(); ) {
                MethodInstance mj = k.next();
                if (!mj.flags().isAbstract() && 
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
            if (curr == mi.container()) {
                // we've reached the definition of the abstract 
                // method. We don't want to look higher in the 
                // hierarchy; this is not an optimization, but is 
                // required for correctness. 
                break;
            }
            
            curr = curr.superType() ==  null ?
                   null : curr.superType().toReference();
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
