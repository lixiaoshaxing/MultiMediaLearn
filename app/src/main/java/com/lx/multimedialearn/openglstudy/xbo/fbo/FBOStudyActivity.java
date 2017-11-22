package com.lx.multimedialearn.openglstudy.xbo.fbo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.BitmapUtils;

import java.nio.IntBuffer;

/**
 * 1. 创建EGL上下文
 * 2. 创建EGLSurfaceView渲染数据
 */
public class FBOStudyActivity extends AppCompatActivity {
    private GLRender mRender;
    private SurfaceView mSurfaceView;
    private ImageView mImageView;
    private Handler mHandler;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fbostudy);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_fbostudy);
        mImageView = (ImageView) findViewById(R.id.img_fbostudy);
        mRender = new GLRender(this);
        mRender.start(); //开启线程，创建上下文，准备绘制
        mHandler = new Handler(mRender.getLooper()); //需要mRender处理，则追加到其消息队列里
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() { //使用SurfaceView进行展示
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, final int width, final int height) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRender.render(holder.getSurface(), width, height);
                    }
                });
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        mRender.setmCallback(new GLRender.Callback() { //离屏渲染完成后的回调
            @Override
            public void onBitmapGenerated(final IntBuffer data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap temp = BitmapFactory.decodeResource(FBOStudyActivity.this.getResources(), R.drawable.q);
                        Bitmap result = BitmapUtils.frame2Bitmap(temp.getWidth(), temp.getHeight(), data);
                        mImageView.setImageBitmap(result);
                    }
                });
            }
        });
        mHandler.post(new Runnable() { //需要追加到消息队列里
            @Override
            public void run() {
                mRender.render(1128, 1080); //图片的位置，大小都需要优化
            }
        });
    }

    @Override
    protected void onDestroy() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mRender.release();
                mRender = null;
            }
        });
        super.onDestroy();
    }
}
