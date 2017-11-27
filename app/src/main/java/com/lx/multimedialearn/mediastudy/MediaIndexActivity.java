package com.lx.multimedialearn.mediastudy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;
import com.lx.multimedialearn.mediastudy.audio.AudioRecorderActivity;
import com.lx.multimedialearn.mediastudy.mediarecord.MediaRecordActivity;
import com.lx.multimedialearn.mediastudy.videoplayer.VideoPlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 音视频处理导航页
 * 1. MediaPlayer,MediaRecord,AudioRecoder最上层，硬编码，支持格式有限
 * 2. MediaCodec,MediaMuxer，4.3之后，硬编码，能够单独对音视频进行处理，设置对应格式
 * 3. FFMpeg，OpenSL，OpenGL，是对获取的数据做编码，封装处理，软编码，4.3之前建议使用软编码
 * 4. FFMpeg,OpenGL对视频加滤镜，美颜后：
 * （1）存储到本地： 使用MediaCodec进行编码，MediaMuxer进行音视频的合并，存到本地，也可以使用JNI存储到本地
 * （2）推流：FFMpeg编码为x264格式使用libRTMP推流，OpenGL使用PBO，读取到处理后的RGBA，交给FFMpeg进行推流
 * 5. 屏幕录制，推流，ImageReader
 * 6. 本地视频处理，剪裁，滤镜，处理后保存
 * 7. 录制小视频，滤镜，水印，保存
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
        list.add(new TabModel("2. MediaRecord，MediaPlayer使用-mediarecord", "MediaRecord结合Camera录制音视频，使用MediaPlayer进行播放", MediaRecordActivity.class));
        list.add(new TabModel("3. AudioRecord，AudioTrack使用-audio", "使用AudioRecorder录制音频，可以单独处理声音，AudioTrack解析PCM播放声音", AudioRecorderActivity.class));
        list.add(new TabModel("4. 硬编码学习-hardcode", "硬编码AudioRecoder,MediaCodec,MediaMuxer使用", VideoPlayerActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
