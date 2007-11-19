/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import polyglot.util.StringUtil;

/** The base class for most passes. */
public abstract class AbstractPass implements Pass
{
    /**
     * If the pass is running, the time that the pass started.
     * If the pass has completed, the time in ms the pass took to run,
     * excluding spawned passes.
     */
    protected long exclusive_time = 0;

    /**
     * If the pass is running, the time that the pass started.
     * If the pass has completed, the time in ms the pass took to run,
     * including spawned passes.
     */
    protected long inclusive_time = 0;

    protected Job job;
    protected String name;
    protected Goal goal;

    protected AbstractPass(Goal goal) {
        this.goal = goal;
        this.name = StringUtil.getShortNameComponent(getClass().getName());
    }

    protected AbstractPass(Goal goal, String name) {
        this.goal = goal;
        this.name = name;
    }

    protected AbstractPass(Goal goal, Job job) {
        this(goal);
        this.job = job;
    }

    protected AbstractPass(Goal goal, Job job, String name) {
        this(goal, name);
        this.job = job;
    }

    public boolean isReentrant() {
        return false;
    }
    
    public Goal goal() {
        return goal;
    }
    
    public Job job() {
        return job;
    }

    /** The human-readable name of the pass. */
    public String name() {
        return name;
    }

    /** Run the pass, returning true on success. */
    public abstract boolean run();

    /** Start or stop the pass timer. */
    public void toggleTimers(boolean exclusive_only) {
        // How this works:
        // reset: time = 0
        // start: time = T - 0 = T
        //  stop: time = T' - T = delta1
        // start: time = T'' - delta1 = T'' - T' + T
        //  stop: time = T''' - (T'' - T' + T) = delta2 + delta1
        if (! exclusive_only) {
            inclusive_time = System.currentTimeMillis() - inclusive_time;
        }
        exclusive_time = System.currentTimeMillis() - exclusive_time;
    }

    /** Reset the pass timer. */
    public void resetTimers() {
        inclusive_time = 0;
        exclusive_time = 0;
    }

    /** Return the time in ms taken to run the pass, excluding the time in
     * spawned passes */
    public long exclusiveTime() {
        return exclusive_time;
    }

    /** Return the time in ms taken to run the pass, including the time in
     * spawned passes */
    public long inclusiveTime() {
        return inclusive_time;
    }

    public String toString() {
        return goal.toString();
    }
}
