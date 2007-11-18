/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import polyglot.main.Report;

/**
 * Statistics collection and reporting object.
 * Extensions can override this to collect more stats or to change
 * reporting.
 */
public class Stats
{
    protected static class Times {
        public long inclusive;
        public long exclusive;
    }

    /** Extension we're collecting stats for. */
    protected ExtensionInfo ext;

    /** Map from Objects to pair of inclusive and exclusive times. */
    protected Map passTimes = new HashMap();

    /**
     * List of Objects used as keys to passTimes.  We have an explicit
     * list in order to report the keys in order.
     */
    protected List keys = new ArrayList(20);

    public Stats(ExtensionInfo ext) {
        this.ext = ext;
    }

    /** Reset the accumulated times for a pass. */
    public void resetPassTimes(Object key) {
        passTimes.remove(key);
    }

    /** Return the accumulated times for a pass. */
    public long passTime(Object key, boolean inclusive) {
        Times t = (Times) passTimes.get(key);
        if (t == null) {
            return 0;
        }

        return inclusive ? t.inclusive : t.exclusive;
    }

    /** Accumulate inclusive and exclusive times for a pass. */
    public void accumPassTimes(Object key, long in, long ex) {
        Times t = (Times) passTimes.get(key);
        if (t == null) {
            keys.add(key);
            t = new Times();
            passTimes.put(key, t);
        }
        t.inclusive += in;
        t.exclusive += ex;
    }

    /** Report the stats. */
    public void report() {
        if (Report.should_report(Report.time, 1)) {
            Report.report(1, "\nStatistics for " + ext.compilerName() +
                          " (" + ext.getClass().getName() + ")");
            Report.report(1, "Inclusive Exclusive Key");
            Report.report(1, "--------- --------- ---");

            for (Iterator i = keys.iterator(); i.hasNext(); ) {
                Object key = i.next();
                Times t = (Times) passTimes.get(key);

                Report.report(1, t.inclusive + " " + t.exclusive + " " +
                                 key.toString());
            }
        }
    }
}
