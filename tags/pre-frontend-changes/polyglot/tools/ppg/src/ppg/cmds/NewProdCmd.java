package jltools.util.jlgen.cmds;

import jltools.util.jlgen.atoms.*;

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