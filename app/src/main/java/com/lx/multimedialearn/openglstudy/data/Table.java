package com.lx.multimedialearn.openglstudy.data;

import android.opengl.GLES20;

import com.lx.multimedialearn.openglstudy.program.TextureShaderProgram;
import com.lx.multimedialearn.utils.GlUtil;

/**
 * 根据VertexArray对Table进行封装
 *
 * @author lixiao
 * @since 2017-10-12 15:47
 */
public class Table {
    private static final int POSITION_COMPONENT_COUNT = 2;
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2;
    private static final int STRIDE = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * GlUtil.SIZEOF_FLOAT;

    private static final float[] VERTEX_DATA = {
            //X, Y(顶点坐标), S, T（纹理坐标）
            0f, 0f, 0.5f, 0.5f, //纹理坐标是向右为x轴，向下为y轴正轴，逐渐变大，和Y正好相反
            -0.5f, -0.8f, 0f, 0.9f,
            0.5f, -0.8f, 1f, 0.9f,
            0.5f, 0.8f, 1f, 0.1f,
            -0.5f, 0.8f, 0f, 0.1f,   //使用0.1-0.9，是对纹理图像进行了边缘剪裁
            -0.5f, -0.8f, 0f, 0.9f
    };

    private VertexArray vertexArray; //定义桌子的顶点属性

    public Table() {
        vertexArray = new VertexArray(VERTEX_DATA);
    }

    /**
     * 对桌子绑定数据
     *
     * @param textureProgram
     */
    public void bindData(TextureShaderProgram textureProgram) {
        vertexArray.setVertexAttribPointer(
                0,
                textureProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT,
                STRIDE
        );

        vertexArray.setVertexAttribPointer(
                POSITION_COMPONENT_COUNT,
                textureProgram.getTextureCoordinatesAttributeLocation(),
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                STRIDE);
    }

    public void draw() {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 6);
    }
}
