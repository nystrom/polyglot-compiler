package polyglot.util.ppg.cmds;

import polyglot.util.ppg.atoms.*;

public class ExtendCmd implements Command
{
	private Production prod;
	
	public ExtendCmd(Production p)
	{
		prod = p;
	}
	public Production getProduction() { return prod; }

		//cw.begin(0);
		cw.allowBreak(2);
		prod.unparse(cw);
	}
}