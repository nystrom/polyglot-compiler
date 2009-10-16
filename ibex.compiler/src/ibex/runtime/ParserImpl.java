package ibex.runtime;

import java.io.IOException;


public interface ParserImpl extends Parser {
    String[] encodedActionTable();
    String[] encodedOverflowTable();
    String[] encodedGotoTable();
    String[] encodedRuleTable();
    String[] encodedMergeTable();
    Object semanticAction(int rule, Object[] args);
    Terminal scanTerminal() throws IOException;
    // void reportError(String message);
}
