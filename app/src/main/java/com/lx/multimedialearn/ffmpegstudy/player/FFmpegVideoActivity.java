package com.lx.multimedialearn.ffmpegstudy.player;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;

import java.io.File;

/**
 * 使用ffmpeg播放视频
 * 1. 解析视频中的视频帧，渲染到Surface上
 * 2. 音视频同步
 */
public class FFmpegVideoActivity extends AppCompatActivity {
    private SurfaceView mSurfaceView;
    private Button mBtnVideo;
    private Button mBtnSync;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_video);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_ffmpeg_video_player);
        mBtnVideo = (Button) findViewById(R.id.btn_ffmpeg_video_video);
        mBtnSync = (Button) findViewById(R.id.btn_ffmpeg_video_sync);
        mBtnVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "a8.mp4";
                FFmpegUtils.videoPlay(inputPath, mSurfaceView.getHolder().getSurface());
            }
        });
        mBtnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "a8.mp4";
                new FFmpegUtils().videoPthreadPlay(inputPath, mSurfaceView.getHolder().getSurface());
            }
        });
    }
}
