package com.lx.multimedialearn.mediastudy.hardcode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 使用硬编码：Camera，AudioRecord，MediaCodec，MediaMutex录制视频
 * 1. 音频录制AudioRecoder/OpenSL，变声FOD
 * 2. 视频录制MediaCodec，交给OpenGL加滤镜，保存
 * 3. 声音+视频合成，生成视频
 * 4. 视频+音乐配音
 * 5. 生成gif
 */
public class HardCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_code);
    }
}
