package com.lx.multimedialearn.ffmpegstudy.live.pusher;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;
import com.lx.multimedialearn.ffmpegstudy.live.params.AudioParam;
import com.lx.multimedialearn.ffmpegstudy.live.params.VideoParam;


/**
 * 音视频推送管理类
 *
 * @author lixiao
 * @since 2017-08-09 18:41
 */
public class LivePusher implements SurfaceHolder.Callback {
    private SurfaceView surfaceView;
    private VideoPusher videoPusher;
    private AudioPusher audioPusher;
    private Context mContext;

    public LivePusher(Context context, SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        this.mContext = context;
        surfaceView.getHolder().addCallback(this);
        prepare();
    }

    private void prepare() {
        VideoParam videoParam = new VideoParam(480, 320, Camera.CameraInfo.CAMERA_FACING_FRONT);
        videoPusher = new VideoPusher(mContext, videoParam, surfaceView);

        AudioParam audioParam = new AudioParam();
        audioPusher = new AudioPusher(audioParam);

    }

    public void switchCamera() {
        videoPusher.switchCamera();
    }

    public void startPush(String url) {
        videoPusher.startPush();
        audioPusher.startPush();
        FFmpegUtils.startPush(url);
    }

    /**
     * 停止推流
     */
    public void stopPush() {
        videoPusher.stopPush();
        audioPusher.stopPush();
        FFmpegUtils.stopPush();
    }

    /**
     * 释放资源
     */
    private void release() {
        videoPusher.release();
        audioPusher.release();
        FFmpegUtils.release();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPush();
        release();
    }
}
