/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.ImportTable;

/**
 * A <code>SourceFile</code> is an immutable representations of a Java
 * language source file.  It consists of a package name, a list of 
 * <code>Import</code>s, and a list of <code>GlobalDecl</code>s.
 */
public interface SourceFile extends Node
{
    /** Get the source's declared package. */
    PackageNode package_();

    /** Set the source's declared package. */
    SourceFile package_(PackageNode package_);

    /** Get the source's declared imports.
     * @return A list of {@link polyglot.ast.Import Import}.
     */
    List<Import> imports();

    /** Set the source's declared imports.
     * @param imports A list of {@link polyglot.ast.Import Import}.
     */
    SourceFile imports(List<Import> imports);

    /** Get the source's top-level declarations.
     * @return A list of {@link polyglot.ast.TopLevelDecl TopLevelDecl}.
     */
    List<TopLevelDecl> decls();

    /** Set the source's top-level declarations.
     * @param decls A list of {@link polyglot.ast.TopLevelDecl TopLevelDecl}.
     */
    SourceFile decls(List<TopLevelDecl> decls);

    /** Get the source's import table. */
    ImportTable importTable();

    /** Set the source's import table. */
    SourceFile importTable(ImportTable importTable);
 
    /** Get the source file. */
    Source source();

    /** Set the source file. */
    SourceFile source(Source source);
}
