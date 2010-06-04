package ibex.ast;

import ibex.types.IbexClassType;
import ibex.types.Nonterminal;
import ibex.types.RuleDef;
import ibex.types.RuleInstance;

import java.util.List;

import polyglot.ast.Call;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.CodeDef;
import polyglot.types.Context;
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
    private boolean assocTag;
    Nonterminal sym;

    public RhsInvoke_c(Position pos, Call call) {
        super(pos);
        this.call = call;
    }
    
    public Nonterminal symbol() {
        return (Nonterminal) sym;
    }

    public RhsInvoke symbol(Nonterminal s) {
        RhsInvoke_c n = (RhsInvoke_c) copy();
        n.sym = s;
        return n;
    }
    
    public boolean assocTag() {
        return assocTag;
    }
    
    public RhsInvoke assocTag(boolean assocTag) {
        RhsInvoke_c n = (RhsInvoke_c) copy();
        n.assocTag = assocTag;
        return n;
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
        RuleDef sym = null;

        MethodInstance mi = call.methodInstance();
        IbexClassType ct = (IbexClassType) mi.container();
        for (RuleInstance rule : ct.rules()) {
            if (rule.name() == mi.name())
                sym = rule.def();
        }

        if (sym == null)
            throw new SemanticException("Cannot find rule for " + mi);
        
        RhsInvoke n = (RhsInvoke) symbol(sym.asNonterminal()).type(call.type());
        
        if (assocTag) {
            Context c = tc.context();
            CodeDef code = c.currentCode();
            if (code instanceof RuleDef) {
                RuleDef rd = (RuleDef) code;
                if (rd != sym)
                    throw new SemanticException("Associativity annotation must be self-recursive.", position());
            }
        }
        
        if (! call.type().isVoid()) {
            TypeSystem ts = tc.typeSystem();
            LocalDef li = ts.localDef(position(), Flags.FINAL, Types.ref(call.type()), call.name().id());
            // Formal parameters are never compile-time constants.
            li.setNotConstant();
            
            IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();
            LocalDecl ld = nf.LocalDecl(position(), nf.FlagsNode(position(), li.flags()), nf.CanonicalTypeNode(position(), li.type()), nf.Id(position(), li.name()));
            ld = ld.localDef(li);
            ld = ld.init(n);
            
            return nf.RhsSyntheticBind(position(), ld).type(n.type());
        }
        
        return n;
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
        return (assocTag ? "^" : "") + call.toString();
    }
}