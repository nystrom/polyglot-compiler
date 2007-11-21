package polyglot.ast;

import java.util.List;

import polyglot.types.Def;

public interface FragmentRoot extends Node {
    List<Def> defs();
}
