package polyglot.ext.jl5.ast;

import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class JL5FieldDecl_c extends FieldDecl_c implements JL5FieldDecl, ApplicationCheck {

    protected boolean compilerGenerated;
    protected List<AnnotationElem> annotations;
    protected List runtimeAnnotations;
    protected List classAnnotations;
    protected List sourceAnnotations;
       
    public JL5FieldDecl_c(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init){
        super(pos, flags.classicFlags(), type, name, init);
        if (flags.annotations() != null){
            annotations = flags.annotations();
        }
        else {
            annotations = new TypedList(new LinkedList(), AnnotationElem.class, true);
        }
    }

    public List<AnnotationElem> annotations(){
        return this.annotations;
    }

    public JL5FieldDecl annotations(List<AnnotationElem> annotations){
        JL5FieldDecl_c n = (JL5FieldDecl_c) copy();
        n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, true);
        return n;
    }
    
    protected JL5FieldDecl reconstruct(TypeNode type, Expr init, List<AnnotationElem> annotations){
        if( this.type() != type || this.init() != init || !CollectionUtil.allEqual(this.annotations, annotations)){
            JL5FieldDecl_c n = (JL5FieldDecl_c) copy();
            n.type = type;
            n.init = init;
            n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, true);
            return n;
        }
        return this;
    }
    
    public Node visitChildren(NodeVisitor v){
    	JL5FieldDecl_c n = (JL5FieldDecl_c) super.visitChildren(v);
        List<AnnotationElem> annotations = n.visitList(this.annotations, v);
        return n.reconstruct(n.type(), n.init(), annotations);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        JL5ParsedClassType currentClass = (JL5ParsedClassType) tc.context().currentClass();
        Type type = type().type();
        // captures the scenario of a generic static inner class<T> declaring a static field of type T
        if ((type instanceof TypeVariable) && 
        		(currentClass.flags().isStatic() || flags().flags().isStatic())){
            if (currentClass.flags().isStatic() && 
            		currentClass.hasTypeVariable(((TypeVariable)type).name())){
            }
            else {
                throw new SemanticException("Cannot access non-static type "+((TypeVariable)type().type()).name()+" in a static context.", position());
            }
            
        }
        return super.typeCheck(tc);
    }
    
    @Override
    public Node conformanceCheck(ContextVisitor tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        ts.checkDuplicateAnnotations(annotations);
    	return super.conformanceCheck(tc);
    }
    
    public Node applicationCheck(ApplicationChecker appCheck, Context ctx) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)appCheck.typeSystem();
        for(AnnotationElem ae : annotations){
            ts.checkAnnotationApplicability(ae, this);
        }
        return this;
   }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
        if (isCompilerGenerated()) return;

        for(AnnotationElem ae : annotations){
            print(ae, w, tr);
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
    
    public JL5FieldDecl setCompilerGenerated(boolean val){
        JL5FieldDecl_c n = (JL5FieldDecl_c) copy();
        n.compilerGenerated = val;
        return n;
    }
}
