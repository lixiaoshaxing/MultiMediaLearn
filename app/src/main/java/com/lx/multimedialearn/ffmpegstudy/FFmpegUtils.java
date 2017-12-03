package com.lx.multimedialearn.ffmpegstudy;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.Surface;

/**
 * ffmpeg调用的本地方法集合
 *
 * @author lixiao
 * @since 2017-12-02 17:14
 */
public class FFmpegUtils {
    static {
        System.loadLibrary("ffmpeg_utils");
        System.loadLibrary("native-video");
    }

    /**
     * 推流本地视频
     *
     * @param inputUrl
     * @param outputUrl
     */
    public static native void push(String inputUrl, String outputUrl);

    /********************直播相关方法****************************/
    public static native void startPush(String url);

    public static native void stopPush();

    public static native void release();

    /**
     * 设置视频参数
     *
     * @param width
     * @param height
     * @param bitrate
     * @param fps
     */
    public static native void setVideoOptions(int width, int height, int bitrate, int fps);

    /**
     * 设置音频参数
     *
     * @param sampleRateInHz
     * @param channel
     */
    public static native void setAudioOptions(int sampleRateInHz, int channel);


    /**
     * 发送视频
     *
     * @param data       推流的一帧图像，yuv->x264格式，可以使用mediaCodec进行编码，这里使用ffmpeg编码
     * @param output_str 采集照片的地址，这里没有用
     * @param cameraId   根据前后摄像头，需要调整画面旋转方向
     */
    public static native void fireVideo(byte[] data, String output_str, int cameraId);

    /**
     * 发送音频数据
     *
     * @param data 声音数据，pcm，使用faac，pcm->aac，使用rtmp推流
     * @param len
     */
    public static native void fireAudio(byte[] data, int len);
    /*********************************************************/

    /**
     * 在java层创建AudioTrack，使用反射在jni层调用该对象（ps，为什么不直接传下去？）
     *
     * @return
     */
    public AudioTrack createAudioTrack() {
        int sampleRateInHz = 44100;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //声道布局
        int channelConfig = android.media.AudioFormat.CHANNEL_OUT_STEREO;

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelConfig,
                audioFormat,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);
        //播放
        //audioTrack.play();
        //写入PCM
        //audioTrack.write(byte[]buffer);
        return audioTrack;
    }

    public AudioTrack createAudioTrack(int sampleRateInHz, int nb_channels) {
        //固定格式的音频码流
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //声道布局
        int channelConfig;
        if (nb_channels == 1) {
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_MONO;
        } else if (nb_channels == 2) {
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            channelConfig = android.media.AudioFormat.CHANNEL_OUT_STEREO;
        }

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRateInHz, channelConfig,
                audioFormat,
                bufferSizeInBytes, AudioTrack.MODE_STREAM);
        //播放
        //audioTrack.play();
        //写入PCM
        //audioTrack.write(audioData, offsetInBytes, sizeInBytes);
        return audioTrack;
    }

    public native void videoPthreadPlay(String inputPath, Surface surface);

    /**
     * 格式转换 所有视频格式转换为YUM，对应于解码
     *
     * @param inputPath
     * @param outputPath
     * @return
     */
    public static native void videoDecode(String inputPath, String outputPath);

    /**
     * 播放视频，播放任意格式，转码为yum-rgb-surfaceview上进行播放
     *
     * @param path
     * @param surface
     */
    public static native void videoPlay(String path, Surface surface);

    /**
     * 音频转码，任意格式转为pcm格式
     *
     * @param input
     * @param output
     */
    public static native void ffmpeg_decode(String input, String output);

    /**
     * 转为pcm格式，使用audiotracker进行播放，audiotracker只能播放pcm格式
     *
     * @param input
     * @param output
     */
    public native void ffmpeg_play(String input, String output);

    /**
     * 使用opensl录制，录制为pcm格式
     */
    public static native void startCapture();

    /**
     * 停止opensl录制
     */
    public static native void stopCapture();

    /**
     * 使用opensl开始播放，必须播放pcm格式
     */
    public static native void startPlay();

    /**
     * 停止opensl播放
     */
    public static native void stopPlay();
}
