//******************************************************************** 
//* Licensed Materials - Property of IBM 
//* Thorn programming language processor 
//* (C) Copyright IBM Corp. 2009. All Rights Reserved. 
//* US Government Users Restricted Rights - Use, duplication or 
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
//******************************************************************** 

package polyglot.interp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import polyglot.frontend.Globals;
import polyglot.frontend.Goal;
import polyglot.types.QName;
import polyglot.util.InternalCompilerError;

/**
 * Java code is run by an Evaluator. When an Evaluator needs to load a class, it
 * uses a ThornClassLoader (eval.loader()). When a ThornClassLoader needs to
 * compile a .th file, it uses a Compiler (eval.loader().compiler()).
 * 
 * @author nate, jv
 */
final public class Evaluator {

    // final private Compiler compiler = Compiler.make();
    private ThornClassLoader loader = null;

    static public Evaluator make() {
	final Evaluator result = new Evaluator();
	return result;
    }

    private Evaluator() {}

    public BytecodeCache bytecodeCache() {
	return Globals.Extension().bytecodeCache();
    }

    public ThornClassLoader loader() {
	if (loader == null)
	    loader = new ThornClassLoader(Globals.Extension().bytecodeCache());
	return loader;
    }

    /** Evaluate the code in the given compilation unit. */
    public void eval(final QName mainClass, final String[] args) {
	Goal.Status s = polyglot.frontend.Globals.Scheduler().BytecodeCached(mainClass).get();

	if (s != Goal.Status.SUCCESS) {
	    return;
	}
	try {
	    Class<?> c = loader().loadClass(mainClass.toString());
	    Method m = c.getDeclaredMethod("main", new String[0].getClass());
	    m.invoke(null, (Object) args);
	}
	catch (final ClassNotFoundException e1) {
	    e1.printStackTrace();
	    throw new InternalCompilerError("Could not find main class: " + mainClass, e1);
	}
	catch (final IllegalAccessException e1) {
	    throw new InternalCompilerError("Could not instantiate main class: " + mainClass, e1);
	}
	catch (final NoSuchMethodException e1) {
	    e1.printStackTrace();
	    throw new InternalCompilerError("Could not call main method of class: " + mainClass, e1);
	}
	catch (IllegalArgumentException e1) {
	    e1.printStackTrace();
	    throw new InternalCompilerError("Could not call main method of class: " + mainClass, e1);
	}
	catch (InvocationTargetException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
    }

    public void exit(final int status) {
	throw new polyglot.main.Main.TerminationException(status);
    }

    boolean running;

    public boolean isRunning() {
	return running;
    }

    @Override
    public String toString() {
	return "Evaluator";
    }
}
