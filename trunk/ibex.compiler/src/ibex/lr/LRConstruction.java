package ibex.lr;

import ibex.types.*;
import polyglot.util.*;
import java.util.*;

import polyglot.main.Report;

public class LRConstruction {
    final static boolean LALR = true;

    Grammar g;

    int[][] gotoTable;
    Set<Action>[][] actionTable;

    List<State>[] edgeTable;
    List<Collection<State>>[] revEdgeTable;

    Collection<Conflict> conflicts;

    /**
     * Map from source grammar start symbols to augmented grammar start states.
     */
    State[] startStatesMap;

    List<State> startStates;

    List<State> states;
    Map<Set<Item>,State> stateCache;
    boolean allowNewStates = true;

    public LRConstruction(Grammar g) {
        this.g = g;

        allowNewStates = true;

        initStates();
        buildGotoStates();

        // Force an exception if we try to create any more states.
        allowNewStates = false;

        if (Report.should_report(TOPICS, 1)) {
            Report.report(1, "created " + states.size() + " states");
            Report.report(1, "    for " + g.nonterminals().size() + " nonterminals");
            Report.report(1, "        " + g.terminals().size() + " terminals");
            Report.report(1, "        " + g.rules().size() + " rules");
        }

        buildTables();
    }

    /** Initialize the state machine by creating the start states. */
    void initStates() {
        states = new ArrayList<State>();
        stateCache = new HashMap<Set<Item>, State>();
        startStatesMap = new State[g.nonterminals().size()];
        startStates = new ArrayList<State>();

        edgeTable = new List[g.terminals().size()+g.nonterminals().size()];
        revEdgeTable = new List[g.terminals().size()+g.nonterminals().size()];

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "creating start states");

