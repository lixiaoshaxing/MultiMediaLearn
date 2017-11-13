package com.lx.multimedialearn.openglstudy.animation.cameraetc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.openglstudy.animation.loadetc.ZipPkmReader;
import com.lx.multimedialearn.usecamera.render.CameraRender;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.MatrixUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 在相机预览界面展示动画
 * 首先以camera为背景，画camera预览图
 * 画动画，保证动画在camera上
 *
 * @author lixiao
 * @since 2017-11-09 21:52
 */
public class AnimOnCameraRender implements GLSurfaceView.Renderer {
    private Context mContext;
    private GLSurfaceView mGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureID; //cameraID
    private long time;
    private long timeStep = 50;
    private ZipPkmReader mPkmReader;

    public AnimOnCameraRender(Context context, SurfaceTexture surfaceTexture, GLSurfaceView surfaceView, int textureID) {
        this.mContext = context;
        this.mGLSurfaceView = surfaceView;
        this.mSurfaceTexture = surfaceTexture;
        this.mTextureID = textureID;
        mPkmReader = new ZipPkmReader(context.getResources().getAssets());
        mPkmReader.setZipPath("assets/etc/cc.zip");
        mPkmReader.open();
    }

    /**
     * 关闭PKM流，重新读取，重新播放
     */
    public void replay() {
        mPkmReader.close();
        mPkmReader.open();
        mGLSurfaceView.requestRender();
    }

    /*****************画相机预览**********************/
    private FloatBuffer mVertexBuffer; // 顶点缓存
    private FloatBuffer mTextureCoordsBuffer; // 纹理坐标映射缓存
    private ShortBuffer drawListBuffer; // 绘制顺序缓存
    private int mProgram; // OpenGL 可执行程序
    private int mPositionHandle;
    private int mTextureCoordHandle;
    private int mMVPMatrixHandle;

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
            1.0f, 1.0f,
            0.0f, 1.0f,
    };
    private float[] mMVP = {
            -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };


    /*****************画动画使用*******************/
    private int mAnimProgram;
    private int[] texture;
    private int mGlHAlpha;
    private int mHPosition;
    private int mHCoord;
    private int mHMatrix;
    private int mHTexture;
    private int width;
    private int height;
    private ByteBuffer emptyBuffer;
    private float[] SM = MatrixUtils.getOriginalMatrix();
    public static final float[] OM = MatrixUtils.getOriginalMatrix();
    private int type = MatrixUtils.TYPE_CENTERINSIDE;

    /**
     * 顶点坐标Buffer
     */
    protected FloatBuffer mVerBuffer;

    /**
     * 纹理坐标Buffer
     */
    protected FloatBuffer mTexBuffer;

    //顶点坐标
    private float pos[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };

    //纹理坐标
    private float[] coord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        /*******画相机************/
        String vertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.camera_vertex_shader);
        String fragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.camera_fragment_shader);
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        //(2)获取gl程序中参数，进行赋值
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //(3)初始化显示的顶点等坐标，在这些坐标范围内显示相机预览数据?
        mVertexBuffer = GlUtil.createFloatBuffer(mVertices);
        mTextureCoordsBuffer = GlUtil.createFloatBuffer(mTextureCoords);
        drawListBuffer = GlUtil.createShortBuffer(drawOrder);

        /******画动画*************/
        String vertexString = FileUtils.readTextFileFromResource(mContext, R.raw.etc_camera_vertex_shader);
        String fragmentString = FileUtils.readTextFileFromResource(mContext, R.raw.etc_camera_fragment_shader);
        mAnimProgram = GlUtil.createProgram(vertexString, fragmentString);
        texture = new int[2];
        createEtcTexture(texture);
        mGlHAlpha = GLES20.glGetUniformLocation(mAnimProgram, "vTextureAlpha");
        mHPosition = GLES20.glGetAttribLocation(mAnimProgram, "vPosition");
        mHCoord = GLES20.glGetAttribLocation(mAnimProgram, "vCoord");
        mHMatrix = GLES20.glGetUniformLocation(mAnimProgram, "vMatrix");
        mHTexture = GLES20.glGetUniformLocation(mAnimProgram, "vTexture");

        mVerBuffer = GlUtil.createFloatBuffer(pos);
        mTexBuffer = GlUtil.createFloatBuffer(coord);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        emptyBuffer = ByteBuffer.allocateDirect(ETC1.getEncodedDataSize(width, height));
        this.width = width;
        this.height = height;
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        /******************画相机**********************/
        synchronized (CameraRender.class) {
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); //清理屏幕,设置屏幕为白板
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
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
            mSurfaceTexture.detachFromGLContext();
        }

        /****************画动画***********************/
        time = System.currentTimeMillis();
        GLES20.glUseProgram(mAnimProgram);
        ETC1Util.ETC1Texture t = mPkmReader.getNextTexture();
        ETC1Util.ETC1Texture tAlpha = mPkmReader.getNextTexture();
        if (t != null && tAlpha != null) {
            MatrixUtils.getMatrix(SM, type, t.getWidth(), t.getHeight(), width, height);
            GLES20.glUniformMatrix4fv(mHMatrix, 1, false, SM, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                    .GL_UNSIGNED_SHORT_5_6_5, t);
            GLES20.glUniform1i(mHTexture, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[1]);
            ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                    .GL_UNSIGNED_SHORT_5_6_5, tAlpha);
            GLES20.glUniform1i(mGlHAlpha, 1);
        } else {
            GLES20.glUniformMatrix4fv(mHMatrix, 1, false, SM, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                    .GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(width, height, emptyBuffer));
            GLES20.glUniform1i(mHTexture, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[1]);
            ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0, GLES20.GL_RGB, GLES20
                    .GL_UNSIGNED_SHORT_5_6_5, new ETC1Util.ETC1Texture(width, height, emptyBuffer));
            GLES20.glUniform1i(mGlHAlpha, 1);
        }
        GLES20.glEnableVertexAttribArray(mHPosition);
        GLES20.glVertexAttribPointer(mHPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(mHCoord);
        GLES20.glVertexAttribPointer(mHCoord, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mHPosition);
        GLES20.glDisableVertexAttribArray(mHCoord);
        long last = System.currentTimeMillis() - time;
        if (last < timeStep) {
            try {
                Thread.sleep(timeStep - last);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mGLSurfaceView.requestRender();
    }

    /**
     * 生成两个纹理
     *
     * @param texture
     */
    private void createEtcTexture(int[] texture) {
        //生成纹理
        GLES20.glGenTextures(2, texture, 0);
        for (int i = 0; i < texture.length; i++) {
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[i]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
        }
    }
}
