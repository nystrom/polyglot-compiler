package polyglot.ext.pao.extension;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import polyglot.ext.pao.ast.*;
import polyglot.ext.pao.types.*;
import polyglot.types.*;
import polyglot.frontend.*;
import polyglot.visit.*;
import polyglot.util.*;

public interface PaoDel extends Del {
    public Node rewrite(PaoTypeSystem ts, PaoNodeFactory nf);
}