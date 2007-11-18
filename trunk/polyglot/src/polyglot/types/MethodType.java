package polyglot.types;

import java.util.List;

public interface MethodType extends FunctionType<MethodDef>, MemberType<MethodDef>, Use<MethodDef> {
    /**
     * The method's name.
     */
    String name();
    
    Flags flags();

    /**
     * Get the list of methods this method (potentially) overrides, in order
     * from this class (i.e., including <code>this</code>) to super classes.
     * @return A list of <code>MethodInstance</code>, starting with
     * <code>this</code>. Note that this list does not include methods declared
     * in interfaces. Use <code>implemented</code> for that.
     * @see polyglot.types.MethodDef
     */
    List<MethodType> overrides();

    /**
     * Return true if this method can override <code>mi</code>, false otherwise.
     */
    boolean canOverride(MethodType mi);

    /**
     * Return true if this method can override <code>mi</code>, throws
     * a SemanticException otherwise.
     */
    void checkOverride(MethodType mi) throws SemanticException;

    /**
     * Get the set of methods this method implements.  No ordering is
     * specified since the superinterfaces need not form a linear list
     * (i.e., they can form a tree).  
     * @return List[MethodInstance]
     */
    List<MethodType> implemented(); 
    List<MethodType> implemented(ReferenceType container); 

    /**
     * Return true if this method has the same signature as <code>mi</code>.
     */
    boolean isSameMethod(MethodType mi);

    /**
     * Return true if this method can be called with name <code>name</code>
     * and actual parameters of types <code>actualTypes</code>.
     * @param name The method to call.
     * @param actualTypes A list of argument types of type <code>Type</code>.
     * @see polyglot.types.Type
     */
    boolean methodCallValid(String name, List<Type> actualTypes);

    /**
     * Return true if this method can override <code>mi</code>.
     * This method should not be called except by <code>TypeSystem</code>
     * and by subclasses.
     * If quiet is true and this method cannot override <code>mi</code>, then
     * false is returned; otherwise, if quiet is false and this method cannot 
     * override <code>mi</code>, then a SemanticException is thrown.
     */
    boolean canOverride(MethodType mi, boolean quiet) throws SemanticException;
}
