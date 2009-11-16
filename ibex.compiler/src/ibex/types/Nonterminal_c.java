package ibex.types;

import java.util.List;

import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

public class Nonterminal_c extends TypeObject_c implements Nonterminal {
    RuleInstance rule;

    public Nonterminal_c(IbexTypeSystem ts, Position pos, RuleInstance rule) {
        super(ts, pos);
        this.rule = rule;
    }
    
    public List<Type> throwTypes() {
        return rule.throwTypes();
    }
    
    public boolean equalsImpl(TypeObject o) {
        if (o instanceof Symbol) {
            return matches((Symbol) o);
        }
        return false;
    }
    
    public int hashCode() {
        return rule.def().hashCode();
    }

    public boolean matches(Symbol sym) {
        if (sym instanceof Nonterminal) {
            Nonterminal n = (Nonterminal) sym;
            return rule.def() == n.rule().def();
        }
        return false;
    }
    
    public boolean isRegular() {
        return rule.isRegular();
    }

    public Type type() {
        return rule.type();
    }
    
    public Name name() {
        return rule.name();
    }
    
    public RuleInstance rule() {
        return rule;
    }

    public String toString() {
        return rule.name().toString();
    }
}
