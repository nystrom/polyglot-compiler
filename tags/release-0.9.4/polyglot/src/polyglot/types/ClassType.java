package polyglot.types;

import polyglot.util.Enum;
import java.util.List;

/**
 * A <code>ClassType</code> represents a class, either loaded from a
 * classpath, parsed from a source file, or obtained from other source.
 * A <code>ClassType</code> is not necessarily named.
 */
public interface ClassType extends Importable, ReferenceType, MemberInstance
{
    public static class Kind extends Enum {
        public Kind(String name) {
            super(name);
        }
    }

    public static final Kind TOP_LEVEL = new Kind("top-level");
    public static final Kind MEMBER = new Kind("member");
    public static final Kind LOCAL = new Kind("local");
    public static final Kind ANONYMOUS = new Kind("anonymous");

    /** Get the class's kind. */
    Kind kind();

    /**
     * Return true if the class is top-level (i.e., not inner).
     * Equivalent to kind() == TOP_LEVEL.
     */
    boolean isTopLevel();

    /**
     * Return true if the class is an inner class.
     * Equivalent to kind() == MEMBER || kind() == LOCAL || kind() == ANONYMOUS.
     */
    boolean isInner();

    /**
     * Return true if the class is a member class.
     * Equivalent to kind() == MEMBER.
     */
    boolean isMember();

    /**
     * Return true if the class is a local class.
     * Equivalent to kind() == LOCAL.
     */
    boolean isLocal();

    /**
     * Return true if the class is an anonymous class.
     * Equivalent to kind() == ANONYMOUS.
     */
    boolean isAnonymous();

    /**
     * The class's constructors.
     * A list of <code>ConstructorInstance</code>.
     * @see polyglot.types.ConstructorInstance
     */
    List constructors();

    /**
     * The class's member classes.
     * A list of <code>ClassType</code>.
     * @see polyglot.types.ClassType
     */
    List memberClasses();

    /** Returns the member class with the given name, or null. */
    ClassType memberClassNamed(String name);

    /** Get a field by name, or null. */
    FieldInstance fieldNamed(String name);

    /** Return true if the class is strictly contained in <code>outer</code>. */
    boolean isEnclosed(ClassType outer);

    /**
     * Implementation of <code>isEnclosed</code>.
     * This method should only be called by the <code>TypeSystem</code>
     * or by a subclass.
     */
    boolean isEnclosedImpl(ClassType outer);

    /** The class's outer class if this is an inner class, or null. */
    ClassType outer();
}