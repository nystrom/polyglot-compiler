/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import java.util.*;

import polyglot.frontend.Job;
import polyglot.frontend.Source;
import polyglot.util.*;

/**
 * ParsedClassType
 *
 * Overview: 
 * A ParsedClassType represents a information that has been parsed (but not
 * necessarily type checked) from a .java file.
 **/
public class ParsedClassType_c extends ClassType_c implements ParsedClassType
{
    protected ParsedClassType_c() {
	super();
    }
    
    public Job job() {
        return def().job();
    }
    
    public ParsedClassType_c(ClassDef def) {
        this(def.typeSystem(), def.position(), Ref_c.ref(def));
    }

    public ParsedClassType_c(TypeSystem ts, Position pos, Ref<ClassDef> def) {
        super(ts, pos, def);
    }
    
    public Source fromSource() {
        return def().fromSource();
    }
    
    public ClassDef.Kind kind() {
        return def().kind();
    }

    public boolean inStaticContext() {
        return def().inStaticContext();
    }
    
    public ClassType outer() {
        ClassDef outer = get(def().outer());
        if (outer == null) return null;
        return outer.asType();
    }

    public String name() {
        return def().name();
    }

    /** Get the class's super type. */
    public Type superType() {
        return get(def().superType());
    }

    /** Get the class's package. */
    public Package package_() {
        return get(def().package_());
    }

    /** Get the class's flags. */
    public Flags flags() {
        return def().flags();
    }
    
    public boolean defaultConstructorNeeded() {
        if (flags().isInterface()) {
            return false;
        }
        return def().constructors().isEmpty();
    }
    
    /** Return an immutable list of constructors */
    public List<ConstructorType> constructors() {
        return new TransformingList<ConstructorDef,ConstructorType>(
                                    def().constructors(),
                                    new ConstructorAsTypeTransform());
}

    /** Return an immutable list of member classes */
    public List<ClassType> memberClasses() {
        return new TransformingList<ClassDef,ClassType>(def().memberClasses(),
                                    new ClassAsTypeTransform());
    }

    /** Return an immutable list of methods. */
    public List<MethodType> methods() {
        return new TransformingList<MethodDef,MethodType>(
                                    def().methods(),
                                    new MethodAsTypeTransform());
    }
    
    /** Return a list of all methods with the given name. */
    public List<MethodType> methodsNamed(String name) {
        List<MethodType> l = new ArrayList<MethodType>();
        for (MethodType mi : methods()) {
            if (mi.name().equals(name)) {
                l.add(mi);
            }
        }

        return l;
    }

    /** Return an immutable list of fields */
    public List<FieldType> fields() {
        return new TransformingList<FieldDef, FieldType>(def().fields(),
                                                         new FieldAsTypeTransform());
    }
    
    /** Get a field of the class by name. */
    public FieldType fieldNamed(String name) {
        for (Iterator i = fields().iterator(); i.hasNext(); ) {
            FieldType fi = (FieldType) i.next();
            if (fi.name().equals(name)) {
                return fi;
            }
        }

        return null;
    }

    /** Return an immutable list of interfaces */
    public List<Type> interfaces() {
        return new TransformingList<Ref<? extends Type>, Type>(
                                                    def().interfaces(),
                                                    new DerefTransform());
    }

    public String toString() {
        if (! def.nonnull()) {
            return "<unknown class>";
        }
        
        return def().toString();
    }
}
