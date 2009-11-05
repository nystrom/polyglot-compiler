package ibex.ast;

import ibex.types.CharTerminal_c;
import ibex.types.IbexTypeSystem;
import ibex.types.RAnd_c;
import ibex.types.RLookahead_c;
import ibex.types.RSeq;
import ibex.types.RSeq_c;
import ibex.types.RSub_c;
import ibex.types.Rhs;
import ibex.types.RuleDef;
import ibex.types.Terminal_c;
import ibex.visit.GrammarNormalizer;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassMember;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class IbexClassBody_c extends ClassBody_c {


    public IbexClassBody_c(Position pos, List<ClassMember> members) {
        super(pos, members);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexClassBody_c n = (IbexClassBody_c) super.typeCheck(tc);
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        IbexNodeFactory nf = (IbexNodeFactory) tc.nodeFactory();
        GrammarNormalizer v = new GrammarNormalizer(tc.job(), ts, nf);
        v = (GrammarNormalizer) v.context(tc.context());
        n = (IbexClassBody_c) n.visit(v);
        
        if (tc.errorQueue().hasErrors())
            return n;
        
        return n.updateRuleDefs(tc);
    }

    private Node updateRuleDefs(ContextVisitor tc) {
        List<ClassMember> members = new ArrayList<ClassMember>();
        for (ClassMember cm : members()) {
            if (cm instanceof RuleDecl) {
                RuleDecl d = (RuleDecl) cm;
                RuleDef def = d.rule();
                RhsExpr e = d.rhs();
                e = getRhs(e, tc);
                List<Rhs> choices = new ArrayList<Rhs>();
                add(choices, e, tc);
                def.setChoices(choices);
                d = d.rhs(e);
                members.add(d);
            }
            else {
                members.add(cm);
            }
        }

        return this.members(members);
    }

    RhsExpr getRhs(RhsExpr e, ContextVisitor tc) {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        if (e instanceof RhsOr) {
            RhsOr c = (RhsOr) e;
            RhsExpr e1 = c.left();
            RhsExpr e2 = c.right();
            return c.left(getRhs(e1, tc)).right(getRhs(e2, tc)).rhs(null);
        }
        else if (e instanceof RhsAnd) {
            RhsAnd m = (RhsAnd) e;
            RhsExpr e1 = m.left();
            RhsExpr e2 = m.right();
            RhsExpr r1 = getRhs(e1, tc);
            RhsExpr r2 = getRhs(e2, tc);
            return m.left(r1).right(r2).rhs(new RAnd_c(ts, m.position(), r1.rhs(), r2.rhs()));
        }
        else if (e instanceof RhsMinus) {
            RhsMinus m = (RhsMinus) e;
            RhsExpr e1 = m.left();
            RhsExpr e2 = m.right();
            RhsExpr r1 = getRhs(e1, tc);
            RhsExpr r2 = getRhs(e2, tc);
            return m.left(r1).right(r2).rhs(new RSub_c(ts, m.position(), r1.rhs(), r2.rhs()));
        }
        else if (e instanceof RhsAction) {
            RhsAction a = (RhsAction) e;
            RhsExpr r = getRhs(a.item(), tc);
            return a.item(r).rhs(r.rhs());
        }
        else if (e instanceof RhsBind) {
            RhsBind a = (RhsBind) e;
            RhsExpr r = getRhs(a.item(), tc);
            return a.item(r).rhs(r.rhs());
        }
        else if (e instanceof RhsSequence) {
            return e.rhs(getSeq(e, tc));
        }
        else if (e instanceof RhsLit) {
            return e.rhs(getSeq(e, tc));
        }
        else if (e instanceof RhsInvoke) {
            return e.rhs(getSeq(e, tc));
        }
        else if (e instanceof RhsLookahead) {
            return e.rhs(getSeq(e, tc));
        }
        else if (e instanceof RhsBind) {
            return e.rhs(getSeq(e, tc));
        }
        else {
            // The rest should have been eliminated during normalization.
            throw new InternalCompilerError("unexpected rhs " + e);
        }
    }

    private void add(List<Rhs> choices, RhsExpr e, ContextVisitor tc) {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        if (e instanceof RhsOr) {
            RhsOr c = (RhsOr) e;
            RhsExpr e1 = c.left();
            RhsExpr e2 = c.right();
            add(choices, e1, tc);
            add(choices, e2, tc);
        }
        else if (e.rhs() != null)
            choices.add(e.rhs());
    }

    private Rhs getSeq(RhsExpr e, ContextVisitor tc) {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        if (e instanceof RhsSequence) {
            RhsSequence seq = (RhsSequence) e;
            List<Rhs> syms = new ArrayList<Rhs>(seq.terms().size());
            for (RhsExpr ei : seq.terms()) {
                Rhs ri = atom(ei, ts);
                if (ri instanceof RSeq) {
                    syms.addAll(((RSeq) ri).items());
                }
                else
                    syms.add(ri);
            }
            return new RSeq_c(ts, e.position(), syms, e.type());
        }
        else {
            return atom(e, ts);
        }
    }
    
    private Rhs atom(RhsExpr e, IbexTypeSystem ts) {
        if (e instanceof RhsInvoke) {
            RhsInvoke i = (RhsInvoke) e;
            return i.symbol();
        }
        if (e instanceof RhsBind) {
            RhsBind i = (RhsBind) e;
            return atom(i.item(), ts);
        }
        if (e instanceof RhsAction) {
            RhsAction i = (RhsAction) e;
            return atom(i.item(), ts);
        }
//        if (e instanceof RhsAnyChar) {
//            return Collections.<Rhs>singletonList(new CharRangeTerminal_c(ts, e.position(), (char) 0, (char) 65536));
//        }
//        if (e instanceof RhsRange) {
//            RhsRange r = (RhsRange) e;
//            return Collections.<Rhs>singletonList(new CharRangeTerminal_c(ts, e.position(), (Character) r.lo().constantValue(), (Character) r.hi().constantValue()));
//        }
        if (e instanceof RhsLit) {
            RhsLit r = (RhsLit) e;
            assert r.lit().isConstant();
            Object o = r.lit().constantValue();
            if (o instanceof Character) {
                return new CharTerminal_c(ts, e.position(), (Character) o);
            }
            else if (o instanceof String) {
                List<Rhs> l = new ArrayList<Rhs>();
                String s = (String) o;
                for (byte c : s.getBytes()) {
                    Terminal_c t = new CharTerminal_c(ts, e.position(), (char) c);
                    l.add(t);
                }
                if (l.size() == 1)
                    return l.get(0);
                else
                    return new RSeq_c(ts, e.position(), l, ts.String());
            }
        }
        if (e instanceof RhsLookahead) {
            RhsLookahead r = (RhsLookahead) e;
            boolean f = r.negativeLookahead();
            Rhs rs = atom(r.item(), ts);
            return new RLookahead_c(ts, e.position(), rs, f);
        }
        if (e instanceof RhsAction) {
            RhsAction a = (RhsAction) e;
            return atom(a.item(), ts);
        }
        
        assert false : "unexpected node " + e + ": " + e.getClass().getName();
        return null;
    }
}
