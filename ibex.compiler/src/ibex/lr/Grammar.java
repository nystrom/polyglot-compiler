package ibex.lr;

import ibex.lr.GLRRule.Kind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import polyglot.main.Report;
import polyglot.util.InternalCompilerError;

public class Grammar {
    List<GLRNonterminal> nonterminals;
    List<GLRTerminal> terminals;
    List<GLRNormalRule> rules;
    List<GLRMergeRule> merges;
    GLRTerminal eofSymbol;
    Collection<GLRNonterminal> startSymbols;
    UnionFind terminalClasses;
    Derivation deriv;

    Grammar(Collection<GLRNonterminal> startSymbols,
            List<GLRNonterminal> nonterminals,
            List<GLRTerminal> terminals,
            List<GLRNormalRule> rules,
            List<GLRMergeRule> merges,
            GLRTerminal eofSymbol) {

        this.startSymbols = startSymbols;
        this.nonterminals = nonterminals;
        this.terminals = terminals;
        this.rules = rules;
        this.merges = merges;
        this.eofSymbol = eofSymbol;
    }

    Grammar() { }

    Grammar(Grammar g) {
        this(new ArrayList<GLRNonterminal>(), g.nonterminals(), g.terminals(), g.rules(), g.merges(), g.eofSymbol());

        for (int i = 0; i < nonterminals.size(); i++) {
            GLRNonterminal A = (GLRNonterminal) nonterminals.get(i);
            nonterminals.set(i, new GLRNonterminal(A.nonterm, A.name, i));
        }

        for (Iterator<GLRNonterminal> i = g.startSymbols().iterator(); i.hasNext(); ) {
            GLRNonterminal A = (GLRNonterminal) i.next();
            startSymbols.add(nonterminals.get(A.index()));
        }

        // Copy the rules.  Do the normal rules first.
        for (int i = 0; i < rules.size(); i++) {
            GLRNormalRule r = rules.get(i);
            GLRNormalRule r2 = copyRule(r);
            rules.set(i, r2);
            r2.lhs.rules.add(r2);
        }
        
        for (int i = 0; i < merges.size(); i++) {
            GLRMergeRule r = merges.get(i);
            GLRMergeRule r2 = copyRule(r);
            merges.set(i, r2);
            r2.lhs.merges.add(r2);
        }

        initSets();
    }

    void rebuildNonterminalRuleLists() {
        for (int i = 0; i < nonterminals.size(); i++) {
            GLRNonterminal A = (GLRNonterminal) nonterminals.get(i);
            A.rules = new ArrayList<GLRNormalRule>(A.rules.size());
            A.merges = new ArrayList<GLRMergeRule>(A.merges.size());
        }

        for (int i = 0; i < rules.size(); i++) {
            GLRNormalRule r = rules.get(i);
            r.lhs.rules.add(r);
        }
        for (int i = 0; i < merges.size(); i++) {
            GLRMergeRule r = merges.get(i);
            r.lhs.merges.add(r);
        }
    }

    GLRNormalRule copyRule(GLRNormalRule rule) {
        GLRNonterminal A = (GLRNonterminal) nonterminals.get(rule.lhs.index());
            GLRNormalRule r = (GLRNormalRule) rule;
            List<GLRSymbol> rhs = new ArrayList<GLRSymbol>(r.rhs.size());
            for (int i = 0; i < r.rhs.size(); i++) {
                GLRSymbol X = r.rhs.get(i);
                if (X instanceof GLRNonterminal) {
                    rhs.add(nonterminals.get(X.index()));
                }
                else {
                    rhs.add(X);
                }
            }
            return new GLRNormalRule(r.nonterm, r.nontermRhs, A, rhs, r.index(), r.kind());
    }
    GLRMergeRule copyRule(GLRMergeRule rule) {
        GLRNonterminal A = (GLRNonterminal) nonterminals.get(rule.lhs.index());
            GLRMergeRule r = (GLRMergeRule) rule;
            GLRNormalRule r1 = (GLRNormalRule) rules.get(r.left.index);
            GLRNormalRule r2 = (GLRNormalRule) rules.get(r.right.index);
            return new GLRMergeRule(r.nonterm, r.nontermRhs, A, r1, r2, r.index(), r.kind());
    }

