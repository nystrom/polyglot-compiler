package ibex.types;

import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

public class RSub_c extends TypeObject_c implements RAnd {

    private Rhs choice1;
    private Rhs choice2;

    public RSub_c(IbexTypeSystem ts, Position pos, Rhs choice1, Rhs choice2) {
        super(ts, pos);
        this.choice1 = choice1;
        this.choice2 = choice2;
    }
    
    public Type type() {
        return ts.Void();
    }

    public Rhs choice1() {
        return choice1;
    }

    public Rhs choice2() {
        return choice2;
    }

    public boolean matches(Rhs r) {
        if (r instanceof RAnd) {
            RAnd m = (RAnd) r;

            if (choice1().matches(m.choice1()) && choice2().matches(m.choice2()))
                return true;
            
            if (choice2().matches(m.choice1()) && choice1().matches(m.choice2()))
                return true;
        }
        
        return false;
    }

}
