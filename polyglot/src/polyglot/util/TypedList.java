/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

/*
 * TypedList.java
 */

package polyglot.util;


import java.util.*;

/**
 * A TypedList is an List which will not allow members not belonging
 * to a given type to be added to a collection.  Optionally, it may
 * also present an immutable view.
 *
 * If an attempt is made to change an immutable list, or if an attempt
 * is made to insert an improperly-typed element, an
 * UnsupportedOperationException is thrown.
 *
 * This class is given so that we can present a List for a given class
 * without worrying about outsiders breaking the rep.
 *
 * This is a poor substitute for PolyJ.
 **/
public class TypedList
{
  /**
   * Requires: <list> not null, and every element of <list> may be
   *    cast to class <c>.
   * Creates a new TypedList, containing all the elements of <list>,
   * which restricts all members to belong to class <c>.  If <c> is
   * null, no typing restriction is made.  If <immutable> is true, no
   * modifications are allowed.
   **/
    public static <T> List<T> copy(List<T> list, Class<T> c, boolean immutable) {
	return copy(list, c);
    }
    
    public static <T> List<T> copy(List<T> list, Class<T> c) {
      if (list == null)
	  return null;
      return Collections.<T>checkedList(list, c);
  }

  /**
   * Creates a new TypedList, containing all the elements of <list>,
   * which restricts all members to belong to class <c>.  If <c> is
   * null, no typing restriction is made.  If <immutable> is true, no
   * modifications are allowed.
   *
   * Throws an UnsupportedOperationException if any member of <list>
   * may not be cast to class <c>.
   **/
  public static <T> List<T> copyAndCheck(List<T> list, Class<T> c, boolean immutable) {
    if (c != null)
      check(list,c);
    return copy(list,c,immutable);
  }

  /**
   * Throws an UnsupportedOperationException if any member of <list>
   * may not be cast to class <c>. Otherwise does nothing.
   **/
  public static <T> void check(List<T> list, Class<T> c) {
    if (list == null)
      return;
    for (Iterator<T> i = list.iterator(); i.hasNext(); ) {
      T o = i.next();
      if (o != null && !c.isAssignableFrom(o.getClass())) {
	throw new UnsupportedOperationException(
		     "Tried to add a " + o.getClass().getName() +
   	             " to a list of type " + c.getName());
      }
    }
  }

}

