package polyglot.util.ppg.cmds;

import polyglot.util.ppg.atoms.*;

public class NewProdCmd implements Command
{
	private Production prod;
	
	public NewProdCmd(Production p) 
	{
		prod = p;
	}

	public Production getProduction() { return prod; }
	
		//cw.begin(0);
		prod.unparse(cw);
	}
}