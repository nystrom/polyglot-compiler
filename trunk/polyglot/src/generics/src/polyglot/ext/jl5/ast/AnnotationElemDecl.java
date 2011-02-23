package polyglot.ext.jl5.ast;

import polyglot.ast.ClassMember;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.AnnotationElemInstance;
import polyglot.types.Flags;

public interface AnnotationElemDecl extends ClassMember {

    AnnotationElemDecl type(TypeNode type);
    TypeNode type();
    
    AnnotationElemDecl flags(FlagsNode flags);
    FlagsNode flags();

    AnnotationElemDecl defaultVal(Expr def);
    Expr defaultVal();

    AnnotationElemDecl name(Id name);
    Id name();

    AnnotationElemDecl annotationElemInstance(AnnotationElemInstance ai);
    AnnotationElemInstance annotationElemInstance();
}
