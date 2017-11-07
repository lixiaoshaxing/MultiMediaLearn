package com.lx.multimedialearn.openglstudy.animation.loadetc;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 使用GLSurfaceview加载ETC压缩的png文件，渲染为动画
 * 1. 加载png，解析为bitmap，一帧一帧播放出来会很慢
 * 2. 使用etc（arm mail gpu），直接渲染一帧一帧，会很快，但是etc相对占空间，所以使用zip进行压缩
 * 3. 知识点：使用zip进行解析，生成纹理，etc格式，渲染etc的方法（和普通png一样）
 */
public class LoadETCActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_etc);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_etc_show);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(new ETCRender(this));
    }
}
