package com.lx.multimedialearn.openglstudy.image.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.text.TextUtils;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 使用OpenGL进行图像处理
 * 1. 先画出原图
 *
 * @author lixiao
 * @since 2017-10-25 11:12
 */
public class ImageProcessRender implements GLSurfaceView.Renderer {
    private Context mContext;
    private int mProgram;

    public ImageProcessRender(Context context) {
        this.mContext = context;
    }

    //1. 编写VertexShader和fragmentShader
    //2. 加载图片，绑定纹理，获取着色器中的值
    //3. 赋值：顶点值
    //4. 画图
    private int aPositionLocation;
    private int uMatrixLocation;
    private int aCoordinateLocation;
    private int uTextureUnitLocation;
    private int uIsHalfLocation; //是否处理一半
    private int uTypeLocation; //要变色的类型
    private int uChangeColorLocation; //颜色变化的模板颜色值，把这个值传进去计算出每个点最终颜色值
    private int uXYLocation; //
    private int type;
    private int isHalf = 0;
    private float[] changeColor = {0.0f, 0.0f, 0.0f};
    private float[] postion = new float[]{
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f
    };

    private float[] coordinate = new float[]{
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    private float uXY = 0.0f;

    private FloatBuffer positionBuffer = GlUtil.createFloatBuffer(postion);
    private FloatBuffer coordinateBuffer = GlUtil.createFloatBuffer(coordinate);
    private int[] texture; //存放纹理的相关信息，纹理id，宽，高
    private String mPath;

    /**
     * 设置要变换的类型
     */
    public void setInfo(int type, float[] changeColor) {
        this.type = type;
        this.changeColor = changeColor;
    }

    public void setIsHalf(boolean isHalf) {
        if (isHalf) {
            this.isHalf = 1;
        } else {
            this.isHalf = 0;
        }
    }

    /**
     * 设置加载图片的地址
     *
     * @param path
     */
    public void setImagePath(String path) {
        this.mPath = path;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
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

    }

    private float[] mProjectMatrix = new float[16]; //存储投影变换
    private float[] mViewMatrix = new float[16]; //存储相机位置
    private float[] mMvpMatrix = new float[16]; //存储最终的变化矩阵


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
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
//        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0,
                0f, 0f, 7.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0f
        );
        Matrix.multiplyMM(mMvpMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mMvpMatrix, 0);
        GLES20.glUniform1i(uIsHalfLocation, isHalf);
        GLES20.glUniform1i(uTypeLocation, type);
        GLES20.glUniform1f(uXYLocation, uXY);
        GLES20.glUniform3fv(uChangeColorLocation, 1, changeColor, 0);

        //加载图片纹理
        if (!TextUtils.isEmpty(mPath)) {
            texture = GlUtil.loadTexture(mContext, mPath);
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]); //把纹理绑定到纹理单元0上
        GLES20.glUniform1i(uTextureUnitLocation, 0); //把纹理单元0传给片元着色器进行渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
