package com.lx.multimedialearn.mediastudy.hardcode;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.mediastudy.hardcode.render.GrayCameraRender;
import com.lx.multimedialearn.utils.BitmapUtils;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.MediaUtils;
import com.lx.multimedialearn.utils.ScreenUtils;
import com.lx.multimedialearn.utils.ToastUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.lx.multimedialearn.utils.GlUtil.createCameraTextureID;

/**
 * 结合OpenGL录制视频（虽然是>21,只是个别方法需要替换，都有替换的方法，能支持到4.1，如果需要4.1之下的，就只能用ffmpeg进行合成了）
 * 1. 录制使用MediaMuxActivity中的方法
 * 2. SurfaceView预览相机替换为GLSurfaceView预览相机，图像数据使用readPixel从GLSurfaceView中提供
 * (1)rgba->yuv
 * (2)方向的调整
 * 3. 会掉帧，如果需要流畅的，结合pbo录制，pbo见openglstudy/xbo/pbo，后续会补充上
 * 4. 前置摄像头，采集的数据是镜像的，这就需要转方向了，jni+neon比较快，下文有方法的，libyuv最好
 * 好文章：音视频处理中的坑：http://ragnraok.github.io/android_video_record.html
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class OpenGLRecordActivity extends AppCompatActivity implements View.OnClickListener {
    private GLSurfaceView mGLSurfaceView;
    private Button mBtnRecord;
    private Button mBtnPlay;
    private int mWidth;
    private int mHeight;
    private String mMuxPath;
    private MediaPlayer mPlayer;
    private String mAudioMime = "audio/mp4a-latm"; //音频编码mime
    private String mVideoMime = "video/avc";
    private Status mStatus = Status.END;
    private int mVideoRate = 2048000;       //视频编码波特率,单位时间二进制总数，波特率越高，画面质量越好
    private int mFrameRate = 24;           //视频编码帧率
    private int mFrameInterval = 1;        //视频编码关键帧，1秒一关键帧
    private int mFpsTime = 1000 / mFrameRate; //每一帧持续时间，处理一帧小于这段时间，就会等待，这期间公用这一帧画面
    private int mMinBufferSize;
    private Camera.Parameters mParameters;
    private Camera mCamera;//录制视频
    private AudioRecord mAudioRecord; //录制音频
    private MediaCodec mAudioCodec; //编码器，视频，音视频通用
    private MediaCodec mVideoCodec;
    private MediaMuxer mMediaMux; //合成
    private int mVideoTrack = -1;
    private int mAudioTrack = -1;
    private int mTextureID;
    private SurfaceTexture mSurfaceTexture;
    private GrayCameraRender mRender;

    private byte[] newData;

    enum Status { //状态
        RUNNING, //正在运行
        END //停止
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_glrecord);
        mGLSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_open_gl_record_player);
        mBtnRecord = (Button) findViewById(R.id.btn_gl_record_record);
        mBtnPlay = (Button) findViewById(R.id.btn_gl_record_play);
        mBtnRecord.setOnClickListener(this);
        mBtnPlay.setOnClickListener(this);
        init(); //使用GLSurfaceView预览，需要创建SurfaceTexture
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCamera.startPreview();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
        mGLSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    private void init() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        this.mWidth = 640;
        this.mHeight = 480;
        mGLSurfaceView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mWidth = mGLSurfaceView.getWidth();
                mHeight = mGLSurfaceView.getHeight();
                return true;
            }
        });
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);
        CameraUtils.setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
        int bitsPerPixel = ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()); //这里需要搞清楚WithBuffer和直接callback的区别
        final byte[] buffers = new byte[supportPreviewSize.width * supportPreviewSize.height * bitsPerPixel / 8]; //官方建议这么设置
        mCamera.addCallbackBuffer(buffers);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() { //设置回调几个方法的区别：http://blog.csdn.net/lb377463323/article/details/53338045
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                mCamera.addCallbackBuffer(buffers);//这里能够接收到在预览界面上的数据，NV21格式即yuv420sp
            }
        });
        //使用GLSurfaceView进行预览
        mTextureID = createCameraTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.detachFromGLContext(); //创建完成后立刻detachOpenGL世界的上下文，在CameraRender中进行绘画时，根据ID attachToGLContext，attach到当前GLSurfaceView，进行绘画，注意线程同步
        mCamera.stopPreview();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRender = new GrayCameraRender(this, mSurfaceTexture, mTextureID);
        mRender.setOnFrameOKListener(new GrayCameraRender.OnFrameOK() {
            @Override
            public void output(byte[] data) {
                newData = data;
            }
        });
        mGLSurfaceView.setRenderer(mRender);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Button btn = (Button) v;
        switch (id) {
            case R.id.btn_gl_record_record:
                if (btn.getText().equals("录制")) {
                    startRecord();
                    btn.setText("停止");
                } else {
                    stopRecord();
                    btn.setText("录制");
                }
                break;
            case R.id.btn_gl_record_play:
                if (btn.getText().equals("播放")) {
                    playWithMediaPlayer();
                    btn.setText("停止");
                } else {
                    stopMediaPlayer();
                    btn.setText("播放");
                }
                break;
        }
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        /*******************准备音频，视频录制参数，初始化MediaMux，然后开始录制******************************/
        mMinBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize);
        mAudioRecord.startRecording(); //开始录制
        //8000, 11025, 22050, 44100, 48000,设置音频采样率，44100是目前的标准
        MediaFormat audioFormat = MediaFormat.createAudioFormat(mAudioMime, 44100, 2); //双声道，描述文件格式
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); //录制为aac
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000); //64000, 96000, 128000,比特率,声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
        MediaFormat videoFormat = MediaFormat.createVideoFormat(mVideoMime, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoRate);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mFrameInterval); //关键帧的间隔，1秒一个关键帧
        int[] colorFormat = MediaUtils.checkColorFormat(mVideoMime);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat[0]);
        try {
            mAudioCodec = MediaCodec.createEncoderByType(mAudioMime);
            mAudioCodec.configure(
                    audioFormat, //编码格式信息
                    null, //预览界面
                    null, //编码器
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mAudioCodec.start(); //开始编码，之上配置完录音，编码器
            mVideoCodec = MediaCodec.createEncoderByType(mVideoMime);
            mVideoCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoCodec.start();

            mMuxPath = FileUtils.createFilePath("mp4", 0);
            //初始化MediaMux
            mMediaMux = new MediaMuxer(mMuxPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMediaMux.setOrientationHint(180); //readPixel数据是倒置的，传入这个角度，后期播放mp4，会使用矩阵进行旋转，方向就正确了，但是前置摄像头又镜像了。。。。使用neon进行镜像旋转吧。。。
            mStatus = Status.RUNNING;
            startAudioRecord();
            startVideoRecord();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    /**
     * 声音录制，编码，生成aac
     * 1. 使用AudioRecord进行录制
     * 2. 使用MediaCodec编码，pcm->aac
     */
    private void startAudioRecord() {
        final long startTime = System.nanoTime();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mStatus == Status.RUNNING) {
                    int index = mAudioCodec.dequeueInputBuffer(-1); //获取可以buffer地址， 参数表示需要得到的毫秒数，-1表示一直等，0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
                    if (index >= 0) {
                        ByteBuffer byteBuffer = mAudioCodec.getInputBuffer(index); //获取缓冲区
                        byteBuffer.clear();
                        int length = mAudioRecord.read(byteBuffer, mMinBufferSize);
                        if (length != AudioRecord.ERROR_INVALID_OPERATION) {
                            //放入mediacodec
                            mAudioCodec.queueInputBuffer(index, 0, length, (System.nanoTime() - startTime) / 1000, mStatus == Status.RUNNING ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                    }
                    //从MediaCodec中取编码好的数据，并放入文件中
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    int outIndex;
                    do {
                        outIndex = mAudioCodec.dequeueOutputBuffer(info, 0); //获取info信息
                        if (outIndex >= 0) {
                            ByteBuffer buffer = mAudioCodec.getOutputBuffer(outIndex); //读出编码好的数据
                            //放入mux
                            buffer.position(info.offset);
                            if (mAudioTrack >= 0 && mVideoTrack >= 0 && info.size > 0 && info.presentationTimeUs > 0) {
                                try {
                                    mMediaMux.writeSampleData(mAudioTrack, buffer, info);
                                } catch (Exception e) {
                                    Log.e("sys.out", "audio error:size=" + info.size + "/offset="
                                            + info.offset + "/timeUs=" + info.presentationTimeUs);
                                    e.printStackTrace();
                                }
                            }
                            mAudioCodec.releaseOutputBuffer(outIndex, false); //释放，回收继续使用
                        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            //需在这里获取该MediaCodec的format，并注冊到MediaMuxer里
                            mAudioTrack = mMediaMux.addTrack(mAudioCodec.getOutputFormat());
                            if (mAudioTrack >= 0 && mVideoTrack >= 0) {
                                mMediaMux.start(); //开始了两次，这样有问题
                            }
                        }
                    } while (outIndex >= 0);
                }
            }
        }).start();
    }

    /**
     * 录制视频
     */
    private void startVideoRecord() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.nanoTime();
                while (mStatus == Status.RUNNING) {
                    long time = System.currentTimeMillis();
                    //需要从Camera中拿数据，并且处理
                    int index = mVideoCodec.dequeueInputBuffer(-1);
                    if (index >= 0) {
                        //mVideoCodec.getInputBuffers()[index];//支持4.1之上
                        if (newData != null) {
                            byte[] yuv = new byte[mWidth * mHeight * 3 / 2];
                            BitmapUtils.rgbaToYuv(newData, mWidth, mHeight, yuv); //rgb边yuv数据
                            ByteBuffer byteBuffer = mVideoCodec.getInputBuffer(index); //支持5.0之上
                            byteBuffer.clear();
                            byteBuffer.put(yuv);
                            mVideoCodec.queueInputBuffer(index, 0, yuv.length, (System.nanoTime() - startTime) / 1000, mStatus == Status.RUNNING ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex = mVideoCodec.dequeueOutputBuffer(info, 0);
                        do {
                            if (outIndex >= 0) {
                                if (mStatus != Status.RUNNING) {
                                    return;
                                }
                                ByteBuffer outBuf = mVideoCodec.getOutputBuffer(outIndex);
                                if (mAudioTrack >= 0 && mVideoTrack >= 0 && info.size > 0 && info.presentationTimeUs > 0) {
                                    mMediaMux.writeSampleData(mVideoTrack, outBuf, info);
                                }
                                mVideoCodec.releaseOutputBuffer(outIndex, false);
                                outIndex = mVideoCodec.dequeueOutputBuffer(info, 0); //接着读取
                            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                mVideoTrack = mMediaMux.addTrack(mVideoCodec.getOutputFormat());
                                if (mAudioTrack >= 0 && mVideoTrack >= 0) {
                                    mMediaMux.start();
                                }
                            }
                        } while (outIndex >= 0);
                        long cost = System.currentTimeMillis() - time; //读取这一帧并保存到file中耗费的时间
                        if (mFpsTime > cost) { //还不到一帧持续的时间
                            try {
                                Thread.sleep(mFpsTime - cost);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private void stopRecord() {
        mStatus = Status.END;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
        mAudioCodec.stop();
        mAudioCodec.release();
        mVideoCodec.stop();
        mVideoCodec.release();
        mVideoTrack = -1;
        mAudioTrack = -1;
        mMediaMux.stop();
        mMediaMux.release();
    }

    private void playWithMediaPlayer() {
        ToastUtils.show(this, "去本地查看生成的mp4文件吧，^_^");
    }

    private void stopMediaPlayer() {
    }
}
