package com.lx.multimedialearn.openglstudy.obj;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

/**
 * 加载3D模型
 * 参考blog：http://blog.csdn.net/junzia/article/details/54300202
 */
public class ObjModelActivity extends AppCompatActivity {

    private GLSurfaceView mGlSurface;
    private ObjRender mRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obj_model);
        mGlSurface = (GLSurfaceView) findViewById(R.id.glsurface_obj_model_player);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持opengl 2.0");
            return;
        }
        mRender = new ObjRender(this);
        mGlSurface.setEGLContextClientVersion(2);
        mGlSurface.setRenderer(mRender);
        mGlSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGlSurface.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGlSurface.onPause();
    }
}
