/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.frontend.ExtensionInfo;
import polyglot.main.Report;
import polyglot.util.*;

/**
 * The <code>SystemResolver</code> is the main resolver for
 * fully-qualified names.
 */
public class SystemResolver extends CachingResolver implements TopLevelResolver {
    protected Map<String,Boolean> packageCache;
    protected ExtensionInfo extInfo;
    
    /**
     * Create a caching resolver.
     * @param inner The resolver whose results this resolver caches.
     */
    public SystemResolver(TopLevelResolver inner, ExtensionInfo extInfo) {
        super(inner);
        this.extInfo = extInfo;
        this.packageCache = new HashMap<String, Boolean>();
    }

    public Object copy() {
        SystemResolver r = (SystemResolver) super.copy();
        r.packageCache = new HashMap<String, Boolean>(this.packageCache);
        return r;
    }
    
    public void installInAll(String name, Named n) {
        this.install(name, n);
    }

    public boolean installedInAll(String name, Named q) {
        if (check(name) != q) {
            return false;
        }
        return true;
    }

    /** Check if a package exists in the resolver cache. */
    protected boolean packageExistsInCache(String name) {
        for (Iterator i = cachedObjects().iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof Importable) {
                Importable im = (Importable) o;
                if (im.package_() != null &&
                    im.package_().fullName() != null &&
                    (im.package_().fullName().equals(name) ||
                     im.package_().fullName().startsWith(name + "."))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a package exists.
     */
    public boolean packageExists(String name) {
	Boolean b = packageCache.get(name);
	if (b != null) {
	    return b.booleanValue();
	}
	else {
            String prefix = StringUtil.getPackageComponent(name);

            if (packageCache.get(prefix) == Boolean.FALSE) {
                packageCache.put(name, Boolean.FALSE);
                return false;
            }

            boolean exists;
            exists = packageExistsInCache(name);
            if (! exists) {
                exists = ((TopLevelResolver) inner).packageExists(name);
            }

            if (exists) {
                packageCache.put(name, Boolean.TRUE);

                do {
                    packageCache.put(prefix, Boolean.TRUE);
                    prefix = StringUtil.getPackageComponent(prefix);
                } while (! prefix.equals(""));
            }
            else {
                packageCache.put(name, Boolean.FALSE);
            }

            return exists;
	}
    }

    protected void cachePackage(Package p) {
        if (p != null) {
            packageCache.put(p.fullName(), Boolean.TRUE);
            Package prefix = Types.get(p.prefix());
            cachePackage(prefix);
        }
    }

    /**
     * Check if a type is in the cache, returning null if not.
     * @param name The name to search for.
     */
    public Type checkType(String name) {
        return (Type) check(name);
    }

    /**
     * Find a type (or package) by name. For most code, this should be called
     * with the Java source name (p.A.B), not the class file name (p.A$B). The
     * exceptions are for resolving names in deserialized types and in types
     * loaded from raw class files.
     */
    public Named find(Matcher<Named> matcher) throws SemanticException {
        Named n = super.find(matcher);

        if (Report.should_report(TOPICS, 2))
            Report.report(2, "Returning from SR.find(" + matcher + "): " + n);

        return n;
    }

    public void install(String name, Named q) {
        if (Report.should_report(TOPICS, 2) && check(name) == null)
            Report.report(2, "SR installing " + name + "->" + q);
        
        super.install(name, q);
    }

    /**
     * Install a qualifier in the cache.
     * @param name The name of the qualifier to insert.
     * @param q The qualifier to insert.
     */
    public void addNamed(String name, Named q) throws SemanticException {
        super.addNamed(name, q);

        if (q instanceof ClassType) {
            ClassType ct = (ClassType) q;
            String containerName = StringUtil.getPackageComponent(name);
            if (ct.isTopLevel()) {
                Package p = ((ClassType) q).package_();
                cachePackage(p);
                if (p != null && containerName.equals(p.fullName())) {
                    addNamed(containerName, p);
                }
            }
            else if (ct.isMember()) {
                if (name.equals(ct.fullName())) {
                    // Check that the names match; we could be installing
                    // a member class under its class file name, not its Java
                    // source full name.
                    addNamed(containerName, ct.outer());
                }
            }
        }
        else if (q instanceof Package) {
            Package p = (Package) q;
            cachePackage(p);
            String containerName = StringUtil.getPackageComponent(name);
            Package prefix = Types.get(p.prefix());
            if (prefix != null && containerName.equals(prefix.fullName())) {
                addNamed(containerName, prefix);
            }
        }

        if (q instanceof Type && packageExists(name)) {
            throw new SemanticException("Type \"" + name +
                        "\" clashes with package of the same name.", q.position());
        }
    }

    private static final Collection<String> TOPICS =
                    CollectionUtil.list(Report.types,
                                        Report.resolver,
                                        "sysresolver");
}
