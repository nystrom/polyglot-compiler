package polyglot.frontend;

import polyglot.util.StringUtil;
import polyglot.visit.NodeVisitor;


public class VisitorGoal extends SourceGoal_c {
    NodeVisitor v;

    public VisitorGoal(Job job, NodeVisitor v) {
        super(StringUtil.getShortNameComponent(v.getClass().getName()), job);
        this.v = v;
    }

    public VisitorGoal(String name, Job job, NodeVisitor v) {
        super(name, job);
        this.v = v;
    }

    public Pass createPass() {
        return new VisitorPass(this, job, v);
    }
}
