package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;

public class JL5MethodInstance_c extends JL5FunctionInstance_c<MethodDef> implements JL5MethodInstance{

    protected Name name;
    protected Flags flags;
    protected StructType container;
    
    public JL5MethodInstance_c(TypeSystem ts, Position pos, Ref<? extends MethodDef> def) {
        super(ts, pos, def);
    }
    
    public JL5MethodInstance container(StructType container) {
    	JL5MethodInstance_c p = (JL5MethodInstance_c) copy();
        p.container = container;
        return p;
    }

    public StructType container() {
        if (this.container == null) {
            return Types.get(def().container());
        }
        return this.container;
    }
    
    public JL5MethodInstance flags(Flags flags) {
    	JL5MethodInstance_c p = (JL5MethodInstance_c) copy();
        p.flags = flags;
        return p;
    }
    
    public Flags flags() {
        if (this.flags == null) { 
            return def().flags();
        }
        return this.flags;
    }
    
    public JL5MethodInstance name(Name name) {
    	JL5MethodInstance_c p = (JL5MethodInstance_c) copy();
        p.name = name;
        return p;
    }

    public Name name() {
        if (this.name == null) { 
            return def().name();
        }
        return this.name;
    }
    
    public JL5MethodInstance returnType(Type returnType) {
        return (JL5MethodInstance) super.returnType(returnType);
    }
    public JL5MethodInstance returnTypeRef(Ref<? extends Type> returnType) {
	return (JL5MethodInstance) super.returnTypeRef(returnType);
    }

    public JL5MethodInstance formalTypes(List<Type> formalTypes) {
        return (JL5MethodInstance) super.formalTypes(formalTypes);
    }
    
    public JL5MethodInstance throwTypes(List<Type> throwTypes) {
        return (JL5MethodInstance) super.throwTypes(throwTypes);
    }

    /** Returns true iff <this> is the same method as <m> */
    public final boolean isSameMethod(MethodInstance m, Context context) {
	return ts.isSameMethod(this, m, context);
    }

    public final List<MethodInstance> overrides(Context context) {
	return ts.overrides(this, context);
    }
    
    @Override
    public String signature() {
	return name() + super.signature();
    }

    /**
     * Leave this method in for historic reasons, to make sure that extensions
     * modify their code correctly.
     */
    public final boolean canOverride(MethodInstance mj, Context context) {
	return ts.canOverride(this, mj, context);
    }

    public final void checkOverride(MethodInstance mj, Context context) throws SemanticException {
	ts.checkOverride(this, mj, context);
    }

    public final List<MethodInstance> implemented(Context context) {
        return ts.implemented(this, context);
    }

    public JL5MethodInstance erasure() {
        JL5MethodInstance e = (JL5MethodInstance) super.erasure();
        return (JL5MethodInstance) e.returnType(((JL5TypeSystem)typeSystem()).erasure(returnType()));
    }
}
