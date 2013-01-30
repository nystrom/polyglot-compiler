package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Term;
import polyglot.ast.Term_c;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.EnumInstance;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5ParsedClassType_c;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.MemberDef;
import polyglot.types.ParsedClassType;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

/**
 * A constant enum declaration
 * 
 * Example: 'PENNY(1)' in the following example:
 * 
 * public enum Coin {
 *    PENNY(1), NICKEL(5), DIME(10), QUARTER(25);
 * 
 *   Coin(int value) { this.value = value; }
 * 
 *   private final int value;
 * 
 *   public int value() { return value; }
 * }
 * 
 * The trick with enum constant is that we need to get the class type
 * from the enclosing enum declaration. Then instantiate an anonymous 
 * type for the constant declaration 
 * 
 * 
 */
// CHECK but enum constant implementation on hold until the compiler can compile
public class EnumConstantDecl_c // extends JL5ClassDecl_c implements EnumConstantDecl
{   
//    protected List annotations;
//    
//    /** The name of the constant
//     */
//    protected Id name;
//
//    /** Arguments of the enum constant declaration (to be used when calling the enum constructor)
//     */
//    protected List args;
//     
//    public EnumConstantDecl_c(Position pos, FlagAnnotations flags, Id name, List args, ClassBody body){
//        super(pos, flags.classicFlags(), name, ts.Object(), Collections.EMPTY_LIST, body);
//        this.name = name;
//        this.args = args;
//        this.body = body;
//        // discard flags as JLS forbids flags for enum constants
//        this.annotations = flags.annotations();
//    }
//
//    public List args(){
//        return args;
//    }
//
//    public List annotations(){
//        return annotations;
//    }
//
//    public EnumConstantDecl args(List args){
//        EnumConstantDecl_c n = (EnumConstantDecl_c)copy();
//        n.args = args;
//        return n;
//    }
//
//    public EnumConstantDecl name(Id name){
//        EnumConstantDecl_c n = (EnumConstantDecl_c)copy();
//        n.name = name;
//        return n;
//    }
//    
//    public EnumConstantDecl body(ClassBody body){
//        EnumConstantDecl_c n = (EnumConstantDecl_c)copy();
//        n.body = body;
//        return n;
//    }
//
//    public EnumConstantDecl classDef(ClassDef type) {
//        if (type == this.type) { 
//        	return this;
//        }
//        EnumConstantDecl_c n = (EnumConstantDecl_c) copy();
//        n.type = type;
//        return n;
//    }
//
//    public EnumConstantDecl flags(FlagsNode flags) {
//    	EnumConstantDecl_c n = (EnumConstantDecl_c) copy();
//        n.flags = flags;
//        return n;
//    }
//
//    protected EnumConstantDecl_c reconstruct(List args, ClassBody body){
//        if (!CollectionUtil.allEqual(args, this.args) || body != this.body) {
//            EnumConstantDecl_c n = (EnumConstantDecl_c) copy();
//            n.args = TypedList.copyAndCheck(args, Expr.class, true);
//            n.body = body;
//            return n;
//        }
//        return this;
//    }
//
//    protected EnumConstantDecl_c reconstruct(List args, List annotations){
//        if (!CollectionUtil.allEqual(args, this.args) ||
//                !CollectionUtil.allEqual(annotations, this.annotations)) {
//            EnumConstantDecl_c n = (EnumConstantDecl_c) copy();
//            n.args = TypedList.copyAndCheck(args, Expr.class, true);
//            n.annotations = annotations;
//            return n;
//        }
//        return this;
//    }
//
//    protected EnumConstantDecl_c reconstruct(FlagsNode flags, Id name, ClassBody body){
//        if (flags != this.flags || body != this.body || name != this.name) {
//            EnumConstantDecl_c n = (EnumConstantDecl_c) copy();
//            n.flags = flags;
//            n.body = body;
//            n.name = name;
//            return n;
//        }
//        return this;
//    }
//
//    /**
//     * Also need to visit args and annotations.
//     */
//    public Node visitChildren(NodeVisitor v){
//    	EnumConstantDecl_c n = (EnumConstantDecl_c) super.visitChildren(v);
//        List args = n.visitList(this.args, v);
//        List annotations = n.visitList(this.annotations, v);
//        return n.reconstruct(args, annotations);
//    }
//
//    public Node buildTypesOverride(TypeBuilder tb) throws SemanticException {
//    	EnumConstantDecl_c n = this;
//    	n = n.preBuildTypes(tb);
//    	n = n.buildTypesBody(tb);
//    	n = n.postBuildTypes(tb);
//    	return n;
//        }
//
//    public EnumConstantDecl_c preBuildTypes(TypeBuilder tb) throws SemanticException {
//        tb = tb.pushAnonClass(position());
//        ClassDef type = tb.currentClass();
//
//        EnumConstantDecl_c n = this;
//        FlagsNode flags = (FlagsNode) n.visitChild(n.flags, tb);
//        Id name = (Id) n.visitChild(n.name, tb);
//
//        TypeNode superClass = (TypeNode) n.visitChild(n.superClass, tb);
//        List<TypeNode> interfaces = n.visitList(n.interfaces, tb);
//
//        n = (EnumConstantDecl_c) n.reconstruct(flags, name, superClass, interfaces, n.body);
//        
//        n.setSuperClass(tb.typeSystem(), type);
//        n.setInterfaces(tb.typeSystem(), type);
//
//        n = (EnumConstantDecl_c) n.classDef(type).flags(flags.flags(type.flags()));
//
//        //CHECK Need to handle args types to build the constructor instance ?
//        
//        assert(false);
//        return n;
////        JL5TypeSystem ts = (JL5TypeSystem)tb.typeSystem();
////        EnumConstantDecl n = this;
////        if (n.body() != null){
////            TypeBuilder bodyTB = (TypeBuilder)tb.visitChildren();
////            bodyTB = bodyTB.pushAnonClass(position());
////
////            n = (EnumConstantDecl)n.body((ClassBody)n.body().visit(bodyTB));
////            ParsedClassType bodyType = (ParsedClassType) bodyTB.currentClass();
////            n = (EnumConstantDecl)n.anonType(bodyType);
////        }
////            
////        EnumInstance ei = ts.enumInstance(n.position(), ts.Enum(), JL5Flags.NONE, n.name(), n.anonType());
////
////        List l = new ArrayList(n.args().size());
////        for (int i = 0; i < n.args().size(); i++){
////            l.add(ts.unknownType(position()));
////        }
////
////        ConstructorInstance ci = ts.constructorInstance(position(), ts.Object(), Flags.NONE, l, Collections.EMPTY_LIST);
////
////        n = (EnumConstantDecl)n.constructorInstance(ci);
////        
////        return n.enumInstance(ei);
//    }
//    
//    private EnumConstantDecl_c buildTypesBody(TypeBuilder tb) throws SemanticException {
//    	// Replaces enterScope, pushes the enumDef 
//    	EnumConstantDecl_c n = this;
//    	TypeBuilder tb2 = tb.pushClass(n.type);
//    	ClassBody body = (ClassBody) n.visitChild(n.body, tb2);
//    	n = (EnumConstantDecl_c) n.body(body);
//    	return n;
//    }
//
//    /** 
//     * @deprecated
//     * @param child
//     * @param c
//     * @return
//     */
//    public Context enterScope(Node child, Context c) {
//    	return null;
////        if (child == body && anonType != null && body != null){
////            c = c.pushClass(anonType, anonType);
////        }
////        return super.enterScope(child, c);
//    }
//    
//    /**
//     * @deprecated
//     */
//    public NodeVisitor buildTypesEnter(TypeBuilder tb) throws SemanticException {
//        tb = (TypeBuilder)tb.pushCode();
//        if (body != null){
//            tb = (TypeBuilder) tb.bypass(body);
//        }
//        return tb;
//    }
//
//    @Override
//    protected ConstructorDecl createDefaultConstructor(ClassDef thisType, TypeSystem ts, NodeFactory nf)
//    throws SemanticException
//    {
//        // If no constructor provided, enum has a default private constructor
//    	ConstructorDecl cd = super.createDefaultConstructor(thisType, ts, nf);
//    	Position pos = body().position().startOf();
//    	return cd.flags(nf.FlagsNode(pos, Flags.PRIVATE));
//    }
//    
//    public Node typeCheck(ContextVisitor tc) throws SemanticException {
//        JL5TypeSystem ts = (JL5TypeSystem)tc.typeSystem();
//        Context c = tc.context();
//        ClassType ct = c.currentClass();
//
//        List argTypes = new LinkedList();
//        for (Iterator it = this.args.iterator(); it.hasNext();){
//            Expr e = (Expr) it.next();
//            argTypes.add(e.type());
//        }
//        
//        ConstructorInstance ci = ts.findConstructor(ct, argTypes, c.currentClass());
//        EnumConstantDecl_c n = (EnumConstantDecl_c)this.constructorInstance(ci);
//        
//        if (n.flags() != Flags.NONE){
//            throw new SemanticException("Cannot have modifier(s): "+flags+" on enum constant declaration", this.position());
//        }
//        
//        ts.checkDuplicateAnnotations(n.annotations());
//
//        // set superType for anon class
//        if (body != null){
//            anonType().superType(ct);
//            ClassBody body = n.typeCheckBody(tc, ct);
//            return n.body(body);
//        }
//        return n;
//    }
//
//    protected ClassBody typeCheckBody(TypeChecker tc, ClassType superType) throws SemanticException {
//        Context bodyCtxt = tc.context().pushClass(anonType, anonType);
//        Job sj = tc.job().spawn(bodyCtxt, body, Pass.CLEAN_SUPER, Pass.DISAM_ALL);
//        if (!sj.status()){
//            if (!sj.reportedErrors()){
//                throw new SemanticException("Could not disambiguate body of anonymous subclass "+name+" of "+superType+".");
//                            
//            }
//            throw new SemanticException();
//        }
//
//        ClassBody b = (ClassBody)sj.ast();
//
//        TypeChecker bodyTC = (TypeChecker)tc.context(bodyCtxt);
//        b = (ClassBody) visitChild(b, bodyTC.visitChildren());
//
//        bodyTC.typeSystem().checkClassConformance(anonType());
//
//        return b;
//    }
//    
//    public String toString(){
//        return name + "(" + args + ")" + body != null ? "..." : "";
//    }
//    
//    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
//        w.write(name.toString());
//        if (args != null){
//            w.write(" ( ");
//            Iterator it = args.iterator();
//            while (it.hasNext()){
//                Expr e = (Expr) it.next();
//                print(e, w, tr);
//                if (it.hasNext()){
//                    w.write(", ");
//                    w.allowBreak(0);
//                }
//            }
//            w.write(" )");
//        }
//        if (body != null){
//            w.write(" {");
//            print(body, w, tr);
//            w.write("}");
//        }
//    }
//
//    public List acceptCFG(CFGBuilder v, List<Term> succs){
//        return succs;
//    }
//
//    public Term firstChild(){
//        return this;
//    }
//
//    public NodeVisitor addMembersEnter(AddMemberVisitor am){
//        JL5ParsedClassType ct = (JL5ParsedClassType_c)am.context().currentClassScope();
//
//        ct.addEnumConstant(enumInstance);
//        return am.bypassChildren(this);
//    }
//
//	@Override
//	public MemberDef memberDef() {
//		// TODO Auto-generated method stub
//		return null;
//	}
}
