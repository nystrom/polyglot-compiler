package polyglot.frontend;

import polyglot.types.Ref;
import polyglot.types.TypeObject;

public class TypeObjectGoal_c<T extends TypeObject> extends AbstractGoal_c implements
        TypeObjectGoal<T> {

    Ref<T> v;
    
    public TypeObjectGoal_c(String name, Ref<T> v) {
        super(name);
        this.v = v;
    }

    public TypeObjectGoal_c(Ref<T> v) {
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