        for (GLRNonterminal s : g.startSymbols()) {
            for (GLRRule r : s.rules()) {
                if (r instanceof GLRNormalRule) {
                    // Create a state for the start item.
                    // As a side effect, it is added to the states set.
                    State I = startState((GLRNormalRule) r);
                    startStatesMap[s.index()] = I;
                    startStates.add(I);
                }
            }
        }
    }

    /** Construct the goto states. */
    void buildGotoStates() {
        if (Report.should_report(TOPICS, 1))
            Report.report(1, "building states");

        // The start state(s) should already be in the list.
        LinkedList<State> statesWorklist = new LinkedList<State>(states);

        while (! statesWorklist.isEmpty()) {
            State I = statesWorklist.removeFirst();

            for (Item item : I.items()) {
                GLRSymbol X = item.afterDot();

                // create a (possibly) new state
                State J = createGotoOrShiftState(I, X, statesWorklist);

                if (J != null) {
                    // Insert I ->^X J in the edge table and
                    // insert J ->^X I in the reverse edge table.

                    if (edgeTable[symbolIndex(X)] == null) {
                        edgeTable[symbolIndex(X)] = new ArrayList<State>();
                    }

                    while (I.index() >= edgeTable[symbolIndex(X)].size()) {
                        edgeTable[symbolIndex(X)].add(null);
                    }

                    edgeTable[symbolIndex(X)].set(I.index(), J);

                    if (revEdgeTable[symbolIndex(X)] == null) {
                        revEdgeTable[symbolIndex(X)] = new ArrayList<Collection<State>>();
                    }

                    while (J.index() >= revEdgeTable[symbolIndex(X)].size()) {
                        revEdgeTable[symbolIndex(X)].add(null);
                    }

                    Collection<State> l = (Collection<State>) revEdgeTable[symbolIndex(X)].get(J.index());
                    if (l == null) {
                        l = new HashSet<State>();
                        revEdgeTable[symbolIndex(X)].set(J.index(), l);
                    }

                    l.add(I);
                }
            }
        }
    }

    /** Build the action and goto tables. */
    void buildTables() {
        if (Report.should_report(TOPICS, 1))
            Report.report(1, "building tables");

        conflicts = new HashSet<Conflict>();

        actionTable = new Set[states.size()][g.terminals().size()];
        gotoTable = new int[states.size()][g.nonterminals().size()];

        for (int i = 0; i < actionTable.length; i++) {
            for (int j = 0; j < actionTable[i].length; j++) {
                actionTable[i][j] = new HashSet<Action>();
            }
        }

        for (int i = 0; i < states.size(); i++) {
            State I = (State) states.get(i);

            for (Item item : I.items()) {
                GLRSymbol X = item.afterDot();

                if (X == null) {
                    for (GLRTerminal t : item.lookahead) {
                        addAction(I, t, new Reduce(item.rule));
                    }
                }
                else if (X.equals(g.eofSymbol())) {
                    addAction(I, g.eofSymbol(), new Accept(item.rule.lhs()));
                }
                else {
                    State J = gotoOrShiftState(I, X);

                    if (J == null) {
                        throw new InternalCompilerError(
                                                        "Unreachable next state for " + I + ": " + item);
                    }

                    // if J is a new state, it will be appended to the
                    // states list and we'll process it soon

                    if (X instanceof GLRNonterminal) {
                        setGoto(I, (GLRNonterminal) X, J);
                    }
                    else {
                        addAction(I, (GLRTerminal) X,
                                  new Shift(J, (GLRTerminal) X));
                    }
                }
            }
        }

        if (Report.should_report(TOPICS, 1)) {
            Report.report(1, "done building tables");
            if (conflicts.size() > 0) {
                Report.report(1, conflicts.size() + " LR conflicts found:");
                int k = 0;
                for (Conflict c : conflicts) {
                    if (Report.should_report("merge", 4) ||
                            Report.should_report(TOPICS, 2)) {
                        Report.report(4, " Conflict #" + (++k));
                        c.dump();
                    }
                }
            }
        }
    }

    Collection<Conflict> conflicts() {
        return conflicts;
    }

    Set<Action> actions(State I, GLRTerminal t) {
        return actionTable[I.index()][t.index()];
    }

    void addAction(State I, GLRTerminal t, Action a) {
        Set<Action> c = actions(I, t);

        final Collection<String> TOPICS = new ArrayList<String>(LRConstruction.TOPICS);
        TOPICS.add("prec");

        if (! c.isEmpty()) {
            // Check if the conflict can be resolved using operator precedence
            if (a instanceof Shift) {
                Shift si = (Shift) a;
                GLRTerminal ti = si.terminal;

                for (Action aj : c) {
                    GLRNormalRule rule = null;

                    if (aj instanceof Reduce) {
                        // shift-reduce
                        Reduce rj = (Reduce) aj;
                        rule = rj.rule;
                    }
                    else if (aj instanceof Accept) {
                        // shift-reduce
                        Accept rj = (Accept) aj;
                        GLRNonterminal A = rj.nonterminal;
                        if (A.rules().size() != 1) {
                            throw new InternalCompilerError("Start symbol " + A + " should have exactly 1 rule.");
                        }
                        rule = (GLRNormalRule) A.rules().get(0);
                    }

                    //                    if (rule != null) {
                    //                        if (rule.prec > si.prec || (rule.prec == si.prec && si.assoc == -1)) {
                    //                            // reduce; just return without adding the shift action
                    //        if (Report.should_report(TOPICS, 2)) {
                    //            Report.report(2, "resolving conflict between:");
                    //            Report.report(2, "     " + si);
                    //            Report.report(2, " and " + aj);
                    //            Report.report(2, " in favor of reduce/accept");
                    //        }
                    //                            
                    //                            return;
                    //                        }
                    //                        else if (rule.prec < si.prec || (rule.prec == si.prec && si.assoc == 1)) {
                    //                            // shift; remove the reduce rule and fall through to add the shift
                    //        if (Report.should_report(TOPICS, 2)) {
                    //            Report.report(2, "resolving conflict between:");
                    //            Report.report(2, "     " + si);
                    //            Report.report(2, " and " + aj);
                    //            Report.report(2, " in favor of shift");
                    //        }
                    //                            j.remove();
                    //                        }
                    //                        else {
                    //                            // ambiguous; keep both actions
                    //                        }
                    //                    }
                }
            }
            else if (a instanceof Reduce) {
                Reduce ri = (Reduce) a;
                GLRNormalRule rule = ri.rule;

                for (Action aj : c) {
                    if (aj instanceof Shift) {
                        // shift-reduce
                        Shift sj = (Shift) aj;
                        GLRTerminal tj = sj.terminal;

                        //                        if (rule.prec > sj.prec || (rule.prec == sj.prec && sj.assoc == -1)) {
                        //                            // reduce; remove the shift rule and fall through to add the reduce
                        //        if (Report.should_report(TOPICS, 2)) {
                        //            Report.report(2, "resolving conflict between:");
                        //            Report.report(2, "     " + ri);
                        //            Report.report(2, " and " + aj);
                        //            Report.report(2, " in favor of reduce");
                        //        }
                        //                            j.remove();
                        //                        }
                        //                        else if (rule.prec < sj.prec || (rule.prec == sj.prec && sj.assoc == 1)) {
                        //                            // shift; just return without adding the reduce action
                        //        if (Report.should_report(TOPICS, 2)) {
                        //            Report.report(2, "resolving conflict between:");
                        //            Report.report(2, "     " + ri);
                        //            Report.report(2, " and " + aj);
                        //            Report.report(2, " in favor of shift");
                        //        }
                        //                            return;
                        //                        }
                        //                        else {
                        //                            // ambiguous; keep both actions
                        //                        }
                    }
                }
            }
            else if (a instanceof Accept) {
                Accept ri = (Accept) a;
                GLRNonterminal A = ri.nonterminal;
                if (A.rules().size() != 1) {
                    throw new InternalCompilerError("Start symbol " + A + " should have exactly 1 rule.");
                }

                for (Action aj : c) {
                    if (aj instanceof Shift) {
                        // shift-reduce
                        Shift sj = (Shift) aj;
                        GLRTerminal tj = sj.terminal;

                        //                        if (rule.prec > sj.prec || (rule.prec == sj.prec && sj.assoc == -1)) {
                        //                            // reduce; remove the shift rule and fall through to add the reduce
                        //        if (Report.should_report(TOPICS, 2)) {
                        //            Report.report(2, "resolving conflict between:");
                        //            Report.report(2, "     " + ri);
                        //            Report.report(2, " and " + aj);
                        //            Report.report(2, " in favor of accept");
                        //        }
                        //                            j.remove();
                        //                        }
                        //                        else if (rule.prec < sj.prec || (rule.prec == sj.prec && sj.assoc == 1)) {
                        //                            // shift; just return without adding the reduce action
                        //        if (Report.should_report(TOPICS, 2)) {
                        //            Report.report(2, "resolving conflict between:");
                        //            Report.report(2, "     " + ri);
                        //            Report.report(2, " and " + aj);
                        //            Report.report(2, " in favor of shift");
                        //        }
                        //                            return;
                        //                        }
                        //                        else {
                        //                            // ambiguous; keep both actions
                        //                        }
                    }
                }
            }
        }

        c.add(a);

        if (c.size() > 1) {
            conflicts.add(new Conflict(I, t, this));
        }
    }

    State gotoState(State I, GLRNonterminal t) {
        return (State) states.get(gotoTable[I.index()][t.index()]);
    }

    void setGoto(State I, GLRNonterminal t, State J) {
        gotoTable[I.index()][t.index()] = J.index();
    }

    State gotoOrShiftState(State I, GLRSymbol X) {
        if (X != null && edgeTable[symbolIndex(X)] != null &&
                I.index() < edgeTable[symbolIndex(X)].size()) {
            return (State) edgeTable[symbolIndex(X)].get(I.index());
        }

        return null;
    }

    Collection<State> reverseGotoOrShiftStates(State I, GLRSymbol X) {
        if (X != null && revEdgeTable[symbolIndex(X)] != null &&
                I.index() < revEdgeTable[symbolIndex(X)].size()) {
            Collection<State> c = revEdgeTable[symbolIndex(X)].get(I.index());
            if (c != null) return c;
        }

        return Collections.EMPTY_LIST;
    }

    void dump() {
        for (int i = 0; i < g.terminals().size(); i++) {
            GLRTerminal t = (GLRTerminal) g.terminals().get(i);
            System.out.println("Terminal " + i + ": " + t);
        }
        System.out.println();

        for (int i = 0; i < g.nonterminals().size(); i++) {
            GLRNonterminal t = (GLRNonterminal) g.nonterminals().get(i);
            System.out.println("Nonterminal " + i + ": " + t);
        }
        System.out.println();

        for (int i = 0; i < g.rules().size(); i++) {
            GLRNormalRule t = (GLRNormalRule) g.rules().get(i);
            System.out.println("Rule " + t);
        }
        System.out.println();

        for (int i = 0; i < states.size(); i++) {
            State I = (State) states.get(i);

            I.dump();

            System.out.println();

            int[] gotoRow = gotoTable[I.index()];

            for (int j = 0; j < gotoRow.length; j++) {
                // Assume we never goto state 0.
                // This should be true since state 0 is a start state
                // and any reductions of the initial rule in the
                // state will transition to an accept state.
                //
                // This isn't important anyway since we're just dumping
                // debug output.
                if (gotoRow[j] != 0) {
                    System.out.println("    on " + g.nonterminals().get(j) +
                                       ", goto " + states.get(gotoRow[j]));
                }
            }

            Set<Action>[] actionRow = actionTable[I.index()];

            for (int j = 0; j < actionRow.length; j++) {
                if (! actionRow[j].isEmpty()) {
                    System.out.println("    on " + g.terminals().get(j) +
                                       ", " + actionRow[j]);
                }
            }

            System.out.println();
        }
    }

    int symbolIndex(GLRSymbol X) {
        if (X instanceof GLRTerminal) {
            return X.index() + g.nonterminals().size();
        }
        return X.index();
    }

    Item startItem(GLRNormalRule rule) {
        return new Item(rule, 0, Collections.EMPTY_SET);
    }

    State startState(GLRNormalRule rule) {
        return closure(Collections.singleton(startItem(rule)), null);
    }

    State createStateForItemSet(Set<Item> items, List<State> statesWorklist) {
        State s = (State) stateCache.get(items);

        if (s == null) {
            s = new State(states.size(), items);
            states.add(s);
            stateCache.put(items, s);

            if (statesWorklist != null) {
                statesWorklist.add(s);
            }
        }
        else if (LALR) {
            // LALR(1): merge items
            boolean changed = false;

            for (Item item : items) {
                for (Item sitem : s.items) {
                    if (item.equals(sitem)) {
                        changed |= sitem.lookahead.addAll(item.lookahead);
                    }
                }
            }

            if (changed && statesWorklist != null) {
                statesWorklist.add(s);
            }
        }

        return s;
    }

    State createGotoOrShiftState(State I, GLRSymbol X, List<State> statesWorklist) {
        if (X == null || X.equals(g.eofSymbol())) {
            return null;
        }

        Set<Item> J = new HashSet<Item>();
        for (Item item : I.items()) {
            if (item.afterDot() != null && item.afterDot().equals(X)) {
                J.add(item.shiftDot());
            }
        }

        if (J.isEmpty()) {
            return null;
        }

        return closure(J, statesWorklist);
    }

    State closure(Set<Item> items, List<State> statesWorklist) {
        ArrayList<Item> worklist = new ArrayList<Item>(items);
        Set<Item> closure = new HashSet<Item>(items);

        while (worklist.size() > 0) {
            Item item = worklist.remove(worklist.size()-1);

            // item == (A -> alpha . X beta, z)
            GLRSymbol X = item.afterDot();

            List<GLRSymbol> beta = new ArrayList<GLRSymbol>();

            if (item.dot < item.rule.rhs().size()) {
                beta.addAll(item.rule.rhs().subList(item.dot+1, item.rule.rhs().size()));
            }

            Set<GLRTerminal> first = first(beta, item.lookahead);

            if (X instanceof GLRNonterminal) {
                // item == (A -> alpha . X beta, z)
                List<GLRNormalRule> rules = ((GLRNonterminal) X).rules();

                for (GLRNormalRule r : rules) {
                    // r == X -> gamma
                    boolean changed = false;
                    boolean found = false;

                    List<Item> addToClosure = new ArrayList<Item>();

                    for (Iterator<Item> k = closure.iterator(); k.hasNext(); ) {
                        Item ritem = (Item) k.next();
                        if (ritem.rule == r && ritem.dot == 0) {
                            found = true;
                            changed |= ritem.lookahead.addAll(first);

                            if (changed) {
                                // The hash code may have changed; remove from
                                // the closure, to add back after the iterator
                                // finishes.  If we don't remove and add back,
                                // the item could be in the wrong hash
                                // bucket and won't be found.
                                k.remove();
                                addToClosure.add(ritem);
                            }
                        }
                    }

                    if (! found) {
                        Item ritem = new Item((GLRNormalRule) r, 0, first);
                        closure.add(ritem);
                        worklist.add(ritem);
                    }

                    closure.addAll(addToClosure);
                    worklist.addAll(addToClosure);
                }
            }
        }

        return createStateForItemSet(closure, statesWorklist);
    }

    static Set<GLRTerminal> first(List<GLRSymbol> beta, Set<GLRTerminal> z) {
        Set<GLRTerminal> first = new HashSet<GLRTerminal>();

        for (GLRSymbol s : beta) {
            first.addAll(s.first());
            if (!s.isNullable()) {
                return first;
            }
        }

        first.addAll(z);

        return first;
    }

    static Collection TOPICS = Arrays.asList( new String[] { "lr", "ibex" });
}
