package polyglot.ext.jl5.ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Node;
import polyglot.ext.jl5.types.AnnotationElemInstance;
import polyglot.ext.jl5.types.EnumInstance;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5MethodDef;
import polyglot.ext.jl5.types.JL5MethodInstance;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

public class JL5ClassBody_c extends ClassBody_c implements JL5ClassBody {

    public JL5ClassBody_c(Position pos, List members) {
        super(pos, members);
    }

    
    public Node conformanceCheck(ContextVisitor tc) throws SemanticException {
    	JL5ClassBody_c n = (JL5ClassBody_c) super.conformanceCheck(tc);
    	
    	n.duplicateEnumConstantDeclCheck(tc); 
    	n.duplicateEnumConstantDeclFieldCheck(tc);
    	n.checkCCallEnumConstructors(tc);
    	//CHECK a bunch of code in checkAbsMembers is commented
    	n.checkAbsMembers(tc);
    	n.checkGenMethConflicts(tc);
    	n.duplicateAnnotationElemDeclCheck(tc);
    	n.duplicateErasureMethodCheck(tc);

        return n;
    }


    protected void duplicateErasureMethodCheck(ContextVisitor tc) throws SemanticException{
        JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();
        List<MethodInstance> l = new ArrayList<MethodInstance>(tc.context().currentClass().methods());
        for (int i = 0; i < l.size(); i++){
            MethodInstance mi = l.get(i);
            MethodInstance emi = ts.erasureMethodInstance(mi);
            for (int j = i+1; j < l.size(); j++){
                MethodInstance mj = l.get(j);
                MethodInstance emj = ts.erasureMethodInstance(mj);
                if (emi.isSameMethod(emj, tc.context())){
                    throw new SemanticException("Method "+mi+" and "+mj+" have the same erasure.", mj.position());
                }
            }
        }
    }

    protected void checkGenMethConflicts(ContextVisitor tc) throws SemanticException{
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        List<JL5MethodInstance> list = new ArrayList(type.methods());
        for (Iterator<JL5MethodInstance> it = list.iterator(); it.hasNext();){
            JL5MethodInstance mi = it.next();
            JL5MethodDef def = ((JL5MethodDef)mi.def());
            if (!def.isCompilerGenerated() && mi.name().equals(Name.make("values"))  && 
            		mi.formalTypes().isEmpty()){
                throw new SemanticException("method "+mi.name()+" is already defined in type: "+type, mi.position());
            }
            if (!def.isCompilerGenerated() && mi.name().equals(Name.make("valueOf")) && 
            		(mi.formalTypes().size() == 1) && 
            		((Type)mi.formalTypes().get(0)).isClass() && 
            		((ClassType)mi.formalTypes().get(0)).fullName().equals(QName.make("java.lang.String"))){
                throw new SemanticException("method "+mi.name()+" is already defined in type: "+type, mi.position());
            }
        }
    }
    
    protected void checkAbsMembers(ContextVisitor tc) throws SemanticException {
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

//    protected boolean checkAbstractOverride(EnumConstantDecl ec, MethodInstance mi){
//        ClassBody cb = ec.body();
//        Iterator mIt = cb.members().iterator();
//        while (mIt.hasNext()){
//            Object o = mIt.next();
//            if (o instanceof MethodDecl){
//                MethodDecl md = (MethodDecl)o;
//                if (md.methodInstance().isSameMethod(mi)) return true;
//            }
//        }
//        return false;
//    }

    protected void checkCCallEnumConstructors(ContextVisitor tc) throws SemanticException{
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        Iterator it = members().iterator();
        while (it.hasNext()){
       
            Object next = it.next();
            if (next instanceof ConstructorDecl){
                Block body = ((ConstructorDecl)next).body();
                if (!body.statements().isEmpty() && JL5Flags.isEnumModifier(type.flags())){
                    if (body.statements().get(0) instanceof ConstructorCall && 
                    		((ConstructorCall)body.statements().get(0)).kind() == ConstructorCall.SUPER){
                        throw new SemanticException("Cannot have: "+body.statements().get(0)+" in enum constructor", ((Node)body.statements().get(0)).position());
                    }
                }
            }
        }
    }
    
    protected void duplicateEnumConstantDeclFieldCheck(ContextVisitor tc) throws SemanticException {
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();

        List<EnumInstance> l = new ArrayList<EnumInstance>(type.enumConstants());
        List<FieldInstance> lf = new ArrayList<FieldInstance>(type.fields());
        for (int i = 0; i < l.size(); i++){
            EnumInstance ei = l.get(i);
            for (int j = 0; j < lf.size(); j++){
                FieldInstance fi = lf.get(j);
                if (ei.name().equals(fi.name())){
                    throw new SemanticException("Duplicate enum constant / field \"" + fi + "\" at: ",fi.position());
                }
            }
        }
    }

    protected void duplicateEnumConstantDeclCheck(ContextVisitor tc) throws SemanticException {
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();

        List<EnumInstance> l = new ArrayList<EnumInstance>(type.enumConstants());
        for (int i = 0; i < l.size(); i++){
            EnumInstance ei = l.get(i);

            for (int j = i+1; j < l.size(); j++){
                EnumInstance ej = l.get(j);

                if (ei.name().equals(ej.name())){
                    throw new SemanticException("Duplicate enum constant \"" + ej + "\" at ", ej.position());
                }
            }
        }
    }

    public void duplicateAnnotationElemDeclCheck(ContextVisitor tc) throws SemanticException {
        JL5ParsedClassType type = (JL5ParsedClassType)tc.context().currentClass();
        List<AnnotationElemInstance> l = new ArrayList<AnnotationElemInstance>(type.annotationElems());
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
