package com.lx.multimedialearn.ffmpegstudy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.ffmpegstudy.flow.FlowNativeMp4Activity;
import com.lx.multimedialearn.ffmpegstudy.live.LiveActivity;
import com.lx.multimedialearn.ffmpegstudy.opensl.OpenSLActivity;
import com.lx.multimedialearn.ffmpegstudy.player.FFmpegAudioActivity;
import com.lx.multimedialearn.ffmpegstudy.player.FFmpegVideoActivity;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;

import java.util.ArrayList;
import java.util.List;

/**
 * FFMpeg先关内容
 * 1. ffmpeg，librtmp，faac，x264编译
 * 2. nginx-rtmp服务器搭建
 * 3. 推流本地视频
 * 4. 推流Camera，AudioRecord，实现直播
 * 5. 推流带滤镜，动画的直播（待）
 * 6. ffmpeg解析音频为pcm，ffmpeg能够解析视频，音频，图像，加水印，滤镜，对视频进行截断，生成gif，超级工具箱，需要深入学习
 * 7. ffmpeg解析视频
 */
public class FFmpegIndexActivity extends AppCompatActivity {

    private RecyclerView mRecContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_index);
        mRecContent = (RecyclerView) findViewById(R.id.rec_ffmpeg_index);
        mRecContent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<TabModel> list = new ArrayList<>();
        list.add(new TabModel("1. 对本地视频推流", "使用librtmp，对本地视频进行推流", FlowNativeMp4Activity.class));
        list.add(new TabModel("2. 采集Camera，麦克风数据，直播", "Camera，AudioTrack，librtmp，x264，faac使用", LiveActivity.class));
        list.add(new TabModel("3. FFMpeg解析本地音频为pcm", "FFMpeg解析音频", FFmpegAudioActivity.class));
        list.add(new TabModel("4. FFMpeg解析本地视频", "FFMpeg解析视频", FFmpegVideoActivity.class));
        list.add(new TabModel("5. OpenSL播放音频", "OpenSL播放音频", OpenSLActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
