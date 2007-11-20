/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.*;

import polyglot.ast.*;
import polyglot.types.LocalDef;
import polyglot.util.InternalCompilerError;
import polyglot.util.UniqueID;

/**
 * The <code>AlphaRenamer</code> runs over the AST and alpha-renames any local
 * variable declarations that it encounters.
 **/
public class AlphaRenamer extends NodeVisitor {

  protected NodeFactory nf;

  // Each set in this stack tracks the set of local decls in a block that
  // we're traversing.
  protected Stack<Set<String>> setStack;

  protected Map<String,String> renamingMap;

  // Tracks the set of variables known to be fresh.
  protected Set<String> freshVars;

  /**
   * Creates a visitor for alpha-renaming locals.
   *
   * @param nf  The node factory to be used when generating new nodes.
   **/
  public AlphaRenamer(NodeFactory nf) {
    this.nf = nf;

    this.setStack = new Stack<Set<String>>();
    this.setStack.push( new HashSet<String>() );

    this.renamingMap = new HashMap<String,String>();
    this.freshVars = new HashSet<String>();
  }

  public NodeVisitor enter( Node n ) {
    if ( n instanceof Block ) {
      // Push a new, empty set onto the stack.
      setStack.push( new HashSet<String>() );
    }

    if ( n instanceof LocalDecl ) {
      LocalDecl l = (LocalDecl)n;
      String name = l.id().id();

      if ( !freshVars.contains(name) ) {
	// Add a new entry to the current renaming map.
	String name_ = UniqueID.newID(name);

	freshVars.add(name_);

	setStack.peek().add( name );
	renamingMap.put( name, name_ );
      }
    }

    return this;
  }

  public Node leave( Node old, Node n, NodeVisitor v ) {
    if ( n instanceof Block ) {
      // Pop the current name set off the stack and remove the corresponding
      // entries from the renaming map.
      Set<String> s = setStack.pop();
      renamingMap.keySet().removeAll(s);
      return n;
    }

    if ( n instanceof Local ) {
      // Rename the local if its name is in the renaming map.
      Local l = (Local)n;
      String name = l.name();

      if ( !renamingMap.containsKey(name) ) {
	return n;
      }
      
      // Update the local instance as necessary.
      String newName = renamingMap.get(name);
//      LocalType li = l.localInstance();
//      if (li != null) li.setName(newName);

      return l.name(newName);
    }

    if ( n instanceof LocalDecl ) {
      // Rename the local decl.
      LocalDecl l = (LocalDecl)n;
      String name = l.id().id();

      if ( freshVars.contains(name) ) {
	return n;
      }

      if ( !renamingMap.containsKey(name) ) {
	throw new InternalCompilerError( "Unexpected error encountered while "
					 + "alpha-renaming." );
      }

      // Update the local instance as necessary.
      String newName = renamingMap.get(name);
      LocalDef li = l.localDef();
      if (li != null) li.setName(newName);
      return l.id(l.id().id(newName));
    }

    return n;
  }
}
