package polyglot.types;

import java.util.HashMap;
import java.util.Map;

import polyglot.frontend.*;
import polyglot.util.InternalCompilerError;

public class SymbolTable_c implements SymbolTable {
    Map<String,Symbol<? extends TypeObject>> map;

    public SymbolTable_c() {
        this.map = new HashMap<String,Symbol<? extends TypeObject>>();
    }
    
    public <T extends TypeObject> Symbol<T> symbol(T v) {
        return symbol(v, Globals.currentPhase());
    }
    
    public <T extends TypeObject> TypeRef<T> typeRef() {
        TypeRef_c<T> sym = new TypeRef_c<T>();
        map.put(sym.name, sym);
        return sym;
    }

    public <T extends TypeObject> Symbol<T> symbol(T v, GoalSet view) {
        Symbol_c<T> sym = new Symbol_c<T>(v, view);
        v.setSymbol(sym);
        map.put(sym.name, sym);
        return sym;
    }

//    public <T extends TypeObject> TypeRef<T> typeRef(T v) {
//        TypeRef_c<T> sym = new TypeRef_c<T>(v);
//        v.setSymbol(sym);
//        map.put(sym.name, sym);
//        return sym;
//    }

    public <T extends TypeObject> TypeRef<T> typeRef(T v, Goal goal) {
        TypeRef_c<T> sym = new TypeRef_c<T>(v, goal);
        v.setSymbol(sym);
        map.put(sym.name, sym);
        return sym;
    }

    public <T extends TypeObject> Symbol<T> update(String key, T v) {
        return update(key, v, Globals.currentPhase());
    }
    
    public <T extends TypeObject> Symbol<T> update(String key, T v, GoalSet view) {
        Symbol<T> sym = this.<T>get(key);
        
        if (sym == null) {
            sym = new Symbol_c<T>(v, view);
            map.put(key, sym);
        }
        else {
            sym.update(v, view);
        }
        
        return sym;
    }
    
    public <T extends TypeObject> Symbol<T> insert(String key, T v) {
        return insert(key, v, Globals.currentPhase());
    }
    
    public <T extends TypeObject> Symbol<T> insert(String key, T v, GoalSet view) {
        Symbol<T> sym = this.<T>get(key);
       
        if (sym == null) {
            sym = new Symbol_c<T>(v, view);
            map.put(key, sym);
        }
        else {
            throw new InternalCompilerError("Symbol with key " + key + " already exists.", v.position());
        }
        
        return sym;
    }
    
    public <T extends TypeObject> T find(String key) throws SemanticException {
        return this.<T>find(key, Globals.currentView());
    }
    
    public <T extends TypeObject> T find(String key, GoalSet view) throws SemanticException { 
        Symbol<T> sym = this.<T>get(key);
        
        if (sym != null) {
            return sym.get(view);
        }
        else {
            throw new SemanticException(key + " not found.");
        }
    }
    
    public <T extends TypeObject> Symbol<T> get(String key) {
        return (Symbol<T>) map.get(key);
    }
}
