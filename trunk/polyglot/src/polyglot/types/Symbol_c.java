package polyglot.types;

import java.util.Collections;

import polyglot.frontend.*;
import polyglot.util.UniqueID;

public class Symbol_c<T extends TypeObject> implements Symbol<T> {
    String name;
    History<T> history;
    
    public Symbol_c(T v) {
        this(v, Globals.currentPhase());
    }
    
    public Symbol_c(T v, GoalSet view) {
        name = UniqueID.newID("sym");

        if (v != null) {
            history = new History<T>();
            history.previous = null;
            history.value = v;
            history.valid = view;
        }
    }
    
    public boolean nonnull() {
        return history != null;
    }

    public String toString() {
        return name + "(" + (nonnull() ? history.value : "") + ")";
    }
    
    protected static class History<T> {
        History<T> previous;
        T value;
        GoalSet valid;
        
        protected boolean validIn(GoalSet view) {
            return view.containsAll(this.valid);
        }
    }

    public void update(T v) {
        update(v, Globals.currentPhase());
    }
    
    public void update(T v, Goal goal) {
        update(v, new SimpleGoalSet(Collections.<Goal>singleton(goal)));
    }
    
    public void update(T v, GoalSet view) {
        if (history != null) {
            view = history.valid.union(view);
            if (v == history.value) {
                history.valid = view;
                return;
            }
        }

        History<T> h = new History<T>();
        h.previous = history;
        h.value = v;
        h.valid = view;
        history = h;
    }
    
    protected History<T> getHistory(GoalSet view) {
        for (History<T> h = history; h != null; h = h.previous) {
            if (h.validIn(view)) {
                return h;
            }
        }
        return null;
    }

    /** Update the value to v if there is not valid value for the view. */
    public void conditionalUpdate(T v, GoalSet view) {
        History<T> h = getHistory(view);
        if (h != null) {
            update(v, view);
        }
    }
    
    public T get() {
        return get(Globals.currentView());
    }
    
    public T get(GoalSet view) {
        History<T> h = getHistory(view);
        
        if (h != null) {
            return h.value;
        }

        // Nothing in the history is valid.  Bring the latest version up-to-date.
        complete(view);

        // Not successful.  Probably should have thrown an exception here.
        if (history == null) {
            return null;
        }
        
        return history.value;
    }

    protected void complete(GoalSet view) {
        History<T> h = history;
        if (h != null && h.value != null) {
            h.value.complete(view);
            conditionalUpdate(h.value, view);
        }
    }
}