//******************************************************************** 
//* Licensed Materials - Property of IBM 
//* Thorn programming language processor 
//* (C) Copyright IBM Corp. 2009. All Rights Reserved. 
//* US Government Users Restricted Rights - Use, duplication or 
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
//******************************************************************** 

package polyglot.interp;

import java.util.*;

import polyglot.frontend.Goal;
import polyglot.main.Report;
import polyglot.types.*;

public class ThornClassLoader extends ClassLoader {

    final private Map<String,Class<?>> classes = new HashMap<String,Class<?>>();
    final private Set<String> nocache = new HashSet<String>();
    final private BytecodeCache cache;

    public ThornClassLoader(final BytecodeCache cache_) {
    	cache = cache_;
    }

    public BytecodeCache bytecodeCache() {
    	return cache;
    }

    @Override
	public String toString() {
    	return "ThornClassLoader";
    }

    public final static String GENERATED_CODE_PACKAGE = "polyglot.generated";
    
    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        if (Report.should_report("classloader", 1))
            Report.report(1, this + " finding " + name);
        if (classes.get(name) != null) return classes.get(name);

        byte[] buf = cache.getBytes(name);

        if (buf == null) {
            if (name.startsWith(GENERATED_CODE_PACKAGE + ".dispatch.I"))
                new DispatcherMaker().create(name, cache);
            else if (name.startsWith(GENERATED_CODE_PACKAGE + ".Frame$")) {
                if (Report.should_report("classloader", 1))
                    Report.report(1, "frame class " + name);
                new FrameMaker().create(name, cache);
            }
            else {
        	QName symName = QName.make(name);
                if (Report.should_report("classloader", 1))
                    Report.report(1, "Java name " + name + " => " + symName);

                Goal g = polyglot.frontend.Globals.Scheduler().BytecodeCached(symName);
                g.get();
            }

            buf = cache.getBytes(name);
        }

        // Try the cache again.
         if (classes.get(name) != null) return classes.get(name);


        if (buf == null) {
            nocache.add(name);
            throw new ClassNotFoundException(name);
        }

        try {
            if (Report.should_report("classloader", 1))
                Report.report(1, this + " defining " + name);
            final Class<?> c = defineClass(name, buf, 0, buf.length);
            classes.put(name, c);
            // remove bytecode from cache to free up space
            cache.remove(name);
            return c;
        }
        catch (final ClassFormatError e) {
            nocache.add(name);
            throw e;
        }
    }

    public void add(final String javaClassName, final byte[] code) {
    	cache.put(null, javaClassName, null, code);
        nocache.remove(javaClassName);  // make sure we actually look for it next time we try to load it
    }
}
