package com.lx.multimedialearn.mediastudy.audio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * OpenSL录制音频为pcm，AudioTrack播放音频
 */
public class OpenSLActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_sl);
    }
}
