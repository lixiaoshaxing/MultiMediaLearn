package com.lx.multimedialearn.openglstudy.xbo.pbo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 使用PBO
 * 1. 初始化PBO
 * 2. 加载纹理到PBO
 * 3. PBO传送给OpenGL，OpenGL处理（渲染到屏幕上）
 * 4. OpenGL传送数据给PBO
 * 5. PBO映射到内存
 * 6. 内存加载bitmap，展示
 *
 * @author lixiao
 * @since 2017-11-22 10:58
 */
public class PBORender implements GLSurfaceView.Renderer {
    private Context mContext;
    private int mWidth;
    private int mheight;

    public PBORender(Context context) {
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
    private int mPBOBufferID;
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

        int[] buffers = new int[1];
        GLES20.glGenBuffers(buffers.length, buffers, 0); //生成bufferid
        mVBOVertexBuffer = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVBOVertexBuffer);
        mVertexBuffer = GlUtil.createFloatBuffer(mVertices); //把数据存储到本地（java空间访问不到）
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertexBuffer.capacity() * GlUtil.SIZEOF_FLOAT, mVertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        mCoordBuffer = GlUtil.createFloatBuffer(mTextureCoords);

        //加载一张图片，创建纹理，加载图片，返回id
        // int[] temp = GlUtil.createImageTexture(mContext, R.drawable.p); //传统的使用texImage2D加载，占用cpu时间
        int[] textureID = new int[1];
        GLES20.glGenTextures(1, textureID, 0);
        mTextureID = textureID[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        int[] pboBuffer = new int[1];
        GLES20.glGenBuffers(1, pboBuffer, 0); //创建pbo，申请pbo空间
        mPBOBufferID = pboBuffer[0];
        GLES20.glBindBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER, mPBOBufferID);//绑定后，加载的纹理都会加载到pbo中
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.p);
        int size = bitmap.getHeight() * bitmap.getWidth() * 4;
        mWidth = bitmap.getWidth();
        mheight = bitmap.getHeight();
        GLES20.glBufferData( //根据图片需要大小，在gpu上申请空间
                GLES30.GL_PIXEL_UNPACK_BUFFER,  //用来向OpenGL上传
                size, //空间大小
                null,  //这里设置为null，初始化为空
                GLES30.GL_STREAM_DRAW); //表示当前要向texture写内容

        ByteBuffer buffer = (ByteBuffer) GLES30.glMapBufferRange(
                GLES30.GL_PIXEL_UNPACK_BUFFER,
                0,
                size,
                GLES30.GL_MAP_WRITE_BIT); //可以向缓冲区中存内容
        //获取空间在内存上映射的地址
        bitmap.copyPixelsToBuffer(buffer); //加载原始图片到pbo
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_UNPACK_BUFFER); //解除绑定pbo，清空pbo中的数据

        GLES30.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, bitmap.getWidth(), bitmap.getHeight(), 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null); //加载纹理到texture上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height); //图像要放的位置

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        //设置属性值
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(mMatrixLocation, 1, false, mMatrix, 0);
        GLES20.glEnableVertexAttribArray(mVertexLocation);
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
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4); //数据处理完后，使用readPiex

        //创建pbo，绑定pbo，读取数据到pbo，映射到内存，展示到ImageView上
        int[] pboBuffer = new int[1];
        GLES30.glGenBuffers(1, pboBuffer, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, pboBuffer[0]);
        //初始化宽高，考虑字节对齐
        //OpenGLES默认应该是4字节对齐应，但是不知道为什么在索尼Z2上效率反而降低
        //并且跟ImageReader最终计算出来的rowStride也和我这样计算出来的不一样，这里怀疑跟硬件和分辨率有关
        //这里默认取得128的倍数，这样效率反而高，为什么？
        int align = 128;//128字节对齐
        int mPixelStride = 4;
        int mRowStride = (mWidth * mPixelStride + (align - 1)) & ~(align - 1); //字节对齐

        int mPboSize = mRowStride * mheight;

        GLES30.glBufferData(
                GLES30.GL_PIXEL_PACK_BUFFER,
                mPboSize,
                null,
                GLES30.GL_STATIC_READ); //从OpenGL中读数据到PBO

//        //int x,int y,int width,int height,int format,int type,int offset
        GLES20.glReadPixels(0, 0, mRowStride, mheight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);//这里是错的
        //映射
        ByteBuffer bf = (ByteBuffer) GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER, 0, mPboSize, GLES30.GL_MAP_READ_BIT);
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
        Bitmap bitmap = Bitmap.createBitmap(mRowStride / mPixelStride, mheight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(bf);
        if (mListener != null) {
            mListener.update(bitmap);
        }
    }

    private onBitmapUpdateListener mListener;

    public void setListener(onBitmapUpdateListener listener) {
        this.mListener = listener;
    }

    /**
     * 图片生成后回调
     */
    public interface onBitmapUpdateListener {
        void update(Bitmap bitmap);
    }
}
