package ibex.lr;

import ibex.ExtensionInfo;
import ibex.ast.RhsAction;
import ibex.ast.RhsAnd;
import ibex.ast.RhsAnyChar;
import ibex.ast.RhsBind;
import ibex.ast.RhsExpr;
import ibex.ast.RhsInvoke;
import ibex.ast.RhsLit;
import ibex.ast.RhsLookahead;
import ibex.ast.RhsMinus;
import ibex.ast.RhsOption;
import ibex.ast.RhsOr;
import ibex.ast.RhsPlus;
import ibex.ast.RhsPlusList;
import ibex.ast.RhsRange;
import ibex.ast.RhsSequence;
import ibex.ast.RhsStar;
import ibex.ast.RhsStarList;
import ibex.ast.RuleDecl;
import ibex.types.ActionDef;
import ibex.types.IbexClassDef;
import ibex.types.Nonterminal;
import ibex.types.Rhs;
import ibex.types.Terminal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import polyglot.ast.ClassDecl;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.main.Report;
import polyglot.types.Name;
import polyglot.util.InternalCompilerError;
import polyglot.util.Pair;
import polyglot.visit.NodeVisitor;

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
    Map<Pair<Integer,Integer>, GLRTerminal> terminalMap;
    
    IbexClassDef def;
    
    int[] terminalRanges;
    
    public GLR(ExtensionInfo ext, ClassDecl cd) {
        this.ext = ext;
        
        def = (IbexClassDef) cd.classDef();

        g = new Grammar();
        g.rules = new ArrayList<GLRRule>(def.allNonterminals().size()*2);
        g.merges = new ArrayList<GLRMerge>(def.allNonterminals().size()*2);

        symbolMap = new HashMap<Object, GLRSymbol>();
        terminalMap = new HashMap<Pair<Integer,Integer>, GLRTerminal>();
       
        g.nonterminals = new ArrayList<GLRNonterminal>(def.allNonterminals().size()+1);
        g.terminals = new ArrayList<GLRTerminal>(257);
        originalStartSymbols = new ArrayList<GLRNonterminal>();
        
        g.eofSymbol = new GLRTerminal(Name.make("$"), g.terminals.size());
        g.terminals.add(g.eofSymbol);
        
        actions = new HashMap<ActionDef, GLRRule>();
        
        final List<Integer> ranges = new ArrayList<Integer>();
        
        cd.visit(new NodeVisitor() {
            @Override
            public Node leave(Node old, Node n, NodeVisitor v) {
                if (n instanceof RhsLit) {
                    RhsLit r = (RhsLit) n;
                    Object c = r.constantValue();
                    if (c instanceof Character) {
                        int ch = (int) (Character) c;
                        ranges.add(ch);
                        ranges.add(ch+1);
                    }
                    else if (c instanceof String) {
                        String s = (String) c;
                        for (int i = 0; i < s.length(); i++) {
                            int ch = s.charAt(i);
                            ranges.add(ch);
                            ranges.add(ch+1);
                        }
                    }
                    else if (c instanceof Byte) {
                        int b = (int) (Byte) c;
                        ranges.add(b);
                        ranges.add(b+1);
                    }
                }
                if (n instanceof RhsRange) {
                    RhsRange r = (RhsRange) n;
                    Object lo = r.lo().constantValue();
                    Object hi = r.hi().constantValue();
                    ranges.add((int) (Character) lo);
                    ranges.add(((int) (Character) hi) + 1);
                }
                if (n instanceof RhsAnyChar) {
                    ranges.add(0);
                }
                return n;
            }
        });
        
        ranges.add(0);
        ranges.add((int) Character.MAX_VALUE+1);
        
        BitSet seen = new BitSet();
        for (Iterator<Integer> i = ranges.iterator(); i.hasNext(); ) {
            int k = i.next();
            if (seen.get(k))
                i.remove();
            seen.set(k);
        }

        {
            int[] a = new int[ranges.size()];
            int i = 0;
            for (Integer k : ranges) {
                a[i++] = k;
            }
            Arrays.sort(a);
            
            // Create the terminals.
            for (i = 0; i < a.length-1; i++) {
                int lo = a[i];
                int hi = a[i+1];
                GLRTerminal t = terminal((char) lo, (char) (hi-1));
            }
        }
        
        cd.visit(new NodeVisitor() {
            @Override
            public Node override(Node n) {
                if (n instanceof Stmt || n instanceof Expr)
                    return n;
                return null;
            }
            
            @Override
            public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
                if (n instanceof RuleDecl) {
                    RuleDecl rule = (RuleDecl) n;
                    RhsExpr rhs = rule.rhs();
                    GLRNonterminal A = nonterminal(rule.rule().asNonterminal());
                    addRulesForRHS(A, rhs);
                    if (! rule.rule().flags().isPrivate())
                        originalStartSymbols.add(A);
                }
                return n;
            }
            
            private GLRSymbol symbolOrAdd(RhsExpr rhs) {
                GLRSymbol X = symbol(rhs);
                if (X == null) {
                    GLRNonterminal A = freshNonterminal();
                    addRulesForRHS(A, rhs);
                    return A;
                }
                return X;
            }

            private GLRSymbol symbol(RhsExpr rhs) {
                if (rhs instanceof RhsBind) {
                    RhsBind r = (RhsBind) rhs;
                    return symbol(r.item());
                }
//                if (rhs instanceof RhsLookahead) {
//                    RhsLookahead r = (RhsLookahead) rhs;
//                    GLRSymbol s = symbol(r.item());
//                    if (s != null)
//                        return lookahead(s, r.negativeLookahead());
//                }
                if (rhs instanceof RhsInvoke) {
                    RhsInvoke r = (RhsInvoke) rhs;
                    return nonterminal(r.symbol());
                }
                if (rhs instanceof RhsLit) {
                    RhsLit r = (RhsLit) rhs;
                    if (r.constantValue() instanceof Character)
                        return terminal((char) (Character) r.constantValue());
                }
                return null;
            }
            
            Map<GLRSymbol,GLRNonterminal> options;
            Map<GLRSymbol,GLRNonterminal> plusses;
            Map<GLRSymbol,GLRNonterminal> stars;
            
            private GLRNonterminal star(GLRSymbol B) {
                if (stars == null)
                    stars = new HashMap<GLRSymbol, GLRNonterminal>();
                
                GLRNonterminal A = stars.get(B);
                
                if (A == null) {
                    A = freshNonterminal();
                    addRule(A);
                    addRule(A, A, B);
                    stars.put(B, A);
                }
                
                return A;
            }
            
            private GLRNonterminal plus(GLRSymbol B) {
                if (plusses == null)
                    plusses = new HashMap<GLRSymbol, GLRNonterminal>();
                
                GLRNonterminal A = plusses.get(B);
                
                if (A == null) {
                    A = freshNonterminal();
                    addRule(A, B);
                    addRule(A, A, B);
                    plusses.put(B, A);
                }
                
                return A;
            }
            
            private GLRNonterminal option(GLRSymbol B) {
                if (options == null)
                    options = new HashMap<GLRSymbol, GLRNonterminal>();
                
                GLRNonterminal A = options.get(B);
                
                if (A == null) {
                    A = freshNonterminal();
                    addRule(A);
                    addRule(A, B);
                    options.put(B, A);
                }
                
                return A;
            }

            Map<Pair<Integer,Integer>, GLRNonterminal> ranges;
            
            private GLRNonterminal range(char lo, char hi) {
                if (ranges == null)
                    ranges = new HashMap<Pair<Integer,Integer>, GLRNonterminal>();
                
                Pair<Integer,Integer> k = new Pair<Integer,Integer>((int) lo, (int) hi);
                GLRNonterminal A = ranges.get(k);
                
                if (A == null) {
                    A = freshNonterminal();
                    BitSet seen = new BitSet();

                    for (int i = lo; i <= hi; i++) {
                        GLRTerminal t = terminal((char) i);
                        if (seen.get(t.index))
                            continue;
                        seen.set(t.index);
                        addRule(A, t);
                    }
                    
                    ranges.put(k, A);
                }
                
                return A;
            }

            private void addRulesForRHS(GLRNonterminal lhs, RhsExpr rhs) {
                GLRSymbol X = symbol(rhs);
                if (X != null) {
                    addRule(lhs, X);
                    return;
                }
                if (rhs instanceof RhsAnd) {
                    RhsAnd r = (RhsAnd) rhs;
                    RhsExpr r1 = r.left();
                    RhsExpr r2 = r.right();

                    GLRNonterminal A = freshNonterminal();
                    GLRNonterminal B = freshNonterminal();
                    GLRNonterminal C = freshNonterminal();
                    addRulesForRHS(B, r1);
                    addRulesForRHS(C, r2);
                    
                    GLRRule rule1 = addRule(A, B);
                    GLRRule rule2 = addRule(A, C);
                    
                    GLRMerge rule = new GLRMerge(A, rule1, rule2, g.merges.size(), GLRMerge.Kind.AND);
                    g.merges.add(rule);
                    A.merges.add(rule);
                    addRule(lhs, A);
                }
                if (rhs instanceof RhsMinus) {
                    RhsMinus r = (RhsMinus) rhs;
                    RhsExpr r1 = r.left();
                    RhsExpr r2 = r.right();

                    GLRNonterminal A = freshNonterminal();
                    GLRNonterminal B = freshNonterminal();
                    GLRNonterminal C = freshNonterminal();
                    addRulesForRHS(B, r1);
                    addRulesForRHS(C, r2);
                    
                    GLRRule rule1 = addRule(A, B);
                    GLRRule rule2 = addRule(A, C);

                    GLRMerge rule = new GLRMerge(A, rule1, rule2, g.merges.size(), GLRMerge.Kind.SUB);
                    g.merges.add(rule);
                    A.merges.add(rule);
                    
                    addRule(lhs, A);
                }
                if (rhs instanceof RhsLookahead) {
                    RhsLookahead r = (RhsLookahead) rhs;
                    GLRNonterminal A = freshNonterminal();
                    GLRNonterminal B = freshNonterminal();
                    addRulesForRHS(B, r.item());
                    addRule(A, B);
                    markLookahead(A, r.negativeLookahead());
                    addRule(lhs, A);
                }
                if (rhs instanceof RhsAction) {
                    RhsAction r = (RhsAction) rhs;
                    GLRNonterminal A = freshNonterminal();
                    addRulesForRHS(A, r.item());
                    GLRRule rule = addRule(lhs, A);
                    recordAction(rule, r.actionDef());
                }
                if (rhs instanceof RhsBind) {
                    RhsBind r = (RhsBind) rhs;
                    addRulesForRHS(lhs, r.item());
                }
                if (rhs instanceof RhsRange) {
                    RhsRange r = (RhsRange) rhs;
                    
                    char lo = (char) (Character) r.lo().constantValue();
                    char hi = (char) (Character) r.hi().constantValue();
                    
                    GLRNonterminal t = range(lo, hi);
                    addRule(lhs, t);
                }
                if (rhs instanceof RhsAnyChar) {
                    RhsAnyChar r = (RhsAnyChar) rhs;
                    
                    char lo = (char) 0;
                    char hi = Character.MAX_VALUE;

                    GLRNonterminal t = range(lo, hi);
                    addRule(lhs, t);
                }
                if (rhs instanceof RhsLit) {
                    RhsLit r = (RhsLit) rhs;
                    
                    Object v = r.constantValue();
                    
                    ArrayList<GLRSymbol> l = new ArrayList<GLRSymbol>();
                    
                    if (v instanceof Character) {
                        char ch = (char) (Character) v;
                        l.add(terminal(ch));
                    }
                    else if (v instanceof String) {
                        String s = (String) v;
                        for (int i = 0; i < s.length(); i++) {
                            char c = s.charAt(i);
                            l.add(terminal(c));
                        }
                    }
                    else {
                        assert false : r + ".value = " + v;
                    }
                    
                    addRule(lhs, l);
                }
                if (rhs instanceof RhsSequence) {
                    RhsSequence r = (RhsSequence) rhs;
                    GLRSymbol[] s = new GLRSymbol[r.items().size()];
                    int i = 0;
                    for (RhsExpr e : r.items()) {
                        s[i++] = symbolOrAdd(e);
                    }
                    addRule(lhs, s);
                }
                if (rhs instanceof RhsOr) {
                    RhsOr r = (RhsOr) rhs;
                    for (RhsExpr e : r.items()) {
                        addRulesForRHS(lhs, e);
                    }
                }
                if (rhs instanceof RhsOption) {
                    RhsOption r = (RhsOption) rhs;
                    GLRSymbol Y = symbol(r.item());
                    if (Y != null) {
                        GLRNonterminal A = option(Y);
                        addRule(lhs, A);
                    }
                    else {
                        addRulesForRHS(lhs, r.item());
                        addRule(lhs);
                    }
//                    GLRNonterminal A = freshNonterminal();
//                    addRule(A);
//                    addRulesForRHS(A, r.item());
//                    addRule(lhs, A);
                }
                if (rhs instanceof RhsStar) {
                    RhsStar r = (RhsStar) rhs;
                    GLRSymbol Y = symbol(r.item());
                    if (Y != null) {
                        GLRNonterminal A = star(Y);
                        addRule(lhs, A);
                    }
                    else {
                        GLRNonterminal A = freshNonterminal();
                        GLRNonterminal B = freshNonterminal();
                        addRulesForRHS(B, r.item());
                        addRule(A);
                        addRule(A, A, B);
                        addRule(lhs, A);
                    }
                }
                if (rhs instanceof RhsPlus) {
                    RhsPlus r = (RhsPlus) rhs;
                    GLRSymbol Y = symbol(r.item());
                    if (Y != null) {
                        GLRNonterminal A = plus(Y);
                        addRule(lhs, A);
                    }
                    else {
                        GLRNonterminal A = freshNonterminal();
                        GLRNonterminal B = freshNonterminal();
                        addRulesForRHS(B, r.item());
                        addRule(A, B);
                        addRule(A, A, B);
                        addRule(lhs, A);
                    }
                }
                if (rhs instanceof RhsStarList) {
                    RhsStarList r = (RhsStarList) rhs;
                    GLRNonterminal A = lhs;
                    GLRNonterminal Ai = freshNonterminal();
                    GLRNonterminal As = freshNonterminal();
                    addRulesForRHS(Ai, r.item());
                    addRulesForRHS(As, r.sep());
                    addRule(A);
                    addRule(A, Ai);
                    addRule(A, A, As, Ai);
                    addRule(lhs);
                    addRule(lhs, A);
                }
                if (rhs instanceof RhsPlusList) {
                    RhsPlusList r = (RhsPlusList) rhs;
                    GLRNonterminal A = freshNonterminal();
                    GLRNonterminal Ai = freshNonterminal();
                    GLRNonterminal As = freshNonterminal();
                    addRulesForRHS(Ai, r.item());
                    addRulesForRHS(As, r.sep());
                    addRule(A, Ai);
                    addRule(A, A, As, Ai);
                    addRule(lhs, A);
                }
            }
            
            GLRRule addRule(GLRNonterminal lhs, List<GLRSymbol> rhs) {
                GLRRule rule = new GLRRule(lhs, rhs, g.rules.size());
                g.rules.add(rule);
                lhs.rules.add(rule);
                return rule;
            }
            GLRRule addRule(GLRNonterminal lhs, GLRSymbol... rhs) {
                return addRule(lhs, Arrays.asList(rhs));
            }
        });
        
        assert g.eofSymbol.index == 0;

        if (g.nonterminals.isEmpty() || g.terminals.isEmpty()) {
            throw new InternalCompilerError(def + " is not a parser.");
        }

        if (originalStartSymbols.isEmpty()) {
            throw new InternalCompilerError(def + " has no start symbols.");
        }
        
        augmentGrammar();

        if (Report.should_report(TOPICS, 9) ||
                Report.should_report("dump-lr", 1)) {
            g.dump();
        }

        if (Report.should_report(TOPICS, 1))
            Report.report(1, "Removing unreachable symbols");

        g.removeUnitRules();
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

        lrc = new LRConstruction(this);

        if (Report.should_report(TOPICS, 9) ||
                Report.should_report("dump-lr", 1)) {
            lrc.dump();
        }
    }
    
    Map<ActionDef,GLRRule> actions;
    
    private void recordAction(GLRRule A, ActionDef action) {
        actions.put(action, A);
    }
    
    public int actionRule(ActionDef A) {
        GLRRule r = actions.get(A);
        return r.index();
    }

    public ActionDef actionDef(int i) {
        for (Map.Entry<ActionDef, GLRRule> e : actions.entrySet()) {
            ActionDef d = e.getKey();
            GLRRule r = e.getValue();
            if (r.index() == i)
                return d;
        }
        return null;
    }

    // -------------------- getters and setters --------------------
    List<GLRNonterminal> nonterminals() { return g.nonterminals(); }
    List<GLRTerminal> terminals() { return g.terminals(); }
    List<GLRRule> rules() { return g.rules(); }
    List<GLRMerge> merges() { return g.merges(); }
    GLRTerminal eofSymbol() { return g.eofSymbol(); }

    public int numRules() {
        return g.rules().size();
    }

    public int eofSymbolNumber() {
        return eofSymbol().index();
    }

    public int nonterminalNumber(Nonterminal s) {
        GLRNonterminal t = nonterminal(s);
        return t.index();
    }
    
    public GLRTerminal terminal(int v) {
        for (Entry<Pair<Integer, Integer>, GLRTerminal> e : terminalMap.entrySet()) {
            Pair<Integer, Integer> r = e.getKey();
            if (r.fst() <= v && v <= r.snd())
                return e.getValue();
        }
        return null;
    }

    public int terminalNumber(int v) {
        GLRTerminal t = terminal(v);
        if (t != null)
            return t.index();
        return -1;
    }
    
    public boolean isReachable(int v) {
        return terminalNumber(v) != -1;
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
    
    public String[] encodedLookaheadTable() {
        if (lre == null) {
            if (Report.should_report(TOPICS, 1))
                Report.report(1, "Encoding tables");
            lre = new LREncoding(g, lrc, def);
        }
        return lre.encodedLookaheadTable();
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
//            GLRTerminal t = terminal(r);
            assert false;
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

    /** Augment the grammar with start rules for the start symbols. */
    void augmentGrammar() {
        startSymbolsMap = new GLRNonterminal[g.nonterminals.size() +
                                             originalStartSymbols.size()];
        List<GLRNonterminal> startSymbols = new ArrayList<GLRNonterminal>(originalStartSymbols.size());
        g.startSymbols = startSymbols;

        // For each start symbol S, create a new rule S' ::= S $
        for (Iterator<GLRNonterminal> i = originalStartSymbols.iterator(); i.hasNext(); ) {
            GLRNonterminal s = i.next();

            GLRNonterminal s_ = new GLRNonterminal(Name.make(s.name() + "!"),
                                                   g.nonterminals.size());
            g.nonterminals.add(s_);

            // create a new rule for the new start symbol, S'
            // S' ::= S $
            GLRRule r = new GLRRule(s_, Arrays.asList(s, eofSymbol()), g.rules.size());
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
    
    void markLookahead(GLRNonterminal X, boolean neg) {
        assert X.rules.size() == 1;
        X.kind = neg ? GLRNonterminal.Kind.NEG : GLRNonterminal.Kind.POS;
    }

    GLRNonterminal nonterminal(Nonterminal n) {
        GLRNonterminal s = (GLRNonterminal) symbolMap.get(n);

        if (s == null) {
            s = new GLRNonterminal(n.name(), g.nonterminals.size());
            g.nonterminals.add(s);
            symbolMap.put(n, s);
        }

        return s;
    }

    GLRNonterminal freshNonterminal() {
        GLRNonterminal s = new GLRNonterminal(Name.makeFresh("A"), g.nonterminals.size());
        g.nonterminals.add(s);
        return s;
    }

    private GLRTerminal terminal(char lo, char hi) {
        Pair<Integer,Integer> k = new Pair<Integer, Integer>((int) lo, (int) hi);
        GLRTerminal s = (GLRTerminal) terminalMap.get(k);

        if (s == null) {
            if (lo == hi) {
                String print = printable(lo);
                s = new GLRTerminal(Name.make("#" + print), g.terminals.size());
            }
            else {
                String print1 = printable(lo);
                String print2 = printable(hi);
                s = new GLRTerminal(Name.make("#" + print1 + "-" + print2), g.terminals.size());
            }
            g.terminals.add(s);
            terminalMap.put(k, s);
        }

        return s;
    }
    
    private String printable(char ch) {
        if (ch >= 128 || (!Character.isDigit(ch) && !Character.isLetter(ch) && "`~!@#$%^&*()-_=+[]{}|\\;:'\"<>,/?".indexOf(ch) < 0))
                return "$" + (int) ch;
        return String.valueOf(ch);
    }

    static Collection TOPICS = Arrays.asList( new String[] { "lr", "ibex" });
}
