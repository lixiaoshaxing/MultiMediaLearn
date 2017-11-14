package com.lx.multimedialearn.utils;

/**
 * 类型转换，数学相关
 *
 * @author lixiao
 * @since 2017-11-13 23:47
 */
public class MathUtils {
    /**
     * 把长度为4的字节数据转换为int
     *
     * @param bytes
     * @param offset 起始位置
     * @return
     */
    public static int byte4ToInt(byte[] bytes, int offset) {
        int b3 = bytes[offset + 3] & 0xFF;
        int b2 = bytes[offset + 2] & 0xFF;
        int b1 = bytes[offset + 1] & 0xFF;
        int b0 = bytes[offset + 0] & 0xFF;
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    /**
     * 字节数组转换为短整型
     *
     * @param bytes
     * @param offset
     * @return
     */
    public static short byte2ToShort(byte[] bytes, int offset) {
        int b1 = bytes[offset + 1] & 0xFF;
        int b0 = bytes[offset + 0] & 0xFF;
        return (short) ((b1 << 8) | b0);
    }

    /**
     * 字节数组转换为浮点型,从byte中读取float的方法
     */
    public static float byte4ToFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(byte4ToInt(bytes, offset));
    }
}
