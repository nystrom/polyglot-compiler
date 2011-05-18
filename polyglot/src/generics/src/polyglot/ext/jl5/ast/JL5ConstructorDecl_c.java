package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5ConstructorDef;
import polyglot.ext.jl5.types.JL5ConstructorInstance;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.ConstructorDef;
import polyglot.types.Context;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;

public class JL5ConstructorDecl_c extends ConstructorDecl_c implements JL5ConstructorDecl, ApplicationCheck {

    protected boolean compilerGenerated;
    protected List<AnnotationElem> annotations;
    protected List runtimeAnnotations;
    protected List classAnnotations;
    protected List sourceAnnotations;
    protected List<ParamTypeNode> paramTypes;
    
    public JL5ConstructorDecl_c(Position pos, FlagAnnotations flags, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        this(pos, flags, name, formals, throwTypes, body, new ArrayList<ParamTypeNode>());
    }

    public JL5ConstructorDecl_c(Position pos, FlagAnnotations flags, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body, List<ParamTypeNode> paramTypes){
        super(pos, flags.classicFlags(), name, formals, throwTypes, body);
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        }
        else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, false);
        }
        this.paramTypes = paramTypes;
    }

    public List<ParamTypeNode> paramTypes(){
        return this.paramTypes;
    }

    public JL5ConstructorDecl paramTypes(List<ParamTypeNode> paramTypes){
        JL5ConstructorDecl_c n = (JL5ConstructorDecl_c) copy();
        n.paramTypes = paramTypes;
        return n;
    }

    protected JL5ConstructorDecl_c reconstruct(List<AnnotationElem> annotations, List<ParamTypeNode> paramTypes) {
    	if (!CollectionUtil.allEqual(annotations, this.annotations) || 
    			!CollectionUtil.allEqual(paramTypes, this.paramTypes)) {
    		JL5ConstructorDecl_c n = (JL5ConstructorDecl_c) this.copy();
    		n.annotations = TypedList.copyAndCheck(this.annotations, AnnotationElem.class, true);
    		n.paramTypes = TypedList.copyAndCheck(this.paramTypes, ParamTypeNode.class, true);
    		return n;
    	}
    	return this;
    }

    public Node visitSignature(NodeVisitor v) {
    	JL5ConstructorDecl_c n = (JL5ConstructorDecl_c) super.visitSignature(v);
    	List<AnnotationElem> annotations = n.visitList(this.annotations, v);
    	List<ParamTypeNode> paramTypes = n.visitList(this.paramTypes, v);
    	return n.reconstruct(annotations, paramTypes);
    }

    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
    	ConstructorDecl n = (ConstructorDecl) super.buildTypesOverride(tb);
    	ConstructorDef def = n.constructorDef();
    	// def has been added as a constructor to the ClassDef
    	
    	// Create a list of type refs for type variables
        List<Ref<? extends Type>> pTypes = new ArrayList<Ref<? extends Type>>(paramTypes().size());
        for (ParamTypeNode p : paramTypes()) {
        	pTypes.add(p.typeRef());
        }
                    
        ((JL5ConstructorDef)def).setTypeVariableTypes(pTypes);
        return n;
    }

    public void prettyPrintHeader(CodeWriter w, PrettyPrinter tr) {
    	w.begin(0);

    	tr.print(this, flags, w);

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
    	tr.print(this, name, w);
    	w.write("(");

    	w.begin(0);

    	for (Iterator i = formals.iterator(); i.hasNext(); ) {
    		Formal f = (Formal) i.next();
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

    public List<AnnotationElem> annotations(){
        return this.annotations;
    }

    public JL5ConstructorDecl annotations(List<AnnotationElem> annotations){
        JL5ConstructorDecl_c n = (JL5ConstructorDecl_c) copy();
        n.annotations = annotations;
        return n;
    }
    
    @Override
    public Context enterScope(Context c) {
        c = super.enterScope(c);
        for (ParamTypeNode pn : paramTypes) {
            c = ((JL5Context)c).addTypeVariable((TypeVariable)pn.type());
        }
        return c;
    }
    
   
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        
        // check throws clauses are not parameterized
        for (TypeNode tn : throwTypes()){
            Type next = tn.type();
            if (next instanceof ParameterizedType){
                throw new SemanticException("Cannot use parameterized type "+next+" in a throws clause", tn.position());
            }
        }
    
        // check at most last formal is variable
        for (int i = 0; i < formals.size(); i++){
            JL5Formal f = (JL5Formal)formals.get(i);
            if (i != formals.size()-1 && f.isVarargs()){
                throw new SemanticException("Only last formal can be variable in constructor declaration.", f.position());
            }
        }

        if (ci instanceof JL5ConstructorInstance) {
            JL5ConstructorInstance c = (JL5ConstructorInstance) ci;
            if (c.isGeneric()) {
                ((JL5TypeSystem)tc.typeSystem()).checkTVForwardReference(c.typeVariables());
            }
        };

        return super.typeCheck(tc);
    }

    // CHECK: don't really understand why we need to decide here if we need to insert a default constructor 
//    public Node addMembers(AddMemberVisitor tc) throws SemanticException {
//        TypeSystem ts = tc.typeSystem();
//        NodeFactory nf = tc.nodeFactory();
//        return addCCallIfNeeded(ts, nf);
//    }
//
//    protected Node addCCallIfNeeded(TypeSystem ts, NodeFactory nf){
//        if (cCallNeeded()){
//            return addCCall(ts, nf);        
//        }
//        return this;
//    }
//
//    protected boolean cCallNeeded(){
//    	// if the first statement of constructor's body is a constructor call
//    	// || This constructor has an enum flag 
//        if (!body.statements().isEmpty() && body.statements().get(0) instanceof ConstructorCall 
//        		|| JL5Flags.isEnumModifier(((ClassType)constructorInstance().container()).flags())) return false;
//        return true;
//    }
//
//    protected Node addCCall(TypeSystem ts, NodeFactory nf){
//        ConstructorInstance sci = ts.defaultConstructor(position(), (ClassType) this.constructorInstance().container().superType());
//        ConstructorCall cc = nf.SuperCall(position(), Collections.EMPTY_LIST);
//        cc = cc.constructorInstance(sci); 
//        body = body.prepend(cc);
//        return reconstruct(formals, throwTypes, body);
//        
//    }

    public Node applicationCheck(ApplicationChecker appCheck, Context ctx) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)appCheck.typeSystem();
        for(Iterator it = annotations.iterator(); it.hasNext(); ){
            AnnotationElem next = (AnnotationElem)it.next();
            ts.checkAnnotationApplicability(next, this);
        }
        return this;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
   
        if (isCompilerGenerated()) return;

        for (AnnotationElem a : annotations()){
            print(a, w, tr);
        }
        
        super.prettyPrint(w, tr);
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
    
    public boolean isCompilerGenerated(){
        return compilerGenerated;
    }

    public JL5ConstructorDecl setCompilerGenerated(boolean val){
        JL5ConstructorDecl_c n = (JL5ConstructorDecl_c)copy();
        n.compilerGenerated = val;
        return n;
    }
}
