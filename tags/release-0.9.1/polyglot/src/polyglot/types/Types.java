package polyglot.types;

import java.util.*;

/** A class used for reporting debug output related to types. */
public class Types
{
    static Collection topics = new ArrayList();

    static {
	topics.add("types");
    }

    public static void report(int level, String msg) {
        polyglot.main.Report.report(topics, level, msg);
    }
}