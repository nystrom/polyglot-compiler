package polyglot.util.ppg.spec;

import java.io.*;
import java.util.*;
import polyglot.util.ppg.*;
import polyglot.util.ppg.code.*;
import polyglot.util.ppg.lex.*;
import polyglot.util.ppg.parse.*;
import polyglot.util.ppg.util.*;

public class CUPSpec extends Spec
{
	private Vector productions;
	// maps nonterminal to its index in the vector of productions
	private String start;
	
	public CUPSpec (String pkg, Vector imp, Vector codeParts, Vector syms,
					Vector precedence, String startSym, Vector prods)
	{
		packageName = pkg;
		imports = imp;
		replaceCode(codeParts);
		symbols = syms;
		prec = precedence;
		start = startSym;
		productions = prods;
		ntProds = new Hashtable();
	}
	public void setStart (String startSym) {
			start = startSym;
	}
	private void hashNonterminals() {
		ntProds.clear();
		for (int i=0; i < productions.size(); i++) {
			ntProds.put(prod.getLHS().getName(), new Integer(i));
		}
	
	
	/**
	 * Provides a copy of the production that was present in the original
	 * grammar, but is equal (minus semantic actions) to the given production set.
	 * Thus, we transfer the semantic actions without having to re-specify them.
	 */
	public Production findProduction (Production p) {
		// find the nonterminal which would contain this production
		Nonterminal nt = p.getLHS();
		Production sourceProd = (Production) productions.elementAt(pos);

		Vector rhs = p.getRHS();
		Production result = new Production(nt, new Vector());

		Vector toMatch, source, clone;
		for (int i=0; i < rhs.size(); i++) {
			toMatch = (Vector) rhs.elementAt(i);
			for (int j=0; j < sourceRHSList.size(); j++) {
				source = (Vector) sourceRHSList.elementAt(j);
				if (Production.isSameProduction(toMatch, source)) {
					clone = new Vector();
					for (int k=0; k < source.size(); k++) {
						clone.addElement( ((GrammarPart)source.elementAt(k)).clone() );
					}
					//result.addToRHS((Vector) source.clone());
					result.addToRHS(clone);
					break;
				}
			}
		}
		
		return result;		
	
	public void removeEmptyProductions () {
		Production prod;
			prod = (Production) productions.elementAt(i);
		}
	
		String newPkgName = (packageName == null) ? null : packageName.toString();
		Vector newImports = new Vector();
		Vector newCode = new Vector();
		if (actionCode != null) newCode.addElement(actionCode);
		if (initCode != null) newCode.addElement(initCode);
		if (parserCode != null) newCode.addElement(parserCode);
		if (scanCode != null) newCode.addElement(scanCode);
		/*for (int i=0; i < code.size(); i++) {
		Vector newSymbols = new Vector();
		Vector newPrec = new Vector();
		String newStart = (start == null) ? null : start.toString();
		Vector newProductions = new Vector();
						   newPrec, newStart, newProductions);
		/*
						   (Vector) imports.clone(),
						   (Vector) code.clone(),
						   (Vector) symbols.clone(),
						   (Vector) prec.clone(),
						   newStart,
						   (Vector) productions.clone());
		*/
	
			return;
	}
	public void dropSymbol(String gs) throws PPGError {
		boolean dropped = false;
		}
		if (!dropped)
			throw new PPGError("file", -1, "symbol "+gs+" not found.");
	}
	
	public void dropProductions(Production p) {
		Nonterminal nt = p.getLHS();
		Production prod = (Production) productions.elementAt(pos);
	}
	
	public void dropProductions(Nonterminal nt) {
		int pos = errorNotFound(findNonterminal(nt), nt);
		Production prod = (Production) productions.elementAt(pos);
	}
	
		// a terminal will not be in the hash
			return;
		productions.removeElementAt(pos);
	}

		int pos = findNonterminal(nt);
		if (pos == NT_NOT_FOUND) {
			ntProds.put(nt.getName(), new Integer(productions.size()));
			// attach to specific nonterminal in our list of productions
			//productions.setElementAt(prod, pos);
	}

	private int findNonterminal(Nonterminal nt) {
		return findNonterminal(nt.getName());
	
		if (pos == null)
			return NT_NOT_FOUND;
		else
	}
	private int errorNotFound(int i, Nonterminal nt) {
		if (i == NT_NOT_FOUND) {
			System.err.println(PPG.HEADER + "nonterminal " + nt + " not found.");
			System.exit(1);
		}
		return i;
	
		cw.begin(0);
		if (packageName != null) {
			cw.newline(); cw.newline();
		}
		// import
		for (int i=0; i < imports.size(); i++) {
			cw.write("import " + (String) imports.elementAt(i) + ";");
			cw.newline();
			cw.newline();

		// code
		if (actionCode != null)
			cw.write(actionCode.toString());
		if (initCode != null)
			cw.write(initCode.toString());
		if (parserCode != null)
			cw.write(parserCode.toString());
		if (scanCode != null)
			cw.write(scanCode.toString());
		cw.newline();
		
		// symbols
		for (int i=0; i < symbols.size(); i++) {
			cw.write( ((SymbolList) symbols.elementAt(i)).toString() );
			cw.newline();
		
		// precedence
		for (int i=0; i < prec.size(); i++) {
			cw.write( ((Precedence) prec.elementAt(i)).toString() );
		cw.newline();
		
		// start
			cw.write("start with " + start + ";");
			cw.newline(); cw.newline();
		}
		// productions
		for (int i=0; i < productions.size(); i++) {
			((Production) productions.elementAt(i)).unparse(cw);
		}		
		cw.newline();
		cw.end();
		
		/*
			export(System.out);
		} catch (Exception e) {
			System.out.println(HEADER+"Exception: "+e.getMessage());
			return;
		}
	
	/**
	 * Write out the CUP specification to the stream
	 */
	public void export(PrintStream out) throws Exception {
		// package
		out.println("package " + packageName + ";");
		out.println();
		
		// import
		for (int i=0; i < imports.size(); i++)
			out.println("import " + (String) imports.elementAt(i) + ";");
		out.println();

		// code
		for (int i=0; i < code.size(); i++)
			out.println( ((Code) code.elementAt(i)).toString() );
			out.println(actionCode.toString());
		if (initCode != null)
			out.println(initCode.toString());
		if (parserCode != null)
			out.println(parserCode.toString());
		if (scanCode != null)
			out.println(scanCode.toString());
		out.println();
		
		// symbols
		for (int i=0; i < symbols.size(); i++)
			out.println( ((SymbolList) symbols.elementAt(i)).toString() );
		out.println();
		
		// precedence
			out.println( ((Precedence) prec.elementAt(i)).toString() );
		out.println();
		
		// start
		out.println("start with " + start + ";");
		out.println();
		
		// productions
		for (int i=0; i < productions.size(); i++)
			out.println( ((Production) productions.elementAt(i)).toString() );
		out.println();
		
		out.flush();
		out.close();
	}
}