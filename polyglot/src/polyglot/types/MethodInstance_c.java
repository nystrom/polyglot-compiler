package polyglot.ext.jl.types;

import polyglot.types.*;
import polyglot.util.*;
import java.util.*;

/**
 * A <code>MethodInstance</code> represents the type information for a Java
 * method.
 */
public class MethodInstance_c extends ProcedureInstance_c
                                implements MethodInstance
{
    protected String name;
    protected Type returnType;

    /** Used for deserializing types. */
    protected MethodInstance_c() { }

    public MethodInstance_c(TypeSystem ts, Position pos,
	 		    ReferenceType container,
	                    Flags flags, Type returnType, String name,
			    List argTypes, List excTypes) {
        super(ts, pos, container, flags, argTypes, excTypes);
	this.returnType = returnType;
	this.name = name;
    }

    public MethodInstance flags(Flags flags) {
        MethodInstance_c n = (MethodInstance_c) copy();
	n.flags = flags;
	return n;
    }

    public String name() {
        return name;
    }

    public MethodInstance name(String name) {
        MethodInstance_c n = (MethodInstance_c) copy();
	n.name = name;
	return n;
    }

    public Type returnType() {
        return returnType;
    }

    public MethodInstance returnType(Type returnType) {
        MethodInstance_c n = (MethodInstance_c) copy();
	n.returnType = returnType;
	return n;
    }

    public MethodInstance argumentTypes(List l) {
        MethodInstance_c n = (MethodInstance_c) copy();
	n.argTypes = new ArrayList(l);
	return n;
    }

    public MethodInstance exceptionTypes(List l) {
        MethodInstance_c n = (MethodInstance_c) copy();
	n.excTypes = new ArrayList(l);
	return n;
    }

    public MethodInstance container(ReferenceType container) {
        MethodInstance_c n = (MethodInstance_c) copy();
	n.container = container;
	return n;
    }

    public int hashCode() {
        return container.hashCode() + flags.hashCode() +
	       returnType.hashCode() + name.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof MethodInstance) {
	    MethodInstance i = (MethodInstance) o;
	    return ts.isSame(returnType, i.returnType())
	        && name.equals(i.name())
		&& super.equals(i);
	}

	return false;
    }

    public String toString() {
	String s = designator() + " " + flags.translate() + returnType + " " +
                   signature();

	if (! excTypes.isEmpty()) {
	    s += " throws ";

	    for (Iterator i = excTypes.iterator(); i.hasNext(); ) {
		Type t = (Type) i.next();
		s += t.toString();

		if (i.hasNext()) {
		    s += ", ";
		}
	    }
	}

	return s;
    }

    public String signature() {
        String s = name + "(";

        for (Iterator i = argTypes.iterator(); i.hasNext(); ) {
            Type t = (Type) i.next();
            s += t.toString();

            if (i.hasNext()) {
                s += ",";
            }
        }

        s += ")";

        return s;
    }

    public String designator() {
        return "method";
    }


    public TypeObject restore_() throws SemanticException {
	ReferenceType c = (ReferenceType) container.restore();
	Type t = (Type) returnType.restore();
	List a = restoreList(argTypes);
	List e = restoreList(excTypes);

	MethodInstance mi = this;

	if (c != container) mi = mi.container(c);
	if (t != returnType) mi = mi.returnType(t);
	if (a != argTypes) mi = mi.argumentTypes(a);
	if (e != excTypes) mi = mi.exceptionTypes(e);

	return mi;
    }

    /** Returns true iff <this> is the same method as <m> */
    public boolean isSameMethod(TypeSystem ts, MethodInstance m) {
        return this.name().equals(m.name()) && ts.hasSameArguments(this, m);
    }

    public List overrides(TypeSystem ts) {
        List l = new LinkedList();

        Type t = container().superType();

        while (t instanceof ReferenceType) {
            ReferenceType rt = (ReferenceType) t;
            t = rt.superType();

            for (Iterator i = rt.methods(name, argTypes).iterator(); i.hasNext(); ) {
                MethodInstance mi = (MethodInstance) i.next();
                l.add(mi);
            }
        }

        return l;
    }

    public boolean isCanonical() {
	return container.isCanonical()
	    && returnType.isCanonical()
	    && listIsCanonical(argTypes)
	    && listIsCanonical(excTypes);
    }

    public boolean methodCallValid(TypeSystem ts, MethodInstance call) {
        return name().equals(call.name()) && ts.callValid(this, call);
    }

    public boolean methodCallValid(TypeSystem ts, String name, List argTypes) {
        return name().equals(name) && ts.callValid(this, argTypes);
    }

    public boolean canOverride(TypeSystem ts, MethodInstance mj) {
        MethodInstance mi = this;

        if (! ts.isSame(mi.returnType(), mj.returnType())) {
            return false;
        } 

        if (! ts.throwsSubset(mi, mj)) {
            return false;
        }   

        if (mi.flags().moreRestrictiveThan(mj.flags())) {
            return false;
        }

        if (! mi.flags().isStatic() && mj.flags().isStatic()) {
            return false;
        }

        if (mj.flags().isFinal()) {
            return false;
        }

        return true;
    }
}
