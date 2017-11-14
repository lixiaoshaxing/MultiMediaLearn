package com.lx.multimedialearn.mediastudy.hardcode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 使用硬编码：AudioRecord，Camera，MediaCodec，MediaMutex录制视频
 */
public class HardCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hard_code);
    }
}
