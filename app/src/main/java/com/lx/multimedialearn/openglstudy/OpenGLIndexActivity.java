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
import com.lx.multimedialearn.openglstudy.hockey.HockeyActivity;
import com.lx.multimedialearn.openglstudy.image.ImageProcessActivity;
import com.lx.multimedialearn.openglstudy.obj.ObjModelActivity;
import com.lx.multimedialearn.openglstudy.obj.ObjMtlActivity;
import com.lx.multimedialearn.openglstudy.particles.ParticlesActivity;
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
        list.add(new TabModel("1. 冰球游戏", "基本图形绘制，基本概念学习，触控，线性代数等", HockeyActivity.class));
        list.add(new TabModel("2. 粒子喷泉", "粒子系统，天空盒，动态壁纸，陀螺仪，光照，高度图等", ParticlesActivity.class));
        list.add(new TabModel("3. 图片处理", "OpenGL处理图片，色调，放大，虚化等", ImageProcessActivity.class));
        list.add(new TabModel("4. 离屏渲染", "OpenGL FBO等特性，保存EGL上下文", FBOStudyActivity.class));
        list.add(new TabModel("5. OpenGL与相机结合，动画，滤镜，美颜，水印", "OpenGL 解析ETC，逐帧渲染动画，滤镜，美颜，水印", AnimIndexActivity.class));
        list.add(new TabModel("6. STL模型数据加载", "解析stl模型数据，加载3D图形", STLModelActivity.class));
        list.add(new TabModel("7. OBJ 3D模型加载", "解析Obj模型数据，加载3D图形", ObjModelActivity.class));
        list.add(new TabModel("8. OBJ-MTL 3D模型加载", "解析Obj模型数据，Mtl加载光照，纹理等材质，加载3D图形", ObjMtlActivity.class));
        mRecContent.setAdapter(new MainAdapter(list));
    }
}
