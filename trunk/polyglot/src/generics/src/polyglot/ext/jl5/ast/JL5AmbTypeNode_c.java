package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.AmbTypeNode_c;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.QualifierNode;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

public class JL5AmbTypeNode_c extends AmbTypeNode_c implements JL5AmbTypeNode {

    protected List typeArguments;

    public JL5AmbTypeNode_c(Position pos, QualifierNode qual, Id name, List typeArguments) {
        super(pos, qual, name);
        this.typeArguments = typeArguments;
    }

    public List typeArguments() {
        return typeArguments;
    }

    public JL5AmbTypeNode typeArguments(List args) {
        JL5AmbTypeNode_c n = (JL5AmbTypeNode_c) copy();
        n.typeArguments = args;
        return n;
    }

    protected JL5AmbTypeNode_c reconstruct(QualifierNode qual, List args) {
        if (qual != this.qual() || !CollectionUtil.allEqual(args, this.typeArguments)) {
            JL5AmbTypeNode_c n = (JL5AmbTypeNode_c) copy();
            n.qual = qual;
            n.typeArguments = args;
            return n;
        }
        return this;
    }

    public Node visitChildren(NodeVisitor v) {
        QualifierNode qual = (QualifierNode) visitChild(this.qual(), v);
        List args = visitList(this.typeArguments, v);
        return reconstruct(qual, args);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        Node n = ar.nodeFactory().disamb().disambiguate(this, ar, position(), qual(), name());

        if (n instanceof CanonicalTypeNode
                && ((CanonicalTypeNode) n).type() instanceof JL5ParsedClassType) {
            CanonicalTypeNode tn = (CanonicalTypeNode) n;
            JL5ParsedClassType ct = (JL5ParsedClassType) tn.type();
            Type t;
            JL5TypeSystem ts = (JL5TypeSystem) ar.typeSystem();
            if (typeArguments.isEmpty()) {
                if (ct.isGeneric())
                    // it's a raw type
                    t = ts.rawType((JL5ParsedClassType) (ct));
                else
                    // it's a nongeneric type, so leave it alone
                    return n;
            }
            else {
                t = ts.parameterizedType((JL5ParsedClassType) (ct));
                ParameterizedType pt = (ParameterizedType) t;
                ArrayList<Type> typeArgs = new ArrayList<Type>();
                for (int i = 0; i < typeArguments.size(); i++) {
                    Type targ = ((TypeNode) typeArguments.get(i)).type();
                    typeArgs.add(targ);
                }
                pt.typeArguments(typeArgs);
            }
            CanonicalTypeNode an = ((JL5NodeFactory) ar.nodeFactory()).CanonicalTypeNode(n.position(), t);
            return an;
        }
        //         else if (n instanceof CanonicalTypeNode){
        //             throw new SemanticException("Unexpected type: "+n+". Only class types can have type arguments.", n.position());
        //         }
        else if (n instanceof TypeNode) {
            return n;
        }

        throw new SemanticException("Could not find type \""
                + (qual() == null ? name() : qual().toString() + "." + name()) + "\".", position());

    }
}
