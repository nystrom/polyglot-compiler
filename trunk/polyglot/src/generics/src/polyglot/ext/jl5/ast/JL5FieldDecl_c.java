package polyglot.ext.jl5.ast;

import java.util.Iterator;
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
import polyglot.ext.jl5.visit.JL5AmbiguityRemover;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.ContextVisitor;

public class JL5FieldDecl_c extends FieldDecl_c implements JL5FieldDecl, ApplicationCheck {

    protected boolean compilerGenerated;
    protected List annotations;
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

    public List annotations(){
        return this.annotations;
    }

    public JL5FieldDecl annotations(List annotations){
        JL5FieldDecl_c n = (JL5FieldDecl_c) copy();
        n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, true);
        return n;
    }
    
    protected JL5FieldDecl reconstruct(TypeNode type, Expr init, List annotations){
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
        TypeNode type = (TypeNode) visitChild(this.type(), v);
        Expr init = (Expr) visitChild(this.init(), v);
        List annotations = visitList(this.annotations, v);
        return reconstruct(type, init, annotations);
    }
   
    public NodeVisitor disambiguateEnter(AmbiguityRemover ar) throws SemanticException {
        if (ar.kind() == JL5AmbiguityRemover.TYPE_VARS){
            return ar.bypass(type).bypass(init);
        }
        return super.disambiguateEnter(ar);
    }
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        if (type().type() instanceof TypeVariable && (tc.context().currentClass().flags().isStatic() || flags().flags().isStatic())){
            if (tc.context().currentClass().flags().isStatic() && tc.context().currentClass() instanceof JL5ParsedClassType && ((JL5ParsedClassType)tc.context().currentClass()).hasTypeVariable(((TypeVariable)type().type()).name())){
            }
            else {
                throw new SemanticException("Cannot access non-static type "+((TypeVariable)type().type()).name()+" in a static context.", position());
            }
            
        }
        ts.checkDuplicateAnnotations(annotations);
        return super.typeCheck(tc);
    }

    public Node applicationCheck(ApplicationChecker appCheck, Context ctx) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)appCheck.typeSystem();
        for( Iterator it = annotations.iterator(); it.hasNext(); ){
            AnnotationElem next = (AnnotationElem)it.next();
            ts.checkAnnotationApplicability(next, this);
        }
        return this;
   }
    

    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
        if (isCompilerGenerated()) return;

        for (Iterator it = annotations.iterator(); it.hasNext(); ){
            print((AnnotationElem)it.next(), w, tr);
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
