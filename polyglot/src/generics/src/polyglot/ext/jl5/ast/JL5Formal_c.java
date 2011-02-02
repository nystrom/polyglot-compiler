package polyglot.ext.jl5.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5ArrayType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.ArrayType;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

public class JL5Formal_c extends Formal_c implements JL5Formal, ApplicationCheck  {

    protected List annotations;
    protected List runtimeAnnotations;
    protected List classAnnotations;
    protected List sourceAnnotations;
    protected boolean variable = false;
    
    public JL5Formal_c(Position pos, FlagAnnotations flags, TypeNode type, Id name){
        super(pos, flags.classicFlags(), type, name);
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        
        }
        else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, true);
        }
    }
    
    public JL5Formal_c(Position pos, FlagAnnotations flags, TypeNode type, Id name, boolean variable){
        super(pos, flags.classicFlags(), type, name);
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        
        }
        else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, true);
        }
        this.variable = variable;
    }
    
    public List annotations(){
        return annotations;
    }
    
    public JL5Formal annotations(List annotations){
        JL5Formal_c n = (JL5Formal_c) copy();
        n.annotations = annotations;
        return n;
    }

    public boolean isVariable(){
        return variable;
    }
    
    protected Formal reconstruct(TypeNode type, List annotations){
        if (this.type() != type || !CollectionUtil.equals(annotations, this.annotations)){
            JL5Formal_c n = (JL5Formal_c)copy();
            n.type = type;
            n.annotations = annotations;
            return n;
        }
        return this;
    }

    public Node visitChildren(NodeVisitor v){
        TypeNode type = (TypeNode)visitChild(this.type(), v);
        List annots = visitList(this.annotations, v);
        return reconstruct(type, annots);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (isVariable()){
            ((JL5ArrayType)type().type()).setVariable();
        }
        return super.disambiguate(ar);
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        if (!flags().clear(Flags.FINAL).equals(Flags.NONE)){
            throw new SemanticException("Modifier: "+flags().clearFinal()+" not allowed here.", position());
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
        w.write(flags.translate());
        if (isVariable()){
            w.write(((ArrayType)type.type()).base().toString());
            //print(type, w, tr);
            w.write(" ...");
        }
        else {
            print(type, w, tr);
        }
        w.write(" " + name);
        
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
}
