package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.ClassDef;
import polyglot.types.ClassDef_c;
import polyglot.types.Ref;
import polyglot.types.Type;

public class JL5ClassDef_c extends ClassDef_c implements ClassDef, JL5ClassDef {
	protected List<Ref<? extends Type>> typeVariables;

	/**
	 * This is used to instantiate ClassDef for TypeVariable
	 * @param jl5TypeSystem_c
	 */
	public JL5ClassDef_c(JL5TypeSystem ts) {
		//CHECK maybe ClassDef for TypeVariable deserves its own class
		this(ts, null);
	}

	public JL5ClassDef_c(JL5TypeSystem ts, Source fromSource) {
		super(ts, fromSource);
		this.typeVariables = new ArrayList<Ref<? extends Type>>();
	}

	/* (non-Javadoc)
	 * @see polyglot.ext.jl5.types.JL5ClassDef#addTypeVariable(polyglot.types.Ref)
	 */
	@Override
	public void addTypeVariable(Ref<? extends Type> t) {
		typeVariables.add(t);
	}

	/* (non-Javadoc)
	 * @see polyglot.ext.jl5.types.JL5ClassDef#setTypeVariables(java.util.List)
	 */
	@Override
	public void setTypeVariables(List<Ref<? extends Type>> l) {
		this.typeVariables = new ArrayList<Ref<? extends Type>>(l);
	}

	/* (non-Javadoc)
	 * @see polyglot.ext.jl5.types.JL5ClassDef#typeVariables()
	 */
	@Override
	public List<Ref<? extends Type>> typeVariables() {
		return Collections.<Ref<? extends Type>>unmodifiableList(typeVariables);
	}

}
