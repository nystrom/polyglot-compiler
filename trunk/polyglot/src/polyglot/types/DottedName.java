package polyglot.types;

import java.util.HashMap;

import polyglot.util.StringUtil;

public class DottedName {
    DottedName prefix;
    ShortName name;
    int hash;

    private DottedName(DottedName prefix, ShortName name, int hash) {
        this.prefix = prefix;
        this.name = name;
        this.hash = hash;
    }

    static HashMap<String,DottedName> internCache = new HashMap<String,DottedName>();

    public static DottedName mkIdentifier(String name) {
        name = name.intern();
        
        assert StringUtil.isNameShort(name);
        
        DottedName dn = internCache.get(name);

        if (dn == null) {
            ShortName sn = ShortName.mkIdentifier(name);
            dn = new DottedName(null, sn, name.hashCode());
            internCache.put(name, dn);
        }

        return dn;
    }

    public DottedName mkDotted(String fullName) {
        if (StringUtil.isNameShort(fullName)) {
            return mkIdentifier(fullName);
        }
        
        fullName = fullName.intern();

        DottedName dn = internCache.get(fullName);

        if (dn == null) {
            String prefixName = StringUtil.getPackageComponent(fullName);
            String shortName = StringUtil.getShortNameComponent(fullName);

            DottedName pn = mkDotted(prefixName);
            ShortName sn = ShortName.mkIdentifier(shortName);

            dn = new DottedName(pn, sn, fullName.hashCode());
            internCache.put(fullName, dn);
        }

        return dn;
    }

    public boolean equals(Object o) {
        return this == o;
    }

    public int hashCode() {
        return hash;
    }

    public String toString() {
        return (prefix != null ? prefix.toString() + "." : "") + name.toString();
    }
}
