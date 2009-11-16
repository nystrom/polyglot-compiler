package ibex.types;

import java.util.ArrayList;
import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.MemberInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ParsedClassType_c;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;
import polyglot.util.Predicate;
import polyglot.util.Transformation;
import polyglot.util.TransformingList;

public class IbexClassType_c extends ParsedClassType_c implements ParsedClassType, IbexClassType {

    protected IbexClassType_c() {
        super();
    }

    public IbexClassType_c(ClassDef def) {
        super(def);
    }

    public IbexClassType_c(TypeSystem ts, Position pos, Ref<? extends ClassDef> def) {
        super(ts, pos, def);
    }

    /** Get a list of all the class's MemberInstances. */
    @Override
    public List<MemberInstance<?>> members() {
        List<MemberInstance<?>> l = new ArrayList<MemberInstance<?>>();
        l.addAll(methods());
        l.addAll(fields());
        l.addAll(rules());
        l.addAll(constructors());
        l.addAll(memberClasses());
        return l;
    }

    public boolean isParser() {
        IbexClassDef def = (IbexClassDef) def();
        return def.isParser();
    }

    public List<RuleInstance> rules() {
        IbexClassDef def = (IbexClassDef) def();
        return new TransformingList<RuleDef, RuleInstance>(def.rules(), new RuleAsTypeTransform());
    }

    public static class RuleAsTypeTransform implements Transformation<RuleDef, RuleInstance> {
        public RuleInstance transform(RuleDef def) {
            return def.asInstance();
        }
    }

}
