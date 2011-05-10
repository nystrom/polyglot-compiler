package polyglot.ext.jl5.ast;

import polyglot.ast.TypeNode;
import polyglot.types.ClassDef.Kind;

public interface BoundedTypeNode extends TypeNode {
      public static final Kind SUPER = new Kind("super");
      public static final Kind EXTENDS = new Kind("extends");

      Kind kind();

      BoundedTypeNode kind(Kind kind);

      TypeNode bound();

      BoundedTypeNode bound(TypeNode bound);
      
}
