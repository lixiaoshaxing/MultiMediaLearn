package com.lx.multimedialearn.mediastudy.hardcode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

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
 *
 */
public class MediaCodecActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_code);
    }
}
