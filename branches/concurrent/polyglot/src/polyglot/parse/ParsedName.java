/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.parse;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.*;

/**
 * Represents an ambiguous, possibly qualified, identifier encountered while parsing.
 */
public class ParsedName {
    public final ParsedName prefix;
    public final Id name;
    public final Position pos;
    public final NodeFactory nf;
    public final TypeSystem ts;

    public ParsedName(NodeFactory nf, TypeSystem ts, Position pos, Id name) {
        this(nf, ts, pos, null, name);
    }
    
    public ParsedName(NodeFactory nf, TypeSystem ts, Position pos, ParsedName prefix, Id name) {
        this.nf = nf;
        this.ts = ts;
        this.pos = pos != null ? pos : Position.COMPILER_GENERATED;
        this.prefix = prefix;
        this.name = name;
    }

    // expr
    public Expr toExpr() {
	return nf.AmbExpr(pos, toPrefix());
    }

    // expr or type
    public Receiver toReceiver() {
	return nf.AmbReceiver(pos, toPrefix());
    }

    // expr, type, or package
    public Node toPrefix() {
        if (prefix == null) {
            return nf.QualifiedName(pos, null, name);
        }

        return nf.QualifiedName(pos, prefix.toPrefix(), name);
    }

    // type or package
    public QualifierNode toQualifier() {
	return nf.AmbQualifierNode(pos, toPrefix());
    }

    // package
    public PackageNode toPackage() {
        if (prefix == null) {
            return nf.PackageNode(pos, Types.ref(ts.createPackage((Ref<? extends Package>) null, name.id())));
        }
        else {
            return nf.PackageNode(pos, Types.ref(ts.createPackage(prefix.toPackage().package_(), name.id())));
        }
    }

    // type
    public TypeNode toType() {
        return nf.AmbTypeNode(pos, toPrefix());
    }

    public String toString() {
        if (prefix == null) {
            return name.toString();
        }

        return prefix.toString() + "." + name.toString();
    }
}
