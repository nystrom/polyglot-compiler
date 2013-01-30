package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.types.ClassDef;
import polyglot.types.ParsedClassType;

/**
 * Enum constant declaration
 */
public interface EnumConstantDecl extends ClassDecl
{    
    /**
     * The type of the class declaration.
     */
    ClassDef classDef();

    /**
     * Set the type of the class declaration.
     */
    EnumConstantDecl classDef(ClassDef type);

    /**
     * The class declaration's flags.
     */
    FlagsNode flags();

    /**
     * Set the class declaration's flags.
     */
    EnumConstantDecl flags(FlagsNode flags);

    /**
     * The class declaration's name.
     */
    Id name();
    
    /**
     * Set the class declaration's name.
     */
    EnumConstantDecl name(Id name);

    /**
     * The class's body.
     */
    ClassBody body();

    /**
     * Set the class's body.
     */
    EnumConstantDecl body(ClassBody body);

	/** get args */
    List args();

    /** set args */
    EnumConstantDecl args(List args);

    List annotations();
}