    void initSets() {
        for (GLRNonterminal A : nonterminals) {
            A.first = new HashSet<GLRTerminal>();
            A.follow = new HashSet<GLRTerminal>();
            A.nullable = false;
        }

        computeNullable();
        computeFirst();
        computeFollow();
    }

    // -------------------- getters and setters --------------------
    List<GLRNonterminal> nonterminals() { return nonterminals; }
    List<GLRTerminal> terminals() { return terminals; }
    List<GLRNormalRule> rules() { return rules; }
    List<GLRMergeRule> merges() { return merges; }
    GLRTerminal eofSymbol() { return eofSymbol; }
    Collection<GLRNonterminal> startSymbols() { return startSymbols; }

    Derivation deriv() {
        if (deriv == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Computing derives relation");
            deriv = new Derivation(this);
        }
        return deriv;
    }

    // Sort the rules by the derives relation.
    void sortRules() {
        Collections.<GLRNormalRule>sort(rules, new Comparator<GLRNormalRule>() {
            public int compare(GLRNormalRule o1, GLRNormalRule o2) {
                GLRNormalRule r1 = o1;
                GLRNormalRule r2 = o2;
                GLRNonterminal A1 = r1.lhs;
                GLRNonterminal A2 = r2.lhs;
                if (deriv().derives(A1, A2)) {
                    // sort r2 later
                    return 1;
                }
                if (deriv().derives(A2, A1)) {
                    // sort r1 later
                    return -1;
                }
                // incomparable, just use the rule index. 
                return r1.index - r2.index;
            }
        });

        // Now, renumber the rules.
        for (int i = 0; i < rules.size(); i++) {
            GLRNormalRule r = rules.get(i);
            r.index = i;
        }
    }

    void dump() {
        for (int i = 0; i < terminals.size(); i++) {
            GLRTerminal t = terminals.get(i);
            System.out.println("Terminal " + i + ": " + t);
        }
        System.out.println();

        for (int i = 0; i < nonterminals.size(); i++) {
            GLRNonterminal t = nonterminals.get(i);
            System.out.println("Nonterminal " + i + ": " + t);
        }
        System.out.println();

        for (int i = 0; i < rules.size(); i++) {
            GLRNormalRule t = rules.get(i);
            System.out.println("Rule " + t);
        }
        System.out.println();
        for (int i = 0; i < merges.size(); i++) {
            GLRMergeRule t = merges.get(i);
            System.out.println("Merge " + t);
        }
        System.out.println();
    }

    // Compute nullable(X) for all nonterminals, X.
    private void computeNullable() {
        // Assume nullable flags initialized to false.

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "computing nullable sets");

        boolean changed = true;

