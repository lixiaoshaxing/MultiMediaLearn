package com.lx.multimedialearn.openglstudy.particles.data;

import android.opengl.GLES20;
import android.util.Log;

import com.lx.multimedialearn.utils.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 顶点缓冲区（公用）
 * 这里使用了VBO
 *
 * @author lixiao
 * @since 2017-10-19 17:31
 */
public class VertexBuffer {
    private int bufferId;

    /**
     * 对于顶点，构建顶点缓冲区，相对顶点数组，效率更高
     *
     * @param vertexData
     */
    public VertexBuffer(float[] vertexData) {
        //申请缓冲区id
        int[] buffers = new int[1];
        GLES20.glGenBuffers(buffers.length, buffers, 0); //使用vbo，申请缓冲区
        if (buffers[0] == 0) {
            Log.w("sys.out", "申请顶点缓冲区出错");
        }
        bufferId = buffers[0];

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);

        //导入数据
        FloatBuffer vertexArray = ByteBuffer
                .allocateDirect(vertexData.length * GlUtil.SIZEOF_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexArray.position(0);

        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexArray.capacity() * GlUtil.SIZEOF_FLOAT, vertexArray, GLES20.GL_STATIC_DRAW);

        //解除绑定
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    /**
     * 对数据进行赋值
     *
     * @param dataOffset
     * @param attributeLocation
     * @param componentCount
     * @param stride
     */
    public void setVertexAttribPointer(int dataOffset, int attributeLocation, int componentCount, int stride) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId);
        GLES20.glVertexAttribPointer(attributeLocation, componentCount, GLES20.GL_FLOAT, false, stride, dataOffset);
        GLES20.glEnableVertexAttribArray(attributeLocation);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}
