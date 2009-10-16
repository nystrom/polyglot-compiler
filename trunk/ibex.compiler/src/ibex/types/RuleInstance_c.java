package ibex.types;

import java.util.List;

import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.Use_c;
import polyglot.util.Position;

public class RuleInstance_c extends Use_c<RuleDef> implements RuleInstance {
    protected Name name;
    protected Flags flags;
    protected StructType container;
    
    public RuleInstance_c(TypeSystem ts, Position position, Ref<RuleDef_c> def) {
        super(ts, position, def);
    }

    /* (non-Javadoc)
     * @see ibex.types.R#container(polyglot.types.StructType)
     */
    public RuleInstance container(StructType container) {
        RuleInstance_c p = (RuleInstance_c) copy();
        p.container = container;
        return p;
    }

    /* (non-Javadoc)
     * @see ibex.types.R#container()
     */
    public StructType container() {
        if (this.container == null) {
            return Types.get(def().container());
        }
        return this.container;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.R#flags(polyglot.types.Flags)
     */
    public RuleInstance flags(Flags flags) {
        RuleInstance_c p = (RuleInstance_c) copy();
        p.flags = flags;
        return p;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.R#flags()
     */
    public Flags flags() {
        if (this.flags == null) { 
            return def().flags();
        }
        return this.flags;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.R#name(polyglot.types.Name)
     */
    public RuleInstance name(Name name) {
        RuleInstance_c p = (RuleInstance_c) copy();
        p.name = name;
        return p;
    }

    /* (non-Javadoc)
     * @see ibex.types.R#name()
     */
    public Name name() {
        if (this.name == null) { 
            return def().name();
        }
        return this.name;
    }
    
    protected Ref<? extends Type> type;

    /* (non-Javadoc)
     * @see ibex.types.R#type(polyglot.types.Type)
     */
    public RuleInstance type(Type type) {
        return typeRef(Types.ref(type));
    }

    /* (non-Javadoc)
     * @see ibex.types.R#typeRef(polyglot.types.Ref)
     */
    public RuleInstance typeRef(Ref<? extends Type> type) {
        RuleInstance_c p = (RuleInstance_c) copy();
        p.type = type;
        return p;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.R#type()
     */
    public Type type() {
        if (type == null) {
            return def().type().get();
        }
        return Types.get(type);
    }
    
    /* (non-Javadoc)
     * @see ibex.types.R#typeRef()
     */
    public Ref<? extends Type> typeRef() {
        if (type == null) {
            return def().type();
        }
        return type;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.R#choices()
     */
    public List<Rhs> choices() {
        return def().choices();
    }
}
