package polyglot.util.ppg.cmds;

import java.util.*;
import polyglot.util.ppg.atoms.*;
import polyglot.util.ppg.util.*;
public class DropCmd implements Command
{
	private Production prod; // productions to be dropped for some nonterminal
	private Vector sym; // or, the single nonterminal to be dropped
	
	public DropCmd(String symbol)
	{
		sym = symbol;
		prod = null;
	}
	public DropCmd(Vector symbols)
	{
		prod = null;
	}
	{
		prod = productions;
		sym = null;
	}

	public boolean isProdDrop() { return prod != null; }
	public boolean isSymbolDrop() { return sym != null; }
	public Vector getSymbols() { return sym; }
	public void unparse(CodeWriter cw) {
		//cw.begin(0);
			for (int i=0; i < sym.size(); i++) {
		//cw.end();
	}
}