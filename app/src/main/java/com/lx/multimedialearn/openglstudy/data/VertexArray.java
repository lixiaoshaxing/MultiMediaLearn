package com.lx.multimedialearn.openglstudy.data;

import android.opengl.GLES20;

import com.lx.multimedialearn.utils.GlUtil;

import java.nio.FloatBuffer;

/**
 * 对物体进行封装
 * 例如对于桌子：封装桌子的坐标，纹理坐标，进行绘图
 *
 * @author lixiao
 * @since 2017-10-12 15:35
 */
public class VertexArray {
    private FloatBuffer floatBuffer;

    public VertexArray(float[] vertexData) {
        floatBuffer = GlUtil.createFloatBuffer(vertexData);
    }

    /**
     * 对顶点数据进行赋值
     *
     * @param dataOffset
     * @param attributeLocation
     * @param componentCount
     * @param stride
     */
    public void setVertexAttribPointer(int dataOffset, int attributeLocation, int componentCount, int stride) {
        floatBuffer.position(dataOffset);
        GLES20.glVertexAttribPointer(
                attributeLocation,
                componentCount,
                GLES20.GL_FLOAT,
                false,
                stride,
                floatBuffer);
        GLES20.glEnableVertexAttribArray(attributeLocation);
        floatBuffer.position(0);
    }

    /**
     * 本地数组中追加新数据
     */
    public void updateBuffer(float[] vertexData, int start, int count) {
        floatBuffer.position(start);
        floatBuffer.put(vertexData, start, count);
        floatBuffer.position(0); //回归到位置0
    }
}
