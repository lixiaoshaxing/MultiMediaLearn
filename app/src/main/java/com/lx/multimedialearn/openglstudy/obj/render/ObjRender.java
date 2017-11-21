package com.lx.multimedialearn.openglstudy.obj.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.openglstudy.obj.data.Obj3D;
import com.lx.multimedialearn.openglstudy.obj.utils.ObjModelUtils;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.MatrixUtils;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 加载Obj模型数据进行渲染
 *
 * @author lixiao
 * @since 2017-11-14 22:37
 */
public class ObjRender implements GLSurfaceView.Renderer {
    private Obj3D mObj3D;
    private Context mContext;
    private int mProgram;

    public ObjRender(Context context) {
        mObj3D = new Obj3D();
        this.mContext = context;
        try {
            ObjModelUtils.read(context.getAssets().open("3dres/hat.obj"), mObj3D);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int mVertexLocation;
    private int mMatrixLocation;
    private int mNormalLocation;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        //加载program
        String vertexShader = FileUtils.readTextFileFromResource(mContext, R.raw.obj_vertex_shader);
        String fragmentShader = FileUtils.readTextFileFromResource(mContext, R.raw.obj_fragment_shader);
        mProgram = GlUtil.createProgram(vertexShader, fragmentShader);
        //找变量
        mVertexLocation = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mMatrixLocation = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mNormalLocation = GLES20.glGetAttribLocation(mProgram, "vNormal");
    }

    private float[] matrix;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //初始化矩阵
        GLES20.glViewport(0, 0, width, height);
        matrix = MatrixUtils.getOriginalMatrix();
        Matrix.scaleM(matrix, 0, 0.2f, 0.2f * width / height, 0.2f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //画
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        Matrix.rotateM(matrix, 0, 0.3f, 0, 1, 0);
        GLES20.glUniformMatrix4fv(mMatrixLocation, 1, false, matrix, 0);
        GLES20.glEnableVertexAttribArray(mVertexLocation);
        GLES20.glVertexAttribPointer(mVertexLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, mObj3D.vert);//3个点画出一个三角形
        GLES20.glEnableVertexAttribArray(mNormalLocation); //法线，用来标明光照
        GLES20.glVertexAttribPointer(mNormalLocation, 3, GLES20.GL_FLOAT, false, 3 * 4, mObj3D.vertNorl);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mObj3D.vertCount);
        GLES20.glDisableVertexAttribArray(mNormalLocation);
        GLES20.glDisableVertexAttribArray(mVertexLocation);
    }
}
