package ibex.ast;

import java.util.List;

import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassMember;
import polyglot.util.Position;

public class IbexClassBody_c extends ClassBody_c {

    public IbexClassBody_c(Position pos, List<ClassMember> members) {
        super(pos, members);
    }
}
