package com.lx.multimedialearn.mediastudy.mediarecord;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.ScreenUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * MediaRecord结合Camera录制音视频，使用MediaPlayer进行播放
 * 1. 开始相机预览
 * 2. 使用MediaRecord进行录制
 * 3. 使用MediaPlayer进行播放
 */
public class MediaRecordActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView mSurfaceViewPreview; //预览界面
    private SurfaceView mSurfaceViewPlayer; //播放界面
    private Button mBtnRecord; //录制
    private Button mBtnPlayer; //播放
    private Camera mCamera;
    private Camera.Parameters mParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_record);
        mSurfaceViewPreview = (SurfaceView) findViewById(R.id.surface_media_record_preview);
        mSurfaceViewPlayer = (SurfaceView) findViewById(R.id.surface_media_record_player);
        mBtnRecord = (Button) findViewById(R.id.btn_media_record_start);
        mBtnPlayer = (Button) findViewById(R.id.btn_media_record_play);
        mBtnRecord.setOnClickListener(this);
        mBtnPlayer.setOnClickListener(this);

        startPreview();
    }

    int mCameraRotation = 0;

    /**
     * 设置相机参数，开始预览
     */
    private void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);
        mCameraRotation = CameraUtils.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_media_record_start:
                Button b = (Button) v;
                if (TextUtils.equals("录制", b.getText())) {
                    b.setText("停止");
                    startRecord();
                } else if (TextUtils.equals("停止", b.getText())) {
                    //释放MediaRecord资源
                    b.setText("录制");
                    stopRecord();
                }
                break;
            case R.id.btn_media_record_play:
                startPlay();
                break;
        }
    }

    private MediaRecorder mRecorder;
    private String mPath;

    /**
     * 开始进行录制
     * 1. 创建输出文件
     * 2. 使用MediaRecord进行录制
     * blog: http://blog.csdn.net/fengyuzhengfan/article/details/38563187
     */
    private void startRecord() {
        String currentTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File file = new File(FileUtils.createCommonDir(), "VID" + currentTime + ".mp4");
        mPath = file.getAbsolutePath();
        mRecorder = new MediaRecorder();
        mCamera.unlock(); //设置自定义Camera之前，必须unLock，Camera可以被录制使用
        mRecorder.setCamera(mCamera);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        mRecorder.setOrientationHint(360 - mCameraRotation); //前置摄像头需要调整，还有镜面
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        // 设置图像编码的格式
//        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mRecorder.setVideoSize(1280, 720);
//        mRecorder.setVideoEncodingBitRate(8);
//        mRecorder.setVideoFrameRate(20);
        mRecorder.setOutputFile(file.getAbsolutePath());
        mRecorder.setPreviewDisplay(mSurfaceViewPreview.getHolder().getSurface());
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start();
    }

    private void stopRecord() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * 开始进行播放
     */
    private void startPlay() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setDisplay(mSurfaceViewPlayer.getHolder());
        try {
            mediaPlayer.setDataSource(mPath);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
