package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5MethodDef;
import polyglot.ext.jl5.types.JL5MethodInstance;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.Flags;
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
import polyglot.visit.Translator;
import polyglot.visit.TypeBuilder;

public class JL5MethodDecl_c extends MethodDecl_c implements JL5MethodDecl, ApplicationCheck {

    protected boolean compilerGenerated;
    protected List<AnnotationElem> annotations;
    protected List runtimeAnnotations;
    protected List classAnnotations;
    protected List sourceAnnotations;
    protected List<ParamTypeNode> paramTypes;
    
    public JL5MethodDecl_c(Position pos, FlagAnnotations flags, TypeNode returnType, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body){
        this(pos, flags, returnType, name, formals, throwTypes, body, new ArrayList<ParamTypeNode>());
    }
    
    public JL5MethodDecl_c(Position pos, FlagAnnotations flags, TypeNode returnType, Id name, 
    		List<Formal> formals, List<TypeNode> throwTypes, Block body, List<ParamTypeNode> paramTypes){
    	this(pos, flags, returnType, name, formals, throwTypes, body, paramTypes, false);
    }

    public JL5MethodDecl_c(Position pos, FlagAnnotations flags, TypeNode returnType, Id name, 
    		List<Formal> formals, List<TypeNode> throwTypes, Block body, List<ParamTypeNode> paramTypes, boolean compilerGenerated){
        super(pos, flags.classicFlags(), returnType, name, formals, throwTypes, body);
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        } else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, true);
        }
        this.paramTypes = paramTypes;
        this.compilerGenerated = compilerGenerated;
    }
    public boolean isGeneric(){
    	return !paramTypes.isEmpty(); 
    }

    public List<AnnotationElem> annotations(){
        return this.annotations;
    }

    public JL5MethodDecl annotations(List<AnnotationElem> annotations){
        JL5MethodDecl_c n = (JL5MethodDecl_c) copy();
        n.annotations = annotations;
        return n;    
    }
    
    public List<ParamTypeNode> paramTypes(){
        return this.paramTypes;
    }

    public JL5MethodDecl paramTypes(List<ParamTypeNode> paramTypes){
        JL5MethodDecl_c n = (JL5MethodDecl_c) copy();
        n.paramTypes = paramTypes;
        return n;
    }
           
    protected MethodDecl_c reconstruct(TypeNode returnType, List formals, List throwTypes, Block body, List annotations, List paramTypes){
        if (returnType != this.returnType || ! CollectionUtil.allEqual(formals, this.formals) || ! CollectionUtil.allEqual(throwTypes, this.throwTypes) || body != this.body || !CollectionUtil.allEqual(annotations, this.annotations) || !CollectionUtil.allEqual(paramTypes, this.paramTypes)) {
            JL5MethodDecl_c n = (JL5MethodDecl_c) copy();
            n.returnType = returnType;
            n.formals = TypedList.copyAndCheck(formals, Formal.class, true);
            n.throwTypes = TypedList.copyAndCheck(throwTypes, TypeNode.class, true);
            n.body = body;
            n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, true);
            n.paramTypes = paramTypes;
            return n;
        }
        return this;
                                                            
    }

    @Override
    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
    	JL5MethodDecl n = (JL5MethodDecl) super.buildTypesOverride(tb);
    	JL5MethodDef def = (JL5MethodDef) n.methodDef();
    	
    	// Create a list of type refs for type variables
        List<Ref<? extends Type>> pTypes = new ArrayList<Ref<? extends Type>>(paramTypes().size());
        for (ParamTypeNode p : n.paramTypes()) {
        	pTypes.add(p.typeRef());
        }
        ((JL5MethodDef)def).setTypeVariableTypes(pTypes);
        return n;
    }

    @Override
    public Node visitSignature(NodeVisitor v) {
    	JL5MethodDecl_c n = (JL5MethodDecl_c) super.visitSignature(v);
    	List<AnnotationElem> annotations = n.visitList(n.annotations, v);
    	List<ParamTypeNode> paramTypes = n.visitList(n.paramTypes, v);
    	return n.reconstruct(annotations, paramTypes);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        // JL5: Check no duplicate annotations used
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        ts.checkDuplicateAnnotations(annotations);
   
        // JL5: Check throws clauses are not parameterized
        for (TypeNode tn : throwTypes){
            Type next = tn.type();
            if (next instanceof ParameterizedType){
                throw new SemanticException("Cannot use parameterized type "+next+" in a throws clause", tn.position());
            }
        }
        
        // JL5: Check at most last formal is variable
        for (int i = 0; i < formals.size(); i++){
            JL5Formal f = (JL5Formal)formals.get(i);
            if (i != formals.size()-1 && f.isVarargs()){
                throw new SemanticException("Only last formal can be variable in method declaration.", f.position());
            }
        }

        return super.typeCheck(tc);
    }


    /**
     * Mostly a copy paste from the super class with variations for JL5
     */
    @Override
    protected void checkFlags(ContextVisitor tc, Flags flags) throws SemanticException {
    	TypeSystem ts = tc.typeSystem();

    	if (tc.context().currentClass().flags().isInterface()) {
    		if (flags.isProtected() || flags.isPrivate()) {
    			throw new SemanticException("Interface methods must be public.", position());
    		}

    		if (flags.isStatic()) {
    			throw new SemanticException("Interface methods cannot be static.", position());
    		}
    	}

    	try {
    		ts.checkMethodFlags(flags);
    	}
    	catch (SemanticException e) {
    		throw new SemanticException(e.getMessage(), position());
    	}

    	Type container = Types.get(methodDef().container());
    	ClassType ct = container.toClass();

    	if (body == null && ! (flags.isAbstract() || flags.isNative())) {
    		throw new SemanticException("Missing method body.", position());
    	}

    	if (body != null && ct.flags().isInterface()) {
    		throw new SemanticException(
    				"Interface methods cannot have a body.", position());
    	}

    	if (body != null && flags.isAbstract()) {
    		throw new SemanticException(
    				"An abstract method cannot have a body.", position());
    	}

    	if (body != null && flags.isNative()) {
    		throw new SemanticException(
    				"A native method cannot have a body.", position());
    	}

    	// check that inner classes do not declare static methods
    	// JL5: unless class is enum
    	//CHECK refactor this to a utility method in super to avoid copy-paste
    	if (ct != null && flags.isStatic() && ct.isInnerClass() &&
    			!JL5Flags.isEnumModifier(flags().flags())) {
    		// it's a static method in an inner class.
    		throw new SemanticException("Inner classes cannot declare " + 
    				"static methods.", this.position());             
    	}

    	// JL5: Checking forward reference on TypeVariable
    	if (mi instanceof JL5MethodInstance) {
    		JL5MethodInstance m = (JL5MethodInstance) mi;
    		if (m.isGeneric()) {
    			((JL5TypeSystem)tc.typeSystem()).checkTVForwardReference(m.typeVariables());
    		}
    	}
    }

    public Node applicationCheck(ApplicationChecker appCheck, Context ctx) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)appCheck.typeSystem();
        for(AnnotationElem a : annotations()){
            ts.checkAnnotationApplicability(a, this);
        }
        return this;         
    }

    protected JL5MethodDecl_c reconstruct(List<AnnotationElem> annotations, List<ParamTypeNode> paramTypes) {
    	if (!CollectionUtil.allEqual(annotations, this.annotations) || 
    			!CollectionUtil.allEqual(paramTypes, this.paramTypes)) {
    		JL5MethodDecl_c n = (JL5MethodDecl_c) this.copy();
    		n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, true);
    		n.paramTypes = TypedList.copyAndCheck(paramTypes, ParamTypeNode.class, true);
    		return n;
    	}
    	return this;
    }
    
    public void translate(CodeWriter w, Translator tr){
        if (isCompilerGenerated()) return;
        
        for (AnnotationElem a : annotations){
            print(a, w, tr);
        }

        super.translate(w, tr);
    }

    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
        w.begin(0);
    	print(flags, w, tr);

        if ((paramTypes != null) && !paramTypes.isEmpty()){
            w.write("<");
            for (Iterator<ParamTypeNode> it = paramTypes.iterator(); it.hasNext(); ){
                ParamTypeNode next = it.next();
                print(next, w, tr);
                if (it.hasNext()){
                    w.write(", ");
                }
            }
            w.write("> ");
        }
    	print(returnType, w, tr);
    	w.allowBreak(2, 2, " ", 1);
    	w.write(name + "(");

    	w.allowBreak(2, 2, "", 0);
    	w.begin(0);

    	for (Iterator<Formal> i = formals.iterator(); i.hasNext(); ) {
    	    Formal f = i.next();
    	    
    	    print(f, w, tr);

    	    if (i.hasNext()) {
    		w.write(",");
    		w.allowBreak(0, " ");
    	    }
    	}

    	w.end();
    	w.write(")");

    	if (! throwTypes().isEmpty()) {
    	    w.allowBreak(6);
    	    w.write("throws ");

    	    for (Iterator i = throwTypes().iterator(); i.hasNext(); ) {
    	        TypeNode tn = (TypeNode) i.next();
    		print(tn, w, tr);

    		if (i.hasNext()) {
    		    w.write(",");
    		    w.allowBreak(4, " ");
    		}
    	    }
    	}

    	w.end();
    }


    public List runtimeAnnotations(){
        return runtimeAnnotations;
    }
    public List classAnnotations(){
        return classAnnotations;
    }
    public List sourceAnnotations(){
        return sourceAnnotations;
    }

    @Override
    public Context enterScope(Context c) {
        c = super.enterScope(c);
        for (ParamTypeNode pn : paramTypes) {
            c = ((JL5Context)c).addTypeVariable(Name.make(pn.nameString()), pn.typeRef());
        }
        return c;
    }

    @Override
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        return super.buildTypes(tb);
    }

	@Override
	public boolean isCompilerGenerated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JL5MethodDecl setCompilerGenerated(boolean val) {
		JL5MethodDecl_c n = (JL5MethodDecl_c)copy();
        n.compilerGenerated = val;
        return n;
	}
    
    
    
    
}
