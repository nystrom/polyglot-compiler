package polyglot.ext.jl5.ast;

import polyglot.ast.Loop;
import polyglot.ast.Stmt;
/**
 * An immutable representation of a Java language extended <code>for</code>
 * statement.  Contains a statement to be executed and an expression
 * to be tested indicating whether to reexecute the statement.
 */
public interface ExtendedFor extends Loop 
{    
    /** Set the loop body */
    ExtendedFor body(Stmt body);
}
