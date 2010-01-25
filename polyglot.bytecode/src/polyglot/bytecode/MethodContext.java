package polyglot.bytecode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import polyglot.ast.Id;
import polyglot.ast.Labeled;
import polyglot.ast.Stmt;
import polyglot.bytecode.rep.IClassGen;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.rep.IMethodGen;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.NonEmpty;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;
import polyglot.types.ClassDef;
import polyglot.types.CodeDef;
import polyglot.types.LocalDef;
import polyglot.types.Name;
import polyglot.types.Types;
import polyglot.util.Copy;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class MethodContext implements BytecodeConstants, Copy {
    MethodContext containingExp;
    CodeDef theMethod;
    IMethodGen mg;
    ClassTranslator ct;
    private Map<MethodContext, String> frameFields;
    public InstructionSequence fieldInits;
    
    public MethodContext(CodeDef sym, IMethodGen mg, ClassTranslator ct, StackType currentStack) {
        this(sym, mg, ct.currentClass, ct.cg, ct.containingExp, ct.frameFields, currentStack);
        assert sym != null;
        this.ct = ct;
    }

    private MethodContext(CodeDef sym, IMethodGen mg, ClassDef currentClass, IClassGen cg, MethodContext t, Map<MethodContext, String> frameFields, StackType currentStack) {
        this.theMethod = sym;
        this.mg = mg;
        this.il = mg.code(); // AsmFactory.makeOpcodes(mg, currentStack);
        this.currentClass = currentClass;
        this.cg = cg;

        this.varMap = new HashMap<Var,Integer>();
        this.breakTargets = new HashMap<Name,ILabel>();
        this.continueTargets = new HashMap<Name,ILabel>();
        this.breakStacks = new HashMap<Name,StackType>();
        this.continueStacks = new HashMap<Name,StackType>();

        this.frameFields = frameFields;

        this.containingExp = t;
    }

    public MethodContext push() {
        MethodContext c = copy();
        c.breakStacks = new HashMap<Name, StackType>();
        c.continueStacks = new HashMap<Name, StackType>();
        c.breakTargets = new HashMap<Name, ILabel>();
        c.continueTargets = new HashMap<Name, ILabel>();
        c.outer = this;
        return c;
    }
    
    public MethodContext copy() {
        try {
            return (MethodContext) super.clone();
        }
        catch (CloneNotSupportedException e) {
            assert false;
            return null;
        }
    }

    abstract static class Var {
        Type type;

        Var(Type type) {
            this.type = type;
        }
        abstract public boolean equals(Object o);
        abstract public int hashCode();
    }

    static class LocalVar extends Var {
        LocalVar(LocalDef def, Type type) {
            super(type);
            this.def = def;
        }

        LocalDef def;

        public boolean equals(Object o) {
            if (o instanceof LocalVar) {
                LocalVar v = (LocalVar) o;
                return v.def == def;
            }
            return false;
        }
        
        public int hashCode() {
            return def.hashCode();
        }
    }
    
    static class ThisVar extends Var {
        ThisVar(Type type) {
            super(type);
        }

        public boolean equals(Object o) {
            return o instanceof ThisVar;
        }
        
        public int hashCode() {
            return 17;
        }
    }
    
    static class DummyVar extends Var {
        String name;
        
        DummyVar(String name, Type type) {
            super(type);
            this.name = name;
        }
        
        public boolean equals(Object o) {
            return this == o;
        }
        
        public int hashCode() {
            return System.identityHashCode(this);
        }
        
        public String toString() {
            return name;
        }
    }

    Map<LocalDef,String> boxingMap;
    Map<Var, Integer> varMap;
    Map<Name,ILabel> breakTargets;
    Map<Name,ILabel> continueTargets;
    Map<Name,StackType> breakStacks;
    Map<Name,StackType> continueStacks;

    IOpcodes il;
    public ClassDef currentClass;
    IClassGen cg;

    public Type frameClass;
    public Name frameLocal;

    public int getThisIndex() {
        int index = getIndex(new ThisVar(Type.OBJECT));
        assert index == 0;
        return index;
    }

    int putThisIndex(int index) {
        assert index == 0;
        return addFormal(new ThisVar(Type.OBJECT), index);
    }

//    int getLabelIndex(Name name) {
//        int index = getIndex(new VarMapKey(VarMapKey.Kind.LABEL, name));
//        assert index >= 0 : "no local index found for label " + name;
//        return index;
//    }
//
//    int getFrameIndex(Name name) {
//        int index = getIndex(new VarMapKey(VarMapKey.Kind.FRAME, name));
//        assert index >= 0 : "no local index found for frame " + name;
//        return index;
//    }

    int getLocalIndex(LocalDef sym) {
        Integer index = checkLocalIndex(sym);
        assert index != null : "no local index found for " + sym;
        assert index >= 0 : "boxed local index found for " + sym;
        return index;
    }

    Integer checkLocalIndex(LocalDef sym) {
        //        if (sym.name() == Name.THIS) return getThisIndex();
        Integer v = varMap.get(new LocalVar(sym, ct.typeof(Types.get(sym.type()))));
        return v;
    }

    int getIndex(Var key) {
        Integer v = varMap.get(key);
        //        System.out.println("getting local " + key + " -> " + i);
        assert v != null : "no local index found for " + key;
        return v;
    }

    int nextVar = 0;
    InstructionSequence finallyBlock;
    
    int addLocal(Var v) {
        Integer i = varMap.get(v);
        assert i == null : "local already allocated for " + v;

        int index = nextVar++;
        if (v.type.isWide())
            nextVar++;
        
        varMap.put(v, index);
        return index;
    }

    void removeLocal(LocalDef sym) {
        varMap.remove(new LocalVar(sym, ct.typeof(sym.type())));
    }

//    void removeLabelLocal(Name name) {
//        varMap.remove(new VarMapKey(VarMapKey.Kind.LABEL, name));
//    }

    public Type localType(int index) {
        for (Map.Entry<Var, Integer> e : varMap.entrySet()) {
            if (e.getValue() == index)
                return e.getKey().type;
        }
        assert false;
        return null;
    }

    int addLocal(LocalDef sym) {
        return addLocal(new LocalVar(sym, ct.typeof(sym.type())));
    }
    
    int addFormal(Var key, int index) {
        if (key instanceof ThisVar) {
            assert index == 0;
        }
        
        Integer v = varMap.get(key);

        if (v == null) {
            varMap.put(key, index);
        }
        else {
            assert v == index;
        }
        
        if (nextVar <= index)
            nextVar = index + (key.type.isWide() ? 2 : 1);

        return index;
    }

    int addFormal(LocalDef sym, int index) {
        return addFormal(new LocalVar(sym, ct.typeof(sym.type())), index);
    }
    
    /** Pop the stack until it's type agrees with t. */
    void popToStack(StackType t, Position pos) {
        while (il.currentStack() instanceof NonEmpty && ! il.currentStack().equals(t)) {
            NonEmpty t2 = (NonEmpty) il.currentStack();
            if (t2.top().isNarrow())
                il.POP(pos);
            else 
                il.POP2(pos);
        }
        il.assertStack(t);
    }

    public String toString() {
        return theMethod + "\n" + (il == null ? "null" : il.toString());
    }

    Name label(Id target) {
        if (target == null)
            return null;
        return target.id();
    }
    ILabel breakTarget(Id target) {
        Name key = label(target);

        if (breakTargets.containsKey(key)) {
            return breakTargets.get(key);
        }
        else if (outer != null) {
            return outer.breakTarget(target);
        }
        else {
            return null;
        }
    }
    ILabel continueTarget(Id target) {
        Name key = label(target);
        
        if (continueTargets.containsKey(key)) {
            return continueTargets.get(key);
        }
        else if (outer != null) {
            return outer.continueTarget(target);
        }
        else {
            return null;
        }
    }
    StackType breakStack(Id target) {
        Name key = label(target);
        
        if (breakStacks.containsKey(key)) {
            return breakStacks.get(key);
        }
        else if (outer != null) {
            return outer.breakStack(target);
        }
        else {
            return null;
        }
    }
    StackType continueStack(Id target) {
        Name key = label(target);
        
        if (continueStacks.containsKey(key)) {
            return continueStacks.get(key);
        }
        else if (outer != null) {
            return outer.continueStack(target);
        }
        else {
            return null;
        }
    }
    
    MethodContext pushBreak(Id codeLabel, ILabel L, StackType st) {
        MethodContext c = push();
        c.setBreakTarget(codeLabel, L);
        c.setBreakStack(codeLabel, st);
        return c;
    }
    
    MethodContext pushContinue(Id codeLabel, ILabel L, StackType st) {
        MethodContext c = push();
        c.setContinueTarget(codeLabel, L);
        c.setContinueStack(codeLabel, st);
        return c;
    }

    void setBreakTarget(Id label, ILabel tmp) {
        breakTargets.put(label(label), tmp);
    }

    void setContinueTarget(Id label, ILabel l) {
        continueTargets.put(label(label), l);
    }

    void setBreakStack(Id label, StackType tmp) {
        breakStacks.put(label(label), tmp);
    }

    void setContinueStack(Id label, StackType l) {
        continueStacks.put(label(label), l);
    }

    protected int freshLocal(Type t) {
        Name name = Name.makeFresh();
        return addLocal(new DummyVar(name.toString(), t));
    }

//    protected int addLabelLocal(Name name, Type t) {
//        return addLocal(new VarMapKey(VarMapKey.Kind.LABEL, name), name, t);
//    }
//
//    protected int addFrameLocal(Name name, Type t) {
//        return addLocal(new VarMapKey(VarMapKey.Kind.FRAME, name), name, t);
//    }

    public MethodContext getDeclaringExp(LocalDef sym) {
        Integer index = checkLocalIndex(sym);
        if (index != null)
            return this;
        if (containingExp != null)
            return containingExp.getDeclaringExp(sym);
        throw new InternalCompilerError("Undeclared local variable: " + sym);   
    }

    public boolean isBoxedHere(LocalDef symbol) {
        if (symbol == null)
            System.out.println("uh-oh");
//        if (symbol.owner() != theMethod)
//            System.out.println("uh-oh");
//        assert symbol.owner() == theMethod : "symbol=" + symbol + " owner=" + symbol.owner() + " theMethod=" + theMethod;
        return boxedField(symbol) != null;
    }

    public String boxedField(LocalDef symbol) {
        String s = boxingMap != null ? boxingMap.get(symbol) : null;
//        if (s != null && ! symbol.escapesFunction()) {
//            // Inconsistency!
//            for (IVarSymbol sym : boxingMap.keySet()) {
//                String t = boxingMap.get(sym);
//                if (s.equals(t)) {
//                    System.out.println("symbol=" + symbol + " also sym=" + sym);
//                    assert symbol == sym : "not equal! " + symbol + " " + symbol.position() + " " + sym.position();
//                }
//            }
//        }
//        if (s == null && symbol.escapesFunction()) {
//            // Inconsistency!
//            System.out.println("symbol=" + symbol + " not in boxingMap, but escapes");
//        }
//        assert (s != null) == symbol.escapesFunction() : symbol + " s=" + s + " escapes=" + symbol.escapesFunction() + " " + symbol.position();
        return s;
    }

    public String getOuterFrameField(MethodContext ti) {
        return frameFields != null ? frameFields.get(ti) : null;
    }
    
    public static interface InstructionSequence {
        void appendInstructions(IOpcodes il, MethodContext context);
    }

    MethodContext outer;
    
    MethodContext outer() { return outer; }
    
    public MethodContext pushFinally(InstructionSequence opcodes) {
        MethodContext c = push();
        c.finallyBlock = opcodes;
        return c;
    }

    public MethodContext pushLabeled(Labeled l) {
        MethodContext c = copy();
        c.labeledStatements = new ArrayList<Labeled>(this.labeledStatements.size()+1);
        c.labeledStatements.addAll(this.labeledStatements);
        c.labeledStatements.add(l);
        return c;
    }
    
    List<Labeled> labeledStatements = Collections.EMPTY_LIST;
    public Id continueLabel(Stmt n) {
        for (Labeled l : labeledStatements) {
            if (l.statement() == n)
                return l.labelNode();
        }
        return null;
    }
}
