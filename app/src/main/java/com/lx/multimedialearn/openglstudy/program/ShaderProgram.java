package com.lx.multimedialearn.openglstudy.program;

import android.content.Context;
import android.opengl.GLES20;

import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;

/**
 * 渲染器执行程序
 *
 * @author lixiao
 * @since 2017-10-12 16:15
 */
public class ShaderProgram {

    protected static final String U_MATRIX = "u_Matrix";
    protected static final String U_TEXTURE_UNIT = "u_TextureUnit";

    protected static final String A_POSITION = "a_Position";
    protected static final String A_COLOR = "a_Color";
    protected static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    protected static final String U_COLOR = "u_Color";

    //粒子系统使用
    protected static final String U_TIME = "u_Time"; //当前时间
    protected static final String A_DIRECTION_VECTOR = "a_DirectionVector"; //开始方向
    protected static final String A_PARTICLE_START_TIME = "a_ParticleStartTime"; //开始时间

    //高度图增加光照
    protected static final String U_VECTOR_TO_LIGHT = "u_VectorToLight";
    protected static final String A_NORMAL = "a_Normal";

    //增加粒子光源，点光
    protected static final String U_MV_MATRIX = "u_MVMatrix";
    protected static final String U_IT_MV_MATRIX = "u_IT_MVMatrix";
    protected static final String U_MVP_MATRIX = "u_MVPMatrix";
    protected static final String U_POINT_LIGHT_POSITIONS = "u_PointLightPositions";
    protected static final String U_POINT_LIGHT_COLORS = "u_PointLightColors";

    protected int program;

    protected ShaderProgram(Context context, int vertexShaderResourceId, int fragmentShaderResourceId) {
        String vertexString = FileUtils.readTextFileFromResource(context, vertexShaderResourceId);
        String fragmentString = FileUtils.readTextFileFromResource(context, fragmentShaderResourceId);
        program = GlUtil.createProgram(vertexString, fragmentString);
    }

    public void useProgram() {
        GLES20.glUseProgram(program);
    }
}
