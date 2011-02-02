package polyglot.ext.jl5.parse;

import polyglot.ast.Expr;
import polyglot.ast.PackageNode;
import polyglot.parse.Name;
import polyglot.ext.jl5.ast.JL5NodeFactory;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.parse.BaseParser;
import polyglot.util.Position;

public class JL5Name extends Name {

    public JL5NodeFactory nf;
    public JL5TypeSystem ts;
    
    public JL5Name(BaseParser parser, Position pos, String name){
        super(parser, pos, name);
        this.nf = (JL5NodeFactory)parser.nf;
        this.ts = (JL5TypeSystem)parser.ts;
    }
   
    public JL5Name(BaseParser parser, Position pos, Name prefix, String name){
        super(parser, pos, prefix, name);
        this.nf = (JL5NodeFactory)parser.nf;
        this.ts = (JL5TypeSystem)parser.ts;
    }
    
    public Expr toExpr(){
        if (prefix == null){
            return nf.AmbExpr(pos, name);
        }
        return nf.JL5Field(pos, prefix.toReceiver(), name);
    }

    public PackageNode toPackage(FlagAnnotations fl) {
        if (prefix == null) {
            return nf.JL5PackageNode(pos, fl, ts.createPackage(null, name));
        }
        else {
            return nf.JL5PackageNode(pos, fl, ts.createPackage(((JL5Name)prefix).toPackage(fl).package_(), name));
        }
    }
   
}
