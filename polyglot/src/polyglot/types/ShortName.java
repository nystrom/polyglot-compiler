package polyglot.types;

import java.util.HashMap;

import polyglot.util.StringUtil;
import polyglot.util.UniqueID;

public class ShortName {
    String name;
    int hash;

    private ShortName(String name, int hash) {
        this.name = name;
        this.hash = hash;
    }

    static HashMap<String,ShortName> internCache = new HashMap<String,ShortName>();

    public static ShortName mkNewIdentifier() {
        return mkNewIdentifier("id");
    }
    
    public static ShortName mkNewIdentifier(String prefix) {
        return mkIdentifier(UniqueID.newID(prefix));
    }
    
    public static ShortName mkIdentifier(String name) {
        name = name.intern();
        
        assert StringUtil.isNameShort(name);
        
        ShortName n = internCache.get(name);

        if (n == null) {
            n = new ShortName(name, name.hashCode());
            internCache.put(name, n);
        }

        return n;
    }

    public boolean equals(Object o) {
        return this == o;
    }

    public int hashCode() {
        return hash;
    }

    public String toString() {
        return name;
    }
}
