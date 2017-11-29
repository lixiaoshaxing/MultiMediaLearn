package com.lx.multimedialearn.mediastudy.hardcode;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
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
import com.lx.multimedialearn.utils.MediaUtils;
import com.lx.multimedialearn.utils.ScreenUtils;
import com.lx.multimedialearn.utils.ToastUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 使用硬编码：Camera，AudioRecord，MediaCodec，MediaMutex录制视频
 * 1. 音频录制AudioRecoder/OpenSL
 * 2. 视频录制Camera
 * 3. 编码（也可以解码）MediaCodec
 * 4. 声音+视频合成，生成视频 MediaMutex
 * 5. 音频，视频提取（两个通道的分离）MediaExtractor
 * 6. 编解码器 MediaCrypto(待)
 * 7. 音视频加密 MediaDrm（待）
 * 加滤镜，加背景音乐，生成gif，多段视频拼接等
 * MediaCodec blog:官方文档翻译 http://www.cnblogs.com/xiaoshubao/archive/2016/04/11/5368183.html
 * 生成gif：http://www.javashuo.com/content/p-6359257.html
 * MediaCodec使用：http://blog.csdn.net/junzia/article/details/54018671
 * 4.1之前 使用ffmpeg，4.1 提供硬编码 MediaCodec，4.3 提供MediaMutex合成
 * 音视频这样不分离，很容易写出shit一样的代码啊！！！
 */
