package com.lx.multimedialearn.mediastudy.hardcode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 结合OpenGL录制视频
 * 1. 录制使用MediaMuxActivity中的方法
 * 2. SurfaceView预览相机替换为GLSurfaceView预览相机，图像数据使用readPixel从GLSurfaceView中提供
 * (1)rgba->yuv
 * (2)方向的调整
 */
public class OpenGLRecordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_glrecord);
    }
}
