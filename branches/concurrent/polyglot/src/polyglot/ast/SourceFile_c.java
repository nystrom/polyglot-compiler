/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.Context;
import polyglot.types.ImportTable;
import polyglot.util.*;
import polyglot.visit.NodeVisitor;

/**
 * A <code>SourceFile</code> is an immutable representations of a Java
 * langauge source file.  It consists of a package name, a list of 
 * <code>Import</code>s, and a list of <code>GlobalDecl</code>s.
 */
public class SourceFile_c extends Node_c implements SourceFile
{
    protected PackageNode package_;
    protected List<Import> imports;
    protected List<TopLevelDecl> decls;
    protected ImportTable importTable;
    protected Source source;

    public SourceFile_c(Position pos, PackageNode package_, List<Import> imports, List<TopLevelDecl> decls) {
	super(pos);
	System.out.println("creating SourceFile");
//	assert(imports != null && decls != null && ! decls.isEmpty()); // package_ may be null, imports empty
	System.out.println("creating SourceFile 2");
	this.package_ = package_;
	this.imports = TypedList.copyAndCheck(imports, Import.class, true);
	this.decls = TypedList.copyAndCheck(decls, TopLevelDecl.class, true);
	
	System.out.println("created " + this);
    }

    /** Get the source of the source file. */
    public Source source() {
	return this.source;
    }

    /** Set the source of the source file. */
    public SourceFile source(Source source) {
	SourceFile_c n = (SourceFile_c) copy();
	n.source = source;
	return n;
    }

    /** Get the package of the source file. */
    public PackageNode package_() {
	return this.package_;
    }

    /** Set the package of the source file. */
    public SourceFile package_(PackageNode package_) {
	SourceFile_c n = (SourceFile_c) copy();
	n.package_ = package_;
	return n;
    }

    /** Get the imports of the source file. */
    public List<Import> imports() {
	return Collections.unmodifiableList(this.imports);
    }

    /** Set the imports of the source file. */
    public SourceFile imports(List<Import> imports) {
	SourceFile_c n = (SourceFile_c) copy();
	n.imports = TypedList.copyAndCheck(imports, Import.class, true);
	return n;
    }

    /** Get the declarations of the source file. */
    public List<TopLevelDecl> decls() {
	return Collections.unmodifiableList(this.decls);
    }

    /** Set the declarations of the source file. */
    public SourceFile decls(List<TopLevelDecl> decls) {
	SourceFile_c n = (SourceFile_c) copy();
	n.decls = TypedList.copyAndCheck(decls, TopLevelDecl.class, true);
	return n;
    }

    /** Get the declarations of the source file. */
    public ImportTable importTable() {
	return this.importTable;
    }

    /** Set the declarations of the source file. */
    public SourceFile importTable(ImportTable importTable) {
	SourceFile_c n = (SourceFile_c) copy();
	n.importTable = importTable;
	return n;
    }

    /** Reconstruct the source file. */
    protected SourceFile_c reconstruct(PackageNode package_, List<Import> imports, List<TopLevelDecl> decls) {
	if (package_ != this.package_ || ! CollectionUtil.allEqual(imports, this.imports) || ! CollectionUtil.allEqual(decls, this.decls)) {
	    SourceFile_c n = (SourceFile_c) copy();
	    n.package_ = package_;
	    n.imports = TypedList.copyAndCheck(imports, Import.class, true);
	    n.decls = TypedList.copyAndCheck(decls, TopLevelDecl.class, true);
	    return n;
	}

	return this;
    }

    /** Visit the children of the source file. */
    public Node visitChildren(NodeVisitor v) {
        PackageNode package_ = (PackageNode) visitChild(this.package_, v);
	List<Import> imports = visitList(this.imports, v);
	List<TopLevelDecl> decls = visitList(this.decls, v);
	return reconstruct(package_, imports, decls);
    }

    public Context enterScope(Context c) {
        return c.pushSource(importTable);
    }

    public String toString() {
        return "<<<< " + source + " >>>>";
    }

    public void dump(CodeWriter w) {
        super.dump(w);
        w.begin(0);
        w.allowBreak(4, " ");
        w.write("(import-table " + importTable + ")");
        w.end();
    }
    

}
