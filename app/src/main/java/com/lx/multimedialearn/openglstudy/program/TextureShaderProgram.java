package com.lx.multimedialearn.openglstudy.program;

import android.content.Context;
import android.opengl.GLES20;

import com.lx.multimedialearn.R;

/**
 * 文件描述
 *
 * @author lixiao
 * @since 2017-10-12 16:24
 */
public class TextureShaderProgram extends ShaderProgram {

    private int uMatrixLocation; //
    private int uTextureUnitLocation;

    private int aPositionLocation; //属性位置
    private int aTextureCoordinatesLocation;

    public TextureShaderProgram(Context context) {
        super(context, R.raw.texture_vertex_shader, R.raw.texture_fragment_shader);

        uMatrixLocation = GLES20.glGetUniformLocation(program, U_MATRIX);
        uTextureUnitLocation = GLES20.glGetUniformLocation(program, U_TEXTURE_UNIT);

        aPositionLocation = GLES20.glGetAttribLocation(program, A_POSITION);
        aTextureCoordinatesLocation = GLES20.glGetAttribLocation(program, A_TEXTURE_COORDINATES);
    }

    /**
     * 设置uniform，并返回属性位置
     *
     * @param matrix
     * @param textureId
     */
    public void setUniforms(float[] matrix, int textureId) {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //由于gpu同时只能渲染有限的纹理，所以引入纹理单元，纹理单元保存纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);//纹理绑定到纹理单元
        GLES20.glUniform1i(uTextureUnitLocation, 0); //把被选定的纹理单元传给片段着色器，进行纹理渲染
    }

    /**
     * 返回顶点属性的位置
     *
     * @return
     */
    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    /**
     * 返回纹理属性的位置
     *
     * @return
     */
    public int getTextureCoordinatesAttributeLocation() {
        return aTextureCoordinatesLocation;
    }

}
