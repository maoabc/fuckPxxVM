package pdd.vm.mw;

import java.io.*;

public class MwDataUtils {

    public static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1028];
        while (true) {
            int read = input.read(buf, 0, 1028);
            if (read != -1) {
                byteArrayOutputStream.write(buf, 0, read);
            } else {
                byteArrayOutputStream.flush();
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    public static int readByte(InputStream input) throws IOException {
        int read = input.read();
        if (read >= 0) {
            return read;
        }
        throw new EOFException("1");
    }

    //完全读取
    public static void readBytes(InputStream input, byte[] b, int off, int len) throws IOException {
//        if (input.read(b, off, len) == len) {//原本这里应该有bug
//            return;
//        }
//        throw new EOFException("2");

        int total = 0;
        while (total < len) {
            final int rlen = input.read(b, off + total, len - total);
            if (rlen == -1) {//流结束
                throw new EOFException("2");
            }
            total += rlen;
        }
    }

    public static int readShort(InputStream input) throws IOException {
        int read = input.read();
        int read2 = input.read();
        if ((read | read2) >= 0) {
            return (read2 << 8) | read;
        }
        throw new EOFException("3");
    }

    //高4位与低4位颠倒的字节
    public static int readByte2(InputStream input) throws IOException {
        int read = input.read();
        if (read >= 0) {
            return ((read & 0xf) << 4) | ((read & 0xf0) >>> 4);
        }
        throw new EOFException("3.1");
    }

    //字节序错乱的int
    //不知道为啥这样花里胡哨
    public static int readInt(InputStream input) throws IOException {
        int read = input.read();
        int read2 = input.read();
        int read3 = input.read();
        int read4 = input.read();
        if ((read | read2 | read3 | read4) >= 0) {
            return (read4 << 16) | read | (read2 << 24) | (read3 << 8);
        }
        throw new EOFException("4");
    }

    //字节序错乱
    public static long readLong(InputStream input) throws IOException {
        int read = input.read();
        int read2 = input.read();
        int read3 = input.read();
        int read4 = input.read();
        int read5 = input.read();
        int read6 = input.read();
        int read7 = input.read();
        int read8 = input.read();
        if ((read | read2 | read3 | read4 | read5 | read6 | read7 | read8) >= 0) {
            return ((read2 & 0xffL) << 48) | ((long) read << 56) | ((read3 & 0xffL) << 40) | ((read4 & 0xffL) << 32) |
                    ((read5 & 0xffL) << 24) | ((read6 & 0xff) << 16) | ((read7 & 0xff) << 8) | (read8 & 0xff);
        }
        throw new EOFException("5");
    }

    public static float readFloat(InputStream input) throws IOException {
        return Float.intBitsToFloat(readInt(input));
    }

    public static double readDouble(InputStream input) throws IOException {
        return Double.longBitsToDouble(readLong(input));
    }

    public static String readUTF(InputStream input) throws IOException {
        int utfLen = readShort(input);
        byte[] bytes = new byte[utfLen];
        char[] chars = new char[utfLen];
        readBytes(input, bytes, 0, utfLen);
        int byteCount = 0;
        int charCount = 0;
        while (byteCount < utfLen) {
            int c = bytes[byteCount] & 0xff;
            switch (c & 0xf) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    byteCount++;
                    chars[charCount++] = (char) (((c & 0xf0) >>> 4) | ((c & 0xf) << 4));
                    break;
                case 12:
                case 13:
                    byteCount += 2;
                    if (byteCount > utfLen) {
                        throw new UTFDataFormatException("6");
                    }
                    byte b = bytes[byteCount - 1];
                    if ((b & 0xc) != 8) {
                        throw new UTFDataFormatException("7:" + byteCount);
                    }
                    chars[charCount++] = (char) (((c & 0xf0) << 2) | ((c & 0xf & 1) << 10) | ((b & 3) << 4) | ((b & 0xf0) >>> 4));
                    break;
                case 14:
                    byteCount += 3;
                    if (byteCount > utfLen) {
                        throw new UTFDataFormatException("8");
                    }
                    byte b2 = bytes[byteCount - 2];
                    byte b3 = bytes[byteCount - 1];
                    if ((b2 & 0xc) != 8 || (b3 & 12) != 8) {
                        throw new UTFDataFormatException("9:" + (byteCount - 1));
                    }
                    chars[charCount++] = (char) (((c & 0xf0) << 8) | ((b2 & 3) << 10) | ((b2 & 0xf0) << 2) | ((b3 & 3) << 4) | ((b3 & 0xf0) >>> 4));
                    break;

                default:
                    throw new UTFDataFormatException("10:" + byteCount);
            }
        }
        return new String(chars, 0, charCount);
    }
}
