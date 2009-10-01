package ibex.ast;

import java.util.List;

import polyglot.ast.Expr_c;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public abstract class RhsExpr_c extends Expr_c {

    public RhsExpr_c(Position pos) {
        super(pos);
    }

    abstract public Node visitChildren(NodeVisitor v);

    abstract public void prettyPrint(CodeWriter w, PrettyPrinter tr);

    abstract public String toString();

    abstract public List<Term> acceptCFG(CFGBuilder v, List<Term> succs);

    abstract public Term firstChild();

    abstract public Node typeCheck(ContextVisitor tc) throws SemanticException;
}
