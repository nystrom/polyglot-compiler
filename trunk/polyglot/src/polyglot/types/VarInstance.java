package polyglot.types;

public interface VarInstance<T extends VarDef> extends Use<T> {
    /**
     * The flags of the variable.
     */
    Flags flags();

    /**
     * The name of the variable.
     */
    String name();

    /**
     * The type of the variable.
     */
    Type type();

    /**
     * Whether the variable's constant value has been set yet.
     */
    boolean constantValueSet();
    
    /**
     * The variable's constant value, or null.
     */
    Object constantValue();
    
    /**
     * Whether the variable has a constant value.
     */
    boolean isConstant();
}
