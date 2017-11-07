package com.lx.multimedialearn.openglstudy.program;

import android.content.Context;
import android.opengl.GLES20;

import com.lx.multimedialearn.R;

/**
 * 处理天空图绘制，结合着色器
 *
 * @author lixiao
 * @since 2017-10-19 15:36
 */
public class SkyBoxShaderProgram extends ShaderProgram {

    private int uMatrixLocation;
    private int uTextureUnitLocation;
    private int aPositionLocation;

    public SkyBoxShaderProgram(Context context) {
        super(context, R.raw.skybox_vertex_shader, R.raw.skybox_fragment_shader);

        uMatrixLocation = GLES20.glGetUniformLocation(program, U_MATRIX); //获取投影变换的位置
        uTextureUnitLocation = GLES20.glGetUniformLocation(program, U_TEXTURE_UNIT);
        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);
    }

    public void setUniforms(float[] matrix, int textureId) {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureId);
        GLES20.glUniform1i(uTextureUnitLocation, 0);
    }

    /**
     * 对位置信息进行赋值
     *
     * @return
     */
    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }
}
