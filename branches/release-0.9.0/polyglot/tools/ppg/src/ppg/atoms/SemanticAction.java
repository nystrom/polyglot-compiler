package polyglot.util.ppg.atoms;

import polyglot.util.ppg.util.*;
public class SemanticAction extends GrammarPart
{
	private String action;
	
	public SemanticAction (String actionCode) {
		action = actionCode;
	}

	public Object clone() {
	
		cw.begin(0);
		cw.write(action);
		cw.write(":}");
	}
	public String toString () {
		return "{:" + action + ":}\n";
	}
}