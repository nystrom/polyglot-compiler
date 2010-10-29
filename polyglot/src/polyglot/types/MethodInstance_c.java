package polyglot.types;

import java.util.*;

import polyglot.main.Report;
import polyglot.util.Position;

public class MethodInstance_c extends FunctionInstance_c<MethodDef> implements MethodInstance {

    public MethodInstance_c(TypeSystem ts, Position pos, Ref<? extends MethodDef> def) {
        super(ts, pos, def);
    }

    public Type container() {
	return Types.get(def().container());
    }

    public Flags flags() {
	return def().flags();
    }

    public Name name() {
	return def().name();
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
	return super.signature();
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
}
