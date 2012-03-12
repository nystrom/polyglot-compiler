package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.ext.jl5.types.attributes.MethodSignatureAttribute;
import polyglot.types.MethodDef;
import polyglot.types.Ref;
import polyglot.types.Type;

public interface JL5MethodDef extends JL5ProcedureDef, MethodDef {
	boolean isCompilerGenerated();
	void eraseWith(List<Ref<? extends Type>> formalsRefs, Ref<? extends Type> retRef);
	
	MethodSignatureAttribute getSignatureAttr();
}
