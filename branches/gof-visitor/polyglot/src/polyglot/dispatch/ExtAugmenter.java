package polyglot.dispatch;

import polyglot.ast.*;

/** This visitor adds a Ext node to the input node. */
public class ExtAugmenter {
    Node visit(FlagsNode n) { return n; }

    Node visit(Id n) { return n; }

    Node visit(AmbAssign n) { return n; }

    Node visit(AmbExpr n) { return n; }

    Node visit(AmbPrefix n) { return n; }

    Node visit(AmbQualifierNode n) { return n; }

    Node visit(AmbReceiver n) { return n; }

    Node visit(AmbTypeNode n) { return n; }

    Node visit(ArrayAccess n) { return n; }

    Node visit(ArrayInit n) { return n; }

    Node visit(ArrayTypeNode n) { return n; }

    Node visit(Assert n) { return n; }

    Node visit(Assign n) { return n; }

    Node visit(LocalAssign n) { return n; }
    Node visit(FieldAssign n) { return n; }
    Node visit(ArrayAccessAssign n) { return n; }

    Node visit(Binary n) { return n; }

    Node visit(Block n) { return n; }

    Node visit(BooleanLit n) { return n; }

    Node visit(Branch n) { return n; }

    Node visit(Call n) { return n; }

    Node visit(CanonicalTypeNode n) { return n; }

    Node visit(Case n) { return n; }

    Node visit(Cast n) { return n; }

    Node visit(Catch n) { return n; }

    Node visit(CharLit n) { return n; }

    Node visit(ClassBody n) { return n; }

    Node visit(ClassDecl n) { return n; }

    Node visit(ClassLit n) { return n; }

    Node visit(ClassMember n) { return n; }

    Node visit(CodeDecl n) { return n; }

    Node visit(CompoundStmt n) { return n; }

    Node visit(Conditional n) { return n; }

    Node visit(ConstructorCall n) { return n; }

    Node visit(ConstructorDecl n) { return n; }

    Node visit(Do n) { return n; }

    Node visit(Empty n) { return n; }

    Node visit(Eval n) { return n; }

    Node visit(Expr n) { return n; }

    Node visit(Field n) { return n; }

    Node visit(FieldDecl n) { return n; }

    Node visit(FloatLit n) { return n; }

    Node visit(For n) { return n; }

    Node visit(Formal n) { return n; }

    Node visit(If n) { return n; }

    Node visit(Import n) { return n; }

    Node visit(Initializer n) { return n; }

    Node visit(Instanceof n) { return n; }

    Node visit(IntLit n) { return n; }

    Node visit(Labeled n) { return n; }

    Node visit(Lit n) { return n; }

    Node visit(Local n) { return n; }

    Node visit(LocalClassDecl n) { return n; }

    Node visit(LocalDecl n) { return n; }

    Node visit(Loop n) { return n; }

    Node visit(MethodDecl n) { return n; }

    Node visit(NewArray n) { return n; }

    Node visit(Node n) { return n; }

    Node visit(NodeList n) { return n; }

    Node visit(New n) { return n; }

    Node visit(NullLit n) { return n; }

    Node visit(NumLit n) { return n; }

    Node visit(PackageNode n) { return n; }

    Node visit(ProcedureDecl n) { return n; }

    Node visit(Return n) { return n; }

    Node visit(SourceCollection n) { return n; }

    Node visit(SourceFile n) { return n; }

    Node visit(Special n) { return n; }

    Node visit(Stmt n) { return n; }

    Node visit(StringLit n) { return n; }

    Node visit(SwitchBlock n) { return n; }

    Node visit(SwitchElement n) { return n; }

    Node visit(Switch n) { return n; }

    Node visit(Synchronized n) { return n; }

    Node visit(Term n) { return n; }

    Node visit(Throw n) { return n; }

    Node visit(Try n) { return n; }

    Node visit(TypeNode n) { return n; }

    Node visit(Unary n) { return n; }

    Node visit(While n) { return n; }
}
