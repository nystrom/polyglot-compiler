package polyglot.frontend;

import polyglot.types.Ref;
import polyglot.types.TypeObject;

public class SymbolGoal_c<T extends TypeObject> extends AbstractGoal_c implements
        TypeObjectGoal<T> {

    Ref<T> v;
    
    public SymbolGoal_c(String name, Ref<T> v) {
        super(name);
        this.v = v;
    }

    public SymbolGoal_c(Ref<T> v) {
        super();
        this.v = v;
    }
    
   public Ref<T> typeRef() {
       return v;
   }
    
   public Pass createPass() {
       return new EmptyPass(this);
   }
}
