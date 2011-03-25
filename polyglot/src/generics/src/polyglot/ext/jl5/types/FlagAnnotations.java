package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.FlagsNode;
import polyglot.ext.jl5.ast.AnnotationElem;
import polyglot.util.TypedList;

public class FlagAnnotations {

    protected FlagsNode classicFlags;
    protected List annotations;

    public FlagAnnotations(FlagsNode classic, List annots){
        this.classicFlags = classic;
        this.annotations = annots;
    }

    public FlagAnnotations(FlagsNode classic){
    	this();
    	this.classicFlags = classic;
    }

    public FlagAnnotations() {
        this.annotations = new ArrayList();
    }

    public FlagsNode classicFlags(){
        return classicFlags;
    }

    public FlagAnnotations classicFlags(FlagsNode flags){
        this.classicFlags = flags;
        return this;
    }

    public FlagAnnotations annotations(List annotations){
        this.annotations = annotations;
        return this;
    }
    
    public List annotations(){
        return annotations;
    }
    
    public FlagAnnotations addAnnotation(Object o){
        if (annotations == null){
            annotations = new TypedList(new LinkedList(), AnnotationElem.class, false);
        }
        annotations.add((AnnotationElem)o);
        return this;
    }
}
