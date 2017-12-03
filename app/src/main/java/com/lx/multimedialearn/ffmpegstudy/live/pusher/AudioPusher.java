package com.lx.multimedialearn.ffmpegstudy.live.pusher;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;
import com.lx.multimedialearn.ffmpegstudy.live.params.AudioParam;


/**
 * 音频采集
 *
 * @author lixiao
 * @since 2017-08-09 17:25
 */
public class AudioPusher extends Pusher {
    private AudioParam audioParam;
    private AudioRecord audioRecord;
    private boolean isPushing = false;
    private int minBufferSize;

    public AudioPusher(AudioParam audioParam) {
        this.audioParam = audioParam;

        //创建AudioRecoder进行音频录制
        int channelConfig = audioParam.getChannel() == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        //最小缓冲区，通过api方法进行计算，不要手动设置
        minBufferSize = AudioRecord.getMinBufferSize(audioParam.getSampleRateInHz(), channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                audioParam.getSampleRateInHz(),
                channelConfig, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize);
    }

    @Override
    public void startPush() {
        isPushing = true;
        FFmpegUtils.setAudioOptions(audioParam.getSampleRateInHz(), audioParam.getChannel());
        new Thread(new RecordRunnabel()).start();
    }

    @Override
    public void stopPush() {
        isPushing = false;
        audioRecord.stop();
    }

    @Override
    public void release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    class RecordRunnabel implements Runnable {
        @Override
        public void run() {
            audioRecord.startRecording();
            while (isPushing) {
                byte[] buffer = new byte[minBufferSize];
                int len = audioRecord.read(buffer, 0, buffer.length);
                if (len > 0) { //如果录入的有声音
                    //录入的是pcm数据
                    FFmpegUtils.fireAudio(buffer, len);
                    Log.i("sys.out", "录制" + len + "byte数据");
                }
            }
        }
    }
}
