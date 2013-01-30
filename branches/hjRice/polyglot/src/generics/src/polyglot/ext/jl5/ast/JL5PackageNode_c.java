package polyglot.ext.jl5.ast;

import java.util.LinkedList;
import java.util.List;

import polyglot.ast.FlagsNode;
import polyglot.ast.Node;
import polyglot.ast.PackageNode_c;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.types.Flags;
import polyglot.types.Package;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.NodeVisitor;
import polyglot.visit.ContextVisitor;


/**
 * An ast node representing a package with support for annotations
 */
public class JL5PackageNode_c extends PackageNode_c implements JL5PackageNode {

    protected FlagsNode classicFlags;
    protected List annotations;

    public JL5PackageNode_c(Position pos, FlagAnnotations flags, Ref<? extends Package> package_){
        super(pos, package_);
        this.classicFlags = flags.classicFlags();
        if (flags.annotations() != null){
            this.annotations = flags.annotations();
        }
        else {
            this.annotations = new TypedList(new LinkedList(), AnnotationElem.class, false);
        }    
    }
    
    public List annotations(){
        return this.annotations;
    }
    
    public JL5PackageNode annotations(List annotations){
        JL5PackageNode_c n = (JL5PackageNode_c) copy();
        n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, false);
        return n;
    }

    protected JL5PackageNode_c reconstruct(List annotations){
        if (!CollectionUtil.allEqual(annotations, this.annotations)){
            JL5PackageNode_c n = (JL5PackageNode_c) copy();
            n.annotations = TypedList.copyAndCheck(annotations, AnnotationElem.class, false);
            return n;
        }
        return this;
    }
    
    public Node visitChildren(NodeVisitor v){
        List annotations = visitList(this.annotations, v);
        return reconstruct(annotations);
    }
    
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        if (!classicFlags.flags().equals(Flags.NONE)){
            throw new SemanticException ("Modifier "+classicFlags+" not allowed here.", position());
        }
        return super.typeCheck(tc);
    }
}

