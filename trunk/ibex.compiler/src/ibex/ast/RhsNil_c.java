package ibex.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsNil_c extends RhsExpr_c implements RhsNil {

    public RhsNil_c(Position pos) {
        super(pos);
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        return this;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        return type(ts.Null());
    }
    
    @Override
    public Term firstChild() {
        return null;
    }
    
    @Override
    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        return succs;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
    }

    @Override
    public String toString() {
        return "";
    }
}
