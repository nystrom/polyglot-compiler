package ibex.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Scanner for the self-hosted parser. Wraps a java.io.Reader and handles backtracking.
 * Called from the Thorn-land scannerless parser. This interface should be generalized to
 * support matching on arbitrary object sequences.
 */
public class Scanner implements IMatchContext {
    /** File contents. */
    protected final byte[] input;

    /** Position in input of the next character to read. */
    protected int pos;

    /** Stack of positions used for rolling back on match failure. */
    protected final List<Integer> posStack;

    /** Memoization table. Map from rule*position to a MemoEntry */
    private final Map<Rule, MemoEntry>[] memo;

    /** Map from positions to Head objects currently involved in left recursion. */
    private final Head[] heads;

    /**
     * Stack of rules, used to detect left recursion and record the seed AST for left
     * recursive rules.
     */
    private LR ruleStack;

    /** Sentinel object indicating match failure. Must be equal only to itself. */
    final static IMatchResult FAIL = new Failure(null);

    public Scanner(byte[] input) {
        this.input = input;

        this.pos = 0;
        this.posStack = new ArrayList<Integer>();

        this.memo = new Map[input.length+1];
        this.heads = new Head[input.length+1];
    }
    

    private void advance(final int length) {
        pos += length;
    }

    int lookaheadIndex = -1;

    /** Save the current position on the stack. Must be matched with a pop (or restore). */
    public void save() {
        posStack.add(pos);
    }

    /** Save the current position on the stack. Must be matched with a pop (or restore). */
    public void saveForLookahead() {
        if (lookaheadIndex >= 0)
            lookaheadIndex = posStack.size();
        save();
    }

    /** Restore the previous position from the stack. */
    public void restore() {
        pos = posStack.get(posStack.size() - 1);
        pop();
    }
    
    static final class Rule {
        final String ruleName;
        final IAction body;

        Rule(final String key, final IAction body_) {
            ruleName = key;
            body = body_;
            assert body_ != null;
            assert key != null;
        }

        IMatchResult eval(final IMatchContext context) {
            try {
                Object result = body.apply(context);
                return new Success(result);
            }
            catch (MatchFailureException e) {
                return FAIL;
            }
        }

        @Override
        public int hashCode() {
            return ruleName.hashCode();
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof Rule && ruleName.equals(((Rule) o).ruleName);
        }

        @Override
        public String toString() {
            return ruleName;
        }
    }

    /** A rule stack entry. */
    static final class LR {
        /**
         * AST used as a seed for left recursion. The initial parse for the associated
         * rule.
         */
        IMatchResult seed;

        /** Rule being evaluated. */
        Rule rule;

        /**
         * Head rule of the left recursion and the set of rules involved in the recursion.
         * This is the rule that started the left recursion.
         */
        Head head;

        /** The next older item on the stack. */
        LR next;

        /** Starting position of this rule; used for error reporting. */
        int pos;

        LR(final IMatchResult seed_, final Rule rule_, final Head head_, final LR next_, final int pos_) {
            this.seed = seed_;
            this.rule = rule_;
            this.head = head_;
            this.next = next_;
            this.pos = pos_;
        }

        @Override
        public String toString() {
            return "LR(" + seed + ", " + rule + ", " + head + ")" + (next != null ? " -> " + next : "");
        }

        // Might be bad if we start to parallelize, but using string
        // builder gives a 33% speed-up on Tobias' machine
        StringBuilder sb = new StringBuilder();

        public String stackString() {
            sb.append("[");
            sb.append(rule.ruleName);
            sb.append(" ");
            sb.append(pos);
            sb.append("]");
            if (next != null) {
                sb.append(" -> ");
                sb.append(next.stackString());
            }
            final String result = sb.toString();
            sb.delete(0, sb.length());
            return result;
            // Old code
            // return rule + "[" + pos + "]" + (next != null ? " -> " + next.stackString()
            // : "");
        }
    }

    /** The head rule is the rule that started the left recursion. */
    static class Head {
        /** Head rule of the left recursion. */
        Rule rule;

        /** Rules involved in the left recursion. */
        Set<Rule> involvedSet;

        /** Subset of involvedSet remaining to be evaluated. */
        Set<Rule> evalSet;

        Head(final Rule rule_, final Set<Rule> i, final Set<Rule> e) {
            this.rule = rule_;
            this.involvedSet = i;
            this.evalSet = e;
        }

