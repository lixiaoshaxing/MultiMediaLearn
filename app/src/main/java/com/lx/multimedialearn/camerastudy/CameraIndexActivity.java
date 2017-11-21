package com.lx.multimedialearn.camerastudy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;

import java.util.ArrayList;
import java.util.List;

public class CameraIndexActivity extends AppCompatActivity {

    private RecyclerView mRecContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_index);
        mRecContent = (RecyclerView) findViewById(R.id.rec_camera_index);
        mRecContent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<TabModel> list = new ArrayList<>();
        list.add(new TabModel("1. Camera使用", "Camera结合SurfaceView，GLSurfaceView，TextureView使用", CameraUseActivity.class));
        list.add(new TabModel("2. Camera数据共享", "Camera数据yuv->rgb，SurfaceView，GLSurfaceView，TextureView共享", CameraShareActivity.class));
        list.add(new TabModel("3. 多滤镜同时预览", "Camera数据结合GLSurfaceView实现同时预览多重滤镜", FourGLSurfaceViewActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
