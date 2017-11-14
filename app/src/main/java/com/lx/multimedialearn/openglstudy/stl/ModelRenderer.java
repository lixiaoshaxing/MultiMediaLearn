package com.lx.multimedialearn.openglstudy.stl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

import com.lx.multimedialearn.utils.GlUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 加载stl文件，并展示出来
 *
 * @author lixiao
 * @since 2017-09-04 15:53
 */
public class ModelRenderer implements GLSurfaceView.Renderer {
    // private Model mModel;

    private float mDegree = 0; //旋转
    private float mScalef = 1; //缩放

    private Point mCenterPoint; //中心点

    private Point eye = new Point(0, 0, -10); //眼睛坐标
    private Point up = new Point(0, -1, 0);
    private Point center = new Point(0, 0, 0);

    private Context mContext;

    private List<Model> models = new ArrayList<>(); //描述3d图像的纹理，分为多个角度的纹理，循环描述

    public ModelRenderer(Context context) {
        STLReader stlReader = new STLReader();
        mContext = context;
        for (int i = 1; i <= 6; i++) {
            Model model = stlReader.parseStlWithTextureFromAsset(context, "chuwang/" + i);
            models.add(model);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glClearColor(0f, 0f, 0f, 0f);
        //设置深度缓存值
        gl.glClearDepthf(1.0f);
        //设置深度缓存比较函数
        gl.glDepthFunc(GL10.GL_LEQUAL);
        //设置阴影模式
        gl.glShadeModel(GL10.GL_SMOOTH);
        //  float r = mModel.getR();
        //r是半径，不是直径，因此用0.5/r可以算出放缩比例
        //   mScalef = 0.5f / r;
        //    mCenterPoint = mModel.getCenterPoint();

        //打开0号灯光， 使用纹理，就不需要打开灯光和材料属性
        //openLight(gl);
        //设置物体的材料属性
        //enableMaterial(gl);
        //设置纹理数据
        initConfigData(gl);
    }


    float[] ambient = {0.9f, 0.9f, 0.9f, 1.0f,}; //环境光：环境颜色（黑色）
    float[] diffuse = {0.5f, 0.5f, 0.5f, 1.0f,}; //漫反射：物体本身颜色
    float[] specular = {1.0f, 1.0f, 1.0f, 1.0f,}; //镜面反射：灯光颜色
    float[] lightPosition = {0.5f, 0.5f, 0.5f, 0.0f,}; //灯光位置

    /**
     * 打开灯光
     *
     * @param gl
     */
    private void openLight(GL10 gl) {
        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);

        //设置0号灯光的参数
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, GlUtil.createFloatBuffer(ambient));
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, GlUtil.createFloatBuffer(diffuse));
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, GlUtil.createFloatBuffer(specular));
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, GlUtil.createFloatBuffer(lightPosition));
    }


    float[] materialAmb = {0.4f, 0.4f, 1.0f, 1.0f};
    float[] materialDiff = {0.0f, 0.0f, 1.0f, 1.0f};//漫反射设置蓝色
    float[] materialSpec = {1.0f, 0.5f, 0.0f, 1.0f};

    /**
     * 设置物体的材料属性，当不同的材料，打上去灯光时，颜色反馈是不一样的
     *
     * @param gl
     */
    private void enableMaterial(GL10 gl) {
        //第一个参数是固定的，android只能使用这一个参数，第二个参数指明要处理的
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, GlUtil.createFloatBuffer(materialAmb)); //环境
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, GlUtil.createFloatBuffer(materialDiff)); //漫反射
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, GlUtil.createFloatBuffer(materialSpec)); //镜面反射
    }


    //纹理pxy文件，图片，stl文件进行绑定
    private void initConfigData(GL10 gl) {
        float r = STLUtils.getR(models);
        mScalef = 0.5f;
        mCenterPoint = STLUtils.getCenter(models);

        //绑定纹理
        for (Model model : models) {
            loadTexture(gl, model, true);
        }
    }

    /**
     * @param gl
     * @param model
     * @param isAssets
     */
    private void loadTexture(GL10 gl, Model model, boolean isAssets) {
        Bitmap bitmap = null;
        try {
            if (isAssets) {
                bitmap = BitmapFactory.decodeStream(mContext.getAssets().open(model.getPictureName()));
            } else {
                //从sd卡中读取图片
                bitmap = BitmapFactory.decodeFile(model.getPictureName());
            }
            // 生成一个纹理对象，将其id保存到成员变量texture中
            int[] textures = new int[1];

            gl.glGenTextures(1, textures, 0);
            Log.i("sys.out", "textures id:" + Arrays.toString(textures));
            model.setTextureIds(textures);

            //将生成的空纹理绑定到当前的2d纹理通道
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

            //设置2D纹理通道当前绑定的纹理的属性
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            //gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

            // 将bitmap应用到2D纹理通道当前绑定的纹理中
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置场景大小
        gl.glViewport(0, 0, width, height);
        //设置投影矩阵
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        //设置透视范围
        GLU.gluPerspective(gl, 45.0f, (float) width / height, 1f, 100f);

        //设置画图所需要的矩阵模型
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //清除屏幕和深度缓存
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        //眼睛对着原点看
        GLU.gluLookAt(gl, eye.x, eye.y, eye.z, center.x, center.y, center.z, up.x, up.y, up.z);

        //模型根据角度的变化旋转
        gl.glRotatef(mDegree, 0, 1, 0);

        //缩放
        gl.glScalef(mScalef, mScalef, mScalef);
        //移动到中心点
        gl.glTranslatef(-mCenterPoint.x, -mCenterPoint.y, -mCenterPoint.z);

        /*****************真正的绘画过程****************************/
        for (Model model : models) { //循环遍历绘制所有的model
            // 开启贴纹理功能
            gl.glEnable(GL10.GL_TEXTURE_2D);
            //根据id绑定对应的纹理
            gl.glBindTexture(GL10.GL_TEXTURE_2D, model.getTextureIds()[0]);
            //通过设置三角形顶点，法向量，属性，绘画
            //允许给每个顶点设置法向量
            gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
            //允许设置顶点
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);


            //设置法向量
            gl.glNormalPointer(GL10.GL_FLOAT, 0, model.getVnormBuffer());
            //设置三角形
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, model.getVertBuffer());
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, model.getTextureBuffer());

            //绘制三角形
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, model.getFacetCount() * 3);

            //关闭当前模型贴纹理，即将纹理id设置为0
            gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);

            gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glDisable(GL10.GL_TEXTURE_2D);
        }
    }

    public void rotate(float degree) {
        mDegree = degree;
    }

    public void setScale(float scale) {
        mScalef = scale;
    }
}
