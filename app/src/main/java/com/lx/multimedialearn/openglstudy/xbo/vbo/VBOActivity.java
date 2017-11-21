package com.lx.multimedialearn.openglstudy.xbo.vbo;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * VBO使用
 * 不采用VBO：把顶点，索引，法线等数据存储在内存，GPU渲染时需要CPU控制，从CPU主存中读取，频繁传递数据，效率低
 * 使用VBO：在GPU中申请显存空间，缓存顶点，索引，法线等数据，不需要传递频繁传递数据
 */
public class VBOActivity extends AppCompatActivity {

    GLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vbo);

    }
}
