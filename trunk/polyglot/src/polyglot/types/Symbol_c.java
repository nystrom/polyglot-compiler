package polyglot.types;

import java.io.Serializable;

import polyglot.frontend.Globals;
import polyglot.frontend.GoalSet;

public class Symbol_c<T extends TypeObject> extends AbstractRef_c<T> implements Symbol<T>, Serializable {
    
    public Symbol_c() {
        super();
    }
    
    public Symbol_c(T v) {
        super(v, Globals.currentPhase());
    }

    @Override
    protected void complete(GoalSet view) {
        History<T> h = history;
        if (h != null && h.value != null) {
            h.value.complete(view);
            conditionalUpdate(h.value, view);
        }
    }
}