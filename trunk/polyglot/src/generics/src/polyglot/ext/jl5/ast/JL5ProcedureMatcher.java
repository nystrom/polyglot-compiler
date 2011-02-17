package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.types.Context;
import polyglot.types.Type;

public interface JL5ProcedureMatcher {

	List<Type> getArgTypes();
	List<Type> getExplicitTypeArgs();
	Context context();
}
