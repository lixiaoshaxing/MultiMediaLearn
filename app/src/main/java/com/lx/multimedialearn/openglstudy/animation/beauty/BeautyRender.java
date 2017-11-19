package com.lx.multimedialearn.openglstudy.animation.beauty;

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
 * 美颜render
 * 使用离屏渲染
 *
 * @author lixiao
 * @since 2017-11-18 22:01
 */
public class BeautyRender implements GLSurfaceView.Renderer {
    private SurfaceTexture mSurfaceTexture; //使用共享，在应用层创建，并传进来获取摄像头数据
    private int mTextureID; //预览Camera画面对应的纹理id，通过该id画图
    private Context mContext;
    private int mWidth;
    private int mHeight;

    public BeautyRender(Context context, SurfaceTexture surfaceTexture, int textureID) {
        this.mSurfaceTexture = surfaceTexture;
        this.mTextureID = textureID;
        this.mContext = context;
        setFlag(0);
    }

    /***************渲染相机的数据**********************/
    private FloatBuffer mCameraVertexBuffer; // 顶点缓存
    private FloatBuffer mCameraTextureCoordsBuffer; // 纹理坐标映射缓存
    private ShortBuffer mCameradrawListBuffer; // 绘制顺序缓存
    private int mCameraProgram; // OpenGL 可执行程序
    private int mCameraVertexLocation;
    private int mCameraTextureCoordLocation;
    private int mCameraTextureLocation;
    private int mCameraMatrixLocation;

    private short mCameraDrawOrder[] =
            {0, 2, 1, 0, 3, 2}; // 绘制顶点的顺序

