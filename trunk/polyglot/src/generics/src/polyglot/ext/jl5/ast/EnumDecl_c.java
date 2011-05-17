package polyglot.ext.jl5.ast;

import java.util.Collections;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;
import polyglot.types.LocalDef;
import polyglot.types.MethodDef;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.TypeBuilder;

/**
 * A <code>ClassDecl</code> is the definition of a class, abstract class, or
 * interface. It may be a public or other top-level class, or an inner named
 * class, or an anonymous class.
 */
public class EnumDecl_c extends JL5ClassDecl_c implements EnumDecl {
    
	/**
	 * 
	 * @param pos
	 * @param flags
	 * @param name
	 * @param superClass Defaults to java.lang.Enum
	 * @param interfaces
	 * @param body
	 */
    public EnumDecl_c(Position pos, FlagAnnotations flags, Id name, TypeNode superClass, List interfaces, ClassBody body) {
        super(pos, flags, name, superClass, interfaces, body);
    }

    public EnumDecl_c(Position pos, FlagAnnotations flags, Id name, TypeNode superClass, List interfaces, ClassBody body, List paramTypes) {
        super(pos, flags, name, superClass, interfaces, body, paramTypes);
    }
    
    @Override
    public Node conformanceCheck(ContextVisitor tc) throws SemanticException {
    	JL5ParsedClassType type = (JL5ParsedClassType) this.type.asType();
    	ClassType superType = (ClassType) type.superClass();
    	Flags flags = flags().flags();

    	if (flags.isAbstract()) {
            throw new SemanticException("Enum types cannot have abstract modifier", this.position());
        }
        if (flags.isPrivate() && !type.isInnerClass()) {
            throw new SemanticException("Enum types cannot have explicit private modifier", this.position());
        }
        if (flags.isFinal()) {
            throw new SemanticException("Enum types cannot have explicit final modifier", this.position());
        }
        if (superType != null && JL5Flags.isEnumModifier(superType.flags())) {
            throw new SemanticException("Cannot extend enum type", position());
        }

        for (ConstructorInstance ci : type.constructors()) {
            if (!ci.flags().clear(Flags.PRIVATE).equals(Flags.NONE)) {
                throw new SemanticException("Modifier " + ci.flags().clear(Flags.PRIVATE)
                        + " not allowed here", ci.position());
            }
        }

    	return super.conformanceCheck(tc);
    }

    /**
     * Enum Declarations are represented as Class Declarations, but we need to
     * generate some additional code wrt the JLS. 
     * @See JLS 8.9 Enums
     */
    @Override
    public ClassDecl_c postBuildTypes(TypeBuilder tb) throws SemanticException {
    	JL5ClassDecl_c cd = (JL5ClassDecl_c) super.postBuildTypes(tb);
    	cd = (JL5ClassDecl_c) cd.addGenEnumMethods(tb);
        return cd.postBuildTypes(tb);
    }

    /**
     * Enums requires to generate additional code
     * @See JLS 8.9 Enums
     * @param tb
     * @return
     */
	protected Node addGenEnumMethods(TypeBuilder tb) {
		JL5ClassBody newBody = (JL5ClassBody) body();

		// Generating enum specific class members
		MethodDecl valuesMeth = this.addEnumMethodValues(tb);
		MethodDecl valueOfMeth = this.addEnumMethodValueOf(tb);
		
		// Adding new methods as class members
		newBody = (JL5ClassBody) newBody.addMember(valuesMeth);
		newBody = (JL5ClassBody) newBody.addMember(valueOfMeth);

		// Adding methods to the type system
		//CHECK when shall we add the method to the type ?
		this.type.addMethod(valuesMeth.methodDef());
		this.type.addMethod(valueOfMeth.methodDef());
		 
		return body(newBody);
	}
	
