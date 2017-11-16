package com.lx.multimedialearn.openglstudy.obj;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

/**
 * 加载Obj模型
 * obj 表示3d图形的模型
 * obj对应的mtl表示图形的光照，贴图等材料信息
 */
public class ObjMtlActivity extends AppCompatActivity {

    private GLSurfaceView mGlSurfaceView;
    private ObjMtlRender mRender;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obj_mtl);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持2.0");
            return;
        }
        mRender = new ObjMtlRender(this);
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_obj_mtl_player);
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.setRenderer(mRender);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGlSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGlSurfaceView.onPause();
    }
}
