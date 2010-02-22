package ibex.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import polyglot.types.FieldAsTypeTransform;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.ReferenceType_c;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.util.Transformation;
import polyglot.util.TransformingList;

public class TupleType_c extends ReferenceType_c implements TupleType {
    private List<FieldDef> fields;

    public TupleType_c(TypeSystem ts, Position pos, Ref<Type>... types) {
        this(ts, pos, Arrays.asList(types));
    }

    public TupleType_c(TypeSystem ts, Position pos, List<Ref<Type>> elementTypes) {
        super(ts, pos);
        this.fields = new ArrayList<FieldDef>(elementTypes.size());
        int i = 0;
        for (Ref<Type> tref : elementTypes) {
            i++;
            fields.add(ts.fieldDef(pos, Types.ref(this), Flags.PUBLIC.Final(), tref, Name.make("f" + i)));
        }
    }

    public List<Type> elementTypes() {
        return new TransformingList<FieldDef, Type>(this.fields, new Transformation<FieldDef,Type>() {
            public Type transform(FieldDef o) {
                return o.asInstance().type();
            }
        });
    }

    public Type elementType(int i) {
        return this.elementTypes().get(i);
    }

    @Override
    public List<FieldInstance> fields() {
        return new TransformingList<FieldDef, FieldInstance>(this.fields, new FieldAsTypeTransform());
    }

    @Override
    public List<Type> interfaces() {
        return Arrays.asList(ts.Cloneable(), ts.Serializable());
    }

    @Override
    public List<MethodInstance> methods() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Type superClass() {
        return ts.Object();
    }

    @Override
    public String toString() {
        StringBuilder
        sb = new StringBuilder();
        sb.append("(");
        String sep = "";
        for (FieldDef f : fields) {
            sb.append(sep);
            sep = ", ";
            sb.append(f.type());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String translate(Resolver c) {
        return "java.lang.Object[]";
    }
}