    private final int COORDS_PER_VERTEX = 2; // 每个顶点的坐标数
    private float mCameraVertices[] = {
            -1.0f, 1.0f, //0 左上
            -1.0f, -1.0f, //1 左下
            1.0f, -1.0f, //2 右下
            1.0f, 1.0f, //3 右上
    };
    private float mCameraTextureCoords[] = {
            0.0f, 0.0f, //左上
            1.0f, 0.0f, //左下
            1.0f, 0.9f, //右下
            0.0f, 0.9f, //右上
    };
    private float[] mCameraMatrix = { //镜像，需要旋转
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    /*****************增加美颜的filter***************************/
    private int mBeautyProgram;
    private int mBeautyVertexLocation;
    private int mBeautyTextureCoordLocation;
    private int mBeautyMatrixLocation;
    private int mBeautyTextureLocation;
    private FloatBuffer mBeautyVertexBuffer;
    private FloatBuffer mBeautyCoordBuffer;
    private int mHaaCoef;
    private int mHmixCoef;
    private int mHiternum;
    private int mHWidth;
    private int mHHeight;

    private float aaCoef;
    private float mixCoef;
    private int iternum;

    private float[] mBeautyVertices = {
            -1.0f, -1.0f, //左下
            1.0f, -1.0f, //右下
            -1.0f, 1.0f, //左上
            1.0f, 1.0f, //右上
    };
    private float[] mBeautyTextureCoords = {
            0.0f, 1.0f, //左下
            1.0f, 1.0f, //右下
            0.0f, 0.0f, //左上
            1.0f, 0.0f, //右上
    };
    private float[] mBeautyMatrix = {
            -1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    /**
     * 设置美颜程度
     *
     * @param flag
     */
    public void setFlag(int flag) {
        switch (flag) {
            case 1:
                a(1, 0.19f, 0.54f);
                break;
            case 2:
                a(2, 0.29f, 0.54f);
                break;
            case 3:
                a(3, 0.17f, 0.39f);
                break;
            case 4:
                a(3, 0.25f, 0.54f);
                break;
            case 5:
                a(4, 0.13f, 0.54f);
                break;
            case 6:
                a(4, 0.19f, 0.69f);
                break;
            default:
                a(0, 0f, 0f);
                break;
        }
    }

    private void a(int a, float b, float c) {
        this.iternum = a;
        this.aaCoef = b;
        this.mixCoef = c;
    }

    /*****************画last texture数据************************/
    private int mLastProgram;
    private int mLastVertexLocation;
    private int mLastCoordLocation;
    private int mLastMatrixLocation;
    private int mTextureLocation;
    private FloatBuffer mLastVertexBuffer;
    private FloatBuffer mLastCoordBuffer;
    private float[] mLastVertices = {
            -1.0f, -1.0f, //左下
            1.0f, -1.0f, //右下
            -1.0f, 1.0f, //左上
            1.0f, 1.0f, //右上
    };
    private float[] mLastTextureCoords = {
            0.0f, 1.0f, //左下
            1.0f, 1.0f, //右下
            0.0f, 0.0f, //左上
            1.0f, 0.0f, //右上
    };
    private float[] mLastMatrix = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); //清理屏幕,设置屏幕为白板
        /***************初始化画相机数据*****************/
        //(1)根据vertexShader，fragmentShader设置绘图程序
        String vertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.camera_vertex_shader);
        String fragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.camera_fragment_shader);
        mCameraProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        //(2)获取gl程序中参数，进行赋值
        mCameraVertexLocation = GLES20.glGetAttribLocation(mCameraProgram, "vPosition");
        mCameraTextureCoordLocation = GLES20.glGetAttribLocation(mCameraProgram, "inputTextureCoordinate");
        mCameraMatrixLocation = GLES20.glGetUniformLocation(mCameraProgram, "uMVPMatrix");
        mCameraTextureLocation = GLES20.glGetUniformLocation(mCameraProgram, "s_texture");

        //(3)初始化显示的顶点等坐标，在这些坐标范围内显示相机预览数据?
        mCameraVertexBuffer = GlUtil.createFloatBuffer(mCameraVertices);
        mCameraTextureCoordsBuffer = GlUtil.createFloatBuffer(mCameraTextureCoords);
        mCameradrawListBuffer = GlUtil.createShortBuffer(mCameraDrawOrder);

        /***************初始化Beauty数据****************/
        String beautyVertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.beauty_vertex_shader);
        String beautyFragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.beauty_fragment_shader);
        mBeautyProgram = GlUtil.createProgram(beautyVertexShader, beautyFragmentShader);
        //(2)获取gl程序中参数，进行赋值
        mBeautyVertexLocation = GLES20.glGetAttribLocation(mBeautyProgram, "aPosition");
        mBeautyTextureCoordLocation = GLES20.glGetAttribLocation(mBeautyProgram, "aCoord");
        mBeautyMatrixLocation = GLES20.glGetUniformLocation(mBeautyProgram, "uMatrix");
        mBeautyTextureLocation = GLES20.glGetUniformLocation(mBeautyProgram, "uTexture");
        mHiternum = GLES20.glGetUniformLocation(mBeautyProgram, "uIternum");
        mHaaCoef = GLES20.glGetUniformLocation(mBeautyProgram, "uAaCoef");
        mHmixCoef = GLES20.glGetUniformLocation(mBeautyProgram, "uMixCoef");
        mHWidth = GLES20.glGetUniformLocation(mBeautyProgram, "mWidth");
        mHHeight = GLES20.glGetUniformLocation(mBeautyProgram, "mHeight");

        //(3)初始化显示的顶点等坐标，在这些坐标范围内显示相机预览数据?
        mBeautyVertexBuffer = GlUtil.createFloatBuffer(mBeautyVertices);
        mBeautyCoordBuffer = GlUtil.createFloatBuffer(mBeautyTextureCoords);

        /***************准备last数据*******************/
        String lastVertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.last_vertex_shader);
        String lastFragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.last_fragment_shader);
        mLastProgram = GlUtil.createProgram(lastVertexShader, lastFragmentShader);
        mLastVertexLocation = GLES20.glGetAttribLocation(mLastProgram, "aPosition");
        mLastCoordLocation = GLES20.glGetAttribLocation(mLastProgram, "aCoord");
        mLastMatrixLocation = GLES20.glGetUniformLocation(mLastProgram, "uMatrix");
        mTextureLocation = GLES20.glGetUniformLocation(mLastProgram, "uTexture");

        mLastVertexBuffer = GlUtil.createFloatBuffer(mLastVertices);
        mLastCoordBuffer = GlUtil.createFloatBuffer(mLastTextureCoords);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);// GlSurfaceView基本参数设置
        this.mWidth = width;
        this.mHeight = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        int[] fbo = GlUtil.createFBO(mWidth, mHeight);
        int[] frameBuffer = new int[]{fbo[0]};
        int[] rbo = new int[]{fbo[1]};
        int[] textureColorBuffer = new int[]{fbo[2]};

        /************使用fbo画相机****************/
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        mSurfaceTexture.updateTexImage(); //拿到最新的数据
        //  GLES20.glEnable(GLES20.GL_BLEND);
        //  GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //绘制预览数据
        GLES20.glUseProgram(mCameraProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glUniform1i(mCameraTextureLocation, 0);
        GLES20.glEnableVertexAttribArray(mCameraVertexLocation);
        GLES20.glVertexAttribPointer(mCameraVertexLocation, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mCameraVertexBuffer);
        GLES20.glEnableVertexAttribArray(mCameraTextureCoordLocation);
        GLES20.glVertexAttribPointer(mCameraTextureCoordLocation, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mCameraTextureCoordsBuffer);
        //进行图形的转换
        GLES20.glUniformMatrix4fv(mCameraMatrixLocation, 1, false, mCameraMatrix, 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mCameraDrawOrder.length, GLES20.GL_UNSIGNED_SHORT, mCameradrawListBuffer);
        GLES20.glDisableVertexAttribArray(mCameraVertexLocation);
        GLES20.glDisableVertexAttribArray(mCameraTextureCoordLocation);
        //  GLES20.glDisable(GLES20.GL_BLEND);

        /*************画美颜Filter*****************************/
        int[] beautyFbo = GlUtil.createFBO(mWidth, mHeight);
        int[] beautyFrameBuffer = new int[]{beautyFbo[0]};
        int[] beautyRbo = new int[]{beautyFbo[1]};
        int[] beautyTextureColorBuffer = new int[]{beautyFbo[2]};
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, beautyFrameBuffer[0]);
        GLES20.glUseProgram(mBeautyProgram);
        GLES20.glViewport(0, 0, mWidth, mHeight);
        //   GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        //放位置，矩阵变换
        GLES20.glUniformMatrix4fv(mBeautyMatrixLocation, 1, false, mBeautyMatrix, 0);
        GLES20.glUniform1i(mHWidth, mWidth);
        GLES20.glUniform1i(mHHeight, mHeight);
        GLES20.glUniform1f(mHaaCoef, aaCoef);
        GLES20.glUniform1f(mHmixCoef, mixCoef);
        GLES20.glUniform1i(mHiternum, iternum);
        GLES20.glEnableVertexAttribArray(mBeautyVertexLocation);
        GLES20.glVertexAttribPointer(mBeautyVertexLocation, 2, GLES20.GL_FLOAT, false, 0, mBeautyVertexBuffer);
        GLES20.glEnableVertexAttribArray(mBeautyTextureCoordLocation);
        GLES20.glVertexAttribPointer(mBeautyTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 0, mBeautyCoordBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureColorBuffer[0]); //把相机采集的纹理绑定到纹理单元0上
        GLES20.glUniform1i(mBeautyTextureLocation, 0); //把纹理单元0传给片元着色器进行渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glViewport(0, 0, mWidth, mHeight);

        /*************使用last texture画在屏幕上**********************/
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0); //绑定回默认输出buffer，就是屏幕，然后绘画
        GLES20.glUseProgram(mLastProgram);
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glUniformMatrix4fv(mLastMatrixLocation, 1, false, mLastMatrix, 0);
        GLES20.glEnableVertexAttribArray(mLastVertexLocation);
        GLES20.glVertexAttribPointer(mLastVertexLocation, 2, GLES20.GL_FLOAT, false, 0, mLastVertexBuffer);
        GLES20.glEnableVertexAttribArray(mLastCoordLocation);
        GLES20.glVertexAttribPointer(mLastCoordLocation, 2, GLES20.GL_FLOAT, false, 0, mLastCoordBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, beautyTextureColorBuffer[0]); //把纹理绑定到纹理单元0上
        GLES20.glUniform1i(mTextureLocation, 0); //把纹理单元0传给片元着色器进行渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glViewport(0, 0, mWidth, mHeight);

        GLES20.glDeleteTextures(1, textureColorBuffer, 0); //先删除
        GLES20.glDeleteRenderbuffers(1, rbo, 0);
        GLES20.glDeleteFramebuffers(1, fbo, 0);
        GLES20.glDeleteTextures(1, beautyTextureColorBuffer, 0);
        GLES20.glDeleteRenderbuffers(1, beautyRbo, 0);
        GLES20.glDeleteFramebuffers(1, beautyFbo, 0);
    }
}
