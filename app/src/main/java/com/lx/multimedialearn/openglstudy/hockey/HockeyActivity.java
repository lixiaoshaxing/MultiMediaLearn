package com.lx.multimedialearn.openglstudy.hockey;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.lx.multimedialearn.openglstudy.hockey.render.OpenGlStudyRender2;
import com.lx.multimedialearn.openglstudy.hockey.view.CustomGLSurfaceView;
import com.lx.multimedialearn.utils.GlUtil;

/**
 * OpenGL ES书籍的学习：空气曲棍球游戏的开发
 */
public class HockeyActivity extends AppCompatActivity {

    private CustomGLSurfaceView glSurfaceView; //GLSurfaceView做了一些OpenGL的初始化工作，因为新开的Window，所以不能做动画，TextureView需要另外初始化OpenGL，不需要单独开辟窗口，所以可以做动画
    private boolean rendererSet = false; //是否设置渲染器
    private OpenGlStudyRender2 render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new CustomGLSurfaceView(this); //初始化GLSurfaceView
        if (GlUtil.checkGLEsVersion_2(this)) {//检查系统是否支持OpenGL 2.0
            glSurfaceView.setEGLContextClientVersion(2);
            render = new OpenGlStudyRender2(this);
            glSurfaceView.setRenderer(render);
            rendererSet = true;
        } else {
            Toast.makeText(this, "不支持OpenGL 2.0", Toast.LENGTH_SHORT).show();
            finish();
        }
        setContentView(glSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();  //控制GLSurfaceView的生命周期，只有正确的暂停和继续后台渲染进程，申请和释放OpenGL上下文，才能保证程序不崩溃
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }
}
