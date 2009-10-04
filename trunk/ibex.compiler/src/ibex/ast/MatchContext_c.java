package ibex.ast;

import java.util.List;

import polyglot.ast.Expr_c;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;


public class MatchContext_c extends Expr_c implements MatchContext {

    public MatchContext_c(Position pos) {
        super(pos);
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
      return this;
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        Type mc = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IMatchContext"));
        return type(mc);
    }

    public Term firstChild() {
        return null;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        return succs;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("MatchContext");
    }
    
    public String toString() {
        return "MatchContext";
    }


}