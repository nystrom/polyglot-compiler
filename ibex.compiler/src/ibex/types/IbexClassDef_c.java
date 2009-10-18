package ibex.types;

import ibex.lr.GLR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import polyglot.frontend.Source;
import polyglot.types.ClassDef_c;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Predicate;

public class IbexClassDef_c extends ClassDef_c implements IbexClassDef {
    List<RuleDef> rules;
    GLR glr;
    
    protected IbexClassDef_c() {
        super();
    }

    public IbexClassDef_c(TypeSystem ts, Source fromSource) {
        super(ts, fromSource);
        this.rules = new ArrayList<RuleDef>();
    }


    protected void addSymbols(List<Rhs> l, Predicate<Rhs> p) {
        for (RuleDef rule : rules()) {
            if (p.isTrue(rule.asNonterminal()))
                l.add(rule.asNonterminal());
            
            LinkedList<Rhs> q = new LinkedList<Rhs>();
            q.addAll(rule.choices());
            
            while (! q.isEmpty()) {
                Rhs rhs = q.removeFirst();
                if (p.isTrue(rhs))
                    l.add(rhs);
                if (rhs instanceof RSeq) {
                    RSeq choice = (RSeq) rhs;
                    q.addAll(choice.items());
                }
                if (rhs instanceof RAnd) {
                    RAnd choice = (RAnd) rhs;
                    q.add(choice.choice1());
                    q.add(choice.choice2());
                }
                if (rhs instanceof RSub) {
                    RSub choice = (RSub) rhs;
                    q.add(choice.choice1());
                    q.add(choice.choice2());
                }
                if (rhs instanceof RLookahead) {
                    RLookahead choice = (RLookahead) rhs;
                    q.add(choice.item());
                }
            }
        }

        Type sup = Types.get(superType());
        if (sup instanceof IbexClassType) {
            IbexClassDef_c s = (IbexClassDef_c) ((IbexClassType) sup).def();
            s.addSymbols(l, p);
        }
    }
    
    public List<Nonterminal> allNonterminals() {
        List<Rhs> l = new ArrayList<Rhs>();
        addSymbols(l, new Predicate<Rhs>() {
            Set<Rhs> seen = new HashSet<Rhs>();
            public boolean isTrue(Rhs o) {
                if (o instanceof Nonterminal && !seen.contains(o)) {
                    seen.add(o);
                    return true;
                }
                return false;
            }});
        return (List) l;
    }

    public List<Terminal> allTerminals() {
        List<Rhs> l = new ArrayList<Rhs>();
        addSymbols(l, new Predicate<Rhs>() {
            public boolean isTrue(Rhs o) {
                Set<Rhs> seen = new HashSet<Rhs>();
                if (o instanceof Terminal && !seen.contains(o)) {
                    seen.add(o);
                    return true;
                }
                return false;
            }});
        return (List) l;
    }

    public boolean isParser() {
        IbexTypeSystem ts = (IbexTypeSystem) this.ts;
        
        if (ts.isSubtype(asType(), ts.runtimeParserType(), ts.emptyContext())) {
            return true;
        }
        return false;
    }

    public GLR glr() {
        return glr;
    }
    
    public void setGLR(GLR glr) {
        this.glr = glr;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.IbexClassDef#rules()
     */
    public List<RuleDef> rules() {
        return Collections.unmodifiableList(rules);
    }
    
    /* (non-Javadoc)
     * @see ibex.types.IbexClassDef#addRule(ibex.types.RuleDef)
     */
    public void addRule(RuleDef n) {
        rules.add(n);
    }
    
    /* (non-Javadoc)
     * @see ibex.types.IbexClassDef#setRules(java.util.List)
     */
    public void setRules(List<? extends RuleDef> l) {
        this.rules = new ArrayList<RuleDef>(l);
    }

}
