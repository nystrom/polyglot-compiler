package polyglot.types;

import polyglot.util.*;
import java.util.List;

/**
 * A context represents a stack of scopes used for looking up types, methods,
 * and variables.
 */
public interface Context extends Resolver, Copy
{
    /** Pop the context. */
    Context pop();

    /** The type system. */
    TypeSystem typeSystem();

    /** Add a variable to the current scope. */
    void addVariable(VarInstance vi);

    /** Add a method to the current scope. */
    void addMethod(MethodInstance mi);

    /** Add a type to the current scope. */
    void addType(NamedType t);

    /** Looks up a method in the current scope. */
    MethodInstance findMethod(String name, List argumentTypes) throws SemanticException;

    /** Looks up a local variable or field in the current scope. */
    VarInstance findVariable(String name) throws SemanticException;

    /** Looks up a local variable or field in the current scope. */
    VarInstance findVariableSilent(String name);

    /** Looks up a local variable in the current scope. */
    LocalInstance findLocal(String name) throws SemanticException;

    /** Looks up a field in the current scope. */
    FieldInstance findField(String name) throws SemanticException;

    /**
     * Finds the class which added a field to the scope.
     * This is usually a subclass of <code>findField(name).container()</code>.
     */
    ClassType findFieldScope(String name) throws SemanticException;

    /**
     * Finds the class which added a method to the scope.
     * This is usually a subclass of <code>findMethod(name).container()</code>.
     */
    ClassType findMethodScope(String name) throws SemanticException;

    /** Get import table currently in scope. */
    ImportTable importTable();

    /** Get the outer-most resolver for the source file currently in scope.
     * This is usually just the import table.
     */
    Resolver outerResolver();

    /** Enter the scope of a source file. */
    Context pushSource(ImportTable it);

    /** Enter the scope of a class. */
    Context pushClass(ParsedClassType scope, ClassType type);

    /** Enter the scope of a method or constructor. */
    Context pushCode(CodeInstance f);

    /** Enter the scope of a block. */
    Context pushBlock();

    /** Return whether innermost non-block scope is a code scope. */
    boolean inCode();

    /** Returns whether the symbol is defined within the current method. */
    boolean isLocal(String name);

    /** Return the innermost class in scope. */
    ClassType currentClass();
 
    /** Return the innermost class in scope. */
    ParsedClassType currentClassScope();

    /** Return the innermost method or constructor in scope. */
    CodeInstance currentCode();
}
