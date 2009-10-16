package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

public class RLookahead_c extends TypeObject_c implements RLookahead {

    private Rhs item;
    private boolean neg;

    public RLookahead_c(IbexTypeSystem ts, Position pos, Rhs item, boolean neg) {
        super(ts, pos);
        this.item = item;
        this.neg = neg;
    }
    
    public boolean negative() {
        return neg;
    }

    public Type type() {
        return ts.Void();
    }

    public Rhs item() {
        return item;
    }

    public boolean matches(Rhs r) {
        if (r instanceof RLookahead) {
            RLookahead n = (RLookahead) r;
            return item().matches(n.item());
        }
        return false;
    }
}
