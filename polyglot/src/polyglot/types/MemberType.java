package polyglot.types;

public interface MemberType<T extends Def> extends Use<T> {
    Flags flags();
    ReferenceType container();
}
