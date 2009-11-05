/**
 * 
 */
package polyglot.bytecode.types;

public abstract class Reachable extends StackType {
    public boolean isUnreachable() {
        return false;
    }

    public Reachable push(Type t) {
        if (t.isVoid())
            return this;
        return new NonEmpty(this, t);
    }

    public
    abstract Reachable pop();

    public
    abstract Reachable pop(Type t);

    public
    abstract Reachable pop(Type[] ts);

    public
    abstract Reachable merge(StackType st);
}