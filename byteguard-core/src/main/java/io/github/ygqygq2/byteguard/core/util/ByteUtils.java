package io.github.ygqygq2.byteguard.core.util;

import java.util.Arrays;

/**
 * 字节数组工具类
 *
 * @author ygqygq2
 */
public final class ByteUtils {

    private ByteUtils() {}

    /**
     * 拼接多个字节数组
     *
     * @param arrays 待拼接的字节数组
     * @return 拼接结果
     */
    public static byte[] concat(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] arr : arrays) {
            if (arr != null) totalLength += arr.length;
        }
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] arr : arrays) {
            if (arr != null) {
                System.arraycopy(arr, 0, result, offset, arr.length);
                offset += arr.length;
            }
        }
        return result;
    }

    /**
     * 截取字节数组片段
     *
     * @param src    源数组
     * @param offset 起始偏移
     * @param length 长度
     * @return 子数组
     */
    public static byte[] slice(byte[] src, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(src, offset, result, 0, length);
        return result;
    }

    /**
     * 常量时间比较，防止时序攻击
     *
     * @param a 数组 a
     * @param b 数组 b
     * @return 是否相等
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return a == b;
        if (a.length != b.length) return false;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    /**
     * 清零敏感字节数组
     *
     * @param data 待清零的数组
     */
    public static void wipe(byte[] data) {
        if (data != null) Arrays.fill(data, (byte) 0);
    }

    /**
     * 将 int 编码为大端序 4 字节
     */
    public static byte[] intToBytes(int value) {
        return new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value
        };
    }

    /**
     * 从大端序 4 字节解码 int
     */
    public static int bytesToInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
             | ((bytes[offset + 1] & 0xFF) << 16)
             | ((bytes[offset + 2] & 0xFF) << 8)
             |  (bytes[offset + 3] & 0xFF);
    }
}
