package com.lx.multimedialearn.openglstudy;

/**
 * OpenGL 用到的本地方法
 *
 * @author lixiao
 * @since 2017-11-24 21:38
 */
public class OpenGLJniUtils {

    static {
        System.loadLibrary("multi_media");
    }

    /**
     * 读取OpenGL中像素
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param format
     * @param type
     */
    public static native void glReadPixels(int x,
                                           int y,
                                           int width,
                                           int height,
                                           int format,
                                           int type);
}
