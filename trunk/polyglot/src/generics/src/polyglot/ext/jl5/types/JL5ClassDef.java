package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Ref;
import polyglot.types.Type;

public interface JL5ClassDef {

	public void addTypeVariable(Ref<? extends Type> t);

	public void setTypeVariables(List<Ref<? extends Type>> l);

	/** Return an immutable list of interfaces */
	public List<Ref<? extends Type>> typeVariables();

	public boolean hasTypeVariable();
}