        while (changed) {
            changed = false;

            for (Iterator<GLRNormalRule> it = rules().iterator(); it.hasNext(); ) {
                GLRNormalRule rule = it.next();

                GLRNonterminal lhs = rule.lhs();
                
                    GLRNormalRule r = (GLRNormalRule) rule;
                    List<GLRSymbol> rhs = r.rhs();

                    boolean allNullable = true;

                    for (Iterator<GLRSymbol> rit = rhs.iterator(); rit.hasNext(); ) {
                        GLRSymbol s = rit.next();

                        if (s instanceof GLRNonterminal) {
                            GLRNonterminal nt = (GLRNonterminal) s;

                            if (! nt.isNullable()) {
                                allNullable = false;
                                break;
                            }
                        }
                        else {
                            allNullable = false;
                            break;
                        }
                    }

                    if (allNullable && ! lhs.isNullable()) {
                        changed = true;
                        lhs.setNullable(true);
                    }
            }
        }
    }

    // Compute first(X) for all nonterminals, X.
    private void computeFirst() {
        // Assume first sets initialized to empty.

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "computing first sets");

        boolean changed = true;

        while (changed) {
            changed = false;

            for (Iterator<GLRNormalRule> it = rules().iterator(); it.hasNext(); ) {
                GLRNormalRule rule = it.next();

                GLRNonterminal lhs = rule.lhs();
                
                    GLRNormalRule r = (GLRNormalRule) rule;
                    

                    // Iterate over the RHS = Y1 Y2 ... Yk and add in FIRST(Yi)
                    // until Yi is not nullable.
                    Set<GLRTerminal> firstRHS = firstOfRule(r);

                    // Union FIRST(rhs) into FIRST(lhs) and see if the set grew.
                    changed |= lhs.first().addAll(firstRHS);
            }
        }
    }

    private Set<GLRTerminal> firstOfRule(GLRNormalRule r) {
        Set<GLRTerminal> firstRHS = new HashSet<GLRTerminal>();
        for (GLRSymbol s : r.rhs()) {
            if (s instanceof GLRNonterminal) {
                GLRNonterminal nt = (GLRNonterminal) s;

                firstRHS.addAll(nt.first());

                if (! nt.isNullable()) {
                    break;
                }
            }
            else if (s instanceof GLRTerminal) {
                firstRHS.add((GLRTerminal) s);
                break;
            }
        }
        return firstRHS;
    }

    // Compute follow(X) for all nonterminals, X.
    private void computeFollow() {
	// Assume first sets initialized to empty.

        // unnecessary, since in the rules
        // startSymbol().follow().add(eofSymbol());

	boolean changed = true;

	while (changed) {
	    changed = false;

	    for (GLRNormalRule rule : rules()) {
	        GLRNonterminal lhs = rule.lhs();

	        LinkedList<GLRSymbol> rhs = new LinkedList<GLRSymbol>(((GLRNormalRule) rule).rhs());

	        while (! rhs.isEmpty()) {
	            GLRSymbol e = (GLRSymbol) rhs.removeFirst();

	            if (e instanceof GLRNonterminal) {
	                GLRNonterminal s = (GLRNonterminal) e;

	                // The rule we're examining looks like:
	                //     X -> Y1 Y2 ... Yi Yi+1 ... Yk
	                // s is Yi and rhs contains Yi+1 ... Yk
	                // FOLLOW(Yi) contains FIRST(Yi+1 ... Yk)
	                // and also FOLLOW(Yi+1 ... Yi+j) if that is nullable.

	                Set<GLRTerminal> followSym = new HashSet<GLRTerminal>();
	                boolean allNullable = true;

	                for (GLRSymbol e2 : rhs) {
	                    if (e2 instanceof GLRNonterminal) {
	                        GLRNonterminal nt = (GLRNonterminal) e2;

	                        followSym.addAll(nt.first());

	                        if (! nt.isNullable()) {
	                            allNullable = false;
	                            break;
	                        }
	                    }
	                    else if (e2 instanceof GLRTerminal) {
	                        GLRTerminal t = (GLRTerminal) e2;
	                        followSym.add(t);
	                        allNullable = false;
	                        break;
	                    }
	                }

	                if (allNullable) {
	                    followSym.addAll(lhs.follow());
	                }

	                changed |= s.follow().addAll(followSym);
	            }
	        }
	    }
	}
    }

    /** Count occurrences of A in rj.rhs */
    int countOccurrences(GLRNormalRule rj, GLRNonterminal A) {
        int occurrences = 0;
        for (GLRSymbol X : rj.symbols()) {
            if (X.equals(A)) {
                occurrences++;
            }
        }
        return occurrences;
    }

    Collection<GLRNormalRule>[] computeRhsRules() {
        Collection<GLRNormalRule>[] rhsRules = new Collection[nonterminals.size()];

        for (Iterator<GLRNonterminal> i = nonterminals.iterator(); i.hasNext(); ) {
            GLRNonterminal A = (GLRNonterminal) i.next();
            rhsRules[A.index()] = new HashSet<GLRNormalRule>();
        }

        for (Iterator<GLRNormalRule> i = rules.iterator(); i.hasNext(); ) {
            GLRNormalRule r = i.next();
            for (Iterator<GLRSymbol> j = r.symbols().iterator(); j.hasNext(); ) {
                GLRSymbol X = (GLRSymbol) j.next();
                if (X instanceof GLRNonterminal) {
                    GLRNonterminal A = (GLRNonterminal) X;
                    rhsRules[A.index()].add(r);
                }
            }
        }

        return rhsRules;
    }

    void removeMarkedRules() {
        // Remove the rules marked.
        for (Iterator<GLRNormalRule> i = rules.iterator(); i.hasNext(); ) {
            GLRNormalRule r = i.next();
            if (r.index == -1) {
                System.out.println("removing from rules: " + r);
                i.remove();
            }
            else System.out.println("keeping in rules: " + r);
        }

        // Remove the removed rules from the nonterminal's rule lists.
        for (Iterator<GLRNonterminal> i = nonterminals.iterator(); i.hasNext(); ) {
            GLRNonterminal A = (GLRNonterminal) i.next();
            for (Iterator<GLRNormalRule> j = A.rules().iterator(); j.hasNext(); ) {
                GLRNormalRule r = j.next();
                if (r.index == -1) {
                    System.out.println("removing from " + A + ".rules: " + r);
                    j.remove();
                }
                else System.out.println("keeping in " + A + ".rules: " + r);
            }
        }

        // Now, renumber the rules.
        int index = 0;
        for (Iterator<GLRNormalRule> i = rules.iterator(); i.hasNext(); ) {
            GLRNormalRule r = i.next();
            if (r.index == -1) throw new InternalCompilerError("rule " + r + " not removed");
            System.out.println("renumbering " + r);
            r.index = index++;
            System.out.println("         to " + r);
        }
    }

        String P(BitSet s) {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            boolean first = true;
            for (int j = s.nextSetBit(0); j >= 0; j = s.nextSetBit(j+1)) {
                GLRTerminal tj = (GLRTerminal) terminals.get(j);
                if (! first) sb.append(" ");
                first = false;
                sb.append(tj.toString());
            }
            sb.append("]");
            return sb.toString();
        }

    /**
     * Compute equivalence classes of terminals.
     * Two terminals are in the same equivalence class if they
     * appear in the same position of otherwise equivalent rules.
     */
    void computeTerminalClasses() {
        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Computing equivalence classes of terminals");

        // Each bitset contains one partition of equivalent terminals.
        BitSet[] partitions = new BitSet[terminals.size()];
        int[] partitionMap = new int[terminals.size()]; 
        Collection<GLRNormalRule>[] rulesMap = new Collection[terminals.size()];

        int[] sizeMap = new int[terminals.size()];
        int numPartitions = 0;

        for (int i = 0; i < terminals.size(); i++) {
            rulesMap[i] = new HashSet<GLRNormalRule>();
            partitions[i] = new BitSet();
            sizeMap[i] = -1;
        }

        for (Iterator<GLRNormalRule> i = this.rules.iterator(); i.hasNext(); ) {
            GLRNormalRule r = i.next();

            for (Iterator<GLRSymbol> k = r.symbols().iterator(); k.hasNext(); ) {
                GLRSymbol X = (GLRSymbol) k.next();

                if (X instanceof GLRTerminal) {
                    rulesMap[X.index()].add(r);
                }
            }
        }

        // Create one partition for each set of terminals with the
        // same number of rules.
        for (int i = 0; i < terminals.size(); i++) {
            int n = rulesMap[i].size();
            if (sizeMap[n] == -1) {
                sizeMap[n] = numPartitions++;
            }
            partitions[sizeMap[n]].set(i);
            partitionMap[i] = sizeMap[n];
        }

        // For each pair of terminals in a partition, check if the pair
        // have a rule that is definitely not equivalent.
        for (int i = 0; i < numPartitions; i++) {
            BitSet s = partitions[i];
            
            if (s.cardinality() == 1) {
                continue;
            }

            if (Report.should_report(EQUIVTOPICS, 5))
                Report.report(5, "i=" + i + " partition = " + P(s));

            if (numPartitions >= partitions.length) {
                break;
            }

            BitSet next = partitions[numPartitions];

            for (int j = s.nextSetBit(0); j >= 0; j = s.nextSetBit(j+1)) {
                GLRTerminal tj = (GLRTerminal) terminals.get(j);

                if (Report.should_report(EQUIVTOPICS, 5))
                    Report.report(5, "j=" + tj + ": partition " + i + " = " + P(partitions[i]));
K:
                for (int k = s.nextSetBit(j+1); k >= 0; k = s.nextSetBit(k+1)) {
                    GLRTerminal tk = (GLRTerminal) terminals.get(k);

                    if (Report.should_report(EQUIVTOPICS, 5))
                        Report.report(5, "k=" + tk + ": partition " + i + " = " + P(partitions[i]));

                    if (rulesMap[j].size() != rulesMap[k].size()) {
                        s.clear(k);
                        next.set(k);
                        partitionMap[k] = numPartitions;
                        continue K;
                    }

                    for (Iterator<GLRNormalRule> hj = rulesMap[j].iterator(); hj.hasNext(); ) {
                        GLRNormalRule rj = hj.next();

                        // Look for a matching rule.
                        boolean match = true;

MATCH:
                        for (Iterator<GLRNormalRule> hk = rulesMap[k].iterator(); hk.hasNext(); ) {
                            GLRNormalRule rk = hk.next();
                            
                            if (rj.kind() != rk.kind()) {
                                if (Report.should_report(EQUIVTOPICS, 5))
                                    Report.report(5, "  no match 2: " + rj + " and " + rk);
                                match = false;
                                break MATCH;
                            }

                            if (rj.lhs != rk.lhs) {
                                if (Report.should_report(EQUIVTOPICS, 5))
                                    Report.report(5, "  no match 2: " + rj + " and " + rk);
                                match = false;
                                break MATCH;
                            }

                            if (rj.symbols().size() != rk.symbols().size()) {
                                if (Report.should_report(EQUIVTOPICS, 5))
                                    Report.report(5, "  no match 1: " + rj + " and " + rk);
                                match = false;
                                break MATCH;
                            }

                            Iterator<GLRSymbol> ij = rj.symbols().iterator();
                            Iterator<GLRSymbol> ik = rk.symbols().iterator();

                            while (ij.hasNext()) {
                                GLRSymbol Xj = (GLRSymbol) ij.next();
                                GLRSymbol Xk = (GLRSymbol) ik.next();

                                if (Xj.equals(Xk)) {
                                    continue;
                                }

                                if (Xj instanceof GLRNonterminal ||
                                        Xk instanceof GLRNonterminal) {

                                    if (Report.should_report(EQUIVTOPICS, 5))
                                        Report.report(5, "  no match 3: " + rj + " and " + rk + " (" + Xj + "!=" + Xk + ")");

                                    match = false;
                                    break MATCH;
                                }

                                GLRTerminal sj = (GLRTerminal) tj;
                                GLRTerminal sk = (GLRTerminal) tk;

                                if (partitionMap[sj.index()] != 
                                    partitionMap[sk.index()]) {

                                    if (Report.should_report(EQUIVTOPICS, 5))
                                        Report.report(5, "  no match 4: " + rj + " and " + rk + " (" + sj + "!~" + sk + ")");

                                    match = false;
                                    break MATCH;
                                }
                            }

                            if (Report.should_report(EQUIVTOPICS, 5))
                                Report.report(5, rj + " matches " + rk);
                        }

                        if (! match) {
                            s.clear(k);
                            next.set(k);
                            partitionMap[k] = numPartitions;
                            if (Report.should_report(EQUIVTOPICS, 5))
                                Report.report(5, rj + " does not match any of " + rulesMap[k]);
                            continue K;
                        }
                    }
                }
            }

            if (! next.isEmpty()) {
                numPartitions++;
            }

            if (Report.should_report(EQUIVTOPICS, 5)) {
                Report.report(5, "old partition = " + P(s));
                Report.report(5, "new partition = " + P(next));
            }
        }

        terminalClasses = new UnionFind(terminals.size());

        for (int i = 0; i < numPartitions; i++) {
            BitSet s = partitions[i];

            if (Report.should_report(EQUIVTOPICS, 4))
                Report.report(5, "end partition = " + P(partitions[i]));

            if (s.cardinality() > 1) {
                int first = s.nextSetBit(0);
                for (int j = s.nextSetBit(first+1); j >= 0; j = s.nextSetBit(j+1)) {
                    terminalClasses.union(first, j);
                }
            }
        }
    }


    GLRTerminal terminalClass(GLRTerminal A) {
        if (terminalClasses == null) {
            computeTerminalClasses();
        }

        return (GLRTerminal) terminals.get(terminalClasses.find(A.index()));
    }

    void removeUnreachableSymbols() {
        // Remove unreachable symbols.
        BitSet reachableNonterms = new BitSet();
        BitSet reachableTerms = new BitSet();

        for (Iterator<GLRNonterminal> i = startSymbols.iterator(); i.hasNext(); ) {
            GLRNonterminal A = i.next(); 
            dfs(A, reachableNonterms, reachableTerms);
        }

        // Remove symbols and remap indices.

        // Remove unreachable nonterminals.
        {
            List<GLRNonterminal> l = new ArrayList<GLRNonterminal>(this.nonterminals.size());

            for (int i = 0; i < this.nonterminals.size(); i++) {
                GLRNonterminal A = this.nonterminals.get(i);
                if (reachableNonterms.get(i)) {
                    A.index = l.size();
                    l.add(A);
                }
                else {
                    A.index = -1;
                }
            }

            this.nonterminals = l;
        }

        // Remove unreachable terminals.
        if (false) {
            List<GLRTerminal> l = new ArrayList<GLRTerminal>(this.terminals.size());

            for (int i = 0; i < this.terminals.size(); i++) {
                GLRTerminal t = this.terminals.get(i);
                if (reachableTerms.get(i)) {
                    t.index = l.size();
                    l.add(t);
                }
                else {
                    t.index = -1;
                }
            }

            this.terminals = l;
        }

        // Remove unreachable rules.
        {
            List<GLRNormalRule> l = new ArrayList<GLRNormalRule>(this.rules.size()); 

            for (int i = 0; i < this.rules.size(); i++) {
                GLRNormalRule r = this.rules.get(i);
                if (r.lhs.index() != -1) {
                    r.index = l.size();
                    l.add(r);
                }
                else {
                    r.index = -1;
                }
            }

            this.rules = l;
        }
    }

    void dfs(GLRNonterminal A, BitSet reachableNonterms, BitSet reachableTerms) {
        if (reachableNonterms.get(A.index())) {
            return;
        }

        reachableNonterms.set(A.index());

        for (Iterator<GLRNormalRule> i = A.rules().iterator(); i.hasNext(); ) {
            GLRNormalRule r = i.next();
            for (Iterator<GLRSymbol> j = r.symbols().iterator(); j.hasNext(); ) {
                GLRSymbol X = j.next();
                if (X instanceof GLRNonterminal) {
                    GLRNonterminal B = (GLRNonterminal) X;
                    dfs(B, reachableNonterms, reachableTerms);
                }
                else {
                    GLRTerminal t = (GLRTerminal) X;
                    reachableTerms.set(t.index());
                }
            }
        }
    }

    static Collection<String> EQUIVTOPICS = Arrays.asList( new String[] { "lr", "ibex", "equiv" });
    static Collection<String> TOPICS = Arrays.asList( new String[] { "lr", "ibex" });
}
