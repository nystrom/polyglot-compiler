package polyglot.bytecode.rep;

import java.util.List;

import polyglot.ast.ClassDecl;
import polyglot.bytecode.types.Type;
import polyglot.types.Name;
import polyglot.types.QName;

public interface IClassGen {

    int getFlags();
    
    Type type();

    List<IMethodGen> methods();

    List<IFieldGen> fields();

    List<IClassGen> innerClasses();

    void addMethod(IMethodGen m);

    void addField(IFieldGen m);

    void addInnerClass(IClassGen m);

    byte[] bytes();

    QName fullName();
    Name name();

    String getName();
}