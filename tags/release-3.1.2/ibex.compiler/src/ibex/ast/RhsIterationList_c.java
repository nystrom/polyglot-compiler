package ibex.ast;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public abstract class RhsIterationList_c extends RhsExpr_c implements RhsIterationList {
    RhsExpr item;
    RhsExpr sep;

    public RhsIterationList_c(Position pos, RhsExpr item, RhsExpr sep) {
        super(pos);
        this.item = item;
        this.sep = sep;
    }

    public RhsExpr item() { return item; }
    public RhsIterationList item(RhsExpr item) {
        RhsIterationList_c n = (RhsIterationList_c) copy();
        n.item = item;
        return n;
    }

    public RhsExpr sep() { return sep; }
    public RhsIterationList sep(RhsExpr sep) {
        RhsIterationList_c n = (RhsIterationList_c) copy();
        n.sep = sep;
        return n;
    }

    public Node visitChildren(NodeVisitor v) {
        RhsExpr item = (RhsExpr) visitChild(this.item, v);
        RhsExpr sep = (RhsExpr) visitChild(this.sep, v);
        return item(item).sep(sep);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        return type(ts.arrayOf(item.type()));
    }

    public Term firstChild() {
        return item;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(item, sep, ENTRY);
        v.visitCFG(sep, this, EXIT);
        return succs;
    }

}
