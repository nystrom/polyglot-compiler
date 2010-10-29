package polyglot.ast;

public interface QualifiedName extends Node {
    Node prefix();
    QualifiedName prefix(Node prefix);
    
    Id name();
    QualifiedName name(Id name);
}
