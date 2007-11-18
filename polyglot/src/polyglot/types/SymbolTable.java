package polyglot.types;

import polyglot.frontend.Goal;
import polyglot.frontend.GoalSet;


public interface SymbolTable {
    /** Find a symbol.  Ensure it is up-to-date with the current view.  Throw exception if not found. */
    <T extends TypeObject> T find(String key) throws SemanticException;
    
    /** Find the symbol.  Ensure it is up-to-date with the given view.  Throw exception if not found. */
    <T extends TypeObject> T find(String key, GoalSet view) throws SemanticException;
    
    /** Create a symbol. */
    <T extends TypeObject> Symbol<T> symbol(T v);
    <T extends TypeObject> Symbol<T> symbol(T v, GoalSet view);
    
    <T extends TypeObject> TypeRef<T> typeRef();
//    <T extends TypeObject> TypeRef<T> typeRef(T v);
    <T extends TypeObject> TypeRef<T> typeRef(T v, Goal goal);

    /** Insert a symbol for the given object.  The symbol should not already be in the table. */
    <T extends TypeObject> Symbol<T> insert(String key, T v);

    /** Insert a symbol for the given object.  The symbol should not already be in the table. */
    <T extends TypeObject> Symbol<T> insert(String key, T v, GoalSet view);

    /** Insert a symbol for the given object.  If a symbol with the given key is present in the table, update its value. */
    <T extends TypeObject> Symbol<T> update(String key, T v);
    
    /** Insert a symbol for the given object.  If a symbol with the given key is present in the table, update its value. */
    <T extends TypeObject> Symbol<T> update(String key, T v, GoalSet view);
    
    /** Find the symbol.  Return null if not found. */
    <T extends TypeObject> Symbol<T> get(String key);
}
