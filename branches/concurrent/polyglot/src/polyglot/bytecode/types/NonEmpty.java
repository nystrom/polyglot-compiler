/**
 * 
 */
package polyglot.bytecode.types;

import polyglot.util.InternalCompilerError;

public class NonEmpty extends Reachable {
    Type head;
    Reachable tail;

    NonEmpty(Reachable tail, Type head) {
        this.tail = tail;
        this.head = head;
        assert !head.isVoid();
    }

    public int size() {
        return tail.size() + (head.isWide() ? 2 : 1);
    }

    public Type top() {
        return head;
    }

    public Reachable pop() {
        return tail;
    }

    public Reachable pop(Type t) {
        if (merge(head, t) == null)
            assert merge(head, t) != null : "cannot merge " + head + " and " + t;
        return tail;
    }

    public Reachable pop(Type[] ts) {
        Reachable st = this;
        for (int i = ts.length - 1; i >= 0; i--) {
            st = st.pop(ts[i]);
        }
        return st;
    }

    public boolean isNonempty() {
        return true;
    }

    Type merge(Type t1, Type t2) {
        if (t1.desc().equals(t2.desc()))
            return t1;
        if (t1.isArray() && t2.isArray()) {
            Type b1 = t1.arrayBase();
            Type b2 = t2.arrayBase();
            Type b = merge(b1, b2);
            if (b == null)
                return Type.OBJECT;
            else
                return Type.array(b);
        }
        if (t1.isRef() && t2.isRef()) {
            return Type.OBJECT;
        }
        return null;
    }

    public Reachable merge(StackType st) {
        if (st.isUnreachable())
            return this;
        if (st instanceof NonEmpty) {
            NonEmpty t = (NonEmpty) st;
            Type m = merge(head, t.head);
            if (m == null) {
                throw new InternalCompilerError("Cannot merge stacks.");
            }
            return pop().push(m);
        }
        throw new InternalCompilerError("Cannot merge stacks.");
    }

    public String toString() {
        return tail + " " + head;
    }
}