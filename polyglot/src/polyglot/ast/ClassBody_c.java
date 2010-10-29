/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

/**
 * A <code>ClassBody</code> represents the body of a class or interface
 * declaration or the body of an anonymous class.
 */
public class ClassBody_c extends Term_c implements ClassBody
{
    protected List<ClassMember> members;

    public ClassBody_c(Position pos, List<ClassMember> members) {
        super(pos);
        assert(members != null);
        this.members = TypedList.copyAndCheck(members, ClassMember.class, true);
    }

    public List<ClassMember> members() {
        return this.members;
    }

    public ClassBody members(List<ClassMember> members) {
        ClassBody_c n = (ClassBody_c) copy();
        n.members = TypedList.copyAndCheck(members, ClassMember.class, true);
        return n;
    }

    public ClassBody addMember(ClassMember member) {
        ClassBody_c n = (ClassBody_c) copy();
        List<ClassMember> l = new ArrayList<ClassMember>(this.members.size() + 1);
        l.addAll(this.members);
        l.add(member);
        n.members = TypedList.copyAndCheck(l, ClassMember.class, true);
        return n;
    }

    protected ClassBody_c reconstruct(List<ClassMember> members) {
        if (! CollectionUtil.<ClassMember>allEqual(members, this.members)) {
            ClassBody_c n = (ClassBody_c) copy();
            n.members = TypedList.copyAndCheck(members,
                                               ClassMember.class, true);
            return n;
        }

        return this;
    }

    public Node visitChildren(NodeVisitor v) {
        List<ClassMember> members = visitList(this.members, v);
        return reconstruct(members);
    }

    public String toString() {
        return "{ ... }";
    }

    private static final Collection<String> TOPICS = 
                CollectionUtil.list(Report.types, Report.context);
     
}
