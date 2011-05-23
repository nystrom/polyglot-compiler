package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.AmbTypeNode;
import polyglot.ast.AmbTypeNode_c;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Prefix;
import polyglot.ast.QualifierNode;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.types.LazyRef;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

public class JL5AmbTypeNode_c extends AmbTypeNode_c implements JL5AmbTypeNode {

    protected List typeArguments;

    public JL5AmbTypeNode_c(Position pos, Prefix qual, Id name, List typeArguments) {
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

    protected JL5AmbTypeNode_c reconstruct(Prefix qual, Id name, List args) {
    	JL5AmbTypeNode_c superCopy = (JL5AmbTypeNode_c) super.reconstruct(qual, name);
        if (!CollectionUtil.allEqual(args, superCopy.typeArguments)) {
            JL5AmbTypeNode_c n = (JL5AmbTypeNode_c) superCopy.copy();
            n.typeArguments = args;
            return n;
        }
        return superCopy;
    }

    public Node visitChildren(NodeVisitor v) {
    	JL5AmbTypeNode_c superVisit = (JL5AmbTypeNode_c) super.visitChildren(v);
        List args = superVisit.visitList(this.typeArguments, v);
        return superVisit.reconstruct(superVisit.prefix(), superVisit.name(), args);
    }

    public Node disambiguate(ContextVisitor ar) throws SemanticException {
        SemanticException ex;
        try {
            Node n = ar.nodeFactory().disamb().disambiguate(this, ar, position(), prefix, name);

            //CHECK Can't we create the right type representation at parsing time ?
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
                } else {
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
            
            if (n instanceof TypeNode) {
                TypeNode tn = (TypeNode) n;
                LazyRef<Type> sym = (LazyRef<Type>) type;
                sym.update(tn.typeRef().get());
                
                // Reset the resolver goal to one that can run when the ref is deserialized.
                Goal resolver = Globals.Scheduler().LookupGlobalType(sym);
                resolver.update(Goal.Status.SUCCESS);
                sym.setResolver(resolver);
                return n;
            }

            ex = new SemanticException("Could not find type \"" +
                                       (prefix == null ? name.id() : prefix.toString() + "." + name.id()) +
                                       "\".", position());
        }
        catch (SemanticException e) {
            ex = e;
        }

        // Mark the type as an error, so we don't try looking it up again.
        LazyRef<Type> sym = (LazyRef<Type>) type;
        sym.update(ar.typeSystem().unknownType(position()));

        throw ex;
    }
}
