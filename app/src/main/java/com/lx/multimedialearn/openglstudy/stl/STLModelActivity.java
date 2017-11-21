package com.lx.multimedialearn.openglstudy.stl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.openglstudy.stl.render.ModelRenderer;
import com.lx.multimedialearn.utils.WeakHandler;

/**
 * 加载stl压缩包，解析，后渲染到GLSurfaceView上
 * stl中包括结构+结构对应的纹理
 * 参考blog：http://blog.csdn.net/huachao1001/article/details/51545450
 */
public class STLModelActivity extends AppCompatActivity {
    private GLSurfaceView mGlSurfaceView;
    private SeekBar mSeekBar;
    private ModelRenderer mModelRender;
    private WeakHandler mHandler;
    private float mRotateDegree = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stlmodel);
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_stl_player);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar__stl_scale);
        mModelRender = new ModelRenderer(this);
        mHandler = new WeakHandler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                mModelRender.rotate(mRotateDegree);
                mGlSurfaceView.invalidate();
                return false;
            }
        });

        //定时发送消息，旋转模型
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(100);
                        mHandler.sendEmptyMessage(0x001);
                        mRotateDegree += 5;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        //对模型进行放大缩小
        mSeekBar.setMax(100);
        mSeekBar.setProgress(50);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mModelRender.setScale(1f * progress / 100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mGlSurfaceView.setRenderer(mModelRender);
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
