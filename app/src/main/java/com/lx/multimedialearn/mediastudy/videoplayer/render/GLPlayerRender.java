package com.lx.multimedialearn.mediastudy.videoplayer.render;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Surface;

import com.lx.multimedialearn.utils.GlUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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
 * @since 2017-09-22 00:25
 */
public class GLPlayerRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private int aPositionHandle;
    private int programId;
    private FloatBuffer vertexBuffer;
    private final float[] vertexData = {
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
    };

    private final float[] projectionMatrix = new float[16];
    private int uMatrixHandle;

    private final float[] textureVertexData = {
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
    };
    private FloatBuffer textureVertexBuffer;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int textureId;

    private SurfaceTexture surfaceTexture;
    private MediaPlayer mediaPlayer;
    private float[] mSTMatrix = new float[16];
    private int uSTMMatrixHandle;

    private boolean updateSurface;
    private int screenWidth, screenHeight;


    public GLPlayerRender(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        synchronized (this) {
            updateSurface = false;
        }
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);
    }

    String vertexShader = "attribute vec4 aPosition;\n" +
            "attribute vec4 aTexCoord;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform mat4 uMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "void main() {\n" +
            "    vTexCoord = (uSTMatrix * aTexCoord).xy;\n" +
            "    gl_Position = uMatrix*aPosition;\n" +
            "}";
    String fragmentShader = "#extension GL_OES_EGL_image_external:require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTexCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor=texture2D(sTexture, vTexCoord);\n" +
            "}";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        programId = GlUtil.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");

        uMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);
        mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

            }
        });
        surface.release();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        screenWidth = width;
        screenHeight = height;
        GLES20.glViewport(0, 0, screenWidth, screenHeight);
        float ratio = width > height ?
                (float) width / height :
                (float) height / width;
//        if (width > height) {
//            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f);
//        } else Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -ratio, ratio, -1f, 1f);
        Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -1f, 1f, -1f, 1f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        synchronized (this) {
            if (updateSurface) {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(mSTMatrix);
                updateSurface = false;
            }
        }
        GLES20.glUseProgram(programId);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                12, vertexBuffer);

        textureVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glUniform1i(uTextureSamplerHandle, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

}
