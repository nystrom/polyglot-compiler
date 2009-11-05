/**
 * 
 */
package polyglot.bytecode.types;

public class Unreachable extends StackType {
    public static final Unreachable it = new Unreachable();

    public boolean isUnreachable() {
        return true;
    }

    public StackType pop() {
        return this;
    }

    public StackType pop(Type t) {
        return this;
    }

    public StackType pop(Type[] ts) {
        return this;
    }

    public StackType push(Type t) {
        return this;
    }

    public StackType merge(StackType st) {
        return st;
    }
}