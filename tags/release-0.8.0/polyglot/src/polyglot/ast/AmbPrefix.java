package jltools.ast;

/**
 * An <code>AmbPrefix</code> is an ambiguous AST node composed of dot-separated
 * list of identifiers.  It must resolve to a prefix.
 */
public interface AmbPrefix extends Prefix, Ambiguous
{
    Prefix prefix();
    String name();
}