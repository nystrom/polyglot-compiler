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


public class RhsAnyChar_c extends RhsExpr_c implements RhsAnyChar {

    public RhsAnyChar_c(Position pos) {
        super(pos);
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
      return this;
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        return isRegular(true).type(ts.Char());
    }

    public Term firstChild() {
        return null;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        return succs;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("_");
    }
    
    public String toString() {
        return "_";
    }


}
