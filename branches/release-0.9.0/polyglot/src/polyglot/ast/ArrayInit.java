package polyglot.ast;

import polyglot.types.*;
import java.util.List;

/**
 * An <code>ArrayInit</code> is an immutable representation of
 * an array initializer, such as { 3, 1, { 4, 1, 5 } }.  Note that
 * the elements of these array may be expressions of any type (e.g.,
 * <code>Call</code>).
 */
public interface ArrayInit extends Expr
{
    /**
     * Initializer elements.
     * A list of <code>Expr</code>.
     * @see polyglot.ast.Expr
     */
    List elements();

    /**
     * Set the initializer elements.
     * A list of <code>Expr</code>.
     * @see polyglot.ast.Expr
     */
    ArrayInit elements(List elements);

    /**
     * Type check the individual elements of the array initializer against
     * the left-hand-side type.
     */
    void typeCheckElements(Type lhsType) throws SemanticException;
}