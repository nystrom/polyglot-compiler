package polyglot.types;

public interface VarInstance<T extends VarDef> extends Use<T> {
    /**
     * The flags of the variable.
     */
    Flags flags();

    /**
     * The name of the variable.
     */
    Name name();

    /**
     * The type of the variable.
     */
    Type type();

    /**
     * The variable's constant value, or null.
     */
    Object constantValue();

    /**
     * Whether the variable has a constant value.
     */
    boolean isConstant();
}
