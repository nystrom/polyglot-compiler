package jltools.ext.jl.ast;

import jltools.ast.*;
import jltools.visit.*;
import jltools.util.*;

/**
 * A <code>Stmt</code> represents any Java statement.  All statements must
 * be subtypes of Statement.
 */
public abstract class Stmt_c extends Node_c implements Stmt
{
    public Stmt_c(Del ext, Position pos) {
	super(ext, pos);
    }
}