        @Override
        public String toString() {
            return "Head(" + rule + ", " + involvedSet + ", " + evalSet + ")";
        }
    }

    /** Look up a memo entry, possibly handling left recursion. */
    private MemoEntry recall(final Rule rule, final int beginPos) {
        MemoEntry m = memo[beginPos] != null ? memo[beginPos].get(rule) : null;
        final Head h = heads[beginPos];

        // If not growing a seed parse, just return what is stored in the memo table.
        if (h == null) return m;

        // Do not eval any rule not involved in this left recursion.
        if (m == null && !h.rule.equals(rule) && !h.involvedSet.contains(rule)) { return new MemoEntry(FAIL, beginPos); }

        // Allow involved rules to be evaluated, but only once during seed-growing
        // iteration.
        if (h.evalSet.contains(rule)) {
            h.evalSet.remove(rule);
            final IMatchResult answer = rule.eval(this);
            ///			assert answer != FAIL || this.pos == pos : "failed but advanced: answer=" + answer + " pos=" + pos + "->" + this.pos;
            m = new MemoEntry(answer, this.pos);
            return m;
        }

        return m;
    }

    /** Get the failure object. */
    public IMatchResult fail() {
        return FAIL;
    }

    /** Apply a rule. */
    public <T> T applyRule(final String key, final IAction<T> body) throws MatchFailureException {
        final Rule rule = new Rule(key, body);
        int beginPos = this.pos;
        System.out.println("trying " + key + " at " + beginPos + " (" + (beginPos < input.length ? (char) input[beginPos] : '$') + ")");
        IMatchResult r = applyRule(rule);
        if (r instanceof Success) {
            System.out.println("matched " + key + " at " + beginPos + " (" + (beginPos < input.length ? (char) input[beginPos] : '$') + ")");
            return (T) ((Success) r).o;
        }
        throw new MatchFailureException();
    }

    private final boolean TRACE = false;

    /**
     * Apply a rule. The application first checks if the rule has already been applied at
     * the same location. We also handle left recursion using the algorithm of Warth,
     * Douglass, and Millstein, PEPM'08.
     *
     * Example:
     *
     * Primary = Primary . Id / Id
     *
     * Match against "x.y".
     *
     * On first visit, push Primary onto the stack. Recurse on the RHS, which immediate
     * invokes Primary again. This time, recall() returns a memo entry with (Primary,
     * position) -> LR(seed=FAIL, head=null) Now, setupLR is called, setting
     * head(position) -> Primary and returning the seed (FAIL). We next attempt the other
     * case (Id), and match "x". We return back to applyRule with Id(x) and update the
     * seed: (Primary,position) => LR(seed=Id(x), head=Primary). lrAnswer is then called,
     * which calls growLR, which reevaluates the rule. This time, we try Primary . Id
     * again. Primary is matched by Id(x), found in the memo table. Then ".y" is matched,
     * so Primary --> Primary . Id
     */
    
