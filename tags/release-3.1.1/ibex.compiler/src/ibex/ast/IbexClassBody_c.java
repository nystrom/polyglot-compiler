package ibex.ast;

import ibex.types.CharTerminal_c;
import ibex.types.IbexTypeSystem;
import ibex.types.RAnd_c;
import ibex.types.RLookahead_c;
import ibex.types.RSeq_c;
import ibex.types.RSub_c;
import ibex.types.Rhs;
import ibex.types.RuleDef;
import ibex.types.Terminal_c;
import ibex.visit.GrammarNormalizer;

import java.util.ArrayList;
import java.util.Collections;
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
        for (ClassMember cm : members()) {
            if (cm instanceof RuleDecl) {
                RuleDecl d = (RuleDecl) cm;
                RuleDef def = d.rule();
                RhsExpr e = d.rhs();
                List<Rhs> choices = new ArrayList<Rhs>();
                add(choices, e, tc);
                def.setChoices(choices);
            }
        }

        return this;
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
        else if (e instanceof RhsAnd) {
            RhsAnd m = (RhsAnd) e;
            RhsExpr e1 = m.left();
            RhsExpr e2 = m.right();
            List<Rhs> cs1 = new ArrayList<Rhs>();
            List<Rhs> cs2 = new ArrayList<Rhs>();
            add(cs1, e1, tc);
            add(cs2, e2, tc);
            Rhs r1 = cs1.size() == 1 ? cs1.get(0) : new RSeq_c(ts, e1.position(), cs1, e1.type());
            Rhs r2 = cs2.size() == 1 ? cs2.get(0) : new RSeq_c(ts, e2.position(), cs2, e2.type());
            choices.add(new RAnd_c(ts, m.position(), r1, r2));
        }
        else if (e instanceof RhsMinus) {
            RhsMinus m = (RhsMinus) e;
            RhsExpr e1 = m.left();
            RhsExpr e2 = m.right();
            List<Rhs> cs1 = new ArrayList<Rhs>();
            List<Rhs> cs2 = new ArrayList<Rhs>();
            add(cs1, e1, tc);
            add(cs2, e2, tc);
            Rhs r1 = cs1.size() == 1 ? cs1.get(0) : new RSeq_c(ts, e1.position(), cs1, e1.type());
            Rhs r2 = cs2.size() == 1 ? cs2.get(0) : new RSeq_c(ts, e2.position(), cs2, e2.type());
            choices.add(new RSub_c(ts, m.position(), r1, r2));
        }
        else if (e instanceof RhsAction) {
            RhsAction a = (RhsAction) e;
            add(choices, a.item(), tc);
        }
        //        else if (e instanceof RhsBind) {
        //            RhsBind a = (RhsBind) e;
        //            add(choices, a.item(), tc);
        //        }
        else if (e instanceof RhsSequence) {
            addSequence(choices, e, tc);
        }
        //        else if (e instanceof RhsAnyChar) {
        //            addSequence(choices, e, tc);
        //        }
        //        else if (e instanceof RhsRange) {
        //            addSequence(choices, e, tc);
        //        }
        else if (e instanceof RhsLit) {
            addSequence(choices, e, tc);
        }
        else if (e instanceof RhsInvoke) {
            addSequence(choices, e, tc);
        }
        else if (e instanceof RhsLookahead) {
            addSequence(choices, e, tc);
        }
        else if (e instanceof RhsBind) {
            addSequence(choices, e, tc);
        }
        else {
            // The rest should have been eliminated during normalization.
            throw new InternalCompilerError("unexpected rhs " + e);
        }
    }

    private void addSequence(List<Rhs> choices, RhsExpr e, ContextVisitor tc) {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        if (e instanceof RhsSequence) {
            RhsSequence seq = (RhsSequence) e;
            List<Rhs> syms = new ArrayList<Rhs>(seq.terms().size());
            for (RhsExpr ei : seq.terms()) {
                syms.addAll(atom(ei, ts));
            }
            choices.add(new RSeq_c(ts, e.position(), syms, e.type()));
        }
        else {
            List<Rhs> syms = atom(e, ts);
            choices.add(new RSeq_c(ts, e.position(), syms, e.type()));
        }
    }
    
    private List<Rhs> atom(RhsExpr e, IbexTypeSystem ts) {
        if (e instanceof RhsInvoke) {
            RhsInvoke i = (RhsInvoke) e;
            return Collections.<Rhs>singletonList(i.symbol());
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
                return Collections.<Rhs>singletonList(new CharTerminal_c(ts, e.position(), (Character) o));
            }
            else if (o instanceof String) {
                List<Rhs> l = new ArrayList<Rhs>();
                String s = (String) o;
                for (byte c : s.getBytes()) {
                    Terminal_c t = new CharTerminal_c(ts, e.position(), (char) c);
                    l.add(t);
                }
                return l;
//                    return Collections.<Symbol>singletonList(new Terminal_c(ts, e.position(), String.valueOf(o)));
            }
        }
        if (e instanceof RhsLookahead) {
            RhsLookahead r = (RhsLookahead) e;
            boolean f = r.negativeLookahead();
            List<Rhs> rs = atom(r.item(), ts);
            return Collections.<Rhs>singletonList(new RLookahead_c(ts, e.position(), new RSeq_c(ts, e.position(), rs, e.type()), f));
        }
        if (e instanceof RhsAction) {
            RhsAction a = (RhsAction) e;
            return atom(a.item(), ts);
        }
        
        assert false : "unexpected node " + e + ": " + e.getClass().getName();
        return Collections.EMPTY_LIST;
    }
}
