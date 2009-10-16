package ibex.ast;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Node;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.Term;
import polyglot.types.Context;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsAction_c extends RhsExpr_c implements RhsAction {
    RhsExpr item;
    Block body;

    public RhsAction_c(Position pos, RhsExpr item, Block body) {
        super(pos);
        this.item = item;
        this.body = body;
    }
    
    public RhsExpr item() {
        return this.item;
    }
    
    public RhsAction item(RhsExpr item) {
        RhsAction_c n = (RhsAction_c) copy();
        n.item = item;
        return n;
    }

    public Block body() {
        return this.body;
    }
    
    public RhsAction body(Block stmt) {
        RhsAction_c n = (RhsAction_c) copy();
        n.body = stmt;
        return n;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        RhsExpr item = (RhsExpr) visitChild(this.item, v);
        Block stmt = (Block) visitChild(this.body, v);
        return item(item).body(stmt);
    }
    
    @Override
    public Context enterScope(Context c) {
        return c.pushBlock();
    }

    @Override
    public Term firstChild() {
        return item;
    }

    @Override
    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(item, body, ENTRY);
        v.visitCFG(item, this, EXIT); // hack to make this reachable
        v.visitCFG(body, this, EXIT);
        return succs;
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(item, w, tr);
        w.write(" ");
        print(body, w, tr);
    }

    @Override
    public String toString() {
        return item.toString() + " " + body.toString();
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        final List<Type> types = new ArrayList<Type>();
        body.visit(new NodeVisitor() {
            @Override
            public Node leave(Node old, Node n, NodeVisitor v) {
                if (n instanceof Return) {
                    Return r = (Return) n;
                    if (r.expr() != null) {
                        types.add(r.expr().type());
                    }
                }
                return super.leave(old, n, v);
            }
        });
        if (types.isEmpty())
            return type(ts.Null());
        Type t = types.get(0);
        for (Type tt : types) {
            t = ts.leastCommonAncestor(t, tt, tc.context());
        }
        return type(t);
    }
}