    private IMatchResult applyRule(final Rule rule) {
        final int beginPos = this.pos;

        /// if (TRACE) Globals.reporter().trace(1, "parse", position(beginPos, beginPos), "attempting rule " + rule);

        // Check if the rule has been visited before at this position; possibly evaluate
        // rules involved in
        // left recursion.
        MemoEntry m = recall(rule, beginPos);

        if (m == null) {
            // Create a new LR and push onto rule stack.
            final LR lr = new LR(FAIL, rule, null, ruleStack, beginPos);
            ruleStack = lr;

            // Tobias is optimising
            ////		if (Globals.reporter().shouldTrace(1, "parse.lr")) Globals.reporter().trace(1, "parse.lr", position(beginPos, beginPos),
            ///			"rule stack: " + ruleStack.stackString());

            m = new MemoEntry(lr, beginPos);
            if (memo[beginPos] == null)
                memo[beginPos] = new HashMap<Rule,MemoEntry>();
            memo[beginPos].put(rule, m);

            final IMatchResult answer = rule.eval(this);
            ///			assert answer != FAIL || this.pos == beginPos : "failed but advanced: answer=" + answer + " pos=" + beginPos + "->" + this.pos;

            m.endPos = this.pos;

            // Pop the rule stack.
            ruleStack = ruleStack.next;

            ////	if (Globals.reporter().shouldTrace(1, "parse.lr")) Globals.reporter().trace(1, "parse.lr", position(beginPos, this.pos),
            ///		"returning stack: " + (ruleStack != null ? ruleStack.stackString() : "null"));

            if (lr.head != null) {
                // There was left-recursion below. Set the seed and grow from there.
                assert m.lr == lr;
                assert m.answer == null;
                lr.seed = answer;
                final IMatchResult o = lrAnswer(rule, beginPos, m);
                ///			if (TRACE) Globals.reporter().trace(1, "parse", position(beginPos, m.endPos), "matched rule " + rule + ": computed LR " + answer + "->" + o);
                return o;
            }
            m.lr = null;
            m.answer = answer;
            ///			if (TRACE) Globals.reporter().trace(1, "parse", position(beginPos, m.endPos), "matched rule " + rule + ": computed " + answer);
            return answer;
        }
        // detect left recursion, fail; match the first non-left recursive rule, then
        // backup to reapply the rule again to grow with that seed
        /// assert m.endPos >= this.pos;
        /// assert m.answer != FAIL || m.endPos == this.pos;
        pos = m.endPos;
        if (m.lr != null) {
            ///assert ruleStack != null;
            setupLR(rule, m.lr);
            ///	if (TRACE) Globals.reporter().trace(1, "parse", position(beginPos, m.endPos), "matched rule " + rule + ": LR");
            return m.lr.seed;
        }
        ///if (TRACE) Globals.reporter().trace(1, "parse", position(beginPos, m.endPos), "matched rule " + rule + ": memoized " + m.answer);
        return m.answer;
    }

    /** Setup data structures for handling left recursion. */
    private void setupLR(final Rule rule, final LR lr) {
        if (lr.head == null) lr.head = new Head(rule, new HashSet<Rule>(), new HashSet<Rule>());
        LR s = ruleStack;
        assert ruleStack != null;
        // Note: it is important that 'rule' not be added to the involvedSet; otherwise
        // the initial parse will not be done.
        while (s != null && s.head != lr.head) {
            s.head = lr.head;
            lr.head.involvedSet.add(s.rule);
            s = s.next;
        }

        // TODO:
        // record a stack depth in each entry.
        // when popping, clear the number.

        // contextual left recursion
        // left recursive: find some indirect result
        // specified several different ways to calculate the seed
        // use wrong seed

        // A -> B 0 / 1
        // B -> A 0 / 1
        //
        // C -> A 2 / B 3
    }

    /** Compute a left recursive AST. */
    private IMatchResult lrAnswer(final Rule rule, final int position, final MemoEntry m) {
        ///assert m.lr != null && position <= this.pos;
        final Head h = m.lr.head;
        if (!h.rule.equals(rule)) return m.lr.seed;

        m.answer = m.lr.seed;
        m.lr = null;
        if (m.answer == FAIL) return FAIL;
        return growLR(rule, position, m, h);
    }

    /** Grow the left recursion seed. */
    IMatchResult growLR(final Rule rule, final int position, final MemoEntry m, final Head head) {
        heads[position] = head;

        while (true) {
            // Backup to the the position where the left recursion started.
            this.pos = position;
            head.evalSet = new HashSet<Rule>(head.involvedSet);

            // Reparse the rule, trying to grow the seed.
            final IMatchResult answer = rule.eval(this);
            ///assert answer != FAIL || this.pos == position : "failed but advanced: answer=" + answer + " pos=" + position + "->" + this.pos;

            // If the seed didn't grow, break; restore the position to the point after the
            // last successful parse and return the last answer.
            if (answer == FAIL || this.pos <= m.endPos)
                break;

            m.answer = answer;
            m.endPos = this.pos;
        }

        heads[position] = null;
        this.pos = m.endPos;
        return m.answer;
    }

    static class MemoEntry {
        int endPos;
        IMatchResult answer;
        LR lr;

        MemoEntry(final int pos) {
            this.endPos = pos;
        }

        MemoEntry(final LR lr_, final int pos) {
            this.lr = lr_;
            this.endPos = pos;
        }

        MemoEntry(final IMatchResult ans, final int pos) {
            this.answer = ans;
            this.endPos = pos;
        }

