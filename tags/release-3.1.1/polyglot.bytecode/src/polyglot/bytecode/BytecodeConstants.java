package polyglot.bytecode;

import java.lang.reflect.Modifier;

/** This class produces a Java source file (in a string) for a single Thorn compilation unit. */
public interface BytecodeConstants {
    final static int ACC_ABSTRACT = Modifier.ABSTRACT;
    final static int ACC_FINAL = Modifier.FINAL;
    final static int ACC_INTERFACE = Modifier.INTERFACE;
    final static int ACC_NATIVE = Modifier.NATIVE;
    final static int ACC_PRIVATE = Modifier.PRIVATE;
    final static int ACC_PROTECTED = Modifier.PROTECTED;
    final static int ACC_PUBLIC = Modifier.PUBLIC;
    final static int ACC_STATIC = Modifier.STATIC;
    final static int ACC_STRICT = Modifier.STRICT;
    final static int ACC_SUPER = Modifier.SYNCHRONIZED;
    final static int ACC_SYNCHRONIZED = Modifier.SYNCHRONIZED;
    final static int ACC_TRANSIENT = Modifier.TRANSIENT;
    final static int ACC_VOLATILE = Modifier.VOLATILE;
}
