package com.lx.multimedialearn.openglstudy.xbo.vbo.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 使用vbo
 * 1. 申请存储空间
 * 2. 存储顶点数据
 * 3. 直接从gpu中读取数据
 *
 * @author lixiao
 * @since 2017-11-22 10:58
 */
public class VBORender implements GLSurfaceView.Renderer {
    private Context mContext;

    public VBORender(Context context) {
        this.mContext = context;
    }

    private int mProgram;
    private int mVertexLocation;
    private int mCoordLocation;
    private int mMatrixLocation;
    private int mTextureLocation;
    private int mTextureID;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mCoordBuffer;
    private float[] mVertices = {
            -1.0f, -1.0f, //左下
            1.0f, -1.0f, //右下
            -1.0f, 1.0f, //左上
            1.0f, 1.0f, //右上
    };
    private float[] mTextureCoords = {
            0.0f, 1.0f, //左下
            1.0f, 1.0f, //右下
            0.0f, 0.0f, //左上
            1.0f, 0.0f, //右上
    };
    private float[] mMatrix = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    };

    private int mVBOVertexBuffer; //使用VBO

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        //加载Program，生成纹理，加载图片，查找属性位置
        String vertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.last_vertex_shader);
        String vragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.last_fragment_shader);
        mProgram = GlUtil.createProgram(vertexShader, vragmentShader);
        mVertexLocation = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mCoordLocation = GLES20.glGetAttribLocation(mProgram, "aCoord");
        mMatrixLocation = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        mTextureLocation = GLES20.glGetUniformLocation(mProgram, "uTexture");

        //mVertexBuffer = GlUtil.createFloatBuffer(mVertices); 使用本地内存，下边使用VBO，缓存到GPU中
        int[] buffers = new int[1];
        GLES20.glGenBuffers(buffers.length, buffers, 0); //生成bufferid
        mVBOVertexBuffer = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBOVertexBuffer);
        mVertexBuffer = GlUtil.createFloatBuffer(mVertices); //把数据存储到本地（java空间访问不到）
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * GlUtil.SIZEOF_FLOAT, mVertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mCoordBuffer = GlUtil.createFloatBuffer(mTextureCoords);

        //加载一张图片，创建纹理，加载图片，返回id
        int[] temp = GlUtil.createImageTexture(mContext, R.drawable.p);
        mTextureID = temp[0];
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height); //图像要放的位置

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //设置属性值
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(mMatrixLocation, 1, false, mMatrix, 0);
        GLES20.glEnableVertexAttribArray(mVertexLocation);
        //GLES20.glVertexAttribPointer(mVertexLocation, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer); //读取内存，效率低
        //使用vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBOVertexBuffer); //绑定id，表示要使用
        GLES20.glEnableVertexAttribArray(mVertexLocation); //
        GLES20.glVertexAttribPointer(mVertexLocation, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0); //解绑，同理使用索引，drawElements也有对应的索引Buffer

        GLES20.glEnableVertexAttribArray(mCoordLocation);
        GLES20.glVertexAttribPointer(mCoordLocation, 2, GLES20.GL_FLOAT, false, 0, mCoordBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); //把活动的纹理单元设置为纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID); //把纹理绑定到纹理单元0上
        GLES20.glUniform1i(mTextureLocation, 0); //把纹理单元0传给片元着色器进行渲染
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    }
}
