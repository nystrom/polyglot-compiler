package polyglot.ext.jl5.ast;

import polyglot.ast.ClassLit_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

/**
 * JL5 extension to support parameterized class literals.
 * 
 * Ex:
 * 	Class<JL5ParsedClassType> c = JL5ParsedClassType.class;
 */
public class JL5ClassLit_c extends ClassLit_c implements JL5ClassLit {

    public JL5ClassLit_c(Position pos, TypeNode typenode) {
        super(pos, typenode);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
    	// We return a parameterized type to represent Class<T>
        return type(ts.Class(this.typeNode().type()));
    }
}
