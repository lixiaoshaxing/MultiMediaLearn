package com.lx.multimedialearn.openglstudy.xbo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;
import com.lx.multimedialearn.openglstudy.xbo.fbo.FBOStudyActivity;
import com.lx.multimedialearn.openglstudy.xbo.vbo.VBOActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * vbo，pbo，fbo使用，都是为了提高效率的特性
 */
public class XboIndexActivity extends AppCompatActivity {
    private RecyclerView mRecContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xbo_index);
        mRecContent = (RecyclerView) findViewById(R.id.rec_xbo_index);
        mRecContent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<TabModel> list = new ArrayList<>();
        list.add(new TabModel("1. VBO使用", "VBO GPU中开辟空间缓存顶点数据，减少读取内存占用CPU时间", VBOActivity.class));
        list.add(new TabModel("2. PBO使用", "GPU读取纹理，减少占用DMA时间", FBOStudyActivity.class));
        list.add(new TabModel("3. FBO使用", "FBO离屏渲染，双缓冲，EGL上下文，EGLSurfaceView，多重滤镜", FBOStudyActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
