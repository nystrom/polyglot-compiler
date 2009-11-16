package ibex.lr;

import ibex.lr.GLRNonterminal.Kind;
import ibex.lr.GLRRule.Assoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import polyglot.main.Report;
import polyglot.util.InternalCompilerError;

public class LRConstruction {
    final static boolean LALR = true;

    Grammar g;
    GLR glr;

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
    Map<Set<Item>, State> stateCache;
    boolean allowNewStates = true;

    public LRConstruction(GLR glr) {
        this.glr = glr;
        this.g = glr.g;

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

        edgeTable = new List[g.terminals().size() + g.nonterminals().size()];
        revEdgeTable = new List[g.terminals().size() + g.nonterminals().size()];

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "creating start states");

        for (GLRNonterminal s : g.startSymbols()) {
            for (GLRRule r : s.rules()) {
                // Create a state for the start item.
                // As a side effect, it is added to the states set.
                State I = startState((GLRRule) r);
                startStatesMap[s.index()] = I;
                startStates.add(I);
            }
        }
        
        for (GLRRule r : g.rules()) {
            if (r.lhs.kind != Kind.NORMAL) {
                // Create a state for the lookahead rule.
                // As a side effect, it is added to the states set.
                State I = lookaheadStartState((GLRRule) r);
                startStatesMap[r.lhs.index()] = I;
                startStates.add(I);
            }
        }
    }

    /** Construct the goto states. */
    void buildGotoStates() {
        if (Report.should_report(TOPICS, 1))
            Report.report(1, "building states");

        // The start state(s) should already be in the list.
        LinkedList<State> statesWorklist = new LinkedList<State>(states);

        while (!statesWorklist.isEmpty()) {
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
                    if (item.rule.lhs().kind != Kind.NORMAL) {
                        for (GLRTerminal t : g.terminals) {
                            addAction(I, t, new Accept(item.rule.lhs()));
                        }
                    }
                    else if (item.lookahead != null) {
                        for (GLRTerminal t : item.lookahead) {
                            addAction(I, t, new Reduce(item.rule));
                        }
                    }
                    else {
                        for (GLRTerminal t : g.terminals) {
                            addAction(I, t, new Reduce(item.rule));
                        }
                    }
                }
                else if (X.equals(g.eofSymbol())) {
                    addAction(I, g.eofSymbol(), new Accept(item.rule.lhs()));
                }
                else {
                    State J = gotoOrShiftState(I, X);

                    if (J == null) {
                        throw new InternalCompilerError("Unreachable next state for " + I + ": " + item);
                    }

                    // if J is a new state, it will be appended to the
                    // states list and we'll process it soon

                    if (X instanceof GLRNonterminal) {
                        GLRNonterminal A = (GLRNonterminal) X;
                        switch (A.kind) {
                        case POS:
                            for (GLRTerminal t : g.terminals()) {
                                addAction(I, t, new Lookahead(A.rules.get(0), false));
                                addAction(I, t, new Reduce(A.rules.get(0)));
                            }
                            setGoto(I, A, J);
                            break;
                        case NEG:
                            for (GLRTerminal t : g.terminals()) {
                                addAction(I, t, new Lookahead(A.rules.get(0), true));
                                addAction(I, t, new Reduce(A.rules.get(0)));
                            }
                            setGoto(I, A, J);
                            break;
                        default:
                            setGoto(I, A, J);
                            break;
                        }
                    }
                    else if (X instanceof GLRTerminal) {
                        GLRTerminal x = (GLRTerminal) X;
                        addAction(I, x, new Shift(J, x, item.rule.prec, item.rule.assoc));
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
                    if (Report.should_report("merge", 4) || Report.should_report(TOPICS, 2)) {
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

    static boolean ltprec(int p1, int p2) {
        return p1 > p2;
    }

    static boolean gtprec(int p1, int p2) {
        return p1 < p2;
    }

    void addAction(State I, GLRTerminal t, Action a) {
        Set<Action> c = actions(I, t);

        final Collection<String> TOPICS = new ArrayList<String>(LRConstruction.TOPICS);
        TOPICS.add("prec");

        if (!c.isEmpty()) {
            // Check if the conflict can be resolved using operator precedence
            if (a instanceof Shift) {
                Shift si = (Shift) a;
                GLRTerminal ti = si.terminal;

                for (Iterator<Action> j = c.iterator(); j.hasNext(); ) {
                    Action aj = j.next();
                    GLRRule rule = null;

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
                        rule = (GLRRule) A.rules().get(0);
                    }

                    if (rule != null) {
                        if (gtprec(rule.prec, si.prec) || (rule.prec == si.prec && si.assoc == Assoc.LEFT)) {
                            // reduce; just return without adding the shift action
                            if (Report.should_report(TOPICS, 2)) {
                                Report.report(2, "resolving conflict between:");
                                Report.report(2, "     " + si);
                                Report.report(2, " and " + aj);
                                Report.report(2, " in favor of reduce/accept");
                            }

                            return;
                        }
                        else if (ltprec(rule.prec, si.prec) || (rule.prec == si.prec && si.assoc == Assoc.RIGHT)) {
                            // shift; remove the reduce rule and fall through to add the shift
                            if (Report.should_report(TOPICS, 2)) {
                                Report.report(2, "resolving conflict between:");
                                Report.report(2, "     " + si);
                                Report.report(2, " and " + aj);
                                Report.report(2, " in favor of shift");
                            }
                            j.remove();
                        }
                        else {
                            // ambiguous; keep both actions
                        }
                    }
                }
            }
            else if (a instanceof Reduce) {
                Reduce ri = (Reduce) a;
                GLRRule rule = ri.rule;

                for (Iterator<Action> j = c.iterator(); j.hasNext(); ) {
                    Action aj = j.next();
                    if (aj instanceof Shift) {
                        // shift-reduce
                        Shift sj = (Shift) aj;
                        GLRTerminal tj = sj.terminal;

                        if (gtprec(rule.prec, sj.prec) || (rule.prec == sj.prec && sj.assoc == Assoc.LEFT)) {
                            // reduce; remove the shift rule and fall through to add the reduce
                            if (Report.should_report(TOPICS, 2)) {
                                Report.report(2, "resolving conflict between:");
                                Report.report(2, "     " + ri);
                                Report.report(2, " and " + aj);
                                Report.report(2, " in favor of reduce");
                            }
                            j.remove();
                        }
                        else if (ltprec(rule.prec, sj.prec) || (rule.prec == sj.prec && sj.assoc == Assoc.RIGHT)) {
                            // shift; just return without adding the reduce action
                            if (Report.should_report(TOPICS, 2)) {
                                Report.report(2, "resolving conflict between:");
                                Report.report(2, "     " + ri);
                                Report.report(2, " and " + aj);
                                Report.report(2, " in favor of shift");
                            }
                            return;
                        }
                        else {
                            // ambiguous; keep both actions
                        }
                    }
                }
            }
            else if (a instanceof Accept) {
                Accept ri = (Accept) a;
                GLRNonterminal A = ri.nonterminal;
                if (A.rules().size() != 1) {
                    throw new InternalCompilerError("Start symbol " + A + " should have exactly 1 rule.");
                }

                GLRRule rule = (GLRRule) A.rules().get(0);

                for (Iterator<Action> j = c.iterator(); j.hasNext(); ) {
                    Action aj = j.next();
                    
                    if (aj instanceof Shift) {
                        // shift-reduce
                        Shift sj = (Shift) aj;
                        GLRTerminal tj = sj.terminal;

                        if (gtprec(rule.prec, sj.prec) || (rule.prec == sj.prec && sj.assoc == Assoc.LEFT)) {
                            // reduce; remove the shift rule and fall through to add the reduce
                            if (Report.should_report(TOPICS, 2)) {
                                Report.report(2, "resolving conflict between:");
                                Report.report(2, "     " + ri);
                                Report.report(2, " and " + aj);
                                Report.report(2, " in favor of accept");
                            }
                            j.remove();
                        }
                        else if (ltprec(rule.prec, sj.prec) || (rule.prec == sj.prec && sj.assoc == Assoc.RIGHT)) {
                            // shift; just return without adding the reduce action
                            if (Report.should_report(TOPICS, 2)) {
                                Report.report(2, "resolving conflict between:");
                                Report.report(2, "     " + ri);
                                Report.report(2, " and " + aj);
                                Report.report(2, " in favor of shift");
                            }
                            return;
                        }
                        else {
                            // ambiguous; keep both actions
                        }
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
        if (X != null && edgeTable[symbolIndex(X)] != null && I.index() < edgeTable[symbolIndex(X)].size()) {
            return (State) edgeTable[symbolIndex(X)].get(I.index());
        }

        return null;
    }

    Collection<State> reverseGotoOrShiftStates(State I, GLRSymbol X) {
        if (X != null && revEdgeTable[symbolIndex(X)] != null && I.index() < revEdgeTable[symbolIndex(X)].size()) {
            Collection<State> c = revEdgeTable[symbolIndex(X)].get(I.index());
            if (c != null)
                return c;
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
            GLRRule t = (GLRRule) g.rules().get(i);
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
                    System.out.println("    on " + g.nonterminals().get(j) + ", goto " + states.get(gotoRow[j]));
                }
            }

            Set<Action>[] actionRow = actionTable[I.index()];

            for (int j = 0; j < actionRow.length; j++) {
                if (!actionRow[j].isEmpty()) {
                    System.out.println("    on " + g.terminals().get(j) + ", " + actionRow[j]);
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

    Item startItem(GLRRule rule) {
        return new Item(rule, 0, Collections.EMPTY_SET);
    }
    
    Item lookaheadStartItem(GLRRule rule) {
        return new Item(rule, 0, new HashSet(g.terminals));
    }

    State startState(GLRRule rule) {
        return closure(Collections.singleton(startItem(rule)), null);
    }
    State lookaheadStartState(GLRRule rule) {
        return closure(Collections.singleton(lookaheadStartItem(rule)), null);
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
                        changed |= sitem.addLookaheadAll(item.lookahead);
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
            Item item = worklist.remove(worklist.size() - 1);

            // item == (A -> alpha . X beta, z)
            GLRSymbol X = item.afterDot();

            if (X instanceof GLRNonterminal) {
                GLRNonterminal A = (GLRNonterminal) X;
                
                // Don't add lookahead rules to the closure.
                if (A.kind != Kind.NORMAL)
                    continue;
                
                List<GLRSymbol> beta = new ArrayList<GLRSymbol>();

                if (item.dot < item.rule.rhs().size()) {
                    beta.addAll(item.rule.rhs().subList(item.dot + 1, item.rule.rhs().size()));
                }

                Set<GLRTerminal> first = first(beta, item.lookahead);

                // item == (A -> alpha . X beta, z)
                List<GLRRule> rules = A.rules();

                for (GLRRule r : rules) {
                    // r == X -> gamma
                    boolean changed = false;
                    boolean found = false;

                    List<Item> addToClosure = new ArrayList<Item>();

                    for (Iterator<Item> k = closure.iterator(); k.hasNext();) {
                        Item ritem = (Item) k.next();
                        
                        if (ritem.rule == r && ritem.dot == 0) {
                            found = true;
                            if (LRConstruction.LALR)
                                changed |= ritem.addLookaheadAll(first);

                            if (changed) {
                                // The hash code may have changed; remove from
                                // the closure, to add back after the iterator
                                // finishes. If we don't remove and add back,
                                // the item could be in the wrong hash
                                // bucket and won't be found.
                                k.remove();
                                addToClosure.add(ritem);
                            }
                        }
                    }

                    if (!found) {
                        Item ritem = new Item((GLRRule) r, 0, first);
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

        if (z != null)
            first.addAll(z);

        return first;
    }

    static Collection TOPICS = Arrays.asList(new String[] { "lr", "ibex" });
}
