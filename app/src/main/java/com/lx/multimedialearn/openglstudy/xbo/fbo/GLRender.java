package com.lx.multimedialearn.openglstudy.xbo.fbo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.view.Surface;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;


/**
 * 离屏渲染
 * 1. 使用HandlerThread，构建线程
 * 2. 创建OpenGL上下文
 * （1）获取显示设备
 * （2）初始化egl
 * （3）根据配置创建上下文
 * （4）使用glsl渲染，和GLSurfaceView的render中一样
 * （5）销毁上下文和线程
 * 3. 怎么关联SurfaceView和Render
 *
 * @author lixiao
 * @since 2017-11-05 13:24
 */
public class GLRender extends HandlerThread {
    private Context mContext;

    private EGLConfig eglConfig = null;
    private EGLDisplay eglDisplay = null;
    private EGLContext eglContext1 = null;
    private EGLContext eglContext2 = null;

    /**
     * 在子线程进行渲染
     *
     * @param context
     */
    public GLRender(Context context) {
        super("GLThread");
        this.mContext = context;
    }

    /**
     * 创建EGL上下文
     */
    private void createGL() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw new RuntimeException("EGL error" + EGL14.eglGetError());
            }
            //使用FrameBuffer进行渲染
            int[] configAttribs = { //配置参数，初始化FrameBuffer
                    EGL14.EGL_BUFFER_SIZE, 32,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_NONE
            };
            int[] numConfigs = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, configs.length, numConfigs, 0)) {
                throw new RuntimeException("EGL error" + EGL14.eglGetError());
            } //获取配置
            eglConfig = configs[0];

        } else {
            ToastUtils.show(mContext, "版本太低，暂时不支持");
        }
    }

    /**
     * 销毁上下文
     */
    private void destroyGL() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            EGL14.eglDestroyContext(eglDisplay, eglContext1);
            EGL14.eglDestroyContext(eglDisplay, eglContext2);
            eglContext1 = EGL14.EGL_NO_CONTEXT;
            eglContext2 = EGL14.EGL_NO_CONTEXT;
            eglDisplay = EGL14.EGL_NO_DISPLAY;

        } else {
            ToastUtils.show(mContext, "版本太低，暂时不支持");
        }
    }

    /**
     * HandlerThread开始运行的时候初始化上下文
     */
    @Override
    public synchronized void start() {
        super.start();
        new Handler(getLooper()).post(new Runnable() { //在glRender线程，进行初始化
            @Override
            public void run() {
                createGL();
            }
        });
    }

    /**
     * 释放EGL上下文
     */
    public void release() {
        destroyGL();
        quit();
    }

    /**
     * 使用初始化的EGL上下文，渲染一幅图片
     * egl创建eglSurface
     */
    public void render(Surface surface, int width, int height) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int[] contextAttribs = { //每个绘画过程，都要根据设备信息，配置信息创建自己的上下文
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            eglContext2 = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            if (eglContext2 == EGL14.EGL_NO_CONTEXT) {
                throw new RuntimeException("EGL error" + EGL14.eglGetError());
            }
            final int[] surfaceAttribs = {EGL14.EGL_NONE};
            EGLSurface eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, surfaceAttribs, 0);
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext2);
            //真正的渲染过程
            drawBmp(width, height);

            EGL14.eglSwapBuffers(eglDisplay, eglSurface); //显存交换，surface显存和显示器显存进行交换
            EGL14.eglDestroySurface(eglDisplay, eglSurface);
        } else {
            ToastUtils.show(mContext, "版本太低，暂时不支持");
        }
    }

    private Bitmap mBitmap;
    private IntBuffer mBuffer;
    private Callback mCallback;
    private int[] fFrame = new int[1];
    private int[] fRender = new int[1];
    private int[] fTexture = new int[2];

    public void setmCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    /**
     * 在后台的frameBuffer上渲染
     * 没有与屏幕进行交换，不能渲染
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void render(int viewWidth, int viewHeight) {
        int[] contextAttribs = { //创建上下文
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        eglContext1 = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
        if (eglContext1 == EGL14.EGL_NO_CONTEXT) {
            throw new RuntimeException("EGL error" + EGL14.eglGetError());
        }
        final int[] attributes = {
                EGL14.EGL_WIDTH, viewWidth,
                EGL14.EGL_HEIGHT, viewHeight,
                EGL14.EGL_NONE};
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, attributes, 0); //使用FrameBuffer创建EGLSurface
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext1);

        /************下边还是画一张图片的基本代码*****************/
        mBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.q);
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        //创建EGL上下文
        int mProgram;
        int aPositionLocation;
        int uMatrixLocation;
        int aCoordinateLocation;
        int uTextureUnitLocation;
        int uIsHalfLocation; //是否处理一半
        int uTypeLocation; //要变色的类型
        int uChangeColorLocation; //颜色变化的模板颜色值，把这个值传进去计算出每个点最终颜色值
        int uXYLocation;
        int type = 1;
        int isHalf = 0;
        float[] postion = new float[]{
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, 1.0f,
                1.0f, -1.0f
        };

        float[] coordinate = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };
        float uXY = 0.0f;
        float[] mProjectMatrix = new float[16]; //存储投影变换
        float[] mViewMatrix = new float[16]; //存储相机位置
        float[] mMvpMatrix = new float[16]; //存储最终的变化矩阵
        float[] changeColor = {0.299f, 0.587f, 0.114f};

        FloatBuffer positionBuffer = GlUtil.createFloatBuffer(postion);
        FloatBuffer coordinateBuffer = GlUtil.createFloatBuffer(coordinate);
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f); //设置底色，onDrawFrame会恢复这种颜色
        String vertexString = FileUtils.readTextFileFromResource(mContext, R.raw.bmp_vertex_shader);
        String fragmentString = FileUtils.readTextFileFromResource(mContext, R.raw.bmp_fragment_shader);
        if (TextUtils.isEmpty(vertexString) || TextUtils.isEmpty(fragmentString)) {
            ToastUtils.show(mContext, "渲染器加载失败");
            return;
        }
        mProgram = GlUtil.createProgram(vertexString, fragmentString);
        aPositionLocation = GLES20.glGetAttribLocation(mProgram, "aPosition");
        uMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        aCoordinateLocation = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
        uTextureUnitLocation = GLES20.glGetUniformLocation(mProgram, "uTextureUnit");
        uTypeLocation = GLES20.glGetUniformLocation(mProgram, "uType");
        uIsHalfLocation = GLES20.glGetUniformLocation(mProgram, "uIsHalf");
        uChangeColorLocation = GLES20.glGetUniformLocation(mProgram, "uChangeColor");
        uXYLocation = GLES20.glGetUniformLocation(mProgram, "uXY");

        GLES20.glVertexAttribPointer(
                aPositionLocation,
                2, //每个顶点需要两个点进行描述
                GLES20.GL_FLOAT,
                false,
                0,
                positionBuffer
        );
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aCoordinateLocation,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                coordinateBuffer
        );
        GLES20.glEnableVertexAttribArray(aCoordinateLocation);
        //对matrix进行赋值
        float bitmapRatio = (float) width / (float) height;
        float screenRatio = (float) viewWidth / (float) viewHeight;
        uXY = screenRatio;
        if (viewWidth > viewHeight) {
            if (bitmapRatio > screenRatio) {
                Matrix.orthoM(mProjectMatrix, 0, -bitmapRatio * screenRatio, bitmapRatio * screenRatio, -1, 1, 3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -screenRatio / bitmapRatio, screenRatio / bitmapRatio, -1, 1, 3, 7);
            }
        } else {
            if (bitmapRatio > screenRatio) {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -bitmapRatio / screenRatio, bitmapRatio / screenRatio, 3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / bitmapRatio * screenRatio, 1 / bitmapRatio * screenRatio, 3, 7);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0,
                0f, 0f, 7.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0f
        );
        Matrix.multiplyMM(mMvpMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);

        GLES20.glViewport(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mMvpMatrix, 0);
        GLES20.glUniform1i(uIsHalfLocation, isHalf);
        GLES20.glUniform1i(uTypeLocation, type);
        GLES20.glUniform1f(uXYLocation, uXY);
        GLES20.glUniform3fv(uChangeColorLocation, 1, changeColor, 0);
        /******************之上都为画图的基本代码**********************/

        createFrameBuffer(); //创建
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]); //FrameBuffer和texture，Render的绑定
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fTexture[1], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[0]); //把纹理绑定到纹理单元0上
        GLES20.glUniform1i(uTextureUnitLocation, 0); //把纹理单元0传给片元着色器进行渲染

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glReadPixels(0, 0, mBitmap.getWidth(), mBitmap.getHeight(), GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, mBuffer); //这里缓冲区中的数据方向和理论上是倒置的，应该处理一下

        if (mCallback != null) {
            mCallback.onBitmapGenerated(mBuffer);
        }
        EGL14.eglSwapBuffers(eglDisplay, eglSurface); //显存交换，surface显存和显示器显存进行交换
        EGL14.eglDestroySurface(eglDisplay, eglSurface);
        deleteEnvi();
        mBitmap.recycle();
    }


    /**
     * 使用FBO：创建Framebuffer，创建TextureBuffer，RenderBuffer，绑定到FrameBuffer，之后渲染都是在Framebuffer上
     * 纹理是在TextureBuffer中，深度检测，模板是在RenderBuffer中，最后在屏幕上把TextureBuffer作为普通的一帧渲染出来
     */
    public void createFrameBuffer() {
        GLES20.glGenFramebuffers(1, fFrame, 0);
        GLES20.glGenRenderbuffers(1, fRender, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fRender[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                mBitmap.getWidth(), mBitmap.getHeight());
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glGenTextures(2, fTexture, 0);
        for (int i = 0; i < 2; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[i]);
            if (i == 0) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmap, 0);
            } else {
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mBitmap.getWidth(), mBitmap.getHeight(),
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            }
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        mBuffer = IntBuffer.allocate(mBitmap.getWidth() * mBitmap.getHeight());
    }

    private void deleteEnvi() {
        GLES20.glDeleteTextures(2, fTexture, 0);
        GLES20.glDeleteRenderbuffers(1, fRender, 0);
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
    }

    /**
     * 图片完成的回调
     */
    interface Callback {
        void onBitmapGenerated(IntBuffer data);
    }


    /**
     * 画bmp，整合图片处理中的render中的三个方法
     * 可以封装为三个回调
     */
    private void drawBmp(int width, int height) {
        int mProgram;
        int aPositionLocation;
        int uMatrixLocation;
        int aCoordinateLocation;
        int uTextureUnitLocation;
        int uIsHalfLocation; //是否处理一半
        int uTypeLocation; //要变色的类型
        int uChangeColorLocation; //颜色变化的模板颜色值，把这个值传进去计算出每个点最终颜色值
        int uXYLocation;
        int type = 1;
        int isHalf = 0;
        float[] postion = new float[]{
                -1.0f, 1.0f,
                -1.0f, -1.0f,
                1.0f, 1.0f,
                1.0f, -1.0f
        };

        float[] coordinate = new float[]{
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };
        float uXY = 0.0f;
        float[] mProjectMatrix = new float[16]; //存储投影变换
        float[] mViewMatrix = new float[16]; //存储相机位置
        float[] mMvpMatrix = new float[16]; //存储最终的变化矩阵
        float[] changeColor = {0.299f, 0.587f, 0.114f};

        FloatBuffer positionBuffer = GlUtil.createFloatBuffer(postion);
        FloatBuffer coordinateBuffer = GlUtil.createFloatBuffer(coordinate);
        int[] texture; //存放纹理的相关信息，纹理id，宽，高
        String mPath;
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f); //设置底色，onDrawFrame会恢复这种颜色
        String vertexString = FileUtils.readTextFileFromResource(mContext, R.raw.bmp_vertex_shader);
        String fragmentString = FileUtils.readTextFileFromResource(mContext, R.raw.bmp_fragment_shader);
        if (TextUtils.isEmpty(vertexString) || TextUtils.isEmpty(fragmentString)) {
            ToastUtils.show(mContext, "渲染器加载失败");
            return;
        }
        mProgram = GlUtil.createProgram(vertexString, fragmentString);
        aPositionLocation = GLES20.glGetAttribLocation(mProgram, "aPosition");
        uMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        aCoordinateLocation = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
        uTextureUnitLocation = GLES20.glGetUniformLocation(mProgram, "uTextureUnit");
        uTypeLocation = GLES20.glGetUniformLocation(mProgram, "uType");
        uIsHalfLocation = GLES20.glGetUniformLocation(mProgram, "uIsHalf");
        uChangeColorLocation = GLES20.glGetUniformLocation(mProgram, "uChangeColor");
        uXYLocation = GLES20.glGetUniformLocation(mProgram, "uXY");

        texture = GlUtil.loadTexture(mContext, R.drawable.p);//加载默认图片

        GLES20.glVertexAttribPointer(
                aPositionLocation,
                2, //每个顶点需要两个点进行描述
                GLES20.GL_FLOAT,
                false,
                0,
                positionBuffer
        );
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aCoordinateLocation,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                coordinateBuffer
        );
        GLES20.glEnableVertexAttribArray(aCoordinateLocation);
        GLES20.glViewport(0, 0, width, height); //这里要进行变换
        //对matrix进行赋值
        int w = texture[1];
        int h = texture[2];
        float bitmapRatio = (float) w / (float) h;
        float screenRatio = (float) width / (float) height;
        uXY = screenRatio;
        if (width > height) {
            if (bitmapRatio > screenRatio) {
                Matrix.orthoM(mProjectMatrix, 0, -bitmapRatio * screenRatio, bitmapRatio * screenRatio, -1, 1, 3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -screenRatio / bitmapRatio, screenRatio / bitmapRatio, -1, 1, 3, 7);
            }
        } else {
            if (bitmapRatio > screenRatio) {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -bitmapRatio / screenRatio, bitmapRatio / screenRatio, 3, 7);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / bitmapRatio * screenRatio, 1 / bitmapRatio * screenRatio, 3, 7);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0,
                0f, 0f, 7.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0f
        );
        Matrix.multiplyMM(mMvpMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mMvpMatrix, 0);
        GLES20.glUniform1i(uIsHalfLocation, isHalf);
        GLES20.glUniform1i(uTypeLocation, type);
        GLES20.glUniform1f(uXYLocation, uXY);
        GLES20.glUniform3fv(uChangeColorLocation, 1, changeColor, 0);

        //加载图片纹理
        texture = GlUtil.loadTexture(mContext, R.drawable.p);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]); //把纹理绑定到纹理单元0上
        GLES20.glUniform1i(uTextureUnitLocation, 0); //把纹理单元0传给片元着色器进行渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
