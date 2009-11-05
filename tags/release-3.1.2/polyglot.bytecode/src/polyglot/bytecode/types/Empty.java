/**
 * 
 */
package polyglot.bytecode.types;

import polyglot.util.InternalCompilerError;

public class Empty extends Reachable {
    public static final Empty it = new Empty();

    public boolean isEmpty() {
        return true;
    }

    public Reachable pop() {
        throw new InternalCompilerError("Cannot pop from empty stack.");
    }

    public Reachable pop(Type t) {
        throw new InternalCompilerError("Cannot pop from empty stack.");
    }

    public Reachable pop(Type[] ts) {
        if (ts.length == 0)
            return this;
        throw new InternalCompilerError("Cannot pop from empty stack.");
    }

    public Reachable merge(StackType st) {
        if (st.isUnreachable())
            return this;
        if (st.isEmpty())
            return this;
        throw new InternalCompilerError("Cannot merge stacks.");
    }
    
    public String toString() {
        return "*";
    }
}