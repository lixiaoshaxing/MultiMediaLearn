package com.lx.multimedialearn.mediastudy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;
import com.lx.multimedialearn.mediastudy.videoplayer.VideoPlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 音视频处理导航页
 */
public class MediaIndexActivity extends AppCompatActivity {
    private RecyclerView mRecContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_index);
        mRecContent = (RecyclerView) findViewById(R.id.rec_media_index);
        mRecContent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<TabModel> list = new ArrayList<>();
        list.add(new TabModel("1. MediaPlayer使用-videoplayer", "使用MediaPlayer，在SurfaceView，GLSurfaceView，SurfaceTexture上进行视频同步播放", VideoPlayerActivity.class));
        list.add(new TabModel("2. 硬编码学习-hardcode", "硬编码AudioRecoder,MediaCodec,MediaMuxer使用", VideoPlayerActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
