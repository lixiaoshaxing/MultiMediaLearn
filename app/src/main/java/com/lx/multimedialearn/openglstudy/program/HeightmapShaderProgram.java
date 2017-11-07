package com.lx.multimedialearn.openglstudy.program;

import android.content.Context;

import com.lx.multimedialearn.R;

import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform3fv;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;

/**
 * 画高度图
 * 高度图加入光照
 *
 * @author lixiao
 * @since 2017-10-19 22:08
 */
public class HeightmapShaderProgram extends ShaderProgram {
    // private int uMatrixLocation; //增加点光源，用下边进行替换
    private int aPositionLocation;

    //对高度图加入光照
    private int uVectorToLightLocation; //指向方向光来的地方，只能赋一次值，不能在变
    private int aNormalLocation; //每个点都有法线向量

    //增加点光源后，对整体位置进行了替换
    private int uMVMatrixLocation;
    private int uIT_MVMatrixLocation;
    private int uMVPMatrixLocation;
    private int uPointLightPositionsLocation; //这里是数组，注意赋值
    private int uPointLightColorsLocation;

    public HeightmapShaderProgram(Context context) {
        super(context, R.raw.heightmap_vertex_shader, R.raw.heightmap_fragment_shader);
        // uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        uMVMatrixLocation = glGetUniformLocation(program, U_MV_MATRIX);
        uIT_MVMatrixLocation = glGetUniformLocation(program, U_IT_MV_MATRIX);
        uMVPMatrixLocation = glGetUniformLocation(program, U_MVP_MATRIX);

        uPointLightPositionsLocation = glGetUniformLocation(program, U_POINT_LIGHT_POSITIONS);
        uPointLightColorsLocation = glGetUniformLocation(program, U_POINT_LIGHT_COLORS);

        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        uVectorToLightLocation = glGetUniformLocation(program, U_VECTOR_TO_LIGHT);
        aNormalLocation = glGetAttribLocation(program, A_NORMAL);
    }

    /**
     * 设置方向，设置太阳光照来源的方向
     *
     * @param
     */
//    public void setUniforms(float[] matrix, Geometry.Vector vectorToLight) {
//        //glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
//        //glUniform3f(uVectorToLightLocation, vectorToLight.x, vectorToLight.y, vectorToLight.z);
//    }
    public void setUniforms(float[] mvMatrix, float[] it_mvMatrix, float[] mvpMatrix, float[] vectorToDirectionalLight,
                            float[] pointLightPositions, float[] pointLightColors) {
        glUniformMatrix4fv(uMVMatrixLocation, 1, false, mvMatrix, 0);
        glUniformMatrix4fv(uIT_MVMatrixLocation, 1, false, it_mvMatrix, 0);
        glUniformMatrix4fv(uMVPMatrixLocation, 1, false, mvpMatrix, 0);

        glUniform3fv(uVectorToLightLocation, 1, vectorToDirectionalLight, 0);

        glUniform4fv(uPointLightPositionsLocation, 3, pointLightPositions, 0);
        glUniform3fv(uPointLightColorsLocation, 3, pointLightColors, 0);
    }

    public int getPositionAttributeLocation() {
        return aPositionLocation;
    }

    /**
     * 对法线进行赋值
     *
     * @return
     */
    public int getNormalAttributeLocation() {
        return aNormalLocation;
    }
}
