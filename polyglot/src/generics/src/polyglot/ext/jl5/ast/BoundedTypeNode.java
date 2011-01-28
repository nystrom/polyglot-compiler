package polyglot.ext.jl5.ast;

import polyglot.ast.TypeNode;
import polyglot.util.Enum;

public interface BoundedTypeNode extends TypeNode {

      public static class Kind extends Enum {
          public Kind(String name) {super(name); }
      }

      public static final Kind SUPER = new Kind("super");
      public static final Kind EXTENDS = new Kind("extends");

      Kind kind();

      BoundedTypeNode kind(Kind kind);

      TypeNode bound();

      BoundedTypeNode bound(TypeNode bound);
      
}
