package jltools.util.jlgen;

import java.io.*;
import jltools.util.jlgen.cmds.*;
import jltools.util.jlgen.lex.*;
import jltools.util.jlgen.parse.*;
import jltools.util.jlgen.spec.*;
import jltools.util.jlgen.util.*;

public class JLgen
{
	public static final String HEADER = "jlgen: ";
	public static final String DEBUG_HEADER = "jlgen [debug]: ";
	public static boolean debug = false;

	public static void DEBUG (String s) {
		if (debug)
			System.out.println(DEBUG_HEADER + s);
	}
	
	public static void main (String args[]) {
		FileInputStream fileInput;
		String filename = null;
		try {
				// assume all switches begin with a dash '-'
				if (args[i].charAt(0) == '-') {
					if (args[i].equals("-symbols")) {
							SYMBOL_CLASS_NAME = args[++i];
					}
					else // invalid switch
				} else {
					// not a switch: this must be a filename
					// but only do the 1st filename on the command line
					if (filename == null)
						filename = args[i];
					else
				}
			}
			usage();
		}
		
		
			System.err.println("Error: no filename specified.");
			usage();
		}
		try {
			fileInput = new FileInputStream(filename);
		}
		catch (FileNotFoundException e) {
			System.out.println("Error: "+filename+" is not found.");
			return;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println(HEADER+"Error: No file name given.");
			return;
		}

		File file = new File(filename);
		String simpleName = file.getName(); 

		
		Parser parser = new Parser(filename, lex);
		try {
			parser.parse();
		} catch (Exception e) {
			System.out.println(HEADER+"Exception: "+e.getMessage());
			return;
		}
		Spec spec = (Spec)parser.getProgramNode();
		String parent = file.getParent();
		
		/* now we have a linked list of inheritance, namely
		try {
			CUPSpec combined = spec.coalesce();
			combined.unparse(cw);
			cw.flush();
		} catch (JLgenError e) {
			System.out.println(e.getMessage());
			System.exit(1);
			System.out.println(HEADER+"exception: "+e.getMessage());
			System.exit(1);
		}
	
	public static void usage() {
		System.err.println("Usage: jlgen [-c ConstClass] <input file>\nwhere:\n"+
						   "\t-c <Class>\tclass prepended to token names to pass to <func>\n"+
						   "\t<input>\ta JLgen or CUP source file\n");
		System.exit(1);