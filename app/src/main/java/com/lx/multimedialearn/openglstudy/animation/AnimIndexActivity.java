package com.lx.multimedialearn.openglstudy.animation;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;
import com.lx.multimedialearn.openglstudy.animation.cameraetc.AnimOnCameraActivity;
import com.lx.multimedialearn.openglstudy.animation.loadetc.LoadETCActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载etc动画，渲染动画
 * 1. 如何加载：http://blog.csdn.net/junzia/article/details/53872303
 * 2. gpu纹理压缩模式：https://www.cnblogs.com/luming1979/archive/2013/02/04/2891421.html
 * 3. 骨骼动画加载方式：http://blog.csdn.net/n5/article/details/3105872，有点老但很好的文章
 */
public class AnimIndexActivity extends AppCompatActivity {
    private RecyclerView mRecIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anim_index);
        mRecIndex = (RecyclerView) findViewById(R.id.rec_anim_index);
        mRecIndex.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<TabModel> list = new ArrayList<>();
        list.add(new TabModel("1. ETC1加载动画", "使用ETC加载etc标准的zip包", LoadETCActivity.class));
        list.add(new TabModel("2. 在相机上加载动画", "Camera使用ETC加载etc标准的zip包", AnimOnCameraActivity.class));
        mRecIndex.setAdapter(new MainAdapter(list));
        //test
    }
}
