package polyglot.ext.jl5.ast;

import polyglot.ast.CanonicalTypeNode_c;
import polyglot.ast.Node;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.ext.jl5.types.RawType;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class JL5CanonicalTypeNode_c extends CanonicalTypeNode_c implements JL5CanonicalTypeNode {

    public JL5CanonicalTypeNode_c(Position pos, Ref<? extends Type> type) {
        super(pos, type);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        Type t = type();
        if (t instanceof RawType) {
            ErrorQueue eq = tc.errorQueue();
            eq.enqueue(ErrorInfo.WARNING, "Use of a raw type could lead to unchecked "
                    + " operations.", position());
        } else if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type();
            if (pt.baseType() instanceof TypeVariable) {
                throw new SemanticException("Unexpected type: only class types can have type arguments.", position());
            }
            if (!pt.baseType().isGeneric()) {
                throw new SemanticException("Class " + pt.baseType() + " is not generic.", position());
            }
            if (!pt.typeArguments().isEmpty()
                    && pt.typeArguments().size() != pt.typeVariables().size()) {
                throw new SemanticException("Must give exactly " + pt.typeVariables().size() + " type argument(s).", position());
            }

            JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
            //first we must perform capture conversion. see beginning of JLS 4.5
            pt = (ParameterizedType)pt.capture();

            for (int i = 0; i < pt.typeVariables().size(); i++) {
                Type arg = pt.typeArguments().get(i);
                
                //some type arguments might be newly created type variables 
                //by capture conversion. check their bounds. 
                
                if (arg instanceof TypeVariable) {
                    TypeVariable tvarg = (TypeVariable) arg;
                    ts.checkIntersectionBounds(tvarg.bounds(), false);
                }
                //require that arguments obey their bounds
                Type b = ts.getSubstitution(pt, pt.typeVariables().get(i).upperBound());
                if (!ts.isSubtype(arg,b, tc.context())) {
                    throw new SemanticException("Type argument " + arg + 
                            " is not a subtype of its declared bound " + b, position());
                }
            }
            // updating the type
            ((Ref<Type>)this.type).update(pt);
        }
        return super.typeCheck(tc);
    }

}
