package polyglot.ext.jl5.parse;

import java.util.List;

import polyglot.ast.Id;
import polyglot.util.Position;

/**
 * Encapsulates some of the data in a constructor declaration.  Used only by the parser.
 */
public class ConstructorDeclarator {
	public Position pos;
	public Id name;
	public List formals;

	//CHECK why do we have these in the grammar ?
	public ConstructorDeclarator(Position pos, Id name, List formals) {
		this.pos = pos;
		this.name = name;
		this.formals = formals;
	}
	
	public Position position() {
		return pos;
	}

    public Id name(){
        return name;
    }

    public List formals(){
        return formals;
    }
}
