package com.lx.multimedialearn.openglstudy.animation.cameraetc;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 加载了动画，并结合相机使用
 */
public class AnimOnCameraActivity extends AppCompatActivity {
    private GLSurfaceView mGLSurfaceView;
    private AnimOnCameraRender mRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anim_on_camera);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_anim_camera);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRender = new AnimOnCameraRender(this, mGLSurfaceView);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }


}