	/**
	 * add values method:
	 * public static E[] values();
	 * @param tb
	 * @return
	 */
	protected JL5MethodDecl addEnumMethodValues(TypeBuilder tb) {
		JL5TypeSystem ts = (JL5TypeSystem) tb.typeSystem();
		NodeFactory nf = tb.nodeFactory();
		Position pos = position();

		// creating method body
		//CHECK sounds it's wrong to return null !
		assert(false);
		Block valuesBlock = nf.Block(pos);
		valuesBlock = valuesBlock.append(nf.Return(pos, nf.NullLit(pos)));

		// creating method declaration
		FlagAnnotations vmFlags = new FlagAnnotations();
		FlagsNode vmFlagsNode = nf.FlagsNode(pos, Flags.PUBLIC.Static().Final());
		vmFlags.classicFlags(vmFlagsNode);
		TypeNode returnType = nf.CanonicalTypeNode(pos, ts.arrayOf(this.type.asType()));
		Id name = nf.Id(pos, "values");
		JL5MethodDecl valuesMeth = ((JL5NodeFactory) nf).JL5MethodDecl(
				pos, vmFlags, returnType,
				name, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
				valuesBlock, null);

		// creating associated method definition 
		MethodDef md = ts.methodDef(pos, Types.ref(this.classDef().asType()),
				vmFlagsNode.flags(), Types.ref(returnType.type()), Name.make("values"), 
				Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, true);

		return (JL5MethodDecl) valuesMeth.methodDef(md);
	}

	/**
	 * add values method:
	 * public static E valueOf(String name);
	 * @param tb
	 * @return
	 */
	protected JL5MethodDecl addEnumMethodValueOf(TypeBuilder tb) {
		JL5TypeSystem ts = (JL5TypeSystem) tb.typeSystem();
		JL5NodeFactory nf = (JL5NodeFactory) tb.nodeFactory();
		Position pos = position();

		// Create the method argument
		FlagsNode flags = nf.FlagsNode(pos, Flags.NONE);
		FlagAnnotations fl = new FlagAnnotations(flags);
		
		LocalDef ld = ts.localDef(pos, flags.flags(), Types.ref(ts.String()), Name.make("arg1"));
		JL5Formal formal = nf.JL5Formal(pos, fl, nf.CanonicalTypeNode(pos, ts.String()), nf.Id(pos, "arg1"));
        formal = (JL5Formal) formal.localDef(ld);
		List<JL5Formal> formals = Collections.singletonList(formal);

		// Create the body of the method
		//CHECK it's wrong to always return null from valueOf
		assert(false);
		Block valueOfBody = nf.Block(pos);
		valueOfBody = valueOfBody
				.append(nf.Return(pos, nf.NullLit(pos)));

		// Create the method declaration
		FlagAnnotations voFlags = new FlagAnnotations();
		voFlags.classicFlags(nf.FlagsNode(pos, Flags.PUBLIC.Static()));
		
		TypeNode returnType = nf.CanonicalTypeNode(pos, this.type.asType());
		JL5MethodDecl valueOfMeth = ((JL5NodeFactory) nf).JL5MethodDecl(
				pos, voFlags, returnType, nf.Id(pos,"valueOf"),
				formals, Collections.EMPTY_LIST, valueOfBody, null);

		// Create the associated method definition
		Flags mthFlags = JL5Flags.PUBLIC.set(JL5Flags.STATIC);
		List formalTypes = Collections.singletonList(Types.ref(ts.String()));
		MethodDef md = ts.methodDef(pos, Types.ref(this.classDef().asType()),
				mthFlags, Types.ref(returnType.type()), Name.make("valueOf"), formalTypes, 
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, true);

		return (JL5MethodDecl) valueOfMeth.methodDef(md);
	}
    
    
    @Override
    protected ConstructorDecl createDefaultConstructor(ClassDef thisType, TypeSystem ts, NodeFactory nf) throws SemanticException
    {
        // If no constructor provided, enum has a default private constructor
    	ConstructorDecl cd = super.createDefaultConstructor(thisType, ts, nf);
    	Position pos = body().position().startOf();
    	return cd.flags(nf.FlagsNode(pos, Flags.PRIVATE));
    }
}
