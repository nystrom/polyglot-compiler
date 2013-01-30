package polyglot.ext.jl5.ast;

import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Formal;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.visit.ApplicationCheck;
import polyglot.ext.jl5.visit.ApplicationChecker;
import polyglot.types.ArrayType;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class JL5Formal_c extends Formal_c implements JL5Formal, ApplicationCheck  {

    protected List<AnnotationElem> annotations;
    protected List runtimeAnnotations;
    protected List classAnnotations;
    protected List sourceAnnotations;

    public JL5Formal_c(Position pos, FlagAnnotations flags, TypeNode type, Id name){
        super(pos, flags.classicFlags(), type, name);
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        } else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, true);
        }
    }
    
    public List<AnnotationElem> annotations(){
        return annotations;
    }
    
    public JL5Formal annotations(List annotations){
        JL5Formal_c n = (JL5Formal_c) copy();
        n.annotations = annotations;
        return n;
    }

    public boolean isVarargs(){
    	// Commodity method to get the vararg nature of this parameter from its typenode
        return (this.type instanceof JL5ArrayTypeNode) && ((JL5ArrayTypeNode) type).isVarargs();
    }
    
    protected Formal reconstruct(List<AnnotationElem> annotations){
        if (!CollectionUtil.allEqual(annotations, this.annotations)){
            JL5Formal_c n = (JL5Formal_c)copy();
            n.annotations = annotations;
            return n;
        }
        return this;
    }

    public Node visitChildren(NodeVisitor v){
    	List<AnnotationElem> annots = visitList(this.annotations, v);
        JL5Formal_c n = (JL5Formal_c) super.visitChildren(v);
        return n.reconstruct(annots);
    }
    
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
        ts.checkDuplicateAnnotations(annotations);
        return super.typeCheck(tc);
        
    }

    public Node applicationCheck(ApplicationChecker appCheck, Context ctx) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem)appCheck.typeSystem();
        for (AnnotationElem a : annotations) {
            ts.checkAnnotationApplicability(a, this);
        }
        return this;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
        if (annotations != null){
            for (AnnotationElem a : annotations){
                print(a, w, tr);
            }
        }
        print(flags, w, tr);
        if (isVarargs()){
            w.write(((ArrayType)type.type()).base().toString());
            w.write(" ...");
        } else {
            print(type, w, tr);
        }
        w.write(" ");
        tr.print(this, name, w);
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
