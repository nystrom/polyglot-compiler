/**
 * 
 */
package ibex.runtime;

public class Success implements IMatchResult {
    Object o;

    Success(Object o) {
        this.o = o;
    }
}