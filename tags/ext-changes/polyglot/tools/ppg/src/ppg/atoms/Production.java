package jltools.util.jlgen.atoms;

import java.util.*;
import jltools.util.jlgen.parse.*;
import jltools.util.jlgen.util.*;

{
	private Vector rhs;
	private static String HEADER = "jlgen [nterm]: ";
	public Production (Nonterminal lhs, Vector rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Nonterminal getLHS() { return lhs; }
	public void setLHS(Nonterminal nt) { lhs = nt; }
	
	public Object clone() {
							  (Vector) rhs.clone());
	
		Vector toDrop = prod.getRHS();
		// this is O(n^2)
		Vector target, source;
		for (int i=0; i < toDrop.size(); i++) {
			target = (Vector) toDrop.elementAt(i);
				source = (Vector) rhs.elementAt(j);
					System.err.print(prod.getLHS() + " ::= ");
					for (int k=0; k < target.size(); k++) {
						System.err.print(target.elementAt(k)+" ");	
					}
					System.exit(1);
			}
	}
		GrammarPart ug = null, vg = null;

		while (uIdx < u.size() && vIdx < v.size()) {
			ug = (GrammarPart) u.elementAt(uIdx);
			if (ug instanceof SemanticAction) {
				continue;
			if (vg instanceof SemanticAction) {
				continue;
		}
		
		if (uIdx == u.size() && vIdx == v.size()) {
			// got through all the way, they are the same
			return true;
		} else {
			// one of the lists was not seen all the way, 
				return true;
				return true;
		}
	
	public void union (Vector prodList) {
		for (int i=0; i < prodList.size(); i++) {
	public void add (Production prod) {
		Vector additional = prod.getRHS();
	public void addToRHS (Vector rhsPart) {
		rhs.addElement(rhsPart);
	}
	
	private void assertSameLHS(Production prod, String function) {
	public void unparse (CodeWriter cw) {
		cw.write(lhs.toString() + " ::=");
		cw.allowBreak(3);
		for (int i=0; i < rhs.size(); i++) {
			rhs_part = (Vector) rhs.elementAt(i);
				cw.write(" ");
				((GrammarPart) rhs_part.elementAt(j)).unparse(cw);
				cw.write(" | ");
		cw.newline(); cw.newline();
		cw.end();
	
		result += " ::=";
		for (int i=0; i < rhs.size(); i++) {
			rhs_part = (Vector) rhs.elementAt(i);
				result += " " + rhs_part.elementAt(j).toString();
		}
}