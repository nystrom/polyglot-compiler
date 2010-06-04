package ibex.lr;

import java.util.*;

/** 
 * Implementation of Tarjan's union-find data structure.
 * Elements are integers.
 * A good description of union-find can be found in [Cormen, et. al. 1990].
 */
public class UnionFind {
    /** The trees of Nodes that represent the disjoint sets. */
    Node[] nodes;
    Node[] stack;

    public UnionFind(int size) {
        nodes = new Node[size];
        stack = new Node[size];
    }

    /**
     * Searches the disjoint sets for a given integer.  Returns the set
     * containing the integer a.  Sets are represented by a local class
     * <tt>Node</tt>.
     */
    private Node findNode(int a) {
          Node na = nodes[a];

          if (na == null) {
              // Start a new set with a
              Node root = new Node(a);

              root.child = new Node(a);
              root.child.parent = root;

              nodes[a] = root.child;

              return root;
          }

          return findNode(na);
    }

    /**
     * Returns the integer value associated with the first <tt>Node</tt>
     * in a set.
     */
    public int find(int a) {
        return findNode(a).value;
    }

    /**
     * Finds the set containing a given Node.
     */
    private Node findNode(Node node) {
        int top = 0;

        // Find the child of the root element.
        while (node.parent.child == null) {
            stack[top++] = node;
            node = node.parent;
        }

        // Do path compression on the way back down.
        Node rootChild = node;

        while (top > 0) {
            node = stack[--top];
            node.parent = rootChild;
        }

        return rootChild.parent;
    }

    /**
     * Returns true if a and b are in the same set.
     */
    public boolean isEquiv(int a, int b) {
        return findNode(a) == findNode(b);
    }

    /**
     * Combines the set that contains a with the set that contains b.
     */
    public void union(int a, int b) {
        Node na = findNode(a);
        Node nb = findNode(b);

        if (na == nb) {
            return;
        }

        // Link the smaller tree under the larger.
        if (na.rank > nb.rank) {
            // Delete nb.
            nb.child.parent = na.child;
            na.value = b;
        }
        else {
            // Delete na.
            na.child.parent = nb.child;
            nb.value = b;

            if (na.rank == nb.rank) {
                nb.rank++;
            }
        }
    }

    static class Node {
        Node parent;  // The root of the tree in which this Node resides
        Node child;
        int value;
        int rank;     // This Node's height in the tree

        public Node(int v) {
            value = v;
            rank = 0;
        }
    }
}
