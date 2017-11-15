package com.lx.multimedialearn.openglstudy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.main.MainAdapter;
import com.lx.multimedialearn.main.TabModel;
import com.lx.multimedialearn.openglstudy.animation.AnimIndexActivity;
import com.lx.multimedialearn.openglstudy.fbo.FBOStudyActivity;
import com.lx.multimedialearn.openglstudy.image.ImageProcessActivity;
import com.lx.multimedialearn.openglstudy.obj.ObjModelActivity;
import com.lx.multimedialearn.openglstudy.stl.STLModelActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Opengl学习目录
 */
public class OpenGLIndexActivity extends AppCompatActivity {

    private RecyclerView mRecContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_glindex);
        mRecContent = (RecyclerView) findViewById(R.id.rec_opengl_index);
        mRecContent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        List<TabModel> list = new ArrayList<>();
        list.add(new TabModel("1. 冰球游戏-data,program,render", "基本图形绘制，基本概念学习，触控，线性代数等", OpenGLStudyActivity.class));
        list.add(new TabModel("2. 粒子喷泉-data,program,render", "粒子系统，天空盒，动态壁纸，陀螺仪，光照，高度图等", ParticlesActivity.class));
        list.add(new TabModel("3. 图片处理-image", "OpenGL处理图片，色调，放大，虚化等", ImageProcessActivity.class));
        list.add(new TabModel("4. 离屏渲染-fbo", "OpenGL FBO等特性，保存EGL上下文", FBOStudyActivity.class));
        list.add(new TabModel("5. 逐帧动画渲染-animation", "OpenGL 解析ETC，逐帧渲染动画", AnimIndexActivity.class));
        list.add(new TabModel("6. STL模型数据加载-stl", "解析stl模型数据，加载3D图形", STLModelActivity.class));
        list.add(new TabModel("7. OBJ 3D模型加载-obj", "解析Obj模型数据，加载3D图形", ObjModelActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
