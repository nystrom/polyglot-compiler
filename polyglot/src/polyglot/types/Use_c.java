package polyglot.types;

import polyglot.util.Position;

public class Use_c<T extends Def> extends TypeObject_c implements Use<T> {
   Ref<T> def;

   public Use_c(TypeSystem ts, Position pos, Ref<T> def) {
        super(ts, pos);
        this.def = def;
    }

    public T def() {
        return def.get();
    }
}
