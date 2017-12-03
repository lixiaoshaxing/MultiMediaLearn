package com.lx.multimedialearn.ffmpegstudy.opensl;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;

/**
 * 使用OpenSL播放音频，应该放在media文件夹里。。。
 * 1. 导入OpenSL相关本地文件
 * 2. 使用OpenSL播放
 */
public class OpenSLActivity extends AppCompatActivity {

    private Button mBtnRecord;
    private Button mBtnStop;
    private Button mBtnPlay;
    private Button mBtnPlayStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_sl2);
        mBtnRecord = (Button) findViewById(R.id.btn_open_sl_record);
        mBtnStop = (Button) findViewById(R.id.btn_open_sl_stop);
        mBtnPlay = (Button) findViewById(R.id.btn_open_sl_play);
        mBtnPlayStop = (Button) findViewById(R.id.btn_open_sl_play_stop);
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FFmpegUtils.startCapture();
                    }
                }).start();
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FFmpegUtils.stopCapture();
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FFmpegUtils.startPlay();
                    }
                }).start();
            }
        });

        mBtnPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FFmpegUtils.stopPlay();
            }
        });
    }
}
