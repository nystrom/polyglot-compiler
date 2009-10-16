package ibex.ast;

import ibex.types.IbexClassType;
import ibex.types.IbexTypeSystem;
import ibex.types.Nonterminal;
import ibex.types.Nonterminal_c;
import ibex.types.RuleInstance;

import java.util.List;

import polyglot.ast.Call;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
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
        Nonterminal sym = null;

        MethodInstance mi = call.methodInstance();
        IbexClassType ct = (IbexClassType) mi.container();
        for (RuleInstance rule : ct.rules()) {
            if (rule.name() == mi.name())
                sym = rule.def().asNonterminal();
        }

        if (sym == null)
            throw new SemanticException("Cannot find rule for " + mi);
        
        TypeSystem ts = tc.typeSystem();
        LocalDef li = ts.localDef(position(), Flags.FINAL, Types.ref(call.type()), call.name().id());
        // Formal parameters are never compile-time constants.
        li.setNotConstant();
        
        return symbol(sym).localDef(li).type(call.type());
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
    
    Nonterminal sym;
    
    public Nonterminal symbol() {
        return sym;
    }
    
    public RhsInvoke symbol(Nonterminal sym) {
        RhsInvoke_c n = (RhsInvoke_c) copy();
        n.sym = sym;
        return n;
    }
}
