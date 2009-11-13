package ibex.lr;

import ibex.types.IbexClassDef;
import ibex.types.IbexTypeSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import polyglot.main.Report;
import polyglot.types.Context;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class LREncoding {
    Grammar g;
    LRConstruction lrc;
    IbexClassDef def;

    int[][] outputActionTable;
    int[][] outputGotoTable;
    int[]   outputOverflowTable;
    int[]   outputRuleTable;
    int[]   outputMergeTable;
    int[]   outputTerminalTable;
    int[]   outputLookaheadTable;

    public LREncoding(Grammar g, LRConstruction lrc, IbexClassDef def) {
        this.g = g;
        this.lrc = lrc;
        this.def = def;
        encodeOutputTables();
    }

    protected void encodeOutputTables() {
        if (Report.should_report(TOPICS, 1))
            Report.report(1, "encoding tables");

        int[][] gotoTable = lrc.gotoTable;
        Set<Action>[][] actionTable = lrc.actionTable;
        List<State> states = lrc.states;
        List<GLRTerminal> terminals = g.terminals();
        List<GLRNonterminal> nonterminals = g.nonterminals();

        outputActionTable = new int[states.size()][terminals.size()];
        List<Integer> overflow = new ArrayList<Integer>();

        for (int i = 0; i < actionTable.length; i++) {
            Set<Action>[] actionRow = actionTable[i];
            for (int j = 0; j < actionRow.length; j++) {
                if (actionRow[j].size() == 1) {
                    for (Action a : actionRow[j]) {
                        outputActionTable[i][j] = a.encode();
                    }
                }
                else if (actionRow[j].size() > 1) {
                    outputActionTable[i][j] = Action.encode(Action.OVERFLOW, overflow.size());
                    // add the number of actions
                    overflow.add(new Integer(actionRow[j].size()));

                    // now add the actions.
                    for (Action a : actionRow[j]) {
                        overflow.add(new Integer(a.encode()));
                    }
                }
            }
        }

        outputOverflowTable = new int[overflow.size()];

        for (int j = 0; j < overflow.size(); j++) {
            Integer n = overflow.get(j);
            outputOverflowTable[j] = n.intValue();
        }

        outputGotoTable = gotoTable;

        List<GLRRule> rules = g.rules();

        outputRuleTable = new int[rules.size()];

        for (int j = 0; j < rules.size(); j++) {
            GLRRule r = rules.get(j);
            outputRuleTable[j] = r.encode();
        }

        List<GLRMerge> merges = g.merges();

        outputMergeTable = new int[rules.size()];

        for (int j = 0; j < merges.size(); j++) {
            GLRMerge r = merges.get(j);
            outputMergeTable[r.left.index] = r.encodeLeft();
            outputMergeTable[r.right.index] = r.encodeRight();
            
            assert (outputMergeTable[r.left.index] >>> 3) == r.right.index;
            assert (outputMergeTable[r.right.index] >>> 3) == r.left.index;
        }
        
        outputLookaheadTable = new int[rules.size()];

        for (int j = 0; j < rules.size(); j++) {
            GLRRule r = rules.get(j);
            if (r.lhs.kind == GLRNonterminal.Kind.POS) {
                State s = lrc.lookaheadStartState(r);
                outputLookaheadTable[r.index] = s.index;
            }
            if (r.lhs.kind == GLRNonterminal.Kind.NEG) {
                State s = lrc.lookaheadStartState(r);
                outputLookaheadTable[r.index] = s.index;
            }
        }
        
        IbexTypeSystem ts = (IbexTypeSystem) this.def.typeSystem();
        Context context = ts.emptyContext();
        try {
            Type ByteParser = (Type) ts.systemResolver().find(QName.make("ibex.runtime.IByteParser"));
            Type CharParser = (Type) ts.systemResolver().find(QName.make("ibex.runtime.ICharParser"));
            GLR glr = def.glr();
            Position pos = def.position();
            if (ts.isSubtype(def.asType(), ByteParser, ts.emptyContext())) {
                outputTerminalTable = new int[Byte.MAX_VALUE-Byte.MIN_VALUE+1];
                for (int i = Byte.MIN_VALUE, j = 0; i <= Byte.MAX_VALUE; i++) {
                    byte value = (byte) (i - Byte.MIN_VALUE);
                    if (glr.isReachable(value)) {
                        int n = glr.terminalNumber(value);
                        outputTerminalTable[j++] = n;
                    }
                    else {
                        outputTerminalTable[j++] = glr.eofSymbolNumber();
                    }
                }
            }
            else if (ts.isSubtype(def.asType(), CharParser, context)) {
                outputTerminalTable = new int[Character.MAX_VALUE-Character.MIN_VALUE+1];
                for (int i = Character.MIN_VALUE, j = 0; i <= Character.MAX_VALUE; i++) {
                    char value = (char) (i - Character.MIN_VALUE);
                    if (glr.isReachable(value)) {
                        int n = glr.terminalNumber(value);
                        outputTerminalTable[j++] = n;
                    }
                    else {
                        outputTerminalTable[j++] = glr.eofSymbolNumber();
                    }
                }
            }
            else {
                throw new SemanticException("Parser class must implement either IByteParser or ICharParser", pos);
            }
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }

    }

    public String[] encodedActionTable() {
        if (outputActionTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputActionTable);
    }

    public String[] encodedOverflowTable() {
        if (outputOverflowTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputOverflowTable);
    }

    public String[] encodedGotoTable() {
        if (outputGotoTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputGotoTable);
    }

    public String[] encodedRuleTable() {
        if (outputRuleTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputRuleTable);
    }

    public String[] encodedMergeTable() {
        if (outputMergeTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputMergeTable);
    }

    public String[] encodedTerminalTable() {
        if (outputTerminalTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputTerminalTable);
    }
    
    public String[] encodedLookaheadTable() {
        if (outputLookaheadTable == null) {
            throw new InternalCompilerError("tables not yet encoded");
        }
        return new Encoder().encode(outputLookaheadTable);
    }

    static Collection<String> TOPICS = Arrays.asList( new String[] { "lr", "ibex" });
}
