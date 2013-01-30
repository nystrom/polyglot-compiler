package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.Flags;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;

public class JL5ConstructorInstance_c extends JL5ProcedureInstance_c<ConstructorDef> implements JL5ConstructorInstance {
 
    protected Flags flags;
    protected StructType container;
    
    public JL5ConstructorInstance_c(TypeSystem ts, Position pos, Ref<? extends ConstructorDef> def) {
        super(ts, pos, def);
    }
    
    public JL5ConstructorInstance container(StructType container) {
    	JL5ConstructorInstance_c p = (JL5ConstructorInstance_c) copy();
        p.container = container;
        return p;
    }

    public StructType container() {
        if (this.container == null) {
            return Types.get(def().container());
        }
        return this.container;
    }
    
    public JL5ConstructorInstance flags(Flags flags) {
    	JL5ConstructorInstance_c p = (JL5ConstructorInstance_c) copy();
        p.flags = flags;
        return p;
    }

    public Flags flags() {
        if (this.flags == null) { 
            return def().flags();
        }
        return this.flags;
    }
    
    public JL5ConstructorInstance formalTypes(List<Type> formalTypes) {
        return (JL5ConstructorInstance) super.formalTypes(formalTypes);
    }
    
    public JL5ConstructorInstance throwTypes(List<Type> throwTypes) {
        return (JL5ConstructorInstance) super.throwTypes(throwTypes);
    }
    
    public JL5ConstructorInstance instantiate(ClassType objectType,
    		List<Type> argumentTypes) throws SemanticException {
    	return this;
    }

    public JL5ConstructorInstance erasure() {
        JL5ConstructorInstance_c e = (JL5ConstructorInstance_c) super.erasure();
        if (container instanceof ParameterizedType) {
            e = (JL5ConstructorInstance_c) e.container(((ParameterizedType)e.container).baseType());
        }
        return e;
    }

}
