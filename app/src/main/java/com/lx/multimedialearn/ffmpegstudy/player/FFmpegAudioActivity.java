package com.lx.multimedialearn.ffmpegstudy.player;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;

import java.io.File;

/**
 * 使用ffmpeg解析音频为pcm，使用AudioTrack播放音频（首先解析为pcm，播放）
 * 1. ffmpeg解析
 * 2. jni调用java层AudioTrack播放音频
 */
public class FFmpegAudioActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mBtnDecode;
    private Button mBtnPlay;
    private String inputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "a5.mp3";
    private String outpath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "a5_1.pcm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_audio);
        mBtnDecode = (Button) findViewById(R.id.btn_ffmpeg_audio_decode);
        mBtnPlay = (Button) findViewById(R.id.btn_ffmpeg_audio_play);
        mBtnDecode.setOnClickListener(this);
        mBtnPlay.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_ffmpeg_audio_decode:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        FFmpegUtils.ffmpeg_decode(inputPath, outpath);
                    }
                }).start();
                break;
            case R.id.btn_ffmpeg_audio_play:
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                        new FFmpegUtils().ffmpeg_play(inputPath, outpath);
                    }
                });
                thread.setPriority(Thread.MAX_PRIORITY);
                thread.start();
                break;
        }
    }
}
