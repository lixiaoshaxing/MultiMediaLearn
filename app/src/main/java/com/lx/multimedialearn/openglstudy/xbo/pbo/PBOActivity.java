package com.lx.multimedialearn.openglstudy.xbo.pbo;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ToastUtils;

/**
 * PBO 通过在显存中开辟缓冲空间，减少占用CPU进行像素读取时间
 * 1. 加载纹理到PBO，PBO送到OpenGL处理
 * 2. OpenGL发送图片到PBO，PBO映射到内存，进行展示
 * 3. PBO映射过程，GPU是阻塞的，所以使用两个PBO轮流服务，同步进行读取和处理，该过程在录制视频，直播推流中使用
 */
public class PBOActivity extends AppCompatActivity {

    private GLSurfaceView mGLSurfaceView;
    private ImageView mImgView;
    private PBORender mRender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pbo);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_pbo_player);
     //   mImgView = (ImageView) findViewById(R.id.img_pbo_show);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            ToastUtils.show(this, "不支持OpenGL 2.0");
            finish();
            return;
        }
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRender = new PBORender(this);
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mRender.setListener(new PBORender.onBitmapUpdateListener() {
            @Override
            public void update(final Bitmap bitmap) {
                runOnUiThread(new Runnable() { //切回主线程
                    @Override
                    public void run() {
                        if (bitmap != null) { //为什么Bitmap必须是final，

                        }
                    }
                });
            }
        });
    }
}
