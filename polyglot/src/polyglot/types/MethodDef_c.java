/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.List;

import polyglot.util.CollectionUtil;
import polyglot.util.Position;

/**
 * A <code>MethodInstance</code> represents the type information for a Java
 * method.
 */
public class MethodDef_c extends ProcedureDef_c
                                implements MethodDef
{
    protected String name;
    protected Ref<? extends Type> returnType;

    /** Used for deserializing types. */
    protected MethodDef_c() { }

    public MethodDef_c(TypeSystem ts, Position pos,
	 		    Ref<? extends ReferenceType> container,
	                    Flags flags, Ref<? extends Type> returnType, String name,
			    List<Ref<? extends Type>> formalTypes, List<Ref<? extends Type>> throwTypes) {
        super(ts, pos, container, flags, formalTypes, throwTypes);
	this.returnType = returnType;
	this.name = name;
    }
    
    public MethodType asType() {
        return new MethodType_c(ts, position(), Ref_c.<MethodDef>ref(this));
    }

    public String name() {
        return name;
    }

    public Ref<? extends Type> returnType() {
        return returnType;
    }

    public MethodDef flags(Flags flags) {
        if (!flags.equals(this.flags)) {
            MethodDef_c n = (MethodDef_c) copy();
            n.setFlags(flags);
            return n;
        }
        return this;
    }

    public MethodDef name(String name) {
        if ((name != null && !name.equals(this.name)) ||
            (name == null && name != this.name)) {
            MethodDef_c n = (MethodDef_c) copy();
            n.setName(name);
            return n;
        }
        return this;
    }

    public MethodDef returnType(Ref<? extends Type> returnType) {
        if (this.returnType != returnType) {
            MethodDef_c n = (MethodDef_c) copy();
            n.setReturnType(returnType);
            return n;
        }
        return this;
    }

    public MethodDef formalTypes(List<Ref<? extends Type>> l) {
        if (!CollectionUtil.equals(this.formalTypes, l)) {
            MethodDef_c n = (MethodDef_c) copy();
            n.setFormalTypes(l);
            return n;
        }
        return this;
    }

    public MethodDef throwTypes(List<Ref<? extends Type>> l) {
        if (!CollectionUtil.equals(this.throwTypes, l)) {
            MethodDef_c n = (MethodDef_c) copy();
            n.setThrowTypes(l);
            return n;
        }
        return this;
    }

    public MethodDef container(Ref<? extends ReferenceType> container) {
        if (this.container != container) {
            MethodDef_c n = (MethodDef_c) copy();
            n.setContainer(container);
            return n;
        }
        return this;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * @param returnType The returnType to set.
     */
    public void setReturnType(Ref<? extends Type> returnType) {
        this.returnType = returnType;
    }
    
    public int hashCode() {
        //return container.hashCode() + flags.hashCode() +
	//       returnType.hashCode() + name.hashCode();
	return flags.hashCode() + name.hashCode();
    }

    public boolean equalsImpl(TypeObject o) {
        return o == this;
//        if (o instanceof MethodDef) {
//	    MethodDef i = (MethodDef) o;
//	    return ts.equals(returnType, i.returnType())
//	        && name.equals(i.name())
//                && ts.equals(container, i.container())
//		&& super.equalsImpl(i);
//	}
//
//	return false;
    }

    public String toString() {
	String s = designator() + " " + flags.translate() + returnType + " " +
                   container() + "." + signature();

	if (! throwTypes.isEmpty()) {
	    s += " throws " + TypeSystem_c.listToString(throwTypes);
	}

	return s;
    }

    public String signature() {
        return name + "(" + TypeSystem_c.listToString(formalTypes) + ")";
    }

    public String designator() {
        return "method";
    }
}
