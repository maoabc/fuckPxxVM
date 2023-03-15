package pdd.vm.mw;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * mw.bin 常量池
 */
class Const {
    public static final int TAG_BOOL = 2;
    public static final int TAG_INT = 3;
    public static final int TAG_LONG = 4;
    public static final int TAG_FLOAT = 5;
    public static final int TAG_DOUBLE = 6;
    public static final int TAG_UTF = 7;
    //以下应该大多是间接常量（保存的是索引，索引指向常量池数据）
    public static final int TAG_CLASS = 8;
    public static final int TAG_FIELD = 9;
    public static final int TAG_METHOD = 10;
    public static final int TAG_ANNOTATION = 11;

    public static final int TAG_BYTE_ARRAY = 12;
    public static final int TAG_CHAR_ARRAY = 13;
    public static final int TAG_BOOL_ARRAY = 14;
    public static final int TAG_SHORT_ARRAY = 15;

    public static final int TAG_INT_ARRAY = 16;
    public static final int TAG_LONG_ARRAY = 17;
    public static final int TAG_FLOAT_ARRAY = 18;
    public static final int TAG_DOUBLE_ARRAY = 19;
    public static final int TAG_CLASS_ARRAY = 20;
    public static final int TAG_OBJECT_LIST = 21;

    public final int tag;

    public final Object obj;

    public final Object obj2;

    public final Object obj3;

    Const(int tag, Object obj, Object obj2, Object obj3) {
        this.tag = tag;
        this.obj = obj;
        this.obj2 = obj2;
        this.obj3 = obj3;
    }


    static Object readConst(InputStream input, Object[] pool) throws IOException {
        int tag = MwDataUtils.readByte(input);
        switch (tag) {
            case TAG_BOOL:
                return MwDataUtils.readByte(input) != 0;
            case TAG_INT:
                return MwDataUtils.readInt(input);
            case TAG_LONG:
                return MwDataUtils.readLong(input);
            case TAG_FLOAT:
                return MwDataUtils.readFloat(input);
            case TAG_DOUBLE:
                return MwDataUtils.readDouble(input);
            case TAG_UTF:
                return MwDataUtils.readUTF(input);
            case TAG_CLASS:
                return new Const(TAG_CLASS, pool[MwDataUtils.readShort(input)], null, null);
            case TAG_FIELD:
                return new Const(TAG_FIELD, pool[MwDataUtils.readShort(input)], pool[MwDataUtils.readShort(input)], null);
            case TAG_METHOD:
                return new Const(TAG_METHOD, pool[MwDataUtils.readShort(input)], pool[MwDataUtils.readShort(input)], pool[MwDataUtils.readShort(input)]);
            case TAG_ANNOTATION:
                return new Const(TAG_ANNOTATION, (String) pool[MwDataUtils.readShort(input)], (ArrayList<?>) pool[MwDataUtils.readShort(input)], null);
            case TAG_BYTE_ARRAY:
                return readBytes(input);
            case TAG_CHAR_ARRAY:
                return readChars(input);
            case TAG_BOOL_ARRAY:
                return readBooleans(input);
            case TAG_SHORT_ARRAY:
                return readShorts(input);
            case TAG_INT_ARRAY:
                return readInts(input, pool);
            case TAG_LONG_ARRAY:
                return readLongs(input, pool);
            case TAG_FLOAT_ARRAY:
                return readFloats(input, pool);
            case TAG_DOUBLE_ARRAY:
                return readDoubles(input, pool);
            case TAG_CLASS_ARRAY:
                return readObjectArray(input, pool);
            case TAG_OBJECT_LIST:
                return readObjectList(input, pool);
            default:
                throw new UnsupportedOperationException("t " + tag);
        }
    }

    public static Object[] initConstPool(InputStream input) throws IOException {
        int count = MwDataUtils.readShort(input);
        Object[] pool = new Object[count];
        for (int i = 0; i < count; i++) {
            pool[i] = readConst(input, pool);
        }
        return pool;
    }

    static boolean[] readBooleans(InputStream input) throws IOException {
        int size = MwDataUtils.readByte(input);
        boolean[] bools = new boolean[size];
        for (int i = 0; i < size; i++) {
            bools[i] = MwDataUtils.readByte(input) != 0;
        }
        return bools;
    }

    static char[] readChars(InputStream input) throws IOException {
        int size = MwDataUtils.readByte(input);
        char[] cArr = new char[size];
        for (int i = 0; i < size; i++) {
            cArr[i] = (char) MwDataUtils.readByte2(input);
        }
        return cArr;
    }

    static short[] readShorts(InputStream input) throws IOException {
        int size = MwDataUtils.readByte(input);
        short[] sArr = new short[size];
        for (int i = 0; i < size; i++) {
            sArr[i] = (short) MwDataUtils.readShort(input);
        }
        return sArr;
    }

    static byte[] readBytes(InputStream input) throws IOException {
        int size = MwDataUtils.readByte(input);
        byte[] bArr = new byte[size];
        for (int i = 0; i < size; i++) {
            bArr[i] = (byte) MwDataUtils.readByte(input);
        }
        return bArr;
    }

    static int[] readInts(InputStream input, Object[] pool) throws IOException {
        int size = MwDataUtils.readByte(input);
        int[] iArr = new int[size];
        for (int i = 0; i < size; i++) {
            iArr[i] = (Integer) pool[MwDataUtils.readShort(input)];
        }
        return iArr;
    }

    static long[] readLongs(InputStream input, Object[] pool) throws IOException {
        int size = MwDataUtils.readByte(input);
        long[] jArr = new long[size];
        for (int i = 0; i < size; i++) {
            jArr[i] = (Long) pool[MwDataUtils.readShort(input)];
        }
        return jArr;
    }

    static float[] readFloats(InputStream input, Object[] pool) throws IOException {
        int size = MwDataUtils.readByte(input);
        float[] fArr = new float[size];
        for (int i = 0; i < size; i++) {
            fArr[i] = (Float) pool[MwDataUtils.readShort(input)];
        }
        return fArr;
    }

    static double[] readDoubles(InputStream input, Object[] pool) throws IOException {
        int size = MwDataUtils.readByte(input);
        double[] dArr = new double[size];
        for (int i = 0; i < size; i++) {
            dArr[i] = (Double) pool[MwDataUtils.readShort(input)];
        }
        return dArr;
    }

    static ArrayList<Object> readObjectList(InputStream input, Object[] pool) throws IOException {
        int size = MwDataUtils.readByte(input);
        ArrayList<Object> arrayList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            arrayList.add(pool[MwDataUtils.readShort(input)]);
        }
        return arrayList;
    }

    static Object[] readObjectArray(InputStream input, Object[] pool) throws IOException {
        try {
            final String className = (String) pool[MwDataUtils.readShort(input)];
            final Class<?> cls = Class.forName(className, false, Const.class.getClassLoader());

            int size = MwDataUtils.readByte(input);

            Object[] objs = (Object[]) Array.newInstance(cls, size);
            for (int i = 0; i < size; i++) {
                objs[i] = pool[MwDataUtils.readShort(input)];
            }
            return objs;
        } catch (Throwable e) {
            throw new IOException(e.getCause());
        }
    }
}
