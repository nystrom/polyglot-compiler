package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.types.*;
import jltools.visit.*;
import jltools.util.*;

/**
 * Class to handle statements such as ; or the statement inside of {};
 * This class makes the parser's job easier, as it doesn't have to sift
 * out the null's that would otherwise be generated by an empty statement.
 */
public class Empty_c extends Stmt_c implements Empty
{
    public Empty_c(Ext ext, Position pos) {
	super(ext, pos);
    }

    public void translate_(CodeWriter w, Translator tr) {
	w.write(";");
    }

    public String toString() {
	return "/* empty */";
    }
}
