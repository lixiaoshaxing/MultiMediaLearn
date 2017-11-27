package com.lx.multimedialearn.mediastudy.mediarecord;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.SurfaceHolder;
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
    private int mCameraRotation = 0;
    private MediaRecorder mRecorder;
    private String mPath;

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
        initCamera();
    }

    /**
     * 设置相机参数，开始预览
     */
    private void initCamera() {
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
        int bitsPerPixel = ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()); //这里需要搞清楚WithBuffer和直接callback的区别
        final byte[] buffers = new byte[supportPreviewSize.width * supportPreviewSize.height * bitsPerPixel / 8]; //官方建议这么设置
        mCamera.addCallbackBuffer(buffers);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() { //设置回调几个方法的区别：http://blog.csdn.net/lb377463323/article/details/53338045
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                mCamera.addCallbackBuffer(buffers);//这里能够接收到在预览界面上的数据，NV21格式即yuv420sp
            }
        });
        mSurfaceViewPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera.stopPreview();
                try {
                    mCamera.setPreviewDisplay(mSurfaceViewPreview.getHolder()); //必须在Surfaceview初始化之后才能预览
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
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
        mRecorder = new MediaRecorder(); //初始化
        mCamera.unlock(); //设置自定义Camera之前，必须unLock，Camera可以被录制使用
        mRecorder.setCamera(mCamera); //设置自定义相机
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); //设置音频来源
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); //设置视频来源
        mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P)); //设置输入格式，480p，可以通过下边注释的参数自定义格式，如果自定义这一行需要注释掉
        mRecorder.setOrientationHint(360 - mCameraRotation); //前置摄像头需要调整，还有镜面
//        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //设置输出为mp4格式
//        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //设置音频编码格式
//        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);// 设置视频编码的格式为h264，所有移动设备都支持
//        mRecorder.setVideoSize(1280, 720); //设置视频大小
//        mRecorder.setVideoEncodingBitRate(8); //这个参数不明白，设置每一个像素点的位数？
//        mRecorder.setVideoFrameRate(20); //设置帧率
        mRecorder.setOutputFile(file.getAbsolutePath()); //设置录制后输出地址
        mRecorder.setPreviewDisplay(mSurfaceViewPreview.getHolder().getSurface()); //设置预览SurfaceView
        try {
            mRecorder.prepare(); //准备，必须在SurfaceView创建后
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start(); //录制
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
        MediaPlayer mediaPlayer = new MediaPlayer(); //使用MediaPlayer进行播放
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
