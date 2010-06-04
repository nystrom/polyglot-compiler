/**
 * 
 */
package polyglot.bytecode.types;

import polyglot.util.InternalCompilerError;

public abstract class StackType {
    public int size() { return 0; }
    
    public abstract boolean isUnreachable();

    public boolean isEmpty() {
        return false;
    }

    public abstract StackType pop();

    public abstract StackType pop(Type t);

    public abstract StackType pop(Type[] ts);

    public abstract StackType push(Type t);

    public abstract StackType merge(StackType st);

    public Type top() {
        throw new InternalCompilerError("Cannot get top of empty or unreachable stack.");
    }

    boolean isNonempty() {
        return false;
    }
}