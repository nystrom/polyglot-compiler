package jltools.types;

import java.util.List;
import jltools.util.TypedList;
import jltools.util.AnnotatedObject;
import java.util.Iterator;

/**
 * A <code>ClassTypeImpl</code> is used to implement non-lazy Java classes.
 * That is, ones where information is computed once, rather than on demand. 
 */
public abstract class ClassTypeImpl extends ClassType 
{
  static final long serialVersionUID = 6483392342759261702L;

  protected ClassTypeImpl()
  {
    super();
  }

  public ClassTypeImpl( TypeSystem ts)
  {
    super( ts);
  }
  
  public String getFullName()         { return fullName; }
  public String getShortName()        { return shortName; }
  public String getPackage()          { return packageName; }

  public List getMethods()              { return methods; }
  public List getFields()               { return fields; }
  public List getInterfaces()           { return interfaces; }

  public AccessFlags getAccessFlags()   { return flags; }
  public Type getSuperType()       { return superType; } 
  public boolean isInner()              { return isInner; } 
  public boolean isAnonymous()          { return isAnonymous; }
  public ClassType getContainingClass() { return containingClass; }
  public String getInnerName()          { return innerName; }
  public List getInnerClasses()         { return innerClasses; }

  public ClassType getInnerNamed(String name) {
    if ( innerClasses != null)
      for (Iterator i = innerClasses.iterator(); i.hasNext();) {
        ClassType innerType = (ClassType) i.next();
        if (innerType.getShortName().equals(name))
          return innerType;
      }
    return null;
  }

  public void dump()
  {
    System.out.println( "---------------------");
    System.out.println( "Class: " + fullName);
    System.out.println( "Super: " + superType.getTypeString() + " ("
                        + superType.getClass().getName() + ")");
    System.out.println( "Inner Classes: ");
    for (Iterator i = innerClasses.iterator(); i.hasNext();) {
      ClassType innerType = (ClassType) i.next();
      System.out.println( "  " + innerType.getTypeString());
    }
    System.out.println( "Fields: ");
    for( Iterator iter = fields.iterator(); iter.hasNext(); ) {
      FieldInstance fi = (FieldInstance)iter.next();
      System.out.println( "  " + fi.getName());
      System.out.println( "    " + fi.getAccessFlags().getStringRepresentation());
      System.out.println( "    " + fi.getType().getTypeString() + " ("
                        + fi.getType().getClass().getName() + ")");
    }
    System.out.println( "Methods: ");

    System.out.println( "---------------------");
  }
      

  // The package name.
  protected String packageName;
  // The full name, including package and outers.
  protected String fullName;
  // The short name, not including outers.  
  protected String shortName;

  ////
  // Typing info
  ////
  // The supertype.  (null for JLO)
  protected Type superType;
  // The TypedList of interface types.
  protected TypedList interfaces;
  // The access flags for this class.
  protected AccessFlags flags;

  ////
  // Members
  ////
  protected TypedList methods;
  protected TypedList fields;

  ////
  // Inner info
  ////
  protected boolean isInner;
  protected boolean isAnonymous;
  protected ClassType containingClass;
  protected String innerName;
  protected TypedList innerClasses;
}

