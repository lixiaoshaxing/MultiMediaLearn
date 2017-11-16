package com.lx.multimedialearn.openglstudy.animation.watermark;

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
 * 渲染相机+水印
 * 先画相机预览界面
 * 再画一张水印图片，处理好位置关系
 *
 * @author lixiao
 * @since 2017-11-16 13:04
 */
public class CameraWaterMarkRender implements GLSurfaceView.Renderer {
    private Context mContext;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureID;
    private int mWidth;
    private int mHeight;
    private int mBitmapWidth; //水印图片的宽高，用来保证图片不变形
    private int mBitmapHeight;

    /**
     * 使用SurfaceTexture承载相机预览数据，绑定textureID，使用GLSurfaceView画texture，进行预览
     *
     * @param context
     * @param surfaceTexture 承载预览界面
     * @param textureID      构造surfaceTexture的textureid
     */
    public CameraWaterMarkRender(Context context, SurfaceTexture surfaceTexture, int textureID) {
        this.mContext = context;
        this.mSurfaceTexture = surfaceTexture;
        this.mTextureID = textureID;
    }

    /*****************画相机的数据************************/
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

    /*****************画水印的数据***********************/
    private int mWaterMarkTextureID;
    private int mWaterMarkProgram;
    private int mWarterMarkPositionHandler;
    private int mWarterMarkTextureCoordHandler;
    private int mWarterMarkMVPMatrixHandler;
    private FloatBuffer mWMVertexBuffer;
    private FloatBuffer mWMTextureCoordBuffer;
    private float mWMVertices[] = {
            -1.0f, -1.0f, //左下
            1.0f, -1.0f, //右下
            -1.0f, 1.0f, //左上
            1.0f, 1.0f, //右上
    };
    private float mWMTextureCoords[] = {
            0.0f, 0.9f, //左下
            1.0f, 0.9f, //右下
            0.0f, 0.0f, //左上
            1.0f, 0.0f, //右上
    };
    private float[] mWMMVP = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        /*****************画相机的数据************************/
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
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
        /*****************画水印的数据***********************/
        int[] temp = GlUtil.loadTexture(mContext, R.drawable.logo); //可以获取textureid，宽高
        mWaterMarkTextureID = temp[0];
        mBitmapWidth = temp[1];
        mBitmapHeight = temp[2];

        String wmVertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.logo_vertex_shader);
        String wmFragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.logo_fragment_shader);
        mWaterMarkProgram = GlUtil.createProgram(wmVertexShader, wmFragmentShader);
        mWarterMarkPositionHandler = GLES20.glGetAttribLocation(mWaterMarkProgram, "aPosition");
        mWarterMarkTextureCoordHandler = GLES20.glGetAttribLocation(mWaterMarkProgram, "aCoord");
        mWarterMarkMVPMatrixHandler = GLES20.glGetUniformLocation(mWaterMarkProgram, "uMatrix");

        mWMVertexBuffer = GlUtil.createFloatBuffer(mWMVertices);
        mWMTextureCoordBuffer = GlUtil.createFloatBuffer(mWMTextureCoords);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //gl.glViewport(0, 0, width, height);
        /*****************画相机，水印的数据，这两个移到OnDrawFrame中，画相机和画水印时单独进行窗口，矩阵的变化************************/
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        /******************画相机(预览界面有点变形，变换矩阵不正确)**********************/
        GLES20.glViewport(0, 0, mWidth, mHeight);
        mSurfaceTexture.updateTexImage(); //拿到最新的数据
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //绘制预览数据
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mTextureCoordsBuffer);
        //进行图形的转换
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVP, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);

        /*****************画水印的数据***********************/
        GLES20.glUseProgram(mWaterMarkProgram);
        GLES20.glViewport(mWidth - mBitmapWidth * 2, 20, mBitmapWidth * 2, mBitmapHeight * 2);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        //放位置，矩阵变换
        GLES20.glUniformMatrix4fv(mWarterMarkMVPMatrixHandler, 1, false, mWMMVP, 0);
        GLES20.glEnableVertexAttribArray(mWarterMarkPositionHandler);
        GLES20.glVertexAttribPointer(mWarterMarkPositionHandler, 2, GLES20.GL_FLOAT, false, 0, mWMVertexBuffer);
        GLES20.glEnableVertexAttribArray(mWarterMarkTextureCoordHandler);
        GLES20.glVertexAttribPointer(mWarterMarkTextureCoordHandler, 2, GLES20.GL_FLOAT, false, 0, mWMTextureCoordBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mWaterMarkTextureID); //把纹理绑定到纹理单元0上
        GLES20.glUniform1i(mWarterMarkTextureCoordHandler, 0); //把纹理单元0传给片元着色器进行渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0, 0, mWidth, mHeight);
    }
}
