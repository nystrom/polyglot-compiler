package polyglot.ext.jl5.types;

import polyglot.types.FunctionInstance;
import polyglot.types.ProcedureDef;


/**
 * A <code>ProcedureInstance</code> contains the type information for a Java
 * procedure (either a method or a constructor).
 */
public interface JL5FunctionInstance<T extends ProcedureDef> extends JL5ProcedureInstance<T>, FunctionInstance<T> {

	
}
