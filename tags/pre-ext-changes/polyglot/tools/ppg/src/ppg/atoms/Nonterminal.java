package jltools.util.jlgen.atoms;

import java.util.*;
import jltools.util.jlgen.parse.*;
import jltools.util.jlgen.util.*;

public class Nonterminal extends GrammarSymbol
{
	public Nonterminal(String name, String label) {
		this.name = name;
		this.label = label;
	}

	public Nonterminal(String name) {
		this.name = name;
		label = null;
	}

	}
	public boolean equals(Object o) {
		if (o instanceof Nonterminal) {
			return name.equals( ((Nonterminal)o).getName() );
			// do we even need the nonterminal/terminal distinction?
		return false;
	}
}