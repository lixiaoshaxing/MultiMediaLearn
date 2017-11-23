package com.lx.multimedialearn.openglstudy.xbo.vbo;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

/**
 * VBO使用
 * 不采用VBO：把顶点，索引，法线等数据存储在内存，GPU渲染时需要CPU控制，从CPU主存中读取，频繁传递数据，效率低
 * 使用VBO：在GPU中申请显存空间，缓存顶点，索引，法线等数据，不需要传递频繁传递数据
 */
public class VBOActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;
    private VBORender mRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vbo);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_vbo_player);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持OpenGL 2.0");
            finish();
            return;
        }
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRender = new VBORender(this);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
