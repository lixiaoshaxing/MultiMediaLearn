package com.lx.multimedialearn.ffmpegstudy.flow;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.ffmpegstudy.FFmpegUtils;

/**
 * 推流本地mp4视频
 * 1. 使用ffmpeg，faac解析mp4为x264+aac
 * 2. 使用libRtmp封装x264，aac为flv包，推送到远端推流服务器
 */
public class FlowNativeMp4Activity extends AppCompatActivity {

    private EditText mEtVideoPath;
    private EditText mEtNginxPath;
    private Button mBtnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flow_native_mp4);
        mEtVideoPath = (EditText) findViewById(R.id.et_flow_native_video_path);
        mEtNginxPath = (EditText) findViewById(R.id.et_flow_native_nginx_path);
        mBtnStart = (Button) findViewById(R.id.btn_flow_native_start);
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputurl = Environment.getExternalStorageDirectory().getPath() + "/" + mEtVideoPath.getText().toString();
                FFmpegUtils.push(inputurl, mEtNginxPath.getText().toString());
            }
        });
    }
}
