package ibex.types;

import ibex.lr.GLR;

import java.util.List;

import polyglot.types.ClassDef;

public interface IbexClassDef extends ClassDef {

    boolean isParser();
    List<Nonterminal> allNonterminals();
    List<Terminal> allTerminals();

    GLR glr();
    void setGLR(GLR glr);
    
    List<RuleDef> rules();
    void addRule(RuleDef n);
    void setRules(List<? extends RuleDef> l);

}