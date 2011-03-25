package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.AnnotationElemInstance;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.MethodDef;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;

/**
 * A <code>ClassDecl</code> is the definition of a class, abstract class, or
 * interface. It may be a public or other top-level class, or an inner named
 * class, or an anonymous class.
 */
public class JL5ClassDecl_c extends ClassDecl_c implements JL5ClassDecl, ApplicationCheck {

	protected List<AnnotationElem> annotations;
    protected List runtimeAnnotations;
    protected List classAnnotations;
    protected List sourceAnnotations;
    protected List<ParamTypeNode> paramTypes;
    
    public JL5ClassDecl_c(Position pos, FlagAnnotations flags, Id name, TypeNode superClass,
            List interfaces, ClassBody body) {
        super(pos, flags.classicFlags(), name, superClass, interfaces, body);
        List<AnnotationElem> l;
        if (flags.annotations() != null) {
            l = flags.annotations();
        } else {
        	l = Collections.EMPTY_LIST;
        }
        this.annotations = TypedList.copyAndCheck(flags.annotations(), AnnotationElem.class, false);
    }

    /**
     * 
     * @param pos
     * @param fl
     * @param name
     * @param superType
     * @param interfaces
     * @param body
     * @param paramTypes The list of Parameter type (ParamTypeNode represents a TypeVariable + its bounds)
     */
    public JL5ClassDecl_c(Position pos, FlagAnnotations fl, Id name, TypeNode superType,
            List interfaces, ClassBody body, List<ParamTypeNode> paramTypes) {
    	this(pos, fl, name, superType, interfaces, body);
        this.paramTypes = paramTypes;
    }

    public List<AnnotationElem> annotations() {
        return this.annotations;
    }

    public JL5ClassDecl annotations(List<AnnotationElem> annotations) {
        if (annotations != null) {
            JL5ClassDecl_c n = (JL5ClassDecl_c) copy();
            n.annotations = annotations;
            return n;
        }
        return this;
    }

    public List<ParamTypeNode> paramTypes() {
        return this.paramTypes;
    }

    public JL5ClassDecl paramTypes(List<ParamTypeNode> types) {
        JL5ClassDecl_c n = (JL5ClassDecl_c) copy();
        n.paramTypes = types;
        return n;
    }

    protected ClassDecl reconstruct(FlagsNode flags, Id name, TypeNode superClass, 
    		List<TypeNode> interfaces, ClassBody body,
    		List annotations, List paramTypes) {
    	ClassDecl superCopy = super.reconstruct(flags, name, superClass, interfaces, body);
    	if ( !CollectionUtil.allEqual(annotations, this.annotations)
    			|| !CollectionUtil.allEqual(paramTypes, this.paramTypes)) {
    		JL5ClassDecl_c n = (JL5ClassDecl_c) superCopy.copy();
    		n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, false);
    		n.paramTypes = paramTypes;
    		return n;
    	}
    	return superCopy;
    }

    public Node visitChildren(NodeVisitor v) {
    	List annots = visitList(this.annotations, v);
    	List paramTypes = visitList(this.paramTypes, v);
    	JL5ClassDecl_c cd = (JL5ClassDecl_c) super.visitChildren(v);
    	return cd.reconstruct(cd.flags(), cd.name(), cd.superClass(), 
    			cd.interfaces(), cd.body(), annots, paramTypes);
    }

    /*
    public Context enterScope(Node child, Context c) {
        // System.out.println("enter scop with child : " + child );//for debug
        for (ParamTypeNode tn : paramTypes) {
            c = ((JL5Context) c).pushTypeVariable((TypeVariable) tn.type());
        }
        return super.enterScope(child, c);
    }
*/
    @Override
    public Context enterChildScope(Node child, Context c) {
        //CHECK Need to push type variable so that they are resolved in members and body
    	for (ParamTypeNode tn : paramTypes) {
            c = ((JL5Context) c).addTypeVariable((TypeVariable) tn.type());
        }
        return super.enterChildScope(child, c);
    	
    }