        @Override
        public String toString() {
            if (endPos >= 0) {
                if (lr != null) return "Match " + lr + " pos=" + endPos;
                return "Match |" + answer + "| pos=" + endPos;
            }
            return "Match FAILED";
        }
    }

    /** Pop the position stack, returning the argument (the semantic action result) */
    public <T> T accept(final T o) {
        assert !(o instanceof MemoEntry);
        pop();
        return o;
    }
    
    public boolean accept(boolean o) {
        pop();
        return o;
    }
    public char accept(char o) {
        pop();
        return o;
    }
    public byte accept(byte o) {
        pop();
        return o;
    }
    public short accept(short o) {
        pop();
        return o;
    }
    public int accept(int o) {
        pop();
        return o;
    }
    public long accept(long o) {
        pop();
        return o;
    }
    public float accept(float o) {
        pop();
        return o;
    }
    public double accept(double o) {
        pop();
        return o;
    }

    /** Pop the position stack. Do not update the current position. */
    public void pop() {
        ///Globals.stats().accumulate("pop", 1);
        ///if (Globals.reporter().shouldTrace(1, "parse.stack")) Globals.reporter().trace(1, "parse.stack", "pop: " + this);
        posStack.remove(posStack.size() - 1);
        if (lookaheadIndex == posStack.size()) lookaheadIndex = -1;
    }

    private boolean isLookahead() {
        return lookaheadIndex >= 0;
    }

    private void addError(final ParseError e) {
        /*if (Globals.reporter().shouldTrace(1, "parse.error")) {
			if (TRACE) Globals.reporter().trace(1, "parse.error", position(e.pos, e.endPos), e.msg);
			if (TRACE) Globals.reporter().trace(1, "parse.error", position(e.pos, e.endPos),
					"lr stack at error: " + (ruleStack != null ? ruleStack.stackString() : "null"));
		}*/

        if (isLookahead()) return;
        // errors.clear();
        removeErrors(0, e.endPos);
        errors.add(e);
    }

    public void flushErrors() {
        if (errors.isEmpty()) {
            addError(new ParseError(0, input.length - 1, "Parse error"));
        }
        for (final ParseError e : errors) {
            reportError(e);
            break;
        }
    }

    protected void reportError(ParseError e) {
        System.err.println(e.pos + ".." + e.endPos + ": " + e.msg);
    }

    protected static class ParseError {
        int pos;
        int endPos;
        String msg;

        ParseError(final int pos_, final int end, final String msg_) {
            this.pos = pos_;
            this.endPos = end;
            this.msg = msg_;
        }
    }

    List<ParseError> errors = new ArrayList<ParseError>();

    void removeErrors(final int fromPos, final int toPos) {
        ///if (TRACE) Globals.reporter().trace(1, "parse.errors", position(fromPos, toPos), "remove error " + fromPos + ".." + toPos);
        for (final ListIterator<ParseError> i = errors.listIterator(); i.hasNext();) {
            final ParseError e = i.next();
            ///		if (TRACE) Globals.reporter().trace(1, "parse.errors", position(e.pos, e.endPos), "this error " + e.pos + ".." + e.endPos);
            if (fromPos <= e.pos && e.endPos <= toPos) i.remove();
        }
    }

    /**
     * Return the most specific rule that starts BEFORE the current position. This
     * heuristic seems to produce decent error messages. e.g., if parsing 'x+*y' against
     * MulExp, the error will be caught at +, but the error will be reported for MulExp.
     *
     * @param expected
     *            TODO
     */
    String expectedErrorMessage(final int position, String expected) {
        String whileParsing = null;

        for (LR lr = ruleStack; lr != null; lr = lr.next) {
            if (lr.pos == position) {
                expected = lr.rule.ruleName;
                if (lr.next != null) {
                    whileParsing = lr.next.rule.ruleName;
                }
            }
        }

        if (expected != null && whileParsing != null) {
            if (expected.equals(whileParsing)) {
                for (LR lr = ruleStack; lr != null; lr = lr.next) {
                    if (lr.pos == position && !lr.rule.ruleName.equals(whileParsing)) {
                        expected = lr.rule.ruleName;
                        break;
                    }
                }
            }
            return "; expected '" + expected + "' while parsing '" + whileParsing + "'";
        }

        if (expected != null) return "; expected '" + expected + "'";

        return "";
    }
    
