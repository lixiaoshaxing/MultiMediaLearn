package com.lx.multimedialearn.openglstudy.particles.program;

import android.content.Context;
import android.opengl.GLES20;

import com.lx.multimedialearn.R;

/**
 * 颜色着色器
 *
 * @author lixiao
 * @since 2017-10-12 16:47
 */
public class ColorShaderProgram extends ShaderProgram {
    private int uMatrixLocation;
    private int aPositionLocation;
    private int aColorLocation;
    private int uColorLocation;

    public ColorShaderProgram(Context context) {
        super(context, R.raw.simple_vertex_shader, R.raw.simple_fragment_shader);

        uMatrixLocation = GLES20.glGetUniformLocation(program, U_MATRIX);
        uColorLocation = GLES20.glGetUniformLocation(program, U_COLOR);
        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);
        // aColorLocation = GLES20.glGetAttribLocation(program, A_COLOR);
    }

    /**
     * 通过矩阵，设置顶点颜色，进行平滑着色
     */
    public void setUniforms(float[] matrix, float r, float g, float b) {
        GLES20.glUniformMatrix4fv(
                uMatrixLocation,
                1,
                false,
                matrix,
                0);
        GLES20.glUniform4f(uColorLocation, r, g, b, 1f);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getColorAttributeLocation() {
        return aColorLocation;
    }

}
