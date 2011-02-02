package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.ClassBody_c;
import polyglot.ext.jl5.types.AnnotationElemInstance;
import polyglot.ext.jl5.types.EnumInstance;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5MethodInstance;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeChecker;

public class JL5ClassBody_c extends ClassBody_c implements JL5ClassBody {

    public JL5ClassBody_c(Position pos, List members) {
        super(pos, members);
    }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        duplicateEnumConstantDeclCheck(tc); 
        duplicateEnumConstantDeclFieldCheck(tc);
        checkCCallEnumConstructors(tc);
        checkAbsMembers(tc);
        checkGenMethConflicts(tc);
        duplicateAnnotationElemDeclCheck(tc);
        duplicateErasureMethodCheck(tc);
        return super.typeCheck(tc);
    }

    protected MethodInstance erasureMethod(JL5TypeSystem ts, MethodInstance mi){
        ArrayList miErasureFormals = new ArrayList(mi.formalTypes().size());
        for (Iterator it = mi.formalTypes().iterator(); it.hasNext(); ) {
            miErasureFormals.add(ts.erasure((Type) it.next()));
        }
        ArrayList miErasureExcTypes = new ArrayList(mi.throwTypes().size());
        Type erasureRet = ts.erasure(mi.returnType());
        return ts.methodInstance(mi.position(), mi.container(), mi.flags(), erasureRet, mi.name(), miErasureFormals, miErasureExcTypes);
    }

    protected void duplicateErasureMethodCheck(TypeChecker tc) throws SemanticException{
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        ArrayList l = new ArrayList(tc.context().currentClass().methods());
        for (int i = 0; i < l.size(); i++){
            MethodInstance mi = (MethodInstance)l.get(i);
            MethodInstance emi = erasureMethod(ts, mi);
            for (int j = i+1; j < l.size(); j++){
                MethodInstance mj = (MethodInstance)l.get(j);
                MethodInstance emj = erasureMethod(ts, mj);
                if (isSameMethod(tc.typeSystem(), emi, emj)){
                    throw new SemanticException("Method "+mi+" and "+mj+" have the same erasure.", mj.position());
                }
            }
        }
    }

    protected void checkGenMethConflicts(TypeChecker tc) throws SemanticException{
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        ArrayList list = new ArrayList(type.methods());
        for (Iterator it = list.iterator(); it.hasNext();){
            JL5MethodInstance mi = (JL5MethodInstance)it.next();
            if (!mi.isCompilerGenerated() && mi.name().equals("values")  && mi.formalTypes().isEmpty()){
                throw new SemanticException("method "+mi.name()+" is already defined in type: "+type, mi.position());
            }
            if (!mi.isCompilerGenerated() && mi.name().equals("valueOf") && (mi.formalTypes().size() == 1) && ((Type)mi.formalTypes().get(0)).isClass() && ((ClassType)mi.formalTypes().get(0)).fullName().equals("java.lang.String")){
                throw new SemanticException("method "+mi.name()+" is already defined in type: "+type, mi.position());
            }
        }
    }
    
    protected void checkAbsMembers(TypeChecker tc) throws SemanticException {
        // this will be necessary check that if the enum type 
        // (not its super types) has abstract methods then every enum constant
        // overrides them in an anon method body

        /*JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        ArrayList l = new ArrayList(this.members());
        Iterator it = l.iterator();
        while (it.hasNext()){
            Object o = it.next();
            if (!(o instanceof MethodDecl)) continue;
            MethodInstance mi = ((MethodDecl)o).methodInstance();
            if (!mi.flags().isAbstract()) continue;
            ArrayList el = new ArrayList(type.enumConstants());
            if (el.isEmpty()){
                throw new SemanticException("abstract method "+mi.name()+" is not overridden", this.position());
            }
            Iterator eIt = el.iterator();
            while (eIt.hasNext()){
                EnumInstance ec = (EnumInstance)eIt.next();*/
                /*if (ec.body() == null){
                    throw new SemanticException("enum constant "+ec.name()+" must have a body and override abstract method "+mi.name(), ec.position());
                }
                else if (!checkAbstractOverride(ec, mi)){
                    throw new SemanticException("enum constant "+ec.name()+" must override abstract method "+mi.name(), ec.position());
                }*/
       /*     }
        }*/
    
    }

    protected boolean checkAbstractOverride(EnumConstantDecl ec, MethodInstance mi){
        ClassBody cb = ec.body();
        Iterator mIt = cb.members().iterator();
        while (mIt.hasNext()){
            Object o = mIt.next();
            if (o instanceof MethodDecl){
                MethodDecl md = (MethodDecl)o;
                if (md.methodInstance().isSameMethod(mi)) return true;
            }
        }
        return false;
    }

    protected void checkCCallEnumConstructors(TypeChecker tc) throws SemanticException{
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        Iterator it = members().iterator();
        while (it.hasNext()){
       
            Object next = it.next();
            if (next instanceof ConstructorDecl){
                Block body = ((ConstructorDecl)next).body();
                if (!body.statements().isEmpty() && JL5Flags.isEnumModifier(type.flags())){
                    if (body.statements().get(0) instanceof ConstructorCall && ((ConstructorCall)body.statements().get(0)).kind() == ConstructorCall.SUPER){
                        throw new SemanticException("Cannot have: "+body.statements().get(0)+" in enum constructor", ((Node)body.statements().get(0)).position());
                    }
                }
            }
        }
    }
    
    protected void duplicateEnumConstantDeclFieldCheck(TypeChecker tc) throws SemanticException {
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();

        ArrayList l = new ArrayList(type.enumConstants());
        ArrayList lf = new ArrayList(type.fields());
        for (int i = 0; i < l.size(); i++){
            EnumInstance ei = (EnumInstance) l.get(i);
            for (int j = 0; j < lf.size(); j++){
                FieldInstance fi = (FieldInstance) lf.get(j);
                if (ei.name().equals(fi.name())){
                    throw new SemanticException("Duplicate enum constant / field \"" + fi + "\" at: ",fi.position());
                }
            }
        }
    }

    protected void duplicateEnumConstantDeclCheck(TypeChecker tc) throws SemanticException {
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();

        ArrayList l = new ArrayList(type.enumConstants());
        for (int i = 0; i < l.size(); i++){
            EnumInstance ei = (EnumInstance) l.get(i);

            for (int j = i+1; j < l.size(); j++){
                EnumInstance ej = (EnumInstance) l.get(j);

                if (ei.name().equals(ej.name())){
                    throw new SemanticException("Duplicate enum constant \"" + ej + "\" at ", ej.position());
                }
            }
        }
    }

    public void duplicateAnnotationElemDeclCheck(TypeChecker tc) throws SemanticException {
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        ArrayList l = new ArrayList(type.annotationElems());
        for (int i = 0; i < l.size(); i++){
            AnnotationElemInstance ai = (AnnotationElemInstance) l.get(i);

            for (int j = i+1; j < l.size(); j++){
                AnnotationElemInstance aj = (AnnotationElemInstance) l.get(j);

                if (ai.name().equals(aj.name())){
                    throw new SemanticException("Duplicate annotation element \"" + aj + "\" at ", aj.position());
                }
            }
        }
    }
    
    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
        if (!members.isEmpty()) {
            w.newline(4);
            w.begin(0);

            boolean lastWasEnum = false;
            for (Iterator i = members.iterator(); i.hasNext(); ) {
                ClassMember member = (ClassMember) i.next();
                if (!(member instanceof EnumConstantDecl) && lastWasEnum){
                    w.write(";");
                    w.newline(0);
                    lastWasEnum = false;
                }
                printBlock(member, w, tr);
                if (member instanceof EnumConstantDecl){
                    w.write(",");
                    lastWasEnum = true;
                }
                if (i.hasNext()) {
                    w.newline(0);
                    w.newline(0);
                }
            }

            w.end();
            w.newline(0);
        } 
    }
}
