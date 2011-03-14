package polyglot.ext.jl5.ast;

import polyglot.ast.ArrayTypeNode_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.TypeBuilder;

/**
 * An array type node can be used to represent a variable argument
 * parameter to a method.
 */
public class JL5ArrayTypeNode_c extends ArrayTypeNode_c {

	protected final boolean varargs;

	public JL5ArrayTypeNode_c(Position pos, TypeNode base) {
		this(pos, base, false);
	}

	public JL5ArrayTypeNode_c(Position pos, TypeNode base, boolean varargs) {
		super(pos, base);
		this.varargs = varargs;
	}

	@Override
	public Node buildTypes(TypeBuilder tb) throws SemanticException {
		JL5TypeSystem ts = (JL5TypeSystem) tb.typeSystem();
		return typeRef(Types.<Type> ref(ts.arrayOf(position(), base.typeRef(),
				varargs)));
	}

	@Override
	public Node disambiguate(ContextVisitor ar) throws SemanticException {
		JL5TypeSystem ts = (JL5TypeSystem) ar.typeSystem();
		NodeFactory nf = ar.nodeFactory();
		return nf.CanonicalTypeNode(position(),
				ts.arrayOf(position(), base.typeRef(), varargs));
	}
	
	public boolean isVarargs() {
		return varargs;
	}
}
