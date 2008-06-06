/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.util;

/** A two-element tuple.
 */
public class Pair<S,T>
{
	protected S part1;
	protected T part2;

	public Pair(S p1, T p2) {
		this.part1 = p1;
		this.part2 = p2;
	}

	public S part1() {
		return part1;
	}

	public T part2() {
		return part2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair<?,?> p = (Pair<?,?>) obj;
			boolean r1 = part1 == null ? p.part1 == null : part1.equals(p.part1);
			boolean r2 = part2 == null ? p.part2 == null : part2.equals(p.part2);
			return r1 && r2;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (part1 == null ? 0 : part1.hashCode()) + (part2 == null ? 0 : part2.hashCode());
	}

	public String toString() {
		return "<" + part1 + ", " + part2 + ">";
	}
}
