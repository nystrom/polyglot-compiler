package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.FunctionDef;
import polyglot.types.FunctionInstance;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;

public class JL5FunctionInstance_c<T extends FunctionDef> extends
		JL5ProcedureInstance_c<T> implements JL5FunctionInstance<T> {

	protected Ref<? extends Type> returnType;

	public JL5FunctionInstance_c(TypeSystem ts, Position pos,
			Ref<? extends T> def) {
		super(ts, pos, def);
	}

	public FunctionInstance<T> returnType(Type returnType) {
		return returnTypeRef(Types.ref(returnType));
	}

	public FunctionInstance<T> returnTypeRef(Ref<? extends Type> returnType) {
		JL5FunctionInstance_c<T> p = (JL5FunctionInstance_c<T>) copy();
		p.returnType = returnType;
		return p;
	}

	public Type returnType() {
		Type retType = null; 
		if (returnType == null) {
			retType = def().returnType().get();
		}
		retType = Types.get(returnType);
		
	    if (!isGeneric() || typeArguments == null) {
	    	return retType;
	    }
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        return ts.applySubstitution(retType, typeVariables(), knownTypeArguments());
        
	}

	public Ref<? extends Type> returnTypeRef() {
		if (returnType == null) {
			return def().returnType();
		}
		return returnType;
	}

	public JL5FunctionInstance<T> formalTypes(List<Type> formalTypes) {
		return (JL5FunctionInstance<T>) super.formalTypes(formalTypes);
	}

	public JL5FunctionInstance<T> throwTypes(List<Type> throwTypes) {
		return (JL5FunctionInstance<T>) super.throwTypes(throwTypes);
	}

	public JL5ProcedureInstance<T> erasure() {
		return super.erasure();
	}

}
