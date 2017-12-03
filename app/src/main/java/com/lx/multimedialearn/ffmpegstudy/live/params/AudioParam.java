package com.lx.multimedialearn.ffmpegstudy.live.params;

/**
 * 音频的参数设置
 *
 * @author lixiao
 * @since 2017-08-09 17:19
 */
public class AudioParam {
    // 采样率
    private int sampleRateInHz = 44100;
    // 声道个数
    private int channel = 2;

    public AudioParam() {
    }

    public AudioParam(int sampleRateInHz, int channel) {
        super();
        this.sampleRateInHz = sampleRateInHz;
        this.channel = channel;
    }

    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    public void setSampleRateInHz(int sampleRateInHz) {
        this.sampleRateInHz = sampleRateInHz;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
}
