package com.lx.multimedialearn.utils;

/**
 * 对YUV数据旋转，镜像等工具类，需要导入libyuv.so包
 *
 * @author lixiao
 * @since 2017-11-30 17:21
 */
public class YuvUtils {
    static {
        System.loadLibrary("yuv_utils"); //和yuv区分开
    }

    /**
     * 对rgba进行镜像转换
     *
     * @param src
     */
    public static native void rgba_mirror(byte[] src, byte[] dst, int width, int height);

    public static native void rgba_nv21(byte[] src, byte[] dst, int width, int height);
}
