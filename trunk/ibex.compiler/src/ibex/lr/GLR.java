package ibex.lr;

import ibex.ExtensionInfo;
import ibex.lr.GLRRule.Kind;
import ibex.types.ByteTerminal;
import ibex.types.ByteTerminal_c;
import ibex.types.CharTerminal;
import ibex.types.CharTerminal_c;
import ibex.types.IbexClassDef;
import ibex.types.IbexTypeSystem;
import ibex.types.Nonterminal;
import ibex.types.RAnd;
import ibex.types.RLookahead;
import ibex.types.RSeq;
import ibex.types.RSeq_c;
import ibex.types.RSub;
import ibex.types.Rhs;
import ibex.types.StringTerminal;
import ibex.types.Terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import polyglot.main.Report;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class GLR {
    Grammar g;
    LRConstruction lrc;
    LREncoding lre;
    ExtensionInfo ext;

    /**
     * Map from source grammar start symbols to augmented grammar start
     * symbols.
     */
    GLRNonterminal[] startSymbolsMap;

    /** Keys of startSymbolsMap. */
    List<GLRNonterminal> originalStartSymbols;

    /** Map from type system symbols to LR symbols. */
    Map<Object,GLRSymbol> symbolMap;
    
    IbexClassDef def;

    public GLR(ExtensionInfo ext, IbexClassDef pt) {
        this.ext = ext;
        this.def = pt;

        // A & B
        // -->
        // introduce AB nonterminal
        // add AB -> A | B
        // when reducing AB, fail if no conflict, succeed if conflict
        //
        // A - B
        // add AB -> A | B
        // when reducing AB, fail if there is a conflict, fail if B
        //
        // [A]
        // mark the path as lookahead
        //
        // fail if cannot eventually reduce the A
        // ![A]
        // fail at reduce

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Canonicalizing grammar");

        g = new Grammar();

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Creating symbols");

        initSymbols(pt);

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Creating rules");

        initRules(pt);

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Augmenting grammar");

        augmentGrammar();

        if (Report.should_report(TOPICS, 9) ||
                Report.should_report("dump-lr", 1)) {
            g.dump();
        }

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Removing unreachable symbols");

        g.removeUnreachableSymbols();

        // To avoid the yield-before-merge problem,
        // the parser compares rules to be reduced so
        // that if B ->+ A, a reduction of A occurs before
        // a reduction of B.  To make this comparison simpler
        // we generate the rules so that if B ->+ A, A occurs
        // first in the rules list.
        //
        // Sort the rules by the derives relation.

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Sorting rules");

        g.sortRules();

        if (Report.should_report(TOPICS, 9) ||
                Report.should_report("dump-lr", 1)) {
            g.dump();
        }

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Computing first and follow sets");

        g.initSets();

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Constructing LALR(1) machine");

        lrc = new LRConstruction(g);

        if (Report.should_report(TOPICS, 9) ||
                Report.should_report("dump-lr", 1)) {
            lrc.dump();
        }
    }

    // -------------------- getters and setters --------------------
    List<GLRNonterminal> nonterminals() { return g.nonterminals(); }
    List<GLRTerminal> terminals() { return g.terminals(); }
    List<GLRNormalRule> rules() { return g.rules(); }
    List<GLRMergeRule> merges() { return g.merges(); }
    GLRTerminal eofSymbol() { return g.eofSymbol(); }

    public int numRules() {
        return g.rules().size();
    }

    public Nonterminal ruleLhs(int i) {
        GLRRule rule = (GLRRule) rules().get(i);
        return rule.nonterm;
    }

    public RSeq ruleRhs(int i) {
        GLRRule rule = (GLRRule) rules().get(i);
        return rule.nontermRhs;
    }

    public int eofSymbolNumber() {
        return eofSymbol().index();
    }

    public int ruleNumber(Nonterminal lhs, Rhs rhs) {
        GLRNonterminal t = nonterminal(lhs);

        for (Iterator<GLRNormalRule> i = t.rules().iterator(); i.hasNext(); ) {
            GLRNormalRule r = i.next();
            if (r.nontermRhs.matches(rhs)) {
                return r.index();
            }
        }

        throw new InternalCompilerError("Could not get rule number for " + lhs + " ::= " + rhs);
    }

    public int nonterminalNumber(Nonterminal s) {
        GLRNonterminal t = nonterminal(s);
        return t.index();
    }

    public int terminalNumber(Terminal s) {
        GLRTerminal t = terminal(s);
        return t.index();
    }

    public boolean isReachable(Terminal s) {
        if (! symbolMap.containsKey(s))
            return false;
        GLRTerminal t = terminal(s);
        return t.index() != -1;
    }

    public boolean isReachable(Nonterminal s) {
        if (! symbolMap.containsKey(s))
            return false;
        GLRNonterminal t = nonterminal(s);
        return t.index() != -1;
    }

    public boolean isStartSymbol(Nonterminal s) {
        if (! symbolMap.containsKey(s))
            return false;
        GLRNonterminal t = nonterminal(s);
        t = startSymbolsMap[t.index()];
        if (t == null) return false;
        return lrc.startStatesMap[t.index()] != null;
    }

    /**
     * Get the start symbol number for a particular nonterminal <code>s</code>.
     * This should only be called if the symbol <code>s</code> is a start
     * symbol.  This method is needed to generate code to invoke the parser for
     * a particular symbol.
     */
    public int startSymbolNumber(Nonterminal s) {
        GLRNonterminal t = nonterminal(s);
        t = startSymbolsMap[t.index()];
        if (t == null) {
            throw new InternalCompilerError("Symbol " + s +
            " is not a start symbol.");
        }
        return t.index();
    }

    /**
     * Get the start state number for a particular nonterminal <code>s</code>.
     * This should only be called if the symbol <code>s</code> is a start
     * symbol.  This method is needed to generate code to invoke the parser for
     * a particular symbol.
     */
    public int startStateNumber(Nonterminal s) {
        State I = lrc.startStatesMap[startSymbolNumber(s)];
        if (I == null) {
            throw new InternalCompilerError("Symbol " + s +
            " is not a start symbol.");
        }
        return I.index();
    }

    public String[] encodedActionTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedActionTable();
    }

    public String[] encodedOverflowTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedOverflowTable();
    }

    public String[] encodedGotoTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedGotoTable();
    }

    public String[] encodedRuleTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedRuleTable();
    }
    
    public String[] encodedMergeTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedMergeTable();
    }
    
    public String[] encodedTerminalTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedTerminalTable();
    }

    /**
     * Return a collection of collections of right-hand-sides
     * <code>rhs</code> associated with the nonterminal <code>lhs</code>
     * such that the rule <code>lhs ::= rhs</code> <strong>requires</strong>
     * a merge
     * action.  Each pair of rules in each collection should have a merge
     * action.
     */
    public Collection<Collection<Rhs>> mergeRulesForNonterminal(Nonterminal lhs) {
        if (! ext.getIbexOptions().checkMergeActions) {
            return Collections.EMPTY_LIST;
        }

        System.out.println("TODO: mergeRulesForNonterminal");

        GLRNonterminal n = nonterminal(lhs);
        return Collections.EMPTY_LIST;
    }

    /**
     * Return a collection of collections of right-hand-sides
     * <code>rhs</code> associated with the nonterminal <code>lhs</code>
     * such that the rule <code>lhs ::= rhs</code> <strong>may require</strong>
     * a merge
     * action.  Each pair of rules in each collection should have a merge
     * action.
     */
    public Collection<Collection<GLRRule>> possibleMergeRulesForNonterminal(Nonterminal lhs) {
        if (! ext.getIbexOptions().checkMergeActions) {
            return Collections.EMPTY_LIST;
        }
        GLRNonterminal n = nonterminal(lhs);
        return Collections.EMPTY_LIST;
    }

    boolean isStart(Nonterminal n) {
        // HACK: synthetic rules have $ in their name.
        return ! n.name().toString().contains("$");
    }
    
    /** Initialize the symbols of the grammar. */
    void initSymbols(IbexClassDef pt) {
        symbolMap = new HashMap<Object, GLRSymbol>();

        g.nonterminals = new ArrayList<GLRNonterminal>(pt.allNonterminals().size()+1);
        g.terminals = new ArrayList<GLRTerminal>(257);
        originalStartSymbols = new ArrayList<GLRNonterminal>();
        
        g.eofSymbol = new GLRTerminal(Name.make("$"), g.terminals.size());
        g.terminals.add(g.eofSymbol);

//        IbexTypeSystem ts = (IbexTypeSystem) pt.typeSystem();
//
//        Type ByteParser;
//        Type CharParser;
//        try {
//             ByteParser = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IByteParser"));
//             CharParser = (Type) ts.systemResolver().find(QName.make("ibex.runtime.ICharParser"));
//        }
//        catch (SemanticException e) {
//            throw new InternalCompilerError(e);
//        }
//        
//        if (ts.isSubtype(pt.asType(), ByteParser, ts.emptyContext())) {
//            // Force symbol numbering to be the same as the source order
//            // by creating the symbols now.
//            for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
//                ByteTerminal r = new ByteTerminal_c((IbexTypeSystem) pt.typeSystem(), Position.COMPILER_GENERATED, (byte) i);
//                GLRTerminal t = terminal(r);
//                assert t.index == i+1-Byte.MIN_VALUE;
//            }
//        }
//        
//        if (ts.isSubtype(pt.asType(), CharParser, ts.emptyContext())) {
//            // Force symbol numbering to be the same as the source order
//            // by creating the symbols now.
//            for (int i = Character.MIN_VALUE; i <= Character.MAX_VALUE; i++) {
//                CharTerminal r = new CharTerminal_c((IbexTypeSystem) pt.typeSystem(), Position.COMPILER_GENERATED, (char) i);
//                GLRTerminal t = terminal(r);
//                assert t.index == i+1;
//            }
//        }
        
        assert g.eofSymbol.index == 0;

        for (Iterator<Terminal> i = pt.allTerminals().iterator(); i.hasNext(); ) {
            Terminal r = i.next();
            GLRTerminal t = terminal(r);
        }

        for (Iterator<Nonterminal> i = pt.allNonterminals().iterator(); i.hasNext(); ) {
            Nonterminal r = i.next();
            GLRNonterminal n = nonterminal(r);
            if (isStart(r)) {
                originalStartSymbols.add(n);
            }
        }

        if (g.nonterminals.isEmpty() || g.terminals.isEmpty()) {
            throw new InternalCompilerError(pt + " is not a parser.");
        }

        if (originalStartSymbols.isEmpty()) {
            throw new InternalCompilerError(pt + " has no start symbols.");
        }
    }

    /** Initialize the rules of the grammar. */
    void initRules(IbexClassDef cd) {
        IbexTypeSystem ts = (IbexTypeSystem) cd.typeSystem();

        g.rules = new ArrayList<GLRNormalRule>(cd.allNonterminals().size()*2);
        g.merges = new ArrayList<GLRMergeRule>(cd.allNonterminals().size()*2);

        for (Iterator<Nonterminal> i = cd.allNonterminals().iterator(); i.hasNext(); ) {
            Nonterminal sym = i.next();

            GLRNonterminal n = nonterminal(sym);

            for (Iterator<Rhs> j = sym.rule().choices().iterator(); j.hasNext(); ) {
                Rhs rhs =  j.next();

                Position pos = rhs.position();
                if (rhs instanceof RAnd) {
                    RAnd a = (RAnd) rhs;
                    Rhs rhs1 = a.choice1();
                    Rhs rhs2 = a.choice2();
                    
                    List<GLRSymbol> l1 = symbols(rhs1);
                    List<GLRSymbol> l2 = symbols(rhs2);
                    RSeq seq1 = sequence(rhs1);
                    RSeq seq2 = sequence(rhs2);
                    
                    GLRNormalRule r1 = new GLRNormalRule(sym, seq1, n, l1, g.rules.size(), Kind.NORMAL);
                    g.rules.add(r1);
                    n.rules.add(r1);
                    GLRNormalRule r2 = new GLRNormalRule(sym, seq2, n, l2, g.rules.size(), Kind.NORMAL);
                    g.rules.add(r2);
                    n.rules.add(r2);
                    GLRMergeRule rule = new GLRMergeRule(sym, new RSeq_c(ts, pos, Arrays.<Rhs>asList(a), a.type()), n, r1, r2, g.rules.size(), Kind.AND);
                    g.merges.add(rule);
                    n.merges.add(rule);
                }
                else if (rhs instanceof RSub) {
                    RSub a = (RSub) rhs;
                    Rhs rhs1 = a.choice1();
                    Rhs rhs2 = a.choice2();

                    List<GLRSymbol> l1 = symbols(rhs1);
                    List<GLRSymbol> l2 = symbols(rhs2);
                    RSeq seq1 = sequence(rhs1);
                    RSeq seq2 = sequence(rhs2);

                    GLRNormalRule r1 = new GLRNormalRule(sym, seq1, n, l1, g.rules.size(), Kind.NORMAL);
                    g.rules.add(r1);
                    n.rules.add(r1);
                    GLRNormalRule r2 = new GLRNormalRule(sym, seq2, n, l2, g.rules.size(), Kind.NORMAL);
                    g.rules.add(r2);
                    n.rules.add(r2);
                    GLRMergeRule rule = new GLRMergeRule(sym, new RSeq_c(ts, pos, Arrays.<Rhs>asList(a), a.type()), n, r1, r2, g.rules.size(), Kind.SUB);
                    g.merges.add(rule);
                    n.merges.add(rule);
                }
                else if (rhs instanceof RLookahead) {
                    RLookahead a = (RLookahead) rhs;
                    List<GLRSymbol> l = symbols(a.item());
                    RSeq seq = sequence(a.item());
                    GLRNormalRule rule = new GLRNormalRule(sym, seq, n, l, g.rules.size(), a.negative() ? Kind.NEG_LOOKAHEAD : Kind.POS_LOOKAHEAD);
                    g.rules.add(rule);
                    n.rules.add(rule);
                }
                else if (rhs instanceof RSeq) {
                    RSeq seq = (RSeq) rhs;
                    
                    List<GLRSymbol> l = symbols(rhs);

                    GLRNormalRule rule = new GLRNormalRule(sym, seq, n, l, g.rules.size(), Kind.NORMAL);
                    g.rules.add(rule);
                    n.rules.add(rule);
                }
                else if (rhs instanceof Nonterminal) {
                    Nonterminal a = (Nonterminal) rhs;
                    
                    List<GLRSymbol> l = symbols(rhs);
                    
                    GLRNormalRule rule = new GLRNormalRule(sym, sequence(a), n, l, g.rules.size(), Kind.NORMAL);
                    g.rules.add(rule);
                    n.rules.add(rule);
                }
                else if (rhs instanceof Terminal) {
                    Terminal a = (Terminal) rhs;
                    
                    List<GLRSymbol> l = symbols(rhs);
                    
                    GLRNormalRule rule = new GLRNormalRule(sym, sequence(a), n, l, g.rules.size(), Kind.NORMAL);
                    g.rules.add(rule);
                    n.rules.add(rule);
                }
                else {
                    assert false : "unexpected rhs item " + rhs + " : " + rhs.getClass().getName();
                }
            }
        }
    }

    private RSeq sequence(Rhs rhs) {
        IbexTypeSystem ts = (IbexTypeSystem) rhs.typeSystem();
        Position pos = rhs.position();
        if (rhs instanceof RSeq) {
            RSeq choice = (RSeq) rhs;
            return choice;
        }
        else if (rhs instanceof Terminal) {
            return new RSeq_c(ts, pos, Collections.singletonList(rhs), rhs.type());
        }
        else if (rhs instanceof Nonterminal) {
            return new RSeq_c(ts, pos, Collections.singletonList(rhs), rhs.type());
        }
        else {
            assert false : "unexpected rhs item " + rhs + " : " + rhs.getClass().getName();
        }
        return null;
    }

    private List<GLRSymbol> symbols(Rhs rhs1) {
        List<GLRSymbol> l = new ArrayList<GLRSymbol>();
        if (rhs1 instanceof RSeq) {
            RSeq choice = (RSeq) rhs1;
            for (Iterator<Rhs> k = choice.items().iterator(); k.hasNext(); ) {
                Rhs e = k.next();

                if (e instanceof Terminal) {
                    Terminal s = (Terminal) e;
                    GLRTerminal t = terminal(s);
                    l.add(t);
                }
                else if (e instanceof Nonterminal) {
                    Nonterminal s = (Nonterminal) e;
                    GLRNonterminal t = nonterminal(s);
                    l.add(t);
                }
                else {
                    assert false : "unexpected rhs item " + e + " : " + e.getClass().getName();
                }
            }
        }
        else if (rhs1 instanceof Terminal) {
            Terminal s = (Terminal) rhs1;
            GLRTerminal t = terminal(s);
            l.add(t);
        }
        else if (rhs1 instanceof Nonterminal) {
            Nonterminal s = (Nonterminal) rhs1;
            GLRNonterminal t = nonterminal(s);
            l.add(t);
        }
        else {
            assert false : "unexpected rhs item " + rhs1 + " : " + rhs1.getClass().getName();
        }
        return l;
    }

    /** Augment the grammar with start rules for the start symbols. */
    void augmentGrammar() {
        startSymbolsMap = new GLRNonterminal[g.nonterminals.size() +
                                             originalStartSymbols.size()];
        List<GLRNonterminal> startSymbols = new ArrayList<GLRNonterminal>(originalStartSymbols.size());
        g.startSymbols = startSymbols;

        // For each start symbol S, create a new rule S' ::= S $
        for (Iterator<GLRNonterminal> i = originalStartSymbols.iterator(); i.hasNext(); ) {
            GLRNonterminal s = i.next();

            GLRNonterminal s_ = new GLRNonterminal(s.nonterm,
                                                   Name.make(s.name() + "!"),
                                                   g.nonterminals.size());
            g.nonterminals.add(s_);

            // create a new rule for the new start symbol, S'
            // S' ::= S $
            GLRNormalRule r = new GLRNormalRule(null, null, s_, Arrays.asList(s, eofSymbol()), g.rules.size());
            g.rules.add(r);
            s_.rules.add(r);

            startSymbolsMap[s.index()] = s_;
            g.startSymbols.add(s_);
        }
    }

    void dump() {
        if (g != null)
            g.dump();

        if (lrc != null)
            lrc.dump();
    }

    GLRNonterminal nonterminal(Nonterminal n) {
        GLRNonterminal s = (GLRNonterminal) symbolMap.get(n);

        if (s == null) {
            s = new GLRNonterminal(n, n.name(), g.nonterminals.size());
            g.nonterminals.add(s);
            symbolMap.put(n, s);
        }

        return s;
    }

    private GLRTerminal terminal(Terminal s) {
        return s instanceof CharTerminal ? terminal((CharTerminal) s) : terminal((ByteTerminal) s);
    }

    GLRTerminal terminal(CharTerminal n) {
        String print = printable(n.value());
        GLRTerminal s = (GLRTerminal) symbolMap.get(n);

        if (s == null) {
            s = new GLRTerminal(Name.make("#" + print), g.terminals.size());
            g.terminals.add(s);
            symbolMap.put(n, s);
        }

        return s;
    }

    GLRTerminal terminal(ByteTerminal n) {
        String print = printable((char) (n.value() & 0xff));
        GLRTerminal s = (GLRTerminal) symbolMap.get(n);
        
        if (s == null) {
            s = new GLRTerminal(Name.make("#" + print), g.terminals.size());
            g.terminals.add(s);
            symbolMap.put(n, s);
        }
        
        return s;
    }
    
    private String printable(char ch) {
        if (ch >= 128 || (!Character.isDigit(ch) && !Character.isLetter(ch) && "`~!@#$%^&*()-_=+[]{}|\\;:'\"<>,/?".indexOf(ch) < 0))
                return "$" + (int) ch;
        return String.valueOf(ch);
    }

    GLRTerminal terminal(StringTerminal n) {
        String key = "this::#" + n.value();
        GLRTerminal s = (GLRTerminal) symbolMap.get(key);

        if (s == null) {
            s = new GLRTerminal(Name.make("#" + n.value()), g.terminals.size());
            g.terminals.add(s);
            symbolMap.put(key, s);
        }

        return s;
    }

    static Collection TOPICS = Arrays.asList( new String[] { "lr", "ibex" });
}
