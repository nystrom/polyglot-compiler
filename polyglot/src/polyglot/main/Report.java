package polyglot.main;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

/** Class used for reporting debug messages. */
public class Report {
  /** A collection of string names of topics which can be used with the
      -report command-line switch */
  public static Collection topics = new HashSet();

  /** A collection of string names of topics which we should always check
      if we should report. */
  public static Stack should_report = new Stack();

  static {
    should_report.push("verbose");
  }

  /**
   * Return whether a message on <code>topic</code> of obscurity
   * <code>level</code> should be reported, based on use of the
   * -report command-line switches given by the user.
   */
  public static boolean should_report(String topic, int level) {
    return should_report(Collections.singleton(topic), level); 
  }

  /**
   * Return whether a message on <code>topics</code> of obscurity
   * <code>level</code> should be reported, based on use of the
   * -report command-line switches given by the user.
   */
  public static boolean should_report(String[] topics, int level) {
    return should_report(Arrays.asList(topics), level);
  }

  /**
   * Return whether a message on <code>topics</code> of obscurity
   * <code>level</code> should be reported, based on use of the
   * -report command-line switches given by the user.
   */
  public static boolean should_report(Collection topics, int level) {
    for (Iterator i = should_report.iterator(); i.hasNext();) {
        String topic = (String) i.next();
        if (Options.global.level(topic) >= level) return true;
    }
    if (topics != null) {
	for (Iterator i = topics.iterator(); i.hasNext();) {
	    String topic = (String) i.next();
	    if (Options.global.level(topic) >= level) return true;
	}
    }
    return false;
  }

  /** This is the standard way to report debugging information in the
   *  compiler.  It reports a message of the specified level (which
   *  controls the presentation of the message. To test whether such
   *  message should be reported, use "should_report"
   *
   *  NOTE: This is a change of spec from earlier versions of Report.
   */
  public static void report(int level, String message) {
    for (int j = 1; j < level; j++) System.err.print("  ");
    System.err.println(message);
  }
}
