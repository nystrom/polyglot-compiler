package ibex.lr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import polyglot.main.Report;
import polyglot.util.InternalCompilerError;

public class LREncoding {
    Grammar g;
    LRConstruction lrc;

    int[][] outputActionTable;
    int[][] outputGotoTable;
    int[]   outputOverflowTable;
    int[]   outputRuleTable;
    int[]   outputMergeTable;

    public LREncoding(Grammar g, LRConstruction lrc) {
        this.g = g;
        this.lrc = lrc;
        encodeOutputTables();
    }

    protected void encodeOutputTables() {
        if (Report.should_report(TOPICS, 1))
            Report.report(1, "encoding tables");

        int[][] gotoTable = lrc.gotoTable;
        Set<Action>[][] actionTable = lrc.actionTable;
        List<State> states = lrc.states;
        List<GLRTerminal> terminals = g.terminals();

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

        List<GLRNormalRule> rules = g.rules();

        outputRuleTable = new int[rules.size()];

        for (int j = 0; j < rules.size(); j++) {
            GLRNormalRule r = rules.get(j);
            outputRuleTable[j] = r.encode();
        }
        
        List<GLRMergeRule> merges = g.merges();
        
        outputMergeTable = new int[rules.size()];
        
        for (int j = 0; j < merges.size(); j++) {
            GLRMergeRule r = merges.get(j);
            outputMergeTable[r.left.index] = r.encode(0);
            outputMergeTable[r.right.index] = r.encode(1);
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

    static Collection<String> TOPICS = Arrays.asList( new String[] { "lr", "ibex" });
}
