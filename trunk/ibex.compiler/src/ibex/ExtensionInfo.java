package ibex;

import polyglot.lex.Lexer;
import ibex.parse.Lexer_c;
import ibex.parse.Grm;
import ibex.ast.*;
import ibex.types.*;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;
import polyglot.frontend.*;
import polyglot.main.*;

import java.util.*;
import java.io.*;

/**
 * Extension information for ibex extension.
 */
public class ExtensionInfo extends polyglot.frontend.JLExtensionInfo {
    static {
        // force Topics to load
        Topics t = new Topics();
    }

    public String defaultFileExtension() {
        return "ibex";
    }

    public String compilerName() {
        return "ibexc";
    }

    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        Lexer lexer = new Lexer_c(reader, source, eq);
        Grm grm = new Grm(lexer, ts, nf, eq);
        return new CupParser(grm, source, eq);
    }

    protected NodeFactory createNodeFactory() {
        return new IbexNodeFactory_c();
    }

    protected TypeSystem createTypeSystem() {
        return new IbexTypeSystem_c();
    }

}
