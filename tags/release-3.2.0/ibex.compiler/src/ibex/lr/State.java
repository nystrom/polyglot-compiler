package ibex.lr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class State {
    int index;
    Set<Item> items;

    State(int index, Set<Item> items) {
        this.index = index;
        this.items = items;
    }

    Set<Item> items() { return items; }
    int index() { return index; }

    public boolean equals(Object o) {
        if (o instanceof State) {
            State s = (State) o;
            return index == s.index;
        }
        return false;
    }

    public int hashCode() {
        return index;
    }

    public void dump() {
        System.out.println("State " + index + ":");

        List<Item> items = new ArrayList<Item>(this.items);
        Collections.sort(items, new Comparator<Item>() {
            public int compare(Item i1, Item i2) {
                if (i1.rule.index == i2.rule.index)
                    return i1.dot - i2.dot;
                else
                    return i1.rule.index - i2.rule.index;
            }
        });

        for (Item item : items) {
            System.out.println("    " + item.toString());
        }
    }

    public String toString() {
        return "State " + index;
    }
}
