/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 */
package polyglot.frontend;

/**
 * Version information for the base compiler.
 */
public class JLVersion extends polyglot.main.Version {
    public String name() { return "jl"; }
    public int major() { return 3; }
    public int minor() { return 1; }
    public int patch_level() { return 3; }
    public String toString() { return "3.1.3 (2010-02-19 22:49:32)"; }
}
