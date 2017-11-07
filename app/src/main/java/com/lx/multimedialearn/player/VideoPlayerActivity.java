package com.lx.multimedialearn.player;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.player.render.GLPlayerRender2;

import java.io.IOException;

/**
 * Task3：音视频的处理
 * 1. 在SurfaceView，GLSurfaceView，TextureView播放Mp4视频
 * 2. MediaPlayer需要学习
 */
public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView mSurfaceView; //使用SurfaceView预览
    private TextureView mTextureView; //使用TextureView预览
    private GLSurfaceView mGlSurfaceView; //使用GlSurfaceView预览
    private Button mBtnPlay; //播放视频
    private Button mBtnStop; //停止播放
    private MediaPlayer mSurfaceViewPlayer; //使用MediaPlayer进行播放控制
    private MediaPlayer mGLSurfaceViewPlayer;
    private MediaPlayer mTextureViewPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_video);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_video_play);
        mTextureView = (TextureView) findViewById(R.id.textureview_video_play);
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_video_play);
        mBtnPlay = (Button) findViewById(R.id.btn_video_play);
        mBtnStop = (Button) findViewById(R.id.btn_video_stop);
        mBtnPlay.setOnClickListener(this);
        mBtnStop.setOnClickListener(this);
        startPlayOnSurfaceView();
        startPlayOnTextureView();
        startPlayOnGLSurfaceView();
    }

    /**
     * 在SurfaceView上播放MP4
     */
    private void startPlayOnSurfaceView() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mSurfaceViewPlayer == null) {
                    mSurfaceViewPlayer = new MediaPlayer();
                    mSurfaceViewPlayer.setVolume(0, 0);
                    mSurfaceViewPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                           /* mp.start();*/
                        }
                    });
                    mSurfaceViewPlayer.setDisplay(holder);
                    try {
                        AssetManager as = getAssets();
                        AssetFileDescriptor afd = as.openFd("a.mp4");
                        mSurfaceViewPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mSurfaceViewPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }

    /**
     * 在TextureView上播放mp4
     */
    private void startPlayOnTextureView() {
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //这里初始化Mediaplayer，并准备资源文件
                if (mTextureViewPlayer == null) {
                    mTextureViewPlayer = new MediaPlayer();
                    Surface temp = new Surface(surface);
                    mTextureViewPlayer.setVolume(0, 0);
                    AssetManager as = getAssets();
                    mTextureViewPlayer.setSurface(temp);
                    try {
                        AssetFileDescriptor afd = as.openFd("a.mp4");
                        mTextureViewPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        mTextureViewPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    /**
     * 在GLSurfaceView上播放mp4
     */
    private void startPlayOnGLSurfaceView() {
        if (mGLSurfaceViewPlayer == null) {
            mGLSurfaceViewPlayer = new MediaPlayer();
            mGLSurfaceViewPlayer.setVolume(1, 1);
            AssetManager as = getAssets();
            try {
                AssetFileDescriptor afd = as.openFd("a.mp4");
                mGLSurfaceViewPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mGlSurfaceView.setEGLContextClientVersion(2);
            GLPlayerRender2 mRender = new GLPlayerRender2(mGLSurfaceViewPlayer);
            mGlSurfaceView.setRenderer(mRender);//(2)把数据提供给Render，使用Render在SurfaceView上绘画预览图像
            mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); //可以使用dirty进行手动触发更新界面
            mGLSurfaceViewPlayer.prepareAsync();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_video_play:
                mSurfaceViewPlayer.start(); //点击home直接进入屏幕，这时候mSurfaceViewPlayer会为空？导致crash
                mTextureViewPlayer.start();
                mGLSurfaceViewPlayer.start();
                break;
            case R.id.btn_video_stop:
                mSurfaceViewPlayer.stop();
                mTextureViewPlayer.stop();
                mGLSurfaceViewPlayer.stop();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGlSurfaceView != null) {
            mGlSurfaceView.onResume();
        }
        if (mSurfaceViewPlayer != null) {
            mSurfaceViewPlayer.start();
        }
        if (mTextureViewPlayer != null) {
            mTextureViewPlayer.start();
        }
        if (mGLSurfaceViewPlayer != null) {
            mGLSurfaceViewPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGlSurfaceView != null) {
            mGlSurfaceView.onPause();
        }
        if (mSurfaceViewPlayer != null) {
            mSurfaceViewPlayer.pause();
        }
        if (mTextureViewPlayer != null) {
            mTextureViewPlayer.pause();
        }
        if (mGLSurfaceViewPlayer != null) {
            mGLSurfaceViewPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSurfaceViewPlayer != null) {
            //mSurfaceViewPlayer.stop();
            mSurfaceViewPlayer.release();
        }
        if (mTextureViewPlayer != null) {
            //mTextureViewPlayer.stop();
            mTextureViewPlayer.release();
        }
        if (mGLSurfaceViewPlayer != null) {
            //mGLSurfaceViewPlayer.stop();
            mGLSurfaceViewPlayer.release();
        }
    }
}
