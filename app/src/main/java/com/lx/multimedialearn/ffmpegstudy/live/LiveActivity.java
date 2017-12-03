package com.lx.multimedialearn.ffmpegstudy.live;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.ffmpegstudy.live.pusher.LivePusher;

/**
 * 直播
 * 1. camera-yuv->x264(ffmpeg)->rtmp推流
 * 2. mic-pcm->aac(faac)->rtmp推流，结合推出去为flv
 * 3. 预览界面使用SurfaceView，可以使用GLSurfaceView，加滤镜，动画等特效
 */
public class LiveActivity extends AppCompatActivity implements View.OnClickListener {

    //推送服务器地址
    public static final String URL = ""; //添加推送服务器地址，需要搭建nginx服务器，网上一大堆
    private SurfaceView mSurfaceView;
    private LivePusher pusher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_live_player);
        findViewById(R.id.btn_live_start).setOnClickListener(this);
        findViewById(R.id.btn_live_switch).setOnClickListener(this);
        pusher = new LivePusher(this, mSurfaceView);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_live_start:
                starLive();
                break;
            case R.id.btn_live_switch:
                switchCamera();
                // Utils.saveImage(getWindow().getWindowManager().getDefaultDisplay().getWidth(), getWindow().getWindowManager().getDefaultDisplay().getHeight());
                break;
        }
    }

    public void starLive() {
        Button btn = (Button) findViewById(R.id.btn_live_start);
        if (btn.getText().equals("开始直播")) {
            pusher.startPush(URL);
            btn.setText("停止直播");
        } else {
            pusher.stopPush();
            btn.setText("开始直播");
        }
    }

    public void switchCamera() {
        pusher.switchCamera();
    }
}
