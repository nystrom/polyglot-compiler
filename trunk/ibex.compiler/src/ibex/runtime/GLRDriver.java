package ibex.runtime;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class GLRDriver {
    public static final int DEBUG_ACTIONS = 1;
    public static final int DEBUG_LOOP = 2;
    public static final int DEBUG_SHIFT = 4;
    public static final int DEBUG_REDUCE = 8;
    public static final int DEBUG_ENQUEUE = 16;
    public static final int DEBUG_ACCEPT = 32;

    public static int DEBUG = DEBUG_LOOP | DEBUG_SHIFT | DEBUG_REDUCE | DEBUG_ACTIONS;

    public static boolean USE_FAST_PATH = true;
    public static boolean DO_ERROR_RECOVERY = true;
    public static final int REPAIR_STRING_LENGTH = 4;
    public static final int ERROR_LOOKAHEAD = REPAIR_STRING_LENGTH * 2;

    ParserImpl parser;

    public GLRDriver(ParserImpl parser) {
        this.parser = parser;
        actionTable = decodeActionTable();
        overflowTable = decodeOverflowTable();
        gotoTable = decodeGotoTable();
        ruleTable = decodeRuleTable();
        mergeTable = decodeMergeTable();
    }

    // Lookahead a few tokens for better error handling.
    Terminal[] lookahead;
    int lookaheadScan;

    private void initLookahead(Terminal t) {
        if (lookahead == null) {
            lookahead = new Terminal[ERROR_LOOKAHEAD];
            lookahead[0] = t;
            lookaheadScan = 1;

            for (int i = 1; i < ERROR_LOOKAHEAD; i++) {
                try {
                    lookahead[i] = parser.scanTerminal();
                }
                catch (EOFException e) {
                    lookahead[i] = eof;
                }
                catch (IOException e) {
                    lookahead[i] = new ExceptionTerminal(e);
                }
            }
        }
        else {
            int len = lookahead.length - lookaheadScan;

            if (len < ERROR_LOOKAHEAD) {
                len = ERROR_LOOKAHEAD;
            }

            Terminal[] l = new Terminal[len];

            l[0] = t;

            for (int i = 1; i < len; i++) {
                if (lookaheadScan+i-1 < lookahead.length) {
                    l[i] = lookahead[lookaheadScan+i-1];
                }
                else {
                    try {
                        l[i] = parser.scanTerminal();
                    }
                    catch (EOFException e) {
                        l[i] = eof;
                    }
                    catch (IOException e) {
                        l[i] = new ExceptionTerminal(e);
                    }
                }
            }

            lookahead = l;
            lookaheadScan = 1;
        }
    }

    private Terminal scan() throws IOException {
        if (lookahead != null) {
            if (lookaheadScan < lookahead.length) {
                Terminal t = lookahead[lookaheadScan];

                if (t instanceof ExceptionTerminal) {
                    throw ((ExceptionTerminal) t).exception();
                }

                lookaheadScan++;
                return t;
            }
            else {
                lookahead = null;
            }
        }

        return parser.scanTerminal();
    }

    /** Tag used in the action table to indicate an error. */
    public static final int ERROR = 0;

    /**
     * Tag used in the action table to indicate a shift action.
     * The entry also contains the destination state of the shift.
     */
    public static final int SHIFT = 1;

    /**
     * Tag used in the action table to indicate a reduce action.
     * The entry also contains the index of the rule to reduce.
     */
    public static final int REDUCE = 2;

    /**
     * Tag used in the action table to indicate an accept action.
     * The entry also contains the index of the start symbol to accept.
     */
    public static final int ACCEPT = 3;

    /**
     * Tag used in the action table to indicate a conflict.
     * The entry also contains the index into the overflowTable.
     * of the list of conflicting actions.
     */
    public static final int OVERFLOW = 4;

    /**
     * The LR action table.
     * Indexed by parser state and by a terminal index.
     * Entries are either ERROR, SHIFT(state), REDUCE(rule), OVERFLOW(i), 
     * or ACCEPT(symbol), or SAVE, or RESTORE.
     */
    int[][] actionTable;

    /**
     * The action overflow table; used when there is a conflict in
     * actionTable.  When an action is OVERFLOW(i), i is used to
     * locate a string of actions in the overflow table:
     * overflowTable[i] = N is the length of the list of actions,
     * overflowTable[i+1 .. i+1+N].
     * Each entry is in the same format as in the actionTable,
     * except for the length entries.
     */
    int[] overflowTable;

    /**
     * The goto table, indexed by parser state and a nonterminal.
     */
    int[][] gotoTable;

    /**
     * The rule table, indexed by rule number.
     * The high 24 bits of each entry are the index of the nonterminal.
     * The low 8 bits are the rhs length.
     */
    int[] ruleTable;
    
    /**
     * The merge table, indexed by rule number.
     * An entry is 0 if this is not a merge rule.  Otherwise:
     * The high 29 bits of each entry are the index of the sibling rule in the merge.
     * The low 3 bits encode whether the merge should be allowed or not.
     */
    int[] mergeTable;
    
    static final int MERGE_THIS_NO_SIBLING_NO = 0;
    static final int MERGE_THIS_NO_SIBLING_YES = 1;
    static final int MERGE_THIS_YES_SIBLING_NO = 2;
    static final int MERGE_THIS_YES_SIBLING_YES = 3;

    /** The topmost nodes in the GSS. */
    ArrayList<Node> topmost;

    /** A node in the graph-structured stack (GSS) */
    protected static class Node {
        /** The state the node represents. */
        int state;

        /** Number of links into the node.  This is used to determine when to
         * recompute the deterministic depths for the entire GSS. */ 
        int refcount;

        /**
         * Deterministic depth of the node.  This is the number of
         * links that can be traversed before reaching a node with
         * out-degree &gt; 1.
         */
        int depth;   
        
        /** The rule whose reduction caused this node to be added, or -1. */
        int rule;

        /* Head of a list of links to the node's children. */
        Link out;     

        public Node(int state, int rule) {
            this.state = state;
            this.rule = rule;
            this.out = null;
            this.refcount = 0;
            this.depth = 1;
        }

        public String toString() {
            return "" + state;
        }
    }

    /**
     * A link in the stack.  Each link points to portion of the stack below.
     * A link may also point be part of a chain of sibling links.
     * All links in the chain represent a set of LR states the parser
     * could be in.
     */
    protected static class Link {
        Node bottom; // pointer down the stack
        Action semAction; // the semantic action associated with the link
        Link next;   // a sibling link, or null
        int span;
       
        Link(Node bottom, Node top, Action semAction, int span) {
            this.bottom = bottom;
            this.semAction = semAction;
            this.span = span;
        }

        public String toString() {
            return bottom.toString();
        }
    }

    protected abstract class Action {
        abstract Object run();
    }

    protected class TerminalAction extends Action {
        Terminal terminal;

        TerminalAction(Terminal terminal) {
            this.terminal = terminal;
        }

        Object run() {
            return terminal;
        }

        public String toString() {
            return terminal.toString();
        }
    }

    protected class SemanticAction extends Action {
        int rule;
        Action[] children;
        Object sval;
        SemanticAction ambiguous;

        SemanticAction(int rule, Action[] children) {
            this.rule = rule;
            this.children = children;
        }

        Object run() {
            // Check if already run.
            if (children == null) {
                if ((DEBUG & DEBUG_ACTIONS) != 0) {
                    System.out.println("run: " + this + " cached " + sval);
                }
                return sval;
            }

            if ((DEBUG & DEBUG_ACTIONS) != 0) {
                System.out.println("run: " + this);
            }

            // Run the children actions, building arguments to pass
            // to the semantic action function.
            // Actions for children should be run in the same order
            // as they appear on the rhs of the rule; that is,
            // svals[0] should be the semantic value for the leftmost
            // symbol on the rhs of the rule.  This is the same order
            // as the children array.
            Object[] svals = new Object[children.length];

            for (int i = 0; i < children.length; i++) {
                svals[i] = children[i].run();
                if ((DEBUG & DEBUG_ACTIONS) != 0) {
                    System.out.println("run: [" + i + "] = " + children[i]);
                    System.out.println("run:   --> " + svals[i]);
                }
            }

//            // Now merge if necessary.
//            if (ambiguous != null && rule != ambiguous.rule) {
//                // get the other semantic value, recursively running any
//                // merges in the tail of the ambiguous action list.
//                Object[] svals2 = new Object[ambiguous.children.length];
//
//                for (int i = 0; i < ambiguous.children.length; i++) {
//                    svals2[i] = ambiguous.children[i].run();
//                    if ((DEBUG & DEBUG_ACTIONS) != 0) {
//                        System.out.println("run: amb [" + i + "] = " + ambiguous.children[i]);
//                        System.out.println("run: amb   --> " + svals2[i]);
//                    }
//                }
//
//                // The lower numbered rule should be passed first.
//                if (rule <= ambiguous.rule) {
//                    sval = parser.mergeAction(rule, ambiguous.rule, svals, svals2);
//                }
//                else {
//                    sval = parser.mergeAction(ambiguous.rule, rule, svals2, svals);
//                }
//            }
//            else
            
                // Apply the semantic action.
                sval = parser.semanticAction(rule, svals);

            // Set children to null to indicate that we've run
            // And to permit the tree to be collected.
            children = null;

            if ((DEBUG & DEBUG_ACTIONS) != 0) {
                System.out.println("run: " + this + " returning " + sval);
            }

            return sval;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("action(rule #");
            sb.append(rule);
            sb.append(":");

            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(" ");
                    sb.append(children[i].toString());
                }
            }

            sb.append(")");

            return sb.toString();
        }
    }

    private int editDistance(List<Terminal> continuation, Terminal[] lookahead, int n) {
        int[][] m = new int[continuation.size()+1][n+1];

        for (int i = 0; i < m.length; i++) {
            m[i][0] = i;
        }

        for (int i = 0; i < m[0].length; i++) {
            m[0][i] = i;
        }

        for (int i = 1; i < m.length; i++) {
            Terminal t1 = (Terminal) continuation.get(i-1);

            for (int j = 1; j < m[i].length; j++) {
                Terminal t2 = (Terminal) lookahead[j-1];

                // Add 2 not 1: count a replace as a remove and insert.
                m[i][j] = m[i-1][j-1] + (t1.symbol() == t2.symbol() ? 0 : 2);

                if (m[i][j] > m[i-1][j] + 1)
                    m[i][j] = m[i-1][j] + 1;
                if (m[i][j] > m[i][j-1] + 1)
                    m[i][j] = m[i][j-1] + 1;
            }
        }

        int dist = m[continuation.size()][n];
        return dist;
    }

    String p(List<Terminal> l) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (Iterator<Terminal> i = l.iterator(); i.hasNext(); ) {
            Terminal t = (Terminal) i.next();
            sb.append(t.symbol());
            sb.append("[" + t + "]");
            if (i.hasNext()) sb.append(" ");
        }
        sb.append(")");
        return sb.toString();
    }

    String p(Terminal[] a, int n) {
        if (a.length < n) return p(a);
        return p(Arrays.asList(a).subList(0,n));
    }

    String p(Terminal[] a) {
        return p(Arrays.asList(a));
    }

    BitSet seen;

    private String terminalString(int terminal) {
        return String.valueOf((char) terminal);
    }

    /**
     * Error recovery.  Adapted from J.A. Dain, "A Practical
     * Minimum Distance Method for Syntax Error Handling".
     * Find a repair string that's the minimum distance from the actual
     * lookahead string.
     */
    private void generateRepairs(Node current, List<Terminal> continuation) {
        int e = actionTable[current.state][0];

        if (continuation.size() == REPAIR_STRING_LENGTH || action(e) == ACCEPT) {
            for (int i = 1; i < ERROR_LOOKAHEAD; i++) {
                int dist = editDistance(continuation, lookahead, i);
                if (dist < repairDistance) {
                    System.out.println("ed: cont = " + p(continuation));
                    System.out.println("ed: look = " + p(lookahead, i));
                    System.out.println("ed: distance = " + dist);
                    repairString = continuation;
                    repairDistance = dist;
                    repairLength = i;
                }
            }
        }
        else {
            List<Node> newTopmost = new ArrayList<Node>();

            for (int h = 0; h < actionMap[current.state].length; h++) {
                int b = actionMap[current.state][h];
                if (b == -1)
                    break;
                Terminal t = new ErrorTerminal(b, terminalString(b));

                // Handle reduces.
                int[] rules = reductions(current, t);

                for (int i = 0; i < rules.length && rules[i] != -1; i++) {
                    // reduce N -> alpha
                    int lhs = ruleLhsIndex(ruleTable[rules[i]]);
                    int rhsLength = ruleRhsLength(ruleTable[rules[i]]);

                    if (seen.get((current.state << 16) | rules[i])) {
                        continue;
                    }
                    seen.set((current.state << 16) | rules[i]);

                    newTopmost.clear();
                    simulateReduce(current, rhsLength, rules[i], 0, newTopmost);

                    for (Iterator<Node> j = newTopmost.iterator(); j.hasNext(); ) {
                        Node newTop = (Node) j.next();
                        int dest = gotoTable[newTop.state][lhs];
                        System.out.println(current + ": reduce with rule " + rules[i] + " and goto " + dest);
                        Node pushed = new Node(dest, rules[i]);
                        Link link = new Link(newTop, pushed, null, 1);
                        link.next = pushed.out;
                        pushed.out = link;
                        generateRepairs(pushed, continuation);
                    }
                }

                // Handle shifts.
                int dest = shift(current, t);

                if (dest != -1) {
                    System.out.println(current + ": shift " + t + " to " + dest);
                    Node pushed = new Node(dest, -1);
                    Link link = new Link(current, pushed, null, 1);
                    link.next = pushed.out;
                    pushed.out = link;
                    List<Terminal> k = new ArrayList<Terminal>(continuation.size()+1);
                    k.addAll(continuation);
                    k.add(t);

                    BitSet oldSeen = seen;
                    seen = new BitSet();

                    generateRepairs(pushed, k);

                    seen = oldSeen;
                }
            }
        }
    }

    private void simulateReduce(Node current, int depth, int rule, int currDepth, List<Node> newTopmost) {
        if (currDepth == depth) {
            newTopmost.add(current);
        }
        else {
            for (Link l = current.out; l != null; l = l.next) {
                simulateReduce(l.bottom, depth, rule,
                               currDepth+1, newTopmost);
            }
        }
    }

    private String errorString(Terminal[] oldLookahead, int oldLength,
            Terminal[] newLookahead, int newLength) {
        while (oldLength > 0 && newLength > 0) {
            if (oldLookahead[oldLength-1] != newLookahead[newLength-1]) {
                break;
            }
            oldLength--;
            newLength--;
        }

        int start = 0;

        while (start < oldLength && start < newLength) {
            if (oldLookahead[start] != newLookahead[start]) {
                break;
            }
            start++;
        }

        String ls = "";
        for (int i = start; i < oldLength; i++) {
            if (i != start) ls += " ";
            ls += oldLookahead[i];
        }

        String rs = "";
        for (int i = start; i < newLength; i++) {
            if (i != start) rs += " ";
            rs += newLookahead[i];
        }

        if (ls.length() == 0 && rs.length() == 0) {
            return "Syntax error.";
        }
        else if (ls.length() == 0) {
            return "Syntax error.  Inserting " + rs + ".";
        }
        else if (rs.length() == 0) {
            return "Syntax error.  Deleting " + ls + ".";
        }
        else {
            return "Syntax error.  Replacing " + ls + " with " + rs + "."; 
        }
    }

    int[][] actionMap;

    private void initBitmaps() {
        if (actionMap != null) {
            return;
        }

        actionMap = new int[actionTable.length][actionTable[0].length];

        for (int i = 0; i < actionTable.length; i++) {
            int j = 0;
            for (int b = 0; b < actionTable[i].length; b++) {
                int e = actionTable[i][b];
                int action = action(e);
                if (action == SHIFT || action == REDUCE) {
                    actionMap[i][j++] = b;
                }
            }
            actionMap[i][j] = -1;
        }
    }

    List<Terminal> repairString;
    int repairDistance;
    int repairLength;

    public void error(Node n, Terminal t) {
        // pop the stack until a recovery state s is found.
        // state s corresponds to the item A -> error . alpha

        // discard input until a token in FIRST(alpha), or
        // FOLLOW(A) if alpha is empty, is found.
        // Then resume parsing.
        // Recovery is complete when A -> error alpha .
        // is reduced.

        // the semantic value for all reductions performed in recovery
        // is ERROR.  Do not call apply.

        error = true;

        if (repairLength > 0) {
            // Don't recover again until we've shifted past the previous
            // repair.
            return;
        }

        {
            System.err.println("error in state " + n.state + " at " + t);
            System.err.print("expected one of:");

            for (int i = 0; i < actionTable[n.state].length; i++) {
                if (action(actionTable[n.state][i]) != ERROR) {
                    System.err.print(" " + i);
                }
            }

            System.err.println();
        }

        if (! DO_ERROR_RECOVERY) {
            fatalError = true;
        }
        else {
            repairString = null;
            repairDistance = 2 * ERROR_LOOKAHEAD;
            repairLength = 0;

            initLookahead(t);
            initBitmaps();
            seen = new BitSet();

            System.out.println("error: look = " + p(lookahead));

            System.out.println("generating repairs");

            generateRepairs(n, new ArrayList<Terminal>());

            seen = null;

            System.out.println("generated repairs");
            System.out.println("repair string = " + repairString);

            if (repairString != null) {
                System.out.println("error: look = " + p(lookahead));
                System.out.println("error: repairString = " + repairString);
                System.out.println("error: repairLength = " + repairLength);

                Terminal[] l = new Terminal[repairString.size()+lookahead.length-repairLength];
                int i = 0;
                while (i < repairString.size()) {
                    l[i] = (Terminal) repairString.get(i);
                    // Get the symbol from the current lookahead if possible.
                    BitSet skip = new BitSet();
                    for (int j = 0; j < repairLength; j++) {
                        if (! skip.get(j) &&
                                l[i].symbol() == lookahead[j].symbol()) {

                            l[i] = lookahead[j];
                            skip.set(j);
                        }
                    }
                    i++;
                }

                int j = repairLength;
                while (j < lookahead.length) {
                    l[i] = lookahead[j];
                    i++; j++;
                }

                System.err.println(errorString(lookahead, repairLength,
                                               l, repairString.size()));

                lookahead = l;
            }
            else {
                System.err.println("Syntax error at " + t);
                fatalError = true;
            }

            // restore n to the top of the stack.
            topmost.add(n);
        }
    }

    private boolean error;
    private boolean fatalError;
    private EOF eof;

    // returns the set of accepting stack nodes
    public Object parse(int startState, int startSym) throws IOException {
        // Pre-allocate the return array of reductions().  The parser can, at
        // most, reduce using all rules at once, plus one to indicate the end.
        globalRules = new int[ruleTable.length+1];

        error = false;
        fatalError = false;

        topmost = new ArrayList<Node>();
        topmost.add(new Node(startState, -1));
        boolean accept = false;

        eof = new EOF(parser.eofSymbol());

        SCAN:
            while (! accept && ! fatalError) {
                Terminal t;

                try {
                    t = scan();
                }
                catch (EOFException e) {
                    t = eof;
                }

                if (error && t.symbol() == eof.symbol()) {
                    fatalError = true;
                }

                CHURN:
                    while (! accept) {
                        if ((DEBUG & DEBUG_LOOP) != 0) {
                            System.out.println("------------------------");
                            System.out.println("scan: " + t + " (" + t.symbol() + ")");
                            System.out.println("topmost: " + topmost);
                        }

                        // Do LR(1) parsing if it will be deterministic.
                        if (USE_FAST_PATH && topmost.size() == 1) {
                            Node n = (Node) topmost.get(0);
                            int e = actionTable[n.state][t.symbol()];
                            int action = action(e);

                            SWITCH:
                            switch (action) {
                            case ERROR:
                                error(n, t);
                                continue SCAN;
                            case SHIFT: {
                                int dest = actionData(e);

                                if (repairLength > 0) {
                                    repairLength--;
                                }

                                if ((DEBUG & DEBUG_LOOP) != 0) {
                                    System.out.println("shift " + t + " to " + dest);
                                }

                                Node topSib = new Node(dest, -1);

                                // insert topSib into topmost;
                                // push topSib onto stack
                                addLink(n, topSib, new TerminalAction(t), 1);
                                topmost.set(0, topSib);
                                continue SCAN;
                            }
                            case REDUCE: {
                                int rule = actionData(e);
                                int lhs = ruleLhsIndex(ruleTable[rule]);
                                int rhsLength = ruleRhsLength(ruleTable[rule]);

                                // Check if this is a merge rule.  If so, fall through to the slow case.
                                int mergeEntry = mergeTable[rule];
                                if (mergeEntry != 0)
                                    break SWITCH;
                                
                                if (n.depth >= rhsLength) {
                                    // the reduce is deterministic

                                    Action[] semAction = new Action[rhsLength];

                                    // walk the stack, building the array of
                                    // children for the semantic action node
                                    Node current = n;
                                    int span = 0;

                                    for (int i = 0; i < rhsLength; i++) {
                                        Link l = current.out;
                                        semAction[rhsLength-i-1] = l.semAction;
                                        current = l.bottom;
                                        span += l.span;
                                    }

                                    // create a semantic action node.
                                    SemanticAction v = new SemanticAction(rule, semAction);

                                    // current now points to the new top of stack

                                    int nextState = gotoTable[current.state][lhs];

                                    if ((DEBUG & DEBUG_LOOP) != 0) {
                                        System.out.println("reduce with rule " + rule);
                                        System.out.println("        and goto " + nextState);
                                    }

                                    Node newTop = new Node(nextState, rule);

                                    // discard
                                    if (current != n) current.refcount--;
                                    topmost.remove(0);
                                    topmost.add(newTop);

                                    addLink(current, newTop, v, span);

                                    // resume at the next token
                                    continue CHURN;
                                }
                                else {
                                    // nondeterministic reduce; break to the GLR code
                                    break SWITCH;
                                }
                            }
                            case OVERFLOW:
                                if ((DEBUG & DEBUG_LOOP) != 0) {
                                    System.out.println("overflow!");
                                }
                                // let the GLR code handle it.
                                break SWITCH;
                            case ACCEPT:
                                // let the GLR code handle it.
                                break SWITCH;
                            }
                        }

                        if ((DEBUG & DEBUG_LOOP) != 0) {
                            System.out.println("doing GLR parse");
                            System.out.println("before R topmost = " + topmost);
                        }

                        // Couldn't do LR(1) parsing.  Do GLR parsing.
                        doReductions(t);

                        if ((DEBUG & DEBUG_LOOP) != 0) {
                            System.out.println("before S topmost = " + topmost);
                        }

                        accept = doAccepts(t, startSym);

                        if (! accept) {
                            doShifts(t);
                        }

                        if ((DEBUG & DEBUG_LOOP) != 0) {
                            System.out.println("after S topmost = " + topmost);
                        }

                        break CHURN;
                    }
            }

        if (! error && ! topmost.isEmpty()) {
            // Only nodes in an accepting state should be in topmost.
            // Get all the semantic values in all the accepting states.
            List<Object> l = filter(topmost, startSym);

            if (l.size() == 0) {
                System.err.println("No semantic value.");
            }
            else if (l.size() > 1) {
                System.err.println("Unresolved ambiguity: " + l);
            }

            if ((DEBUG & DEBUG_ACTIONS) != 0) {
                System.out.println("Parser returning " + l.get(0));
            }

            return l.get(0);
        }

        return null;
    }

    private List<Object> filter(ArrayList<Node> topmost, int startSym) {
        List<Object> l = new LinkedList<Object>();
        for (Iterator<Node> i = topmost.iterator(); i.hasNext(); ) {
            Node n = (Node) i.next();
            for (Link link = n.out; link != null; link = link.next) {
                if ((DEBUG & DEBUG_ACTIONS) != 0) {
                    System.out.println("running " + link.semAction);
                }

                Object sval = link.semAction.run();

                if ((DEBUG & DEBUG_ACTIONS) != 0) {
                    System.out.println("parser result = " + sval);
                }

                l.add(sval);
            }
        }
        return l;
    }

    // Pre-allocate the return array of reductions.  The parser can,
    // at most, reduce using all rules at once, plus one to indicate the end.
    private int[] globalRules;

    // return all reduce actions for state n.state for Terminal t.
    private int[] reductions(Node n, Terminal t) {
        int[] rules = globalRules;

        int entry = actionTable[n.state][t.symbol()];
        int action = action(entry);

        if (action == REDUCE) {
            // get the rule to reduce with
            int rule = actionData(entry);
            rules[0] = rule;
            rules[1] = -1;
            return rules;
        }
        if (action == OVERFLOW) {
            // data is an index into the overflowTable
            int start = actionData(entry);

            // get the number of actions in this string of actions.
            int length = overflowTable[start];

            int count = 0;

            for (int i = 0; i < length; i++) {
                entry = overflowTable[start+1+i];
                action = action(entry);
                if (action == REDUCE) {
                    rules[count++] = actionData(entry);
                }
            }

            rules[count] = -1;

            return rules;
        }

        rules[0] = -1;
        return rules;
    }

    // return the shift action for state n.state for Terminal t, if any;
    // -1 otherwise
    private  int shift(Node n, Terminal t) {
        int entry = actionTable[n.state][t.symbol()];
        int action = action(entry);

        if (action == SHIFT) {
            // data is the destination state
            int dest = actionData(entry);
            return dest;
        }
        if (action == OVERFLOW) {
            // data is an index into the overflowTable
            int start = actionData(entry);

            // get the number of actions in this string of actions.
            int length = overflowTable[start];

            for (int i = 0; i < length; i++) {
                entry = overflowTable[start+1+i];
                if (action(entry) == SHIFT) {
                    return actionData(entry);
                }
            }
        }

        return -1;
    }

    private void enqueuePaths(Node root, int depth, int rule,
            Node useTop, Node useBottom, PathQueue pathqueue) {
        enqueuePathsDFS(root, depth, rule, useTop, useBottom, pathqueue,
                        0, new Path(root, depth));
    }

    private void enqueuePathsDFS(Node current, int depth, int rule,
            Node useTop, Node useBottom,
            PathQueue pathqueue, int currDepth, Path p)
    {
        if (currDepth == depth) {
            if (useTop == null && useBottom == null) {
                // Add a _copy_ of the path to the queue, since it will
                // be destructively updated for the next path
                if ((DEBUG & DEBUG_ENQUEUE) != 0) {
                    System.out.println("enqueue path for rule " + rule);
                    System.out.print("    " + 0 + ": " + p.top);
                    for (int i = 0; i < p.path.length; i++) {
                        System.out.println(" " + p.path[i].semAction);
                        System.out.print(" -> " + (i+1) + ": " + p.path[i].bottom);
                    }
                    System.out.println();
                }
                pathqueue.add(new PathQueueEntry(new Path(p), rule));
            }
            return;
        }

        for (Link l = current.out; l != null; l = l.next) {
            // Set useTop, useBottom to null to indicate the link
            // has been seen.
            if (current == useTop && l.bottom == useBottom) {
                useTop = useBottom = null;
            }

            p.path[currDepth] = l;

            enqueuePathsDFS(l.bottom, depth, rule, useTop, useBottom,
                            pathqueue, currDepth+1, p);
        }
    }

    private static class Path {
        Node top;
        Link[] path;
        int span;

        Path(Node top, int depth) {
            this.top = top;
            this.path = new Link[depth];
            this.span = 0;
        }

        Path(Path that) {
            this.path = new Link[that.path.length];
            for (int i = 0; i < that.path.length; i++)
                this.path[i] = that.path[i];
            this.top = that.top;

            // Compute the number of tokens spanned by the path.
            span = 0;
            for (int i = 0; i < path.length; i++) {
                span += path[i].span;
            }
        }

        Node bottom() {
            if (path.length == 0) return top;
            return path[path.length-1].bottom;
        }

        int span() {
            return span;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[" + top);
            for (int i = 0; i < path.length; i++) {
                sb.append("->" + path[i]);
            }
            sb.append(" (spans " + span + ")]");
            return sb.toString();
        }
    }

    private static class PathQueue {
        TreeSet<PathQueueEntry> s;

        PathQueue() {
            s = new TreeSet<PathQueueEntry>();
        }

        void clear() {
            s.clear();
        }

        boolean isEmpty() {
            return s.isEmpty();
        }

        PathQueueEntry get() {
            PathQueueEntry e = (PathQueueEntry) s.first();
            s.remove(e);
            return e;
        }

        void add(PathQueueEntry e) {
            s.add(e);
        }
    }

    private static class PathQueueEntry implements Comparable<PathQueueEntry> {
        Path path;
        int rule;

        PathQueueEntry(Path path, int rule) {
            this.path = path;
            this.rule = rule;
        }

        public int compareTo(PathQueueEntry o) {
            PathQueueEntry that = (PathQueueEntry) o;

            if (this == that) return 0;

            // To avoid the yield-then-merge problem at least for
            // acyclic grammars, the path queue is sorted according
            // to the following two rules.

            // 1. Reductions that span fewer tokens come first.
            int span1 = this.path.span();
            int span2 = that.path.span();

            if (span1 < span2) return -1;
            if (span1 > span2) return 1;

            // 2. If A -> alpha and B -> beta span the same tokens,
            // then A -> alpha comes first if B ->+ A
            // The ruleTable is constructed so that
            // if B ->+ A, all rules with lhs A come first.
            if (this.rule < that.rule) {
                // this is possibly derived from that; this is first
                return -1;
            }
            if (this.rule > that.rule) {
                // that is possibly derived from this; that is first
                return 1;
            }

            // As a tie breaker, order the paths lexicographically.
            if (this.path.top.state != that.path.top.state) {
                return this.path.top.state - that.path.top.state;
            }

            Link[] p1 = this.path.path;
            Link[] p2 = that.path.path;

            if (p1.length != p2.length) {
                return p1.length - p2.length;
            }

            for (int i = 0; i < p1.length; i++) {
                if (p1[i].bottom != p2[i].bottom) {
                    return p1[i].bottom.state - p2[i].bottom.state;
                }
            }

            // if (true)
            // throw new RuntimeException(this + " and " + that + " are incomparable");

            // Same rule, same number of tokens; this should equal that.
            return 0;
        }

        public boolean equals(Object o) {
            return this == o;
        }

        public String toString() {
            return "<" + path + "," + rule + ">";
        }
    }

    private int[][] decodeActionTable() {
        String[] t = parser.encodedActionTable();
        return (int[][]) new Decoder().decode(t);
    }

    private int[] decodeOverflowTable() {
        String[] t = parser.encodedOverflowTable();
        return (int[]) new Decoder().decode(t);
    }

    private int[][] decodeGotoTable() {
        String[] t = parser.encodedGotoTable();
        return (int[][]) new Decoder().decode(t);
    }

    private int[] decodeRuleTable() {
        String[] t = parser.encodedRuleTable();
        return (int[]) new Decoder().decode(t);
    }
    
    private int[] decodeMergeTable() {
        String[] t = parser.encodedMergeTable();
        return (int[]) new Decoder().decode(t);
    }

    // The top 3 bits of an action table entry indicate the kind of action.
    // The bottom 29 bits are of an entry e are one of the following:
    // if action(e) == SHIFT, the state to transition to after the shift
    // if action(e) == REDUCE, the rule to reduce
    // if action(e) == OVERFLOW, the entry in the overflowTable
    //                            containing the list of actions
    // 0 otherwise

    // The run-time compiler should inline these methods.
    private static int action(int entry) {
        return entry >>> 29;
    }

    private static int actionData(int entry) {
        return entry & ~(7 << 29);
    }

    // The top 24 bits of a rule table entry are the lhs nonterminal index.
    // The bottom 8 bits of a rule table entry is the rhs length.
    private static int ruleLhsIndex(int entry) {
        return entry >>> 8;
    }

    private static int ruleRhsLength(int entry) {
        return entry & 0xff;
    }

    // should be a priority queue
    PathQueue globalPathqueue = new PathQueue();

    private void doReductions(Terminal t) {
        // Use a single global path queue so we don't have to create an array
        // for each terminal, and so we don't have to keep growing the array.
        PathQueue pathqueue = globalPathqueue;
        pathqueue.clear();
        
        for (Iterator<Node> j = topmost.iterator(); j.hasNext(); ) {
            Node current = (Node) j.next();
            
            if ((DEBUG & DEBUG_REDUCE) != 0) {
                System.out.println("in state " + current.state);
            }
            
            int[] rules = reductions(current, t);
            
            for (int i = 0; i < rules.length && rules[i] != -1; i++) {
                // reduce N -> alpha
                int rule = rules[i];
                
                if ((DEBUG & DEBUG_REDUCE) != 0) {
                    System.out.println("enqueue for reduce with rule " + rule);
                }
                
                int rhsLength = ruleRhsLength(ruleTable[rule]);
                enqueuePaths(current, rhsLength, rule, null, null,
                             pathqueue);
            }
        }

        boolean[] present = new boolean[ruleTable.length];

        // don't use an iterator; path queue can grow as we iterate
        // through it.
        while (! pathqueue.isEmpty()) {
            PathQueueEntry e = pathqueue.get();
            present[e.rule] = true;
            reduceViaPath(e.path, e.rule, t, pathqueue);
        }
        
        filterFailedMerges(present);
    }

    private void filterFailedMerges(boolean[] present) {
        for (Iterator<Node> i = topmost.iterator(); i.hasNext(); ) {
            Node node = i.next();
            
            int rule = node.rule;
            
            if (rule == -1)
                continue;
            
            int mergeEntry = mergeTable[rule];
            
            if (mergeEntry == 0)
                continue;
            
            int sibling = mergeEntry >>> 3;
            int mergeAction = mergeEntry & 7;

            assert (mergeTable[sibling] >>> 3) == rule;

            boolean discard = false;

            switch (mergeAction) {
            case MERGE_THIS_NO_SIBLING_YES:
                // this is yes, so discard
                discard = true;
                break;
            case MERGE_THIS_YES_SIBLING_NO:
                if (present[sibling])
                    discard = true;
                break;
            case MERGE_THIS_YES_SIBLING_YES:
                if (! present[sibling])
                    discard = true;
                break;
            }

            if (discard) {
                if ((DEBUG & DEBUG_REDUCE) != 0) {
                    String[] s = { "this no sibling no",
                                   "this no sibling yes",
                                   "this yes sibling no",
                                   "this yes sibling yes"
                    };
                    System.out.println("discarding reduction with rule " + rule + " failed merge with sibling " + sibling);
                    System.out.println("  this rule present=" + present[rule] + " sibling present=" + present[sibling]);
                    System.out.println("  merge action = " + s[mergeAction]);
                }
                i.remove();
            }
        }
    }

    /** Reset the deterministic depth of all nodes in the GSS reachable
     * from current. */ 
    private int resetDeterminsticDepth(Node current) {
        if (current.out == null) {
            current.depth = 1;
            return 1;
        }
        else {
            int m = 0;
            for (Link l = current.out; l != null; l = l.next) {
                int d = resetDeterminsticDepth(l.bottom);
                if (m < d) m = d;
            }
            current.depth = m+1;
            return m+1;
        }
    }

    private Link addLink(Node bottom, Node top, Action semAction, int span) {
        Link link = new Link(bottom, top, semAction, span);

        if (top.out == null) {
            top.depth = bottom.depth+1;
        }
        else {
            resetDeterminsticDepth(top);
        }

        // add link to current's list of out links
        link.next = top.out;
        top.out = link;

        // bump the bottom node's ref count
        bottom.refcount++;

        return link;
    }

    /**
     * Return true if accepting, leaving only the accepting states in topmost.
     * Otherwise, do nothing.
     */
    private boolean doAccepts(Terminal t, int startSym) {
        List<Node> acceptingStates = null;

        // If non-reduced positive lookahead -- fail!
        // If reduced negative lookahead -- fail in doReductions!
        
        for (Iterator<Node> i = topmost.iterator(); i.hasNext(); ) {
            Node current = (Node) i.next();
            int e = actionTable[current.state][t.symbol()];
            
            if (action(e) == ACCEPT && actionData(e) == startSym) {
                if ((DEBUG & DEBUG_ACCEPT) != 0) {
                    System.out.println("accept for " + startSym + " in " + current + " at " + t);
                }

                if (acceptingStates == null) {
                    acceptingStates = new ArrayList<Node>();
                }
                acceptingStates.add(current);
            }
        }

        if (acceptingStates != null) {
            topmost.clear();
            topmost.addAll(acceptingStates);
            return true;
        }

        return false;
    }

    private void doShifts(Terminal t) {
        ArrayList<Node> prevTops = new ArrayList<Node>(topmost);

        topmost.clear();

        // "pop" the stack
        // We've done all the reductions for this terminal.
        // The list topmost contains all the states we were in
        // as well as the new states we went to after reducing.
        // Unless there's a parser error, at least one of the new
        // states, should shift.
        // We process the old states as well, since there may have been
        // a shift/reduce conflict in one of them and we need to
        // perform the shift there.
        //
        // In the end, any states for which we cannot shift, are syntax
        // errors on the paths being explored and will be removed from
        // topmost.
        //
        // Idea for error recovery: rather than simply removing these
        // states, pop the stack until reaching a state where the shift
        // can be performed, or pop to a error recovery state and don't
        // shift.  By not shifting the input is effectively being
        // discarded for these states until a parser state can be
        // synchronized with the input again.  Of course, if you can't
        // shift an error state, don't pop it; just leave it on the
        // stack.  Error states can be processed as normal, except when
        // reducing from, or through, an error state record the syntax
        // error.
        //
        // Do error recovery ONLY if no states can shift.

        TerminalAction termAction = new TerminalAction(t);

        TOPMOST:
            for (Iterator<Node> i = prevTops.iterator(); i.hasNext(); ) {
                Node current = (Node) i.next();

                if ((DEBUG & DEBUG_SHIFT) != 0) {
                    System.out.println("in state " + current.state);
                }

                int dest = shift(current, t);

                if (dest == -1) {
                    if ((DEBUG & DEBUG_SHIFT) != 0) {
                        System.out.println("not shifting " + t);
                    }

                    // error(current, t);
                    continue TOPMOST;
                }

                if (repairLength > 0) {
                    repairLength--;
                }

                if ((DEBUG & DEBUG_SHIFT) != 0) {
                    System.out.println("shifting " + t + " to " + dest);
                }

                // check if there is an existing node topSib in topmost with
                // state dest
                for (Iterator<Node> j = topmost.iterator(); j.hasNext(); ) {
                    Node topSib = (Node) j.next();

                    if (topSib.state == dest) {
                        // add another link, then continue with the next
                        // element of topmost
                        addLink(current, topSib, termAction, 1);
                        continue TOPMOST;
                    }
                }

                // no such node; create one and add to topmost
                Node topSib = new Node(dest, -1);
                topmost.add(topSib);

                // push topSib onto stack
                addLink(current, topSib, termAction, 1);
            }

        // Error handling.
        if (topmost.isEmpty()) {
            for (Iterator<Node> i = prevTops.iterator(); i.hasNext(); ) {
                Node current = (Node) i.next();
                error(current, t);
                break;
            }
        }
    }
    
    /**
     * Given a path p in the GSS, which corresponds to an instance of alpha,
     * reduce it to N; t is the current lookahead token; there are three
     * possible outcomes: a new node is created, a new stack link is added
     * between two existing nodes, or a semantic value is merged.
     */
    private void reduceViaPath(Path p, int rule, Terminal t, PathQueue pathqueue) {
        // Apply the semantic action for the reduce action.
        
        // Note that semanticAction expects the semActions array in the
        // same order as the rule rhs; that is,
        // semActions[0] should be the leftmost action.
        // Since, p.path[0].semAction is the semantic action for the rightmost
        // symbol, we need to reverse the list.  symbol on the right-hand-side.
        // Luckily the array was constructed in that order.

        Action[] semActions = new Action[p.path.length];
        for (int i = 0; i < p.path.length; i++) {
            semActions[semActions.length-1-i] = p.path[i].semAction;
        }

        SemanticAction semAction = new SemanticAction(rule, semActions);

        int lhs = ruleLhsIndex(ruleTable[rule]);
        int rhsLength = ruleRhsLength(ruleTable[rule]);

        // let bottomSib be the bottom stack node in p
        Node bottomSib = p.bottom();

        // we want to push the next state, goto(bottomSib,N),
        // onto the stack above bottomSib
        int nextState = gotoTable[bottomSib.state][lhs];

        if ((DEBUG & DEBUG_REDUCE) != 0) {
            System.out.println("reduce with rule " + rule);
            System.out.println("        and goto " + nextState);
        }

        // check if there is already a node topSib with state
        // goto(bottomSib,N) in topmost
        for (Iterator<Node> j = topmost.iterator(); j.hasNext(); ) {
            Node topSib = (Node) j.next();

            if (topSib.state == nextState) {
                if ((DEBUG & DEBUG_REDUCE) != 0) {
                    System.out.println("state " + nextState + " already in topmost");
                }

                // check if there is already a link from topSib to bottomSib
                // if there is, merge their semantic values
                for (Link link = topSib.out; link != null; link = link.next) {
                    if (link.bottom == bottomSib) {
                        // there is already a link from topSib to bottomSib
                        // merge the competing interpretations
                        if ((DEBUG & DEBUG_REDUCE) != 0) {
                            System.out.println("link from " + topSib + " to " + bottomSib + " already found");
                            System.out.println("merge(" + lhs + ", " + link.semAction + ", " + semAction + ")");
                        }

                        if (link.semAction instanceof TerminalAction) {
                            throw new RuntimeException("Fatal error: cannot merge ambiguous terminals.  How did we get here?");
                        }

                        SemanticAction linkAction = (SemanticAction) link.semAction;

                        // prepend semAction to link's ambiguous list
                        semAction.ambiguous = linkAction.ambiguous;
                        linkAction.ambiguous = semAction;
                        return;
                    }
                }

                if ((DEBUG & DEBUG_REDUCE) != 0) {
                    System.out.println("adding link from " + topSib + " to " + bottomSib);
                }

                // there is no link from topSib to bottomSib
                // we need to create one

                // push topSib onto stack by creating a link
                Link link = addLink(bottomSib, topSib, semAction, p.span());

                // the new link might enable reductions in states whose
                // reductions we've already expanded
                enqueueLimitedReductions(bottomSib, topSib, t, pathqueue);

                return;
            }
        }

        // the next state is not already in topmost; we need to add it.
        if ((DEBUG & DEBUG_REDUCE) != 0) {
            System.out.println("pushing " + nextState);
        }

        Node topSib = new Node(nextState, rule);

        // create a link from the new node to the bottom of the stack"
        addLink(bottomSib, topSib, semAction, p.span());

        // insert topSib into topmost;
        topmost.add(topSib);

        // check if the new state can be reduced.  If so, enqueue it
        // the reduction path.
        int[] rules = reductions(topSib, t);

        for (int i = 0; i < rules.length && rules[i] != -1; i++) {
            // reduce N -> alpha
            if ((DEBUG & DEBUG_REDUCE) != 0) {
                System.out.println("secondary enqueue for reduce with rule " + rules[i]);
            }
            rhsLength = ruleRhsLength(ruleTable[rules[i]]);
            enqueuePaths(topSib, rhsLength, rules[i], null, null, pathqueue);
        }
    }

    private void enqueueLimitedReductions(Node bottom, Node top, Terminal t, PathQueue pathqueue) {
        for (Iterator<Node> i = topmost.iterator(); i.hasNext(); ) {
            Node n = (Node) i.next();

            int[] rules = reductions(n, t);

            for (int j = 0; j < rules.length && rules[j] != -1; j++) {
                // reduce N -> alpha
                if ((DEBUG & DEBUG_REDUCE) != 0) {
                    System.out.println("secondary (limited) enqueue for reduce using " + rules[j]);
                }

                int rhsLength = ruleRhsLength(ruleTable[rules[j]]);

                // enqueue <p, N -> alpha> for each path p of length |alpha|
                // from n that uses the link top -> bottom
                enqueuePaths(n, rhsLength, rules[j], top, bottom, pathqueue);
            }
        }
    }
}