//    protected void disambiguateSuperType(AmbiguityRemover ar) throws SemanticException {
//        JL5TypeSystem ts = (JL5TypeSystem) ar.typeSystem();
//        if (JL5Flags.isAnnotationModifier(flags().flags())) {
//            this.type.superType(ts.Annotation());
//        } else {
//            super.disambiguateSuperType(ar);
//        }
//    }

    // still need this - will cause an extra disamb pass which will permit
    // the type variables to fully disambigute themselves
    // before they may be needed as args in superClass or interfaces
// CHECK commented this, wait and see how it will go with p3    
//    public NodeVisitor disambiguateEnter(AmbiguityRemover ar) throws SemanticException {
//        if (ar.kind() == JL5AmbiguityRemover.TYPE_VARS) {
//            NodeVisitor nv = ar.bypass(superClass).bypass(interfaces);
//            return nv;
//        } else {
//            return super.disambiguateEnter(ar);
//        }
//    }

    public Node disambiguate(ContextVisitor ar) throws SemanticException {
        Node n = super.disambiguate(ar);
        addTypeParameters();
        return n;
    }
    
    @Override
    public Node conformanceCheck(ContextVisitor tc) throws SemanticException {
    	Context ctx = tc.context();
    	JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
    	JL5ParsedClassType type = (JL5ParsedClassType) this.type.asType();
    	ClassType superType = (ClassType) type.superClass();
    	Flags flags = flags().flags();
    	
    	/* Enum-related checks */

    	if (JL5Flags.isEnumModifier(flags) && flags.isAbstract()) {
            throw new SemanticException("Enum types cannot have abstract modifier", this.position());
        }
        if (JL5Flags.isEnumModifier(flags) && flags.isPrivate() && !type.isInnerClass()) {
            throw new SemanticException("Enum types cannot have explicit private modifier", this.position());
        }
        if (JL5Flags.isEnumModifier(flags) && flags.isFinal()) {
            throw new SemanticException("Enum types cannot have explicit final modifier", this.position());
        }
        if (JL5Flags.isAnnotationModifier(flags) && flags.isPrivate()) {
            throw new SemanticException("Annotation types cannot have explicit private modifier", this.position());
        }
        if (superType != null && JL5Flags.isEnumModifier(superType.flags())) {
            throw new SemanticException("Cannot extend enum type", position());
        }
        if (ts.typeEquals(ts.Object(), type, ctx) && !paramTypes.isEmpty()) {
            throw new SemanticException("Type: " + type + " cannot declare type variables.", position());
        }

        if (JL5Flags.isEnumModifier(flags().flags())) {
            for (ConstructorInstance ci : type.constructors()) {
                if (!ci.flags().clear(Flags.PRIVATE).equals(Flags.NONE)) {
                    throw new SemanticException("Modifier " + ci.flags().clear(Flags.PRIVATE)
                            + " not allowed here", ci.position());
                }
            }
        }

        // Parameter types checks 
        if(!paramTypes.isEmpty()) {
        	// check not extending java.lang.Throwable (or any of its subclasses) with a generic class
            if (superType != null && ts.isSubtype(superType, ts.Throwable(), ctx)) {
                throw new SemanticException("Cannot subclass java.lang.Throwable or any of its subtypes with a generic class", superClass().position());
            }
            
            // check duplicate type variable decls
            for (int i = 0; i < paramTypes.size(); i++) {
                TypeNode ti = (TypeNode) paramTypes.get(i);
                for (int j = i + 1; j < paramTypes.size(); j++) {
                    TypeNode tj = (TypeNode) paramTypes.get(j);
                    if (ts.typeEquals(ti.type(), tj.type(), ctx)) {
                        throw new SemanticException("Duplicate type variable declaration.", tj.position());
                    }
                }
            }        	
        }
        
        if (type.isGeneric()) {
            ts.checkTVForwardReference(type.typeVariables());
        }
        
        ts.checkDuplicateAnnotations(annotations);
        
        // set up ct with annots
        type.annotations(this.annotations);

        //disallow wildcards in supertypes and super interfaces
        if (superClass() != null) {
            if (!ts.typeEquals(superType, ts.capture(superType), ctx)) {
                throw new SemanticException("Wildcards not allowed here.", superClass().position());
            }
        }
        
        for (TypeNode itNode : interfaces()){
            Type ittype = itNode.type();
            if (!ts.typeEquals(ittype, ts.capture(ittype), ctx)) {
                throw new SemanticException("Wildcards not allowed here.", itNode.position());
            }
        }
        
        this.checkSuperTypeTypeArgs(tc);

    	return super.conformanceCheck(tc);
    }

    private void checkSuperTypeTypeArgs(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        Context ctx = tc.context();
        ClassType type = this.type.asType();
        ClassType superType = (ClassType) type.superClass();
        List<Type>  allInterfaces = new ArrayList<Type>();
        allInterfaces.addAll(type.interfaces());
        if (superType != null) {
            allInterfaces.addAll(superType.interfaces());
        }

        for (int i = 0; i < allInterfaces.size(); i++) {
            Type next = (Type) allInterfaces.get(i);
            for (int j = i + 1; j < allInterfaces.size(); j++) {
                Type other = (Type) allInterfaces.get(j);
                if (next instanceof ParameterizedType && other instanceof ParameterizedType) {
                    if (ts.typeEquals(((ParameterizedType) next).baseType(), ((ParameterizedType) other).baseType(), ctx)
                            && !ts.typeEquals(next, other, ctx)) {
                        throw new SemanticException(((ParameterizedType) next).baseType()
                                + " cannot be inherited with different type arguments.", position());
                    }
                } else if (next instanceof ParameterizedType) {
                    if (ts.typeEquals(((ParameterizedType) next).baseType(), other, ctx)) {
                        throw new SemanticException(((ParameterizedType) next).baseType()
                                + " cannot be inherited with different type arguments.", position());
                    }
                } else if (other instanceof ParameterizedType) {
                    if (ts.typeEquals(((ParameterizedType) other).baseType(), next, ctx)) {
                        throw new SemanticException(((ParameterizedType) other).baseType()
                                + " cannot be inherited with different type arguments.", position());
                    }
                }
            }
        }
    }

    public Node applicationCheck(ApplicationChecker appCheck, Context ctx) throws SemanticException {

        // check proper used of predefined annotations
        JL5TypeSystem ts = (JL5TypeSystem) appCheck.typeSystem();
        for (AnnotationElem ae : annotations) {
            ts.checkAnnotationApplicability(ae, this);
        }

        // check annotation circularity
        if (JL5Flags.isAnnotationModifier(flags().flags())) {
            JL5ParsedClassType ct = (JL5ParsedClassType) type;
            for (AnnotationElemInstance ai : ct.annotationElems()) {
                if (ts.isTypeExtendsAnnotation(ai.type())) {
                    JL5ParsedClassType other = (JL5ParsedClassType) ai.type();
                    for (AnnotationElemInstance aj : other.annotationElems()) {
                        if (ts.typeEquals(aj.type(), ct, ctx)) {
                            throw new SemanticException("cyclic annotation element type", aj.position());
                        }
                    }
                }
            }
        }
        return this;
    }

    @Override
    public ClassDecl_c preBuildTypes(TypeBuilder tb) throws SemanticException {
    	JL5ClassDecl_c cd = (JL5ClassDecl_c) super.preBuildTypes(tb);
    	cd.addTypeParameters();
    	return cd;
    }

    /**
     * Enum Declarations are represented as Class Declarations, but we need to
     * generate some additional code wrt the JLS. 
     * @See JLS 8.9 Enums
     */
    @Override
    public ClassDecl_c postBuildTypes(TypeBuilder tb) throws SemanticException {
    	//CHECK not sure if it's ok to put the annotation super type here
    	//preBuildTypes sets the super type to the type it gets from the superClass ast node
        if (JL5Flags.isAnnotationModifier(flags().flags())) {
            this.type.superType(Types.ref(((JL5TypeSystem) tb.typeSystem()).Annotation()));
        }

    	JL5ClassDecl_c cd = (JL5ClassDecl_c) super.postBuildTypes(tb);
        if (JL5Flags.isEnumModifier(type.flags())) {
        	cd = (JL5ClassDecl_c) cd.addGenEnumMethods(tb);
        }
        
        return cd.postBuildTypes(tb);
    }
    
    /**
     * Add param types to the class type
     */
    protected void addTypeParameters() {
        for (Iterator<ParamTypeNode> it = paramTypes.iterator(); it.hasNext();) {
            TypeVariable tv = (TypeVariable)it.next().type();
            ((JL5ParsedClassType) this.type).addTypeVariable(tv);
        }
    }

    /**
     * Enums requires to generate additional code
     * @See JLS 8.9 Enums
     * @param tb
     * @return
     */
	protected Node addGenEnumMethods(TypeBuilder tb) {
		JL5ClassBody newBody = (JL5ClassBody) body();

		// Generating enum specific class members
		MethodDecl valuesMeth = this.addEnumMethodValues(tb);
		MethodDecl valueOfMeth = this.addEnumMethodValueOf(tb);
		
		// Adding new methods as class members
		newBody = (JL5ClassBody) newBody.addMember(valuesMeth);
		newBody = (JL5ClassBody) newBody.addMember(valueOfMeth);

		// Adding methods to the type system
		//CHECK when shall we add the method to the type ?
		this.type.addMethod(valuesMeth.methodDef());
		this.type.addMethod(valueOfMeth.methodDef());
		 
		return body(newBody);
	}
	
	/**
	 * add values method:
	 * public static E[] values();
	 * @param tb
	 * @return
	 */
	protected JL5MethodDecl addEnumMethodValues(TypeBuilder tb) {
		JL5TypeSystem ts = (JL5TypeSystem) tb.typeSystem();
		NodeFactory nf = tb.nodeFactory();
		Position pos = position();

		// creating method body
		//CHECK sounds it's wrong to return null !
		assert(false);
		Block valuesBlock = nf.Block(pos);
		valuesBlock = valuesBlock.append(nf.Return(pos, nf.NullLit(pos)));

		// creating method declaration
		FlagAnnotations vmFlags = new FlagAnnotations();
		FlagsNode vmFlagsNode = nf.FlagsNode(pos, Flags.PUBLIC.Static().Final());
		vmFlags.classicFlags(vmFlagsNode);
		TypeNode returnType = nf.CanonicalTypeNode(pos, ts.arrayOf(this.type.asType()));
		Id name = nf.Id(pos, "values");
		JL5MethodDecl valuesMeth = ((JL5NodeFactory) nf).JL5MethodDecl(
				pos, vmFlags, returnType,
				name, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
				valuesBlock, null);

		// creating associated method definition 
		MethodDef md = ts.methodDef(pos, Types.ref(this.classDef().asType()),
				vmFlagsNode.flags(), Types.ref(returnType.type()), Name.make("values"), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, true);

		return (JL5MethodDecl) valuesMeth.methodDef(md);
	}

	/**
	 * add values method:
	 * public static E valueOf(String name);
	 * @param tb
	 * @return
	 */
	protected JL5MethodDecl addEnumMethodValueOf(TypeBuilder tb) {
		JL5TypeSystem ts = (JL5TypeSystem) tb.typeSystem();
		JL5NodeFactory nf = (JL5NodeFactory) tb.nodeFactory();
		Position pos = position();

		// Create the method argument
		FlagsNode flags = nf.FlagsNode(pos, Flags.NONE);
		FlagAnnotations fl = new FlagAnnotations(flags);
		
		LocalDef ld = ts.localDef(pos, flags.flags(), Types.ref(ts.String()), Name.make("arg1"));
		JL5Formal formal = nf.JL5Formal(pos, fl, nf.CanonicalTypeNode(pos, ts.String()), nf.Id(pos, "arg1"));
        formal = (JL5Formal) formal.localDef(ld);
		List<JL5Formal> formals = Collections.singletonList(formal);

		// Create the body of the method
		//CHECK it's wrong to always return null from valueOf
		assert(false);
		Block valueOfBody = nf.Block(pos);
		valueOfBody = valueOfBody
				.append(nf.Return(pos, nf.NullLit(pos)));

		// Create the method declaration
		FlagAnnotations voFlags = new FlagAnnotations();
		voFlags.classicFlags(nf.FlagsNode(pos, Flags.PUBLIC.Static()));
		
		TypeNode returnType = nf.CanonicalTypeNode(pos, this.type.asType());
		JL5MethodDecl valueOfMeth = ((JL5NodeFactory) nf).JL5MethodDecl(
				pos, voFlags, returnType, nf.Id(pos,"valueOf"),
				formals, Collections.EMPTY_LIST, valueOfBody, null);

		// Create the associated method definition
		Flags mthFlags = JL5Flags.PUBLIC.set(JL5Flags.STATIC);
		List formalTypes = Collections.singletonList(Types.ref(ts.String()));
		MethodDef md = ts.methodDef(pos, Types.ref(this.classDef().asType()),
				mthFlags, Types.ref(returnType.type()), Name.make("valueOf"), formalTypes, 
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, true);

		return (JL5MethodDecl) valueOfMeth.methodDef(md);
	}
    
    
    protected ConstructorDecl createDefaultConstructor(ClassDef thisType, TypeSystem ts, NodeFactory nf)
    throws SemanticException {

        //CHECK Not 100% sure about this, I assume that if there is a super type, current class is not an enum
        // Have a look at the super implementation if this function, the first check should be like the following
        // if the assertion doesn't hold 
        // if (superType != null && !JL5Flags.isEnumModifier(flags().flags())) {
    	Ref<? extends Type> superType = thisType.superType();
        assert ((superType != null) && !JL5Flags.isEnumModifier(flags().flags()));

        ConstructorDecl decl = super.createDefaultConstructor(thisType, ts, nf);

        if (JL5Flags.isEnumModifier(flags().flags())) {
        	FlagsNode newFlags = nf.FlagsNode(position(), Flags.PRIVATE);
            decl = decl.flags(newFlags);
        }
        return decl;
    }

    public void prettyPrintModifiers(CodeWriter w, PrettyPrinter tr) {
        for (AnnotationElem ae : annotations()) {
            print(ae, w, tr);
        }
        if (flags.flags().isInterface()) {
            if (JL5Flags.isAnnotationModifier(flags.flags())) {
                w.write(JL5Flags.clearAnnotationModifier(flags.flags()).clearInterface().clearAbstract().translate());
                w.write("@");
            } else {
                w.write(flags.flags().clearInterface().clearAbstract().translate());
            }
        } else {
            w.write(flags.flags().translate());
        }

        if (flags.flags().isInterface()) {
            w.write("interface ");
        } else if (JL5Flags.isEnumModifier(flags.flags())) {
        } else {
            w.write("class ");
        }
    }

    public void prettyPrintHeaderRest(CodeWriter w, PrettyPrinter tr) {
        if (superClass() != null && !JL5Flags.isEnumModifier(type.flags())) {
            w.write(" extends ");
            print(superClass(), w, tr);
        }

        if (!interfaces.isEmpty() && !JL5Flags.isAnnotationModifier(type.flags())) {
            if (flags.flags().isInterface()) {
                w.write(" extends ");
            } else {
                w.write(" implements ");
            }

            for (Iterator<TypeNode> i = interfaces().iterator(); i.hasNext();) {
                TypeNode tn = i.next();
                print(tn, w, tr);

                if (i.hasNext()) {
                    w.write(", ");
                }
            }
        }

        w.write(" {");
    }

    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
        prettyPrintModifiers(w, tr);
        tr.print(this, name, w);
        if (paramTypes != null && !paramTypes.isEmpty()) {
            w.write("<");
            for (Iterator<ParamTypeNode> it = paramTypes.iterator(); it.hasNext();) {
                ParamTypeNode next = it.next();
                print(next, w, tr);
                if (it.hasNext()) {
                    w.write(", ");
                }
            }
            w.write("> ");
        }
        prettyPrintHeaderRest(w, tr);

    }

    public List runtimeAnnotations() {
        return runtimeAnnotations;
    }

    public List classAnnotations() {
        return classAnnotations;
    }

    public List sourceAnnotations() {
        return sourceAnnotations;
    }
}
