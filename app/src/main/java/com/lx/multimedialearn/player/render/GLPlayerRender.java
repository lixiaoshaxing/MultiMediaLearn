package com.lx.multimedialearn.player.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.lx.multimedialearn.utils.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GlsurfaceView进行视频播放，仍然拿的是SurfaceTexture上的数据，和CameraRender方法一样
 * 和CameraRender不同：（1）不在进行方向的转换 （2）不再进行缩放？
 * 播放视频要处理方向的变化：
 * bolg1:逆流的鱼：http://blog.csdn.net/hejjunlin/article/details/62976457，这篇博客提到了uv坐标系，即贴图使用，不太明白
 * blog2:!!!这篇博客讲解了很多opengl知识，值得学习：从零开始写一个Android平台下的全景视频播放器——2.3 使用GLSurfaceView和MediaPlayer播放一个平面视频：http://blog.csdn.net/Martin20150405/article/details/53319117
 *
 * @author lixiao
 * @since 2017-09-17 15:13
 */
public class GLPlayerRender implements GLSurfaceView.Renderer {
    private SurfaceTexture mSurfaceTexture; //使用共享，在应用层创建，并传进来获取摄像头数据
    private int mTextureID; //预览Camera画面对应的纹理id，通过该id画图

    public GLPlayerRender(SurfaceTexture surfaceTexture, int textureID) {
        this.mSurfaceTexture = surfaceTexture;
        this.mTextureID = textureID;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    }

    /***************************画笔所需要的相关参数，整合在这里*************************************/
    private String vertextShader =
            "uniform mat4 uMVPMatrix;\n" +
                    "attribute vec4 vPosition;\n" +
                    "attribute vec2 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_Position =  vPosition * uMVPMatrix ;\n" +
                    "    textureCoordinate = inputTextureCoordinate;\n" +
                    "}";

    private String fragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform samplerExternalOES s_texture;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
                    "}";


    //设置opengl的相关程序，以及初始化变量，然后执行，就是画图的全过程
    private FloatBuffer vertexBuffer; // 顶点缓存
    private FloatBuffer mTextureCoordsBuffer; // 纹理坐标映射缓存
    private ShortBuffer drawListBuffer; // 绘制顺序缓存
    private int mProgram; // OpenGL 可执行程序
    private int mPositionHandle;
    private int mTextureCoordHandle;
    private int mMVPMatrixHandle;
    public float[] mMVP = new float[16];

    private short drawOrder[] = {0, 2, 1, 0, 3, 2}; // 绘制顶点的顺序


    private final int COORDS_PER_VERTEX = 2; // 每个顶点的坐标数
    private final int vertexStride = COORDS_PER_VERTEX * 4; //每个坐标数4 bytes，那么每个顶点占8 bytes
    private float mVertices[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f
    };
    private float mTextureCoords[] = new float[8];
    private float mTextHeightRatio = 0.1f;

    private float[] mtx = new float[16];

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);// GlSurfaceView基本参数设置
        //根据TextureID设置画图的初始参数,初始化画图程序，参数
        //(1)根据vertexShader，fragmentShader设置绘图程序
        mProgram = GlUtil.createProgram(vertextShader, fragmentShader);
        //(2)获取gl程序中参数，进行赋值
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //(3)初始化显示的顶点等坐标，在这些坐标范围内显示相机预览数据?
        //float[] mRotationMatrix = new float[16];
        //Matrix.setRotateM(mRotationMatrix, 0, 0.9f, 0, 0, -1.0f);
        //float[] result = new float[16];
        //Matrix.multiplyMM(result, 0, mVertices, 0, mRotationMatrix, 0);
        mSurfaceTexture.getTransformMatrix(mtx);

        vertexBuffer = GlUtil.createFloatBuffer(mVertices);
        setTexCoords();
        //(4)设置连接顶点顺序
        drawListBuffer = GlUtil.createShortBuffer(drawOrder);

        mat4f_LoadOrtho(1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, mMVP);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (mSurfaceTexture) {
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            mSurfaceTexture.attachToGLContext(mTextureID);
            mSurfaceTexture.updateTexImage(); //拿到最新的数据

            //绘制预览数据
            GLES20.glUseProgram(mProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            // Prepare the <insert shape here> coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
            GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
            GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mTextureCoordsBuffer);
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mtx, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
            mSurfaceTexture.detachFromGLContext();
        }
    }

    /*******************************初始化Shader程序相关函数*****************************************/
    public void setTexCoords() {
        mTextureCoords[0] = 0;
        mTextureCoords[1] = 1 - mTextHeightRatio;
        mTextureCoords[2] = 1;
        mTextureCoords[3] = 1 - mTextHeightRatio;
        mTextureCoords[4] = 1;
        mTextureCoords[5] = 0 + mTextHeightRatio;
        mTextureCoords[6] = 0;
        mTextureCoords[7] = 0 + mTextHeightRatio;
        mTextureCoordsBuffer = ByteBuffer.allocateDirect(mTextureCoords.length * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer().put(mTextureCoords);
        mTextureCoordsBuffer.position(0);
    }

    public static void mat4f_LoadOrtho(float left, float right, float bottom, float top, float near, float far, float[] mout) {
        float r_l = right - left;
        float t_b = top - bottom;
        float f_n = far - near;
        float tx = -(right + left) / (right - left);
        float ty = -(top + bottom) / (top - bottom);
        float tz = -(far + near) / (far - near);

        mout[0] = 2.0f / r_l;
        mout[1] = 0.0f;
        mout[2] = 0.0f;
        mout[3] = 0.0f;

        mout[4] = 0.0f;
        mout[5] = 2.0f / t_b;
        mout[6] = 0.0f;
        mout[7] = 0.0f;

        mout[8] = 0.0f;
        mout[9] = 0.0f;
        mout[10] = -2.0f / f_n;
        mout[11] = 0.0f;

        mout[12] = tx;
        mout[13] = ty;
        mout[14] = tz;
        mout[15] = 1.0f;
    }
}
