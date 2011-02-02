package polyglot.ext.jl5.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Expr;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.ext.jl5.types.TypeVariable.TVarDecl;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

public class JL5LocalDecl_c extends LocalDecl_c implements JL5LocalDecl, ApplicationCheck {

    protected List annotations;
    
    public JL5LocalDecl_c(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init){
        super(pos, flags.classicFlags(), type, name, init);
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        }
        else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, true);
        }
    }
    
    public List annotations(){
        return annotations;
    }
    
    public JL5LocalDecl annotations(List annotations){
        JL5LocalDecl_c n = (JL5LocalDecl_c) copy();
        n.annotations = annotations;
        return n;
    }

    protected LocalDecl reconstruct(TypeNode type, Expr init, List annotations){
        if (this.type() != type || this.init() != init ||  !CollectionUtil.equals(annotations, this.annotations)){
            JL5LocalDecl_c n = (JL5LocalDecl_c)copy();
            n.type = type;
            n.init = init;
            n.annotations = annotations;
            return n;
        }
        return this;
    }

    public Node visitChildren(NodeVisitor v){
        TypeNode type = (TypeNode)visitChild(this.type(), v);
        Expr init = (Expr)visitChild(this.init(), v);
        List annots = visitList(this.annotations, v);
        return reconstruct(type, init, annots);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        if (!flags().clear(Flags.FINAL).equals(Flags.NONE)){
            throw new SemanticException("Modifier: "+flags().clearFinal()+" not allowed here.", position());
        }
        if (type().type() instanceof TypeVariable && tc.context().inStaticContext()){
            if (((TypeVariable)type().type()).declaredIn().equals(TVarDecl.CLASSTV))
                throw new SemanticException("Cannot access non-static type: "+((TypeVariable)type().type()).name()+" in a static context.", position());
        }
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        ts.checkDuplicateAnnotations(annotations);
        return super.typeCheck(tc);
    }
   
    public Node applicationCheck(ApplicationChecker appCheck) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)appCheck.typeSystem();
        for( Iterator it = annotations.iterator(); it.hasNext(); ){
            AnnotationElem next = (AnnotationElem)it.next();
            ts.checkAnnotationApplicability(next, this);
        }
        return this;
    }
        
    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
        if (annotations != null){
            for (Iterator it = annotations.iterator(); it.hasNext(); ){
                print((AnnotationElem)it.next(), w, tr);
            }
        }
        super.prettyPrint(w, tr);
    }
}
