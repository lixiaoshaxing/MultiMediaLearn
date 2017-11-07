package com.lx.multimedialearn.openglstudy.program;

import android.content.Context;
import android.opengl.GLES20;

import com.lx.multimedialearn.R;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;

/**
 * 粒子着色器
 * 1. 确定着色器中的变量的位置
 * 2. 预留端口返回位置
 * 3. 粒子系统拿到这里的位置，绑定数据，进行绘制
 *
 * @author lixiao
 * @since 2017-10-18 17:41
 */
public class ParticleShaderProgram extends ShaderProgram {
    private static final String U_TEXTURE_UNIT = "u_TextureUnit";
    private int uMatrixLocation; //投影变换使用
    private int uTimeLocation; //当前时间


    private int aPositionLocation; //粒子初始位置
    private int aColorLocation; //粒子颜色
    private int aDirectionVectorLocation; //粒子发射方向
    private int aParticleStartTimeLocation; //粒子发射的时间，计算粒子已经运行的时间，进行亮度，速度的衰减

    private int uTextureUnitLocation; //使用图片替代点

    public ParticleShaderProgram(Context context) {
        super(context, R.raw.particle_vertex_shader, R.raw.particle_fragment_shader);
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uTimeLocation = glGetUniformLocation(program, U_TIME);

        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        aColorLocation = glGetAttribLocation(program, A_COLOR);
        aDirectionVectorLocation = glGetAttribLocation(program, A_DIRECTION_VECTOR);
        aParticleStartTimeLocation = glGetAttribLocation(program, A_PARTICLE_START_TIME);
        uTextureUnitLocation = GLES20.glGetUniformLocation(program, U_TEXTURE_UNIT);
    }

    public void setUniforms(float[] matrix, float elapsedTime, int textureId) {
        glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        glUniform1f(uTimeLocation, elapsedTime);

        GLES20.glActiveTexture(GL_TEXTURE0); //加载纹理
        glBindTexture(GL_TEXTURE_2D, textureId);
        glUniform1i(uTextureUnitLocation, 0);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    public int getColorAttributeLocation() {
        return aColorLocation;
    }

    public int getDirectionVectorAttributeLocation() {
        return aDirectionVectorLocation;
    }

    public int getParticleStartTimeAttributeLocation() {
        return aParticleStartTimeLocation;
    }
}
