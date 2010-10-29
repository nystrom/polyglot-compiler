package polyglot.types;

import polyglot.types.VarDef_c.ConstantValue;
import polyglot.util.Position;

public class FieldInstance_c extends VarInstance_c<FieldDef> implements FieldInstance {
    public FieldInstance_c(TypeSystem ts, Position pos, Ref<? extends FieldDef> def) {
	super(ts, pos, def);
    }

    public Type container() {
	return Types.get(def().container());
    }

    public String toString() {
	FieldDef r = def.getCached();
	ConstantValue cv = r.constantValueRef().getCached();
	String cvStr = "";

	if (cv != null && cv.isConstant()) {
	    Object v = cv.value();
	    if (v instanceof String) {
		String s = (String) v;

		if (s.length() > 8) {
		    s = s.substring(0, 8) + "...";
		}

		v = "\"" + s + "\"";
	    }

	    cvStr = " = " + v;
	}

	return "field " + r.flags().translate() + r.type() + " " + r.container() + "." + r.name() + cvStr;
    }

}
