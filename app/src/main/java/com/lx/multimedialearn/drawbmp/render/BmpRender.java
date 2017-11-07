package com.lx.multimedialearn.drawbmp.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.lx.multimedialearn.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 使用GLSurfaceView画图的自定义Render
 * 1. 参考blog：https://blog.piasy.com/2016/06/07/Open-gl-es-android-2-part-1/：画三角形，矩形，bitmap
 *
 * @author lixiao
 * @since 2017-09-06 18:08
 */
public class BmpRender implements GLSurfaceView.Renderer {

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 a_texCoord;" +
                    "varying vec2 v_texCoord;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  v_texCoord = a_texCoord;" +
                    "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "varying vec2 v_texCoord;" +
                    "uniform sampler2D s_texture;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(s_texture, v_texCoord);" +
                    "}";

    //画长方形，四个坐标点, 所有的顶点，顶点链接后形成fragment，fragment通过光栅化映射为屏幕上像素的点
    private static final float[] VERTEX = {  //以屏幕中心为原点，屏幕宽高比不是1，所以需要使用矩阵乘法进行转换
            1, 1, 0,  //top right  //逆时针方向
            -1f, 1, 0, //top left
            -1, -1, 0,   //bottom left
            1, -1, 0, //bottom right

    };

    //画三角形的顺序，画出来两个三角形构建矩形
    private static final short[] VERTEX_INDEX = {0, 1, 2, 0, 2, 3};

    /**
     * 截取纹理的一部分绘制到图形上
     */
    private static final float[] TEX_VERTEX = {
            1, 0,
            0, 0,
            0, 1,    //以左下角为原点，x轴向右，y轴向上
            1, 1,
    };

    private Context mContext;
    private final FloatBuffer mVertexBuffer;
    private final ShortBuffer mVertexIndexBuffer;
    private final FloatBuffer mTexVertexBuffer;

    private final float[] mMVPMatrix = new float[16];

    private int mProgram;
    private int mPositionHandle;
    private int mMatrixHandle;
    private int mTexCoordHandle;
    private int mTexSamplerHandle;

    /**
     * 纹理名字
     */
    private int mTexName;

    /**
     * 对加载的GLSL都有对应的编号，glsurface生命周期内不变
     *
     * @param type
     * @param shaderCode
     * @return
     */
    static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        //加载glsl
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public void destory() {
        GLES20.glDeleteTextures(1, new int[]{mTexName}, 0);
    }

    public BmpRender(Context context) {
        mContext = context;
        mVertexBuffer = ByteBuffer.
                allocateDirect(VERTEX.length * 4).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer().
                put(VERTEX);
        mVertexBuffer.position(0);

        mVertexIndexBuffer = ByteBuffer.allocateDirect(VERTEX_INDEX.length * 4).
                order(ByteOrder.nativeOrder()).
                asShortBuffer().
                put(VERTEX_INDEX);

        mVertexIndexBuffer.position(0);

        mTexVertexBuffer = ByteBuffer.
                allocateDirect(TEX_VERTEX.length * 4).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer().
                put(TEX_VERTEX);
        mTexVertexBuffer.position(0);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        //创建GLSL程序
        mProgram = GLES20.glCreateProgram();
        //加载shader代码
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        //attach shader代码
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        //链接glsl程序
        GLES20.glLinkProgram(mProgram);
        //使用glsl程序
        GLES20.glUseProgram(mProgram);
        //获取shader代码中的变量索引-vPosition
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        //投影： 获取uMVPMatrix值
        mMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_texCoord");
        mTexSamplerHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");

        //启动vertex
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        //绑定vertex坐标值
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);

        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexVertexBuffer);


        //1. 加载图片到opengl纹理系统中
        //我们需要先通过 glGenTextures 创建纹理，再通过 glActiveTexture 激活指定编号的纹理，
        // 再通过 glBindTexture 将新建的纹理和编号绑定起来。我们可以对图片纹理设置一系列参数，
        // 例如裁剪策略、缩放策略，这部分更详细的介绍，建议看看《OpenGL ES 2 for Android A Quick - Start Guide (2013)》
        // 这本书，里面有很详细的讲解。最后，我们通过 texImage2D 把图片数据拷贝到纹理中
        int[] texNames = new int[1];
        GLES20.glGenTextures(1, texNames, 0);
        mTexName = texNames[0];
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.p);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexName);
        //设置加载2d图像的剪裁等参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();
    }

    int mWidth;
    int mHeight;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;
        //设en space的大小
        GLES20.glViewport(0, 0, width, height);
        //计算uMvpMatrix的值，这里几个参数的值，以及如何保证图片完整显示出来，有点问题。。。。
        Matrix.perspectiveM(mMVPMatrix, 0, 13, (float) width / height, 0.1f, 100f);
        //坐标系重新变化
        Matrix.translateM(mMVPMatrix, 0, 0f, 0f, -5f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1i(mTexSamplerHandle, 0);

        // 用 glDrawElements 来绘制，mVertexIndexBuffer 指定了顶点绘制顺序，这时候图片在显存中
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, VERTEX_INDEX.length, GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer);

        //读取显存中的图片，保存到本地，图片由于坐标系的问题，是反转的，绕x轴进行反转
        //DrawBmpUtils.saveImage(mWidth, mHeight);

    }
}

