package jltools.util.jlgen.atoms;

import jltools.util.jlgen.util.*;
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