public class MediaCodecActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceView mSurfaceView;
    private Button mBtnAudio; //音频
    private Button mBtnVideo; //视频
    private Button mBtnMux; //混合音视频
    private Button mBtnPlay; //播放
    private Camera mCamera;
    private android.hardware.Camera.Parameters mParameters;
    private LinkedBlockingQueue<byte[]> mQueue; //camera数据存放在队列里，编码器从队列拿数据进行编码
    private int mWidth;
    private int mHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_media_codec_player);
        mBtnAudio = (Button) findViewById(R.id.btn_media_codec_audio);
        mBtnVideo = (Button) findViewById(R.id.btn_media_codec_video);
        mBtnMux = (Button) findViewById(R.id.btn_media_codec_mux);
        mBtnPlay = (Button) findViewById(R.id.btn_media_codec_play);
        mBtnAudio.setOnClickListener(this);
        mBtnVideo.setOnClickListener(this);
        mBtnMux.setOnClickListener(this);
        mBtnPlay.setOnClickListener(this);
        init();
    }

    /**
     * 初始化相机，使用SurfaceView进行预览，获取相机数据回调
     */
    private void init() {
        mQueue = new LinkedBlockingQueue<>();
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        this.mWidth = supportPreviewSize.width;
        this.mHeight = supportPreviewSize.height;
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
                //这个数据需要加到队列，供编码使用，这个数据预览方向是对的，但是data里存的方向是错的，nv21格式
                if (mVideoStatus == Status.RUNNING) {
                    byte[] temp = new byte[data.length];
                    MediaUtils.NV21toI420SemiPlanar(data, temp, mWidth, mHeight);
                    mQueue.add(temp);
                }
            }
        });
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    mCamera.setPreviewDisplay(mSurfaceView.getHolder());
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Button btn = (Button) v;
        switch (id) {
            case R.id.btn_media_codec_audio:
                if (TextUtils.equals("音频", btn.getText())) {
                    btn.setText("停止");
                    startAudioRecord();
                } else {
                    btn.setText("音频");
                    stopAudioRecord();
                }
                break;
            case R.id.btn_media_codec_video:
                if (TextUtils.equals("视频", btn.getText())) {
                    btn.setText("停止");
                    startVideoRecord();
                } else {
                    btn.setText("视频");
                    stopVideoRecord();
                }
                break;
            case R.id.btn_media_codec_mux:
                if (TextUtils.equals("音视频", btn.getText())) {
                    btn.setText("停止");
                    startRecord();
                } else {
                    btn.setText("音视频");
                    stopRecord();
                }
                break;
            case R.id.btn_media_codec_play:
                playWithMediaPlayer();
                break;
        }
    }

    /***********************音频录制相关方法***************************/
    enum Status { //状态
        RUNNING, //正在运行
        END //停止
    }

    private AudioRecord mAudioRecord; //录制音频
    private MediaCodec mCodec; //编码器，视频，音视频通用
    private String mAudioMime = "audio/mp4a-latm"; //音频编码mime
    private Status mAudioStatus = Status.END;
    private String mAudioPath;
    FileOutputStream fos = null;

    /**
     * 声音录制，编码，生成aac
     * 1. 使用AudioRecord进行录制
     * 2. 使用MediaCodec编码，pcm->aac
     */
    private void startAudioRecord() {
        final int mMinBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize);
        mAudioRecord.startRecording(); //开始录制
        mAudioStatus = Status.RUNNING;
        mAudioPath = FileUtils.createFilePath("aac", 1);
        //8000, 11025, 22050, 44100, 48000,设置音频采样率，44100是目前的标准
        MediaFormat format = MediaFormat.createAudioFormat(mAudioMime, 44100, 2); //双声道，描述文件格式
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); //录制为aac
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000); //64000, 96000, 128000,比特率,声音中的比特率是指将模拟声音信号转换成数字声音信号后，单位时间内的二进制数据量，是间接衡量音频质量的一个指标
        try {
            mCodec = MediaCodec.createEncoderByType(mAudioMime);
            mCodec.configure(
                    format, //编码格式信息
                    null, //预览界面
                    null, //编码器
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mCodec.start(); //开始编码，之上配置完录音，编码器
            final long startTime = System.nanoTime();
            try {
                fos = new FileOutputStream(mAudioPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mAudioStatus == Status.RUNNING) {
                        int index = mCodec.dequeueInputBuffer(-1); //获取可以buffer地址， 参数表示需要得到的毫秒数，-1表示一直等，0表示不需要等，传0的话程序不会等待，但是有可能会丢帧。
                        if (index >= 0) {
                            ByteBuffer byteBuffer = mCodec.getInputBuffer(index); //获取缓冲区
                            byteBuffer.clear();
                            int length = mAudioRecord.read(byteBuffer, mMinBufferSize);
                            if (length != AudioRecord.ERROR_INVALID_OPERATION) {
                                //放入mediacodec
                                mCodec.queueInputBuffer(index, 0, length, (System.nanoTime() - startTime) / 1000, mAudioStatus == Status.RUNNING ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            }
                        }
                        //从MediaCodec中取编码好的数据，并放入文件中
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex;
                        do {
                            outIndex = mCodec.dequeueOutputBuffer(info, 0); //获取info信息
                            if (outIndex >= 0) {
                                ByteBuffer buffer = mCodec.getOutputBuffer(outIndex); //读出编码好的数据
                                buffer.position(info.offset);
                                //AAC编码需要加数据头，AAC编码数据头固定为7子杰
                                byte[] temp = new byte[info.size + 7];
                                buffer.get(temp, 7, info.size);
                                MediaUtils.addADTSToPacket(temp, temp.length);
                                if (fos != null) {
                                    try {
                                        fos.write(temp);
                                        mCodec.releaseOutputBuffer(outIndex, false); //释放，回收继续使用
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        try {
                                            fos.flush();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                //需在这里获取该MediaCodec的format，并注冊到MediaMuxer里
                            }
                        } while (outIndex >= 0);
                    }
                    if (fos != null) {
                        try {
                            fos.flush();
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放声音录制相关
     */
    private void stopAudioRecord() {
        mAudioStatus = Status.END;
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
    }

    /****************视频录制相关方法*******************/
    private Status mVideoStatus = Status.END;
    private String mVideoPath;
    private String mVideoMime = "video/avc";
    private int mVideoRate = 2048000;       //视频编码波特率,单位时间二进制总数，波特率越高，画面质量越好
    private int mFrameRate = 24;           //视频编码帧率
    private int mFrameInterval = 1;        //视频编码关键帧，1秒一关键帧
    private int mFpsTime = 1000 / mFrameRate; //每一帧持续时间，处理一帧小于这段时间，就会等待，这期间公用这一帧画面
    private int mNowTime; //当前时间，camera能够获取这个时间
    private FileOutputStream mVideoFos;
    private MediaCodec mVideoCodec;

    /**
     * 录制视频
     */
    private void startVideoRecord() {
        try {
            mVideoPath = FileUtils.createFilePath("mp4", 0); //经过MediaCodec是编码为h264数据，还需要压缩为mp4格式数据
            mVideoFos = new FileOutputStream(mVideoPath);
            MediaFormat format = MediaFormat.createVideoFormat(mVideoMime, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mVideoRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mFrameInterval); //关键帧的间隔，1秒一个关键帧
            int[] colorFormat = MediaUtils.checkColorFormat(mVideoMime);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat[0]);
            mVideoCodec = MediaCodec.createEncoderByType(mVideoMime);
            mVideoCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mVideoCodec.start();
            mVideoStatus = Status.RUNNING;  //准备好了编码器
            final long startTime = System.nanoTime();
            new Thread(new Runnable() { //
                @Override
                public void run() {
                    byte[] headInfo = null;
                    while (mVideoStatus == Status.RUNNING) {
                        long time = System.currentTimeMillis();
                        //需要从Camera中拿数据，并且处理
                        try {
                            byte[] data = mQueue.take(); //拿出一帧画面，nv21格式，如果从GL，使用readPixel，是rgba格式
                            int index = mVideoCodec.dequeueInputBuffer(-1);
                            if (index >= 0) {
                                //mVideoCodec.getInputBuffers()[index];//支持4.1之上
                                ByteBuffer byteBuffer = mVideoCodec.getInputBuffer(index); //支持5.0之上
                                byteBuffer.clear();
                                byteBuffer.put(data);
                                mVideoCodec.queueInputBuffer(index, 0, data.length, (System.nanoTime() - startTime) / 1000, mVideoStatus == Status.RUNNING ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                                int outIndex = mVideoCodec.dequeueOutputBuffer(info, 0);
                                while (outIndex >= 0) {
                                    ByteBuffer outBuf = mVideoCodec.getOutputBuffer(outIndex);
                                    byte[] temp = new byte[info.size];
                                    outBuf.get(temp);
                                    if (info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) { //存储头信息
                                        headInfo = new byte[temp.length];
                                        headInfo = temp;
                                    } else if (info.flags % 8 == MediaCodec.BUFFER_FLAG_KEY_FRAME) { //关键帧，写入头信息
                                        byte[] keyframe = new byte[temp.length + headInfo.length];
                                        System.arraycopy(headInfo, 0, keyframe, 0, headInfo.length);
                                        System.arraycopy(temp, 0, keyframe, headInfo.length, temp.length);
                                        mVideoFos.write(keyframe, 0, keyframe.length);
                                    } else if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                    } else {
                                        mVideoFos.write(temp, 0, temp.length); //这是是h264格式，还需要封装为mp4才能播放
                                    }
                                    mVideoFos.flush();
                                    mVideoCodec.releaseOutputBuffer(outIndex, false);
                                    outIndex = mVideoCodec.dequeueOutputBuffer(info, 0); //接着读取
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        long cost = System.currentTimeMillis() - time; //读取这一帧并保存到file中耗费的时间
                        if (mFpsTime > cost) { //还不到一帧持续的时间
                            try {
                                Thread.sleep(mFpsTime - cost);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (mVideoFos != null) {
                        try {
                            mVideoFos.flush();
                            mVideoFos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制视频
     */
    private void stopVideoRecord() {
        mVideoStatus = Status.END;
        mCamera.stopPreview();
        mCamera.release();
    }

    /*************录制音视频，合成的方法********************/
    private String mMuxPath;

    /**
     * 播放
     * 1. 如果有mux，video，audio地址：播放音视频
     * 2. 如果有video，audio地址：播放视频
     * 3. 如果有audio地址：播放音频
     * 4. 如果都为null，弹toast
     */
    private void playWithMediaPlayer() {
        if (!TextUtils.isEmpty(mMuxPath)) {
            //播放音视频
            return;
        }
        if (!TextUtils.isEmpty(mVideoPath)) {
            //播放视频
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(mVideoPath);
                player.prepare();
                player.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if (!TextUtils.isEmpty(mAudioPath)) {
            //播放音频
            MediaPlayer player = new MediaPlayer();
            try {
                player.setDataSource(mAudioPath);
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
                player.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        ToastUtils.show(this, "必须先录制，才能播放");
    }

    /*****************音视频的录制************************/
    /**
     * 音视频录制合成，把之前存入文件的buffer写入MediaMux轨道中
     */
    private void startRecord() {

    }

    /**
     * 停止录制
     */
    private void stopRecord() {
    }
}
