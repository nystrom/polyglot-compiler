/**
 * 
 */
package polyglot.frontend;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.NodeFactory;
import polyglot.frontend.JLScheduler.LookupGlobalType;
import polyglot.frontend.JLScheduler.LookupGlobalTypeDefAndSetFlags;
import polyglot.types.Def;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;

public abstract class FragmentGoal extends SourceGoal_c {
    protected Def def;
    protected ContextVisitor v;
    
    protected FragmentGoal(String name, Job job, Def def, ContextVisitor v) {
        super(name, job);
        assert def != null;
        assert v != null;
        this.def = def;
        this.v = v;
    }
    
    GoalSet view = null;
    
    @Override
    public GoalSet requiredView() {
        if (view == null)
            view = createRequiredView();    
        return view;
    }

    public Def def() {
        return def;
    }
    
    @Override
    public List<Goal> prereqs() {
        List<Goal> l = new ArrayList<Goal>();
        l.addAll(super.prereqs());
        l.add(Globals.Scheduler().FragmentAST(job()));
        return l;
    }
    
    public GoalSet createRequiredView() {
        return defaultRequiredView();
    }
    
    public GoalSet defaultRequiredView() {
        return new RuleBasedGoalSet() {
            public boolean contains(Goal g) {
                return FragmentGoal.super.requiredView().contains(g) ||
                g instanceof LookupGlobalType ||
                g instanceof LookupGlobalTypeDefAndSetFlags ||
                g instanceof FragmentGoal;
            }
            
            public String toString() { return "DefGoalRuleSet(" + FragmentGoal.this + ")"; }
        };
    }
    
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof FragmentGoal) {
            FragmentGoal g = (FragmentGoal) o;
            return super.equals(o) && name.equals(g.name) && (def != null ? def.equals(g.def) : g.def == null) && v.getClass() == g.v.getClass();
        }
        return false;
    }
    
    public int hashCode() {
        return super.hashCode() + (def != null ? def.hashCode() : 0);
    }
    
    public String toString() {
        return job() + ":" + job().extensionInfo() + ":"
        + name() + ":" + def + " (" + stateString() + ")";
    }
    
    public Pass createPass() {
        ExtensionInfo extInfo = Globals.Extension();
        TypeSystem ts = extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        return new FragmentPass(this, job, def, v);
    }
}