/**
 * 
 */
package polyglot.bytecode.rep.asm;

import org.objectweb.asm.Label;

import polyglot.bytecode.rep.ILabel;

public class MyLabel implements ILabel {
    public MyLabel(Label label) {
        L = label;
    }

    Label L;
}