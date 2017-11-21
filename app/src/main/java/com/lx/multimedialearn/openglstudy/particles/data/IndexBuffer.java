package com.lx.multimedialearn.openglstudy.particles.data;

import android.util.Log;

import com.lx.multimedialearn.utils.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glGenBuffers;

/**
 * 索引缓冲区（公用）VBO
 *
 * @author lixiao
 * @since 2017-10-19 17:31
 */
public class IndexBuffer {
    private final int bufferId;

    public IndexBuffer(short[] indexData) {
        final int buffers[] = new int[1];
        glGenBuffers(buffers.length, buffers, 0);

        if (buffers[0] == 0) {
            Log.w("sys.out", "创建索引缓冲区失败");
        }

        bufferId = buffers[0];

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0]);// 使用vbo

        ShortBuffer indexArray = ByteBuffer
                .allocateDirect(indexData.length * GlUtil.SIZEOF_SHORT)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(indexData);
        indexArray.position(0);

        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArray.capacity() * GlUtil.SIZEOF_SHORT,
                indexArray, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

    }

    public int getBufferId() {
        return bufferId;
    }
}
