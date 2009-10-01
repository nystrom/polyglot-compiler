package ibex.ast;

import java.util.List;

import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;


public class RhsInvoke_c extends RhsExpr_c implements RhsInvoke {

    private Call call;

    public RhsInvoke_c(Position pos, Call call) {
        super(pos);
        this.call = call;
    }

    public Call call() {
        return call;
    }

    public RhsInvoke call(Call call) {
        RhsInvoke_c n = (RhsInvoke_c) copy();
        n.call = call;
        return n;
    }
    
    @Override
    public Node visitChildren(NodeVisitor v) {
        Call call = (Call) visitChild(this.call, v);
        return call(call);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        return type(call.type());
    }

    public Term firstChild() {
        return call;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(call, this, EXIT);
        return succs;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(call, false, w, tr);
    }
    
    public String toString() {
        return call.toString();
    }
}
