package polyglot.types;

public interface MemberInstance<T extends Def> extends Use<T> {
    Flags flags();
    Type container();
}
