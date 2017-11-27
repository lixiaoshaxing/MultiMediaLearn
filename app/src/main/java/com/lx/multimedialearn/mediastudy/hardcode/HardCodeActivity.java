package com.lx.multimedialearn.mediastudy.hardcode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 使用硬编码：Camera，AudioRecord，MediaCodec，MediaMutex录制视频
 * 1. 视频采集，处理
 * 2. 音频采集，处理
 * 3. 音视频合并，同步
 * 4. 生成mp4
 * 5. 生成gif
 */
public class HardCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_code);
    }
}
