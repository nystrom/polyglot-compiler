/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;



/** A <code>Pass</code> represents a compiler pass.
 * A <code>Job</code> runs a series of passes over the AST. 
 * A pass is run to attempt to satisfy a goal.
 */
public interface Pass
{
    /** The goal that should be reached should this pass complete successfully. */
    Goal goal();
    
    /** The job that created the pass, or null. */
    Job job();
    
    /** Return a user-readable name for the pass. */
    String name();

    /** Whether the pass is reentrant. */
    boolean isReentrant();
    
    /** Run the pass. */
    boolean run();

    /** Reset the pass timers to 0. */
    void resetTimers();

    /** Start/stop the pass timers. */
    void toggleTimers(boolean exclusive_only);

    /** The total accumulated time in ms since the last timer reset
      * that the pass was running, including spawned passes. */
    long inclusiveTime();

    /** The total accumulated time in ms since the last timer reset
      * that the pass was running, excluding spawned passes. */
    long exclusiveTime();
}
