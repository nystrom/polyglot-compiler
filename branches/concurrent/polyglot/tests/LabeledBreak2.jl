class LabeledBreak {
    public static void main(String[] args) {
        int i;
        // Even though, ++i is unreachable, the JLS doesn't consider it a
        // statement, so it should not complain.
        a: for (i=0; i<10; ++i) {
            break a;
        }
        System.out.println("i = " + i); // prints "i = 0"
    }
}