    /** Check if next token is in the given range. 
     * @throws MatchFailureException */
    public char nextMatchesRange(final char start, final char end) throws MatchFailureException {
        if (pos >= input.length) {
            addError(new ParseError(pos, pos, "Unexpected end of file" + expectedErrorMessage(pos, start + ".." + end)));
            throw new MatchFailureException();
        }

        final int peek = input[pos];

        final char ch1 = start;
        final char ch2 = end;

        if (ch1 <= peek && peek <= ch2) {
            ///if (Globals.reporter().shouldTrace(2, "parse.nextMatches")) Globals.reporter().trace(2, "parse.nextMatchesRange", "matched |" + (char) peek + "|");
            advance(1);
            ///Globals.evaluator();
            return (char) peek;
        }
        ///if (Globals.reporter().shouldTrace(2, "parse.nextMatches")) Globals.reporter().trace(2, "parse.nextMatchesRange", "not matched |" + start + "|..|" + end + "|");
        addError(new ParseError(pos, pos, "Could not match input against range '" + start + "'..'" + end + "'" + expectedErrorMessage(pos, null)));
        throw new MatchFailureException();
    }
    
    public char nextMatches(final char ch) throws MatchFailureException {
        int i = pos;
        
        if (i < input.length) {
            final char c1 = ch;
            final int c2 = input[i];
            if (c1 == c2) {
                advance(1);
                return ch;
            }
            else {            
                final int peek = input[i];
                addError(new ParseError(pos, i, "Unexpected character '" + (char) peek + "'" + expectedErrorMessage(pos, String.valueOf(ch))));
                throw new MatchFailureException();
            }
        }
        else {        
            addError(new ParseError(pos, i, "Unexpected end of file" + expectedErrorMessage(pos, String.valueOf(ch))));
            throw new MatchFailureException();
        }
    }

    /**
     * Check if the next characters match the string; if so, advance past it and return a
     * MatchResult; otherwise return null.
     */
    public String nextMatches(final CharSequence str) throws MatchFailureException {
        int i, j;
        for (i = pos, j = 0; i < input.length && j < str.length(); i++, j++) {
            final char c1 = str.charAt(j);
            final char c2 = (char) input[i];
            if (c1 != c2) break;
        }

        if (j == str.length()) {
            ///	if (Globals.reporter().shouldTrace(2, "parse.nextMatches")) Globals.reporter().trace(2, "parse.nextMatches", "matched |" + str + "|");
            if (str instanceof String) {
                advance(j);
                return (String) str;
            }
            StringBuilder sb = new StringBuilder();
            for (int k = pos; k < str.length(); k++) {
                sb.append((char) input[k]);
            }
            advance(j);
            return sb.toString();
        }

        ///if (Globals.reporter().shouldTrace(2, "parse.nextMatches")) Globals.reporter().trace(2, "parse.nextMatches", "not matched |" + str + "|");
        if (i >= input.length) {
            addError(new ParseError(pos, i, "Unexpected end of file" + expectedErrorMessage(pos, String.valueOf(str))));
            throw new MatchFailureException();
        }
        
        final int peek = input[i];
        addError(new ParseError(pos, i, "Unexpected character '" + (char) peek + "'" + expectedErrorMessage(pos, String.valueOf(str))));
        throw new MatchFailureException();
    }

    /** Consume the next character, or throw NoMatch if at end of file. 
     * @throws MatchFailureException */
    public char nextMatchesAny() throws MatchFailureException {
        if (pos < input.length) {
            final int peek = input[pos];
            advance(1);
            ///if (Globals.reporter().shouldTrace(2, "parse.nextMatches")) Globals.reporter().trace(2, "parse.nextMatchesAny", "matched |" + (char) peek + "|");
            return (char) peek;
        }

        ///	if (Globals.reporter().shouldTrace(2, "parse.nextMatches")) Globals.reporter().trace(2, "parse.nextMatchesAny", "not matched, EOF");
        addError(new ParseError(pos, pos, "Unexpected end of file" + expectedErrorMessage(pos, null)));
        throw new MatchFailureException();
    }

    static boolean debug = false;

    @Override
    public String toString() {
        if (!debug) return "scanner";
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < pos; i++) {
            buffer.append((char) input[i]);
        }
        return "scanner: " + pos + " |" + buffer.toString() + "|";
    }
}
