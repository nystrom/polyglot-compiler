package polyglot.ext.jl5.ast;

import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.TypeNode_c;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ReferenceType;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class BoundedTypeNode_c extends TypeNode_c implements BoundedTypeNode {


    protected BoundedTypeNode.Kind kind;
    protected TypeNode bound;

    public BoundedTypeNode_c(Position pos, BoundedTypeNode.Kind kind, TypeNode bound){
        super(pos);
        this.kind = kind;
        this.bound = bound;
    }
    
    public Kind kind(){
        return kind;        
    }

    public BoundedTypeNode kind(Kind kind){
        BoundedTypeNode_c n = (BoundedTypeNode_c) copy();
        n.kind = kind;
        return n;
    }
    
    public TypeNode bound(){
        return this.bound;
    }
    
    public BoundedTypeNode bound(TypeNode bound){
        BoundedTypeNode_c n = (BoundedTypeNode_c) copy();
        n.bound = bound;
        return n;
    }
   
    public BoundedTypeNode reconstruct(TypeNode bound){
        if (bound != this.bound){
            BoundedTypeNode_c n = (BoundedTypeNode_c) copy();
            n.bound = bound;
            return n;
        }
        return this;
    }
    
    public Node visitChildren(NodeVisitor v){
        if (bound != null){
            TypeNode bound = (TypeNode)visitChild(this.bound, v);
            return reconstruct(bound);
        }
        return this;
    }
   
    public Node disambiguate(AmbiguityRemover v){
        JL5TypeSystem ts = (JL5TypeSystem)v.typeSystem();
        if (bound == null ) {
            return this.type(ts.anyType());
        }
        else if (kind == BoundedTypeNode.SUPER) {
            return this.type(ts.anySuperType((ReferenceType)bound.type()));
        }
        else if (kind == BoundedTypeNode.EXTENDS){
            return this.type(ts.anySubType((ReferenceType)bound.type()));
        }
        return this;    
    }
    
    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
        w.write("?");
        if (bound != null){
            w.write(kind.toString());
            w.write(" ");
            print(bound, w, tr);
        }
    }
}
