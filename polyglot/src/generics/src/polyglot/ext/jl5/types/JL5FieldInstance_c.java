package polyglot.ext.jl5.types;

import polyglot.types.FieldDef;
import polyglot.types.FieldInstance_c;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class JL5FieldInstance_c extends FieldInstance_c implements JL5FieldInstance {
    public JL5FieldInstance_c(TypeSystem ts, Position pos,
            Ref<? extends FieldDef> def) {
        super(ts, pos, def);
    }

    public JL5FieldInstance erasure() {
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        JL5FieldInstance_c n = (JL5FieldInstance_c) this.copy();
        // Need to get the types from the definition, since we 
        // want erasure to resolve the bounds of TypeVariable
        Type fieldType = def().type().get();
        n = (JL5FieldInstance_c) n.type(ts.erasure(fieldType));
        if (n.container instanceof ParameterizedType) {
            n = (JL5FieldInstance_c) n.container(((ParameterizedType)n.container).baseType());
        }
        return n;
    }

}
