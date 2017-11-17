package com.lx.multimedialearn.openglstudy.animation.filter1;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 相机预览+滤镜
 * 结合image下使用openGL对图片的处理，进行实时滤镜
 *
 * @author lixiao
 * @since 2017-11-16 16:20
 */
public class Filter1Renderer implements GLSurfaceView.Renderer {
    private SurfaceTexture mSurfaceTexture; //使用共享，在应用层创建，并传进来获取摄像头数据
    private int mTextureID; //预览Camera画面对应的纹理id，通过该id画图
    private Context mContext;
    private int isHalf = 0;
    private int type;
    private float[] changeColor = {0.0f, 0.0f, 0.0f};

    public Filter1Renderer(Context context, SurfaceTexture surfaceTexture, int textureID) {
        this.mSurfaceTexture = surfaceTexture;
        this.mTextureID = textureID;
        this.mContext = context;
    }

    /**
     * 设置要变换的类型
     */
    public void setInfo(int type, float[] changeColor) {
        this.type = type;
        this.changeColor = changeColor;
    }

    public void setIsHalf(boolean isHalf) {
        if (isHalf) {
            this.isHalf = 0;
        } else {
            this.isHalf = 1;
        }
    }

    /***************************画笔所需要的相关参数，整合在这里*************************************/
    //设置opengl的相关程序，以及初始化变量，然后执行，就是画图的全过程
    private FloatBuffer mVertexBuffer; // 顶点缓存
    private FloatBuffer mTextureCoordsBuffer; // 纹理坐标映射缓存
    private ShortBuffer drawListBuffer; // 绘制顺序缓存
    private int mProgram; // OpenGL 可执行程序
    private int mPositionHandle;
    private int mTextureCoordHandle;
    private int mMVPMatrixHandle;
    private int uTypeLocation; //处理类型
    private int uIsHalfLocation; //是否处理一半
    private int uChangeColorLocation; //模板颜色值
    private int uXYLocation; //处理放大使用

    private short drawOrder[] =
            {0, 2, 1, 0, 3, 2}; // 绘制顶点的顺序

    private final int COORDS_PER_VERTEX = 2; // 每个顶点的坐标数
    private final int vertexStride = COORDS_PER_VERTEX * 4; //每个坐标数4 bytes，那么每个顶点占8 bytes
    private float mVertices[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
    };
    private float mTextureCoords[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 0.9f,
            0.0f, 0.9f,
    };
    private float[] mMVP = {
            -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    private float uXY = 0.0f;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //根据TextureID设置画图的初始参数,初始化画图程序，参数
        //(1)根据vertexShader，fragmentShader设置绘图程序
        String vertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.camera_filter_vertex_shader);
        String fragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.camera_filter_fragment_shader);
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        //(2)获取gl程序中参数，进行赋值
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        uTypeLocation = GLES20.glGetUniformLocation(mProgram, "uType");
        uIsHalfLocation = GLES20.glGetUniformLocation(mProgram, "uIsHalf");
        uChangeColorLocation = GLES20.glGetUniformLocation(mProgram, "uChangeColor");
        uXYLocation = GLES20.glGetUniformLocation(mProgram, "uXY");

        //(3)初始化显示的顶点等坐标，在这些坐标范围内显示相机预览数据?
        mVertexBuffer = GlUtil.createFloatBuffer(mVertices);
        mTextureCoordsBuffer = GlUtil.createFloatBuffer(mTextureCoords);
        drawListBuffer = GlUtil.createShortBuffer(drawOrder);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);// GlSurfaceView基本参数设置
        uXY = width / height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); //清理屏幕,设置屏幕为白板
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.attachToGLContext(mTextureID);
        mSurfaceTexture.updateTexImage(); //拿到最新的数据

        //绘制预览数据
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mTextureCoordsBuffer);
        //进行图形的转换
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVP, 0);
        GLES20.glUniform1i(uTypeLocation, type);
        GLES20.glUniform1i(uIsHalfLocation, isHalf);
        GLES20.glUniform3fv(uChangeColorLocation, 1, changeColor, 0);
        GLES20.glUniform1f(uXYLocation, uXY);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
        mSurfaceTexture.detachFromGLContext();
    }
}
