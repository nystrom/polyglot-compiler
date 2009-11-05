package ibex.types;

import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.ParsedClassType;
import polyglot.types.ParsedClassType_c;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem_c;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class IbexTypeSystem_c extends TypeSystem_c implements IbexTypeSystem {
    @Override
    public ClassDef createClassDef(Source fromSource) {
        return new IbexClassDef_c(this, fromSource);
    }
    
    public Type nullable(Type t) {
        IbexTypeSystem_c ts = this;
        try {
        if (t.isBoolean())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Boolean"));
        if (t.isChar())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Character"));
        if (t.isByte())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Byte"));
        if (t.isShort())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Short"));
        if (t.isInt())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Integer"));
        if (t.isLong())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Long"));
        if (t.isFloat())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Float"));
        if (t.isDouble())
            return (Type) ts.systemResolver().find(QName.make("java.lang.Double"));
        assert t.isReference();
        return t;
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(e);
        }
    }


    @Override
    public ParsedClassType createClassType(Position pos, Ref<? extends ClassDef> def) {
        // TODO Auto-generated method stub
        return new IbexClassType_c(this, pos, def);
    }

    public RuleDef ruleDef(Position pos, Ref<? extends ClassType> container,
            Flags flags, Ref<? extends Type> type, Name name, List<Rhs> choices) {
        return new RuleDef_c(this, pos, container, flags, type, name, choices);
    }

    public ClassType runtimeParserType() {
        return load("ibex.runtime.IParser");
    }
    
    public ClassType runtimeCharParserType() {
        return load("ibex.runtime.ICharParser");
    }

    public ClassType runtimeByteParserType() {
        return load("ibex.runtime.IByteParser");
    }
    
    public ClassType runtimeParserImplType() {
        return load("ibex.runtime.ParserImpl");
    }

    public RuleInstance findSymbol(IbexClassType container, Name name)
        throws SemanticException
    {
        Symbol s;
        
        for (RuleInstance r : container.rules()) {
            if (r.name() == name)
                return r;
        }

        Type t = container.superClass();

        if (t instanceof IbexClassType) {
            try {
                return findSymbol((IbexClassType) t, name);
            }
            catch (SemanticException e) {
            }
        }

        throw new SemanticException("Could not find nonterminal \"" + name +
                                    "\" in class \"" + container + "\".");
    }
}
