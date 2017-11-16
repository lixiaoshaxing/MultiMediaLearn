package com.lx.multimedialearn.openglstudy.obj;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.MatrixUtils;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 加载带mtl光照，纹理等信息的obj模型
 * 加载obj模型的方法一样，解析obj，加载mtl文件，赋值，画三角形，绑定纹理图片
 *
 * @author lixiao
 * @since 2017-11-15 23:25
 */
public class ObjMtlRender implements GLSurfaceView.Renderer {
    private Obj3D mObj3D;
    private Context mContext;
    private int mProgram;

    public ObjMtlRender(Context context) {
        this.mContext = context;
        mObj3D = new Obj3D();
        try {
            ObjModelUtils.read(context.getAssets().open("3dres/pikachu.obj"), mObj3D);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private int mVertexLocation;
    private int mMatrixLocation;
    private int mNormalLocation;
    private int mHNormal;
    private int mHKa;
    private int mHKd;
    private int mHKs;
    private int textureId;
    private int mHCoord;
    private int mHTexture;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        //加载program
        String vertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.obj_mtl_vertex_shader);
        String fragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.obj_mtl_fragment_shader);
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        //找变量
        mVertexLocation = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mMatrixLocation = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mNormalLocation = GLES20.glGetAttribLocation(mProgram, "vNormal");
        mHNormal = GLES20.glGetAttribLocation(mProgram, "vNormal");
        mHCoord = GLES20.glGetAttribLocation(mProgram, "vCoord");
        mHTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        mHKa = GLES20.glGetUniformLocation(mProgram, "vKa");
        mHKd = GLES20.glGetUniformLocation(mProgram, "vKd");
        mHKs = GLES20.glGetUniformLocation(mProgram, "vKs");
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        if (mObj3D != null && mObj3D.mtl != null) {
            try {
                Log.e("obj", "texture-->" + "3dres/" + mObj3D.mtl.map_Kd);
                textureId = GlUtil.createTexture(BitmapFactory.decodeStream(mContext.getAssets().open("3dres/" + mObj3D.mtl.map_Kd)));
                //setTextureId(textureId);
                GLES20.glUniform3fv(mHKa, 1, mObj3D.mtl.Ka, 0);
                GLES20.glUniform3fv(mHKd, 1, mObj3D.mtl.Kd, 0);
                GLES20.glUniform3fv(mHKs, 1, mObj3D.mtl.Ks, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(mHTexture, 0);

    }

    private float[] matrix;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        matrix = MatrixUtils.getOriginalMatrix();
        Matrix.scaleM(matrix, 0, 0.2f, 0.2f * width / height, 0.2f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        Matrix.rotateM(matrix, 0, 0.3f, 0, 1, 0);
        GLES20.glUniformMatrix4fv(mMatrixLocation, 1, false, matrix, 0);
        GLES20.glEnableVertexAttribArray(mVertexLocation);
        GLES20.glVertexAttribPointer(mVertexLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, mObj3D.vert);//3个点画出一个三角形
        GLES20.glEnableVertexAttribArray(mHNormal);
        GLES20.glVertexAttribPointer(mHNormal, 3, GLES20.GL_FLOAT, false, 0, mObj3D.vertNorl);
        GLES20.glEnableVertexAttribArray(mNormalLocation); //法线，用来标明光照
        GLES20.glVertexAttribPointer(mNormalLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, mObj3D.vertNorl);
        GLES20.glEnableVertexAttribArray(mHCoord);
        GLES20.glVertexAttribPointer(mHCoord, 2, GLES20.GL_FLOAT, false, 0, mObj3D.vertTexture);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj3D.vertCount);
        GLES20.glDisableVertexAttribArray(mNormalLocation);
        GLES20.glDisableVertexAttribArray(mVertexLocation);
        GLES20.glDisableVertexAttribArray(mHCoord);
    }
}
