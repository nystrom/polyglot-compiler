package ibex.runtime;

import java.io.IOException;


public interface ParserImpl extends IParser {
    String[] encodedActionTable();
    String[] encodedOverflowTable();
    String[] encodedGotoTable();
    String[] encodedRuleTable();
    String[] encodedMergeTable();
    String[] encodedTerminalTable();
    Object semanticAction(int rule, Object[] args);
    Terminal scanTerminal() throws IOException;
    int eofSymbol();
    // void reportError(String message);
}
