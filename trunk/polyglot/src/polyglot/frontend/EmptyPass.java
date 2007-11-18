/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;


/**
 * An <code>EmptyPass</code> does nothing.
 */
public class EmptyPass extends AbstractPass
{
    public EmptyPass(Goal goal) {
        super(goal);
    }

    public EmptyPass(Goal goal, Job job) {
        super(goal, job);
    }

    public boolean run() {
        return true;
    }
}
