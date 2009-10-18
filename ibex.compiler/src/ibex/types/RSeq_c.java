package ibex.types;

import java.util.List;

import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

public class RSeq_c extends TypeObject_c implements RSeq {

    private List<Rhs> items;
    private Type type;

    public RSeq_c(IbexTypeSystem ts, Position pos, List<Rhs> items, Type type) {
        super(ts, pos);
        setItems(items);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public boolean matches(Rhs r) {
        if (r instanceof RSeq) {
            RSeq c = (RSeq) r;
            if (items().size() != c.items().size())
                return false;
            for (int i = 0; i < items().size(); i++) {
                Rhs s1 = items().get(i);
                Rhs s2 = c.items().get(i);
                if (!s1.equals(s2))
                    return false;
            }
            return true;
        }
        else {
            return false;
        }
    }

    public void setItems(List<Rhs> items) {
        this.items = items;
    }

    public List<Rhs> items() {
        return items;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (Rhs e : items) {
            sb.append(sep);
            sep = " ";
            sb.append(e);
        }
        return sb.toString();
    }

}
