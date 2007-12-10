package polyglot.types;

import java.io.*;
import java.util.Collections;

import polyglot.frontend.*;
import polyglot.util.TypeInputStream;

public abstract class AbstractRef_c<T extends TypeObject> implements Ref<T>, Serializable {
        History<T> history;
        
        public AbstractRef_c() {
            history = null;
        }

        public AbstractRef_c(T v, GoalSet view) {
            assert v != null;
            history = new History<T>();
            history.previous = null;
            history.value = v;
            history.valid = view;
        }

        public boolean nonnull() {
            return history != null;
        }

        // ### TODO: implement writeObject to not write out history
        protected static class History<T> implements Serializable {
            History<T> previous;
            T value;
            GoalSet valid;
            
            protected boolean validIn(GoalSet view) {
                return this.valid.containsAll(view);
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

        /** Update the value to v only if there is not valid value for the view. */
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
            return history.value;
        }
        
        protected abstract void complete(GoalSet view);
        
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            
            if (in instanceof TypeInputStream) {
                this.history = null;
            }
        }
        
        public String toString() {
            return "REF(" + history.value + ")";
        }

}
