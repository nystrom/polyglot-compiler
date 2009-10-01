package ibex.runtime;

import java.util.List;

public class Arrays {
    public static boolean[] booleanArray(List l) {
        boolean[] a = new boolean[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Boolean) l.get(i);
        }
        return a;
    }

    public static char[] charArray(List l) {
        char[] a = new char[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Character) l.get(i);
        }
        return a;
    }

    public static byte[] byteArray(List l) {
        byte[] a = new byte[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Byte) l.get(i);
        }
        return a;
    }

    public static short[] shortArray(List l) {
        short[] a = new short[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Short) l.get(i);
        }
        return a;
    }

    public static int[] intArray(List l) {
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Integer) l.get(i);
        }
        return a;
    }

    public static long[] longArray(List l) {
        long[] a = new long[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Long) l.get(i);
        }
        return a;
    }

    public static float[] floatArray(List l) {
        float[] a = new float[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Float) l.get(i);
        }
        return a;
    }

    public static double[] doubleArray(List l) {
        double[] a = new double[l.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = (Double) l.get(i);
        }
        return a;
    }

    public static Object[] objectArray(List l) {
        return l.toArray();
    }
}
