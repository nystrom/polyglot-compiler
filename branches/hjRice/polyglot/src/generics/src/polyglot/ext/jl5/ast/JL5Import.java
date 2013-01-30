package polyglot.ext.jl5.ast;

import polyglot.ast.Import;

public interface JL5Import extends Import {

    public static final Kind STATIC_MEMBER = new Kind("member");
    public static final Kind ALL_STATIC_MEMBERS = new Kind("all-members");

}
