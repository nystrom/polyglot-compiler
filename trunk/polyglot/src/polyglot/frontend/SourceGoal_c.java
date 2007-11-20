package polyglot.frontend;



public abstract class SourceGoal_c extends AbstractGoal_c implements SourceGoal {
    Job job;

    SourceGoal_c(String name, Job job) {
        super(name);
        assert job != null;
        this.job = job;
    }
    
    SourceGoal_c(Job job) {
        super();
        assert job != null;
        this.job = job;
    }

    public abstract Pass createPass();

    public Job job() {
        return job;
    }

    public int hashCode() {
        return job().hashCode() + name().hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof SourceGoal) {
            SourceGoal g = (SourceGoal) o;
            return job().equals(g.job()) && name().equals(g.name());
        }
        return false;
    }

    public String toString() {
        return job() + ":" + job().extensionInfo() + ":"
        + name() + " (" + stateString() + ")";
    }
}