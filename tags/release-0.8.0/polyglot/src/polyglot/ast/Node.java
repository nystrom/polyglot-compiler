package jltools.ast;

import jltools.util.CodeWriter;
import jltools.util.Position;
import jltools.util.Copy;
import jltools.types.Context;
import jltools.types.SemanticException;
import jltools.types.TypeSystem;
import jltools.visit.TypeBuilder;
import jltools.visit.TypeAmbiguityRemover;
import jltools.visit.AmbiguityRemover;
import jltools.visit.ConstantFolder;
import jltools.visit.TypeChecker;
import jltools.visit.ExceptionChecker;
import jltools.visit.Translator;

import java.io.Serializable;

/**
 * A <code>Node</code> represents an AST node.  All AST nodes must implement
 * this interface.  Nodes should be immutable: methods which set fields
 * of the node should copy the node, set the field in the copy, and then
 * return the copy.
 */
public interface Node extends Copy, Serializable
{
    /**
     * Return the delegate for this node.  Some operations on the node should
     * be invoked only through the delegate, for instance as:
     * <pre>
     *    n.delegate().typeCheck(c)
     * </pre>
     * rather than:
     * <pre>
     *    n.typeCheck_(c)
     * </pre>
     */
    Ext ext();

    /** Create a copy of the node with a new delegate. */
    Node ext(Ext ext);

    /** Get the position of the node in the source file.  Returns null if
     * the position is not set. */
    Position position();

    /** Create a copy of the node with a new position. */
    Node position(Position position);

    /** Clone the node. */
    Object copy();

    /**
     * Visit the node.  This method is called by a <code>NodeVisitor</code> to
     * traverse the AST starting at this node.  This method should call the
     * <code>override</code>, <code>enter</code>, and <code>leave<code> methods
     * of the visitor.  The method may return a new version of the node.
     *
     * @param v The visitor which will traverse/rewrite the AST.
     * @return A new AST if a change was made, or <code>this</code>.
     */
    Node visit(NodeVisitor v);

    /**
     * Visit the children of the node.
     */
    Node visitChildren(NodeVisitor v);

    /**
     * Adjust the environment on entering the scope of the method.
     */
    void enterScope(Context c);

    /**
     * Adjust the environment on leaving the scope of the method.
     */
    void leaveScope(Context c);

    /**
     * This method can be called after a node is visited to ensure its
     * type information is reconstructed.
     */
    Node reconstructTypes_(NodeFactory nf, TypeSystem ts, Context c)
	throws SemanticException;

    // Implementations of the default passes.  These methods should only
    // be called through the delegate.

    /**
     * Collects classes, methods, and fields from the AST rooted at this node
     * and constructs type objects for these.  These type objects may be
     * ambiguous.
     *
     * @param cb The visitor which adds new type objects to the
     * <code>TypeSystem</code>.
     */
    Node buildTypesOverride_(TypeBuilder tb) throws SemanticException;
    Node buildTypes_(TypeBuilder tb) throws SemanticException;

    /**
     * Constructs a context for each type declared in the AST.
     * A context is used to look up names when cleaning class signatures.
     *
     * @param cb The visitor which builds contexts.
     */
    Node disambiguateTypesOverride_(TypeAmbiguityRemover sc) throws SemanticException;
    Node disambiguateTypes_(TypeAmbiguityRemover sc) throws SemanticException;

    /**
     * Remove any remaining ambiguities from the AST.
     *
     * @param ar The visitor which disambiguates.
     */
    Node disambiguateOverride_(AmbiguityRemover ar) throws SemanticException;
    Node disambiguate_(AmbiguityRemover ar) throws SemanticException;

    /**
     * Fold constants in the AST.
     *
     * @param cf The constant folding visitor.
     */
    Node foldConstantsOverride_(ConstantFolder cf);
    Node foldConstants_(ConstantFolder cf);

    /**
     * Type check the AST.
     *
     * @param tc The type checking visitor.
     */
    Node typeCheckOverride_(TypeChecker tc) throws SemanticException;
    Node typeCheck_(TypeChecker tc) throws SemanticException;

    /**
     * Check that exceptions are properly propagated throughout the AST.
     *
     * @param ec The visitor.
     */
    Node exceptionCheckOverride_(ExceptionChecker ec) throws SemanticException;
    Node exceptionCheck_(ExceptionChecker ec) throws SemanticException;

    /**
     * Translate the AST using the given code writer.
     *
     * @param w The code writer to which to write.
     * @param tr The translation pass.  This is <i>not</i> a visitor.
     */
    void translate_(CodeWriter w, Translator tr);

    /**
     * Dump the AST node for debugging purposes.
     */
    void dump(CodeWriter w);
}