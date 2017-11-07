package com.lx.multimedialearn.openglstudy.animation.loadetc;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 加载PKM渲染动画的渲染器
 *
 * @author lixiao
 * @since 2017-11-06 23:53
 */
public class ETCRender implements GLSurfaceView.Renderer {
    private Context mContext;

    public ETCRender(Context context) {
        this.mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
