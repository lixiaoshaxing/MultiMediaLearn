package com.lx.multimedialearn.openglstudy.hockey.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.openglstudy.hockey.data.Geometry;
import com.lx.multimedialearn.openglstudy.hockey.data.Mallet2;
import com.lx.multimedialearn.openglstudy.hockey.data.Puck;
import com.lx.multimedialearn.openglstudy.hockey.data.Table;
import com.lx.multimedialearn.openglstudy.particles.program.ColorShaderProgram;
import com.lx.multimedialearn.openglstudy.particles.program.TextureShaderProgram;
import com.lx.multimedialearn.utils.GlUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;

/**
 * 可以渲染纹理，对代码解耦，增加灵活性
 * 1. 声明顶点
 * 2. 编写着色器的glsl文件，加载，编译，链接，运行，就能显示出来桌子，中线，球
 *
 * @author lixiao
 * @since 2017-10-09 16:29
 */
public class OpenGlStudyRender2 implements GLSurfaceView.Renderer {

    private Context context;

    private final float[] projectionMatrix = new float[16]; //存储透视投影变换矩阵
    private final float[] modelMatrix = new float[16]; //存储模型矩阵移动

    //添加视图矩阵
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private Table table;
    private Mallet2 mallet;
    private Puck puck;

    private TextureShaderProgram textureShaderProgram;
    private ColorShaderProgram colorShaderProgram;

    private int texture;

    private boolean malletPressed = false; //木槌是否被按
    private Geometry.Point blueMalletPosition; //蓝色木槌的位置

    //三维->二维，通过透视投影和透视除法进行变换，现在需要反转矩阵，取消透视矩阵变换的效果，把归一化的点从二维变化为三维，生成射线
    private float[] invertedViewProjectionMatrix = new float[16];

    //定义桌子的边界
    private final float leftBound = -0.5f;
    private final float rightBound = 0.5f;
    private final float farBound = -0.8f;
    private final float nearBound = 0.8f; //考虑中间挡板为0f

    //根据木槌的原始位置，和当前位置构建向量，判断速度和方向，增加冰球的速度和方向
    private Geometry.Point previousBlurMalletPosition; //上一次木槌的位置
    private Geometry.Point puckPostion; //冰球的位置
    private Geometry.Vector puckVector; //冰球的方向


    public OpenGlStudyRender2(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) { // 程序第一次运行时，即GLSurfaceView创建的时候调用，当Activity创建和销毁的时候也可能会回调
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f); //创建的时候设置初始色，R,G,B,A,其中GL10 gl这个参数是历史遗留问题

        table = new Table();
        mallet = new Mallet2(0.08f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);

        textureShaderProgram = new TextureShaderProgram(context);
        colorShaderProgram = new ColorShaderProgram(context);

        int[] temp = GlUtil.loadTexture(context, R.drawable.air_hockey_surface);
        texture = temp[0];
        //设置蓝色木槌的默认位置
        blueMalletPosition = new Geometry.Point(0f, mallet.height / 2f, 0.4f);
        puckPostion = new Geometry.Point(0f, puck.height / 2f, 0f); //初始化冰球位置
        puckVector = new Geometry.Vector(0f, 0f, 0f); //初始化方向

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) { // GLSurfaceView尺寸发生改变的时候回调，例如横竖屏切换
        glViewport(0, 0, width, height); //当尺寸发生改变时，进行窗口大小的设置
        //生成正交映射矩阵，对虚拟坐标系进行赋值
//        float aspectRatio = width > height ? ((float) width / (float) height) : ((float) height / (float) width);
//        if (width > height) { //横屏，对宽度做放大，生成虚拟空间
//            orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f);
//        } else {
//            orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f);
//        }
        Matrix.perspectiveM(
                projectionMatrix,  //存储变换矩阵
                0,
                48f, //45度的视野
                (float) width / (float) height,
                1f, //视椎体的z值从-1到-10
                10f); //做透视投影矩阵变换，改变视角查看
//
//        Matrix.setIdentityM(modelMatrix, 0);
//        Matrix.translateM(modelMatrix, 0, 0f, 0f, -2.6f); //在z轴上平移-2，需要把平移矩阵和投影矩阵相乘融入到投影矩阵中，是画图生效
//        Matrix.rotateM(modelMatrix, 0, -60f, 1f, 0f, 0f); //绕x轴反向旋转60度
//
//        float[] temp = new float[16];
//        Matrix.multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0); //矩阵相乘，生成新的矩阵
//        System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);

        Matrix.setLookAtM( // 创建视图矩阵
                viewMatrix,
                0,
                0f, 1.2f, 2.2f, //眼睛所在的位置
                0f, 0f, 0f, //眼睛正在看的位置，这里是物体的中心
                0f, 1f, 0f //头指向的位置，y轴为1，即为头向上
        );
    }

    @Override
    public void onDrawFrame(GL10 gl) { // 根据设备的刷新频率，进行更新调用，也可以使用setRenderMode(),在需要的时候进行渲染
        glClear(GL_COLOR_BUFFER_BIT); //清空屏幕，使用onSurfaceCreated中设置的初始色填充屏幕
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        //对反转矩阵进行赋值
        Matrix.invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        //画桌子
        positionTableInScene();
        textureShaderProgram.useProgram();
        textureShaderProgram.setUniforms(modelViewProjectionMatrix, texture);
        table.bindData(textureShaderProgram);
        table.draw();  //这里顺序很重要

        //画木槌1
        positionObjectInScene(0f, mallet.height / 2f, -0.4f);
        colorShaderProgram.useProgram();
        colorShaderProgram.setUniforms(modelViewProjectionMatrix, 1f, 0f, 0f);
        mallet.bindData(colorShaderProgram);
        mallet.draw();


        //画木槌2
        positionObjectInScene(blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z);
        colorShaderProgram.setUniforms(modelViewProjectionMatrix, 0f, 0f, 1f);
        mallet.draw();

        //画冰球
        puckPostion = puckPostion.translate(puckVector); //冰球根据方向向量进行偏移
        //冰球在撞到边界后需要反弹
        if (puckPostion.x < leftBound + puck.radius
                || puckPostion.x > rightBound - puck.radius) {
            puckVector = new Geometry.Vector(-puckVector.x, puckVector.y, puckVector.z); //向左右运动，如果超出边界，就反转向量x轴
            puckVector = puckVector.scale(0.9f);
        }
        if (puckPostion.z < farBound + puck.radius
                || puckPostion.z > nearBound - puck.radius) { //向上下运动，碰到上下边界，z轴表示远近
            puckVector = new Geometry.Vector(puckVector.x, puckVector.y, -puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }
        puckPostion = new Geometry.Point(
                clamp(puckPostion.x, leftBound + puck.radius, rightBound - puck.radius),  //控制在桌子范围内
                puckPostion.y
                , clamp(puckPostion.z, farBound + puck.radius, nearBound - puck.radius));
        puckVector = puckVector.scale(0.99f); //加入摩擦系数，让冰球逐渐减速
        positionObjectInScene(puckPostion.x, puckPostion.y, puckPostion.z); //根据当前位置，刷新冰球的位置
        colorShaderProgram.setUniforms(modelViewProjectionMatrix, 0.8f, 0.8f, 1f);
        puck.bindData(colorShaderProgram);
        puck.draw();
    }

    private void positionTableInScene() {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f); //旋转90度，平放在地上，不需要移动位置，因为视图矩阵已经处理了
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    private void positionObjectInScene(float x, float y, float z) {
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, x, y, z); //木槌和冰球是放在x-z平面上，不需要旋转，根据传进来的参数平移它们，放在正确的位置
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }


    /**
     * 手指按下处理
     * 1. 手指按下坐标的映射
     * 2. 判断是否和3D中的球体（木槌）进行相交测试
     *
     * @param normalizedX
     * @param normalizedY
     */
    public void handleTouchPress(float normalizedX, float normalizedY) {
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);

        Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(
                new Geometry.Point(
                        blueMalletPosition.x
                        , blueMalletPosition.y
                        , blueMalletPosition.z),
                mallet.height / 2); //对木槌构建三维空间中的圆球体（为了简单，定位圆球）
        malletPressed = Geometry.intersects(malletBoundingSphere, ray);
    }

    /**
     * 手指移动，木槌跟随移动
     *
     * @param normalizedX
     * @param normalizedY
     */
    public void handleTouchDrag(float normalizedX, float normalizedY) {
        if (malletPressed) { //如果按到了木槌区域
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            Geometry.Plane plane = new Geometry.Plane(new Geometry.Point(0, 0, 0), new Geometry.Vector(0, 1, 0));
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            previousBlurMalletPosition = blueMalletPosition; //赋值前一个位置
            blueMalletPosition = new Geometry.Point(
                    clamp(touchedPoint.x, leftBound + mallet.radius, rightBound - mallet.radius), //考虑半径，设置左右边界
                    mallet.height / 2f,
                    clamp(touchedPoint.z, 0f + mallet.radius, nearBound - mallet.radius)); //考虑边境，中线到下边界
            //判断冰球和木槌是否相撞，计算两个圆心点的距离，小于半径的和，就是碰撞了
            float distance = Geometry.vectorBetween(blueMalletPosition, puckPostion).length();
            if (distance < (puck.radius + mallet.radius)) {
                puckVector = Geometry.vectorBetween(previousBlurMalletPosition, blueMalletPosition); //方向向量，如果点隔得越远，向量越大，速度越大
            }
        }
    }


    /**
     * 控制点在桌子范围内
     *
     * @param value
     * @param min
     * @param max
     * @return
     */
    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    /**
     * 把二维平面的一个点映射到三维中的射线
     *
     * @return
     */
    public Geometry.Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {
        //定三维平面的两个点
        float[] nearPointNdc = {normalizedX, normalizedY, -1, 1}; //归一化设备上的坐标点，w分量设置为1
        float[] farPointNdc = {normalizedX, normalizedY, 1, 1};

        float[] nearPointWorld = new float[4]; //计算世界坐标系中点的坐标
        float[] farPointWorld = new float[4];

        //使用反转矩阵生成三维空间中的点
        Matrix.multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        Matrix.multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

        //除以w消除透视除法的影响，透视除法能够得到并行轨道从近像远看的立体效果
        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        //生成两个点，构建射线
        Geometry.Point nearPointRay = new Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay = new Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

        //这是这条线的所有数据，通过这些数据和圆球的中心点计算是否相交
        return new Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay));
    }

    /**
     * 除以w，消除透视除法的影响
     *
     * @param vector
     */
    public void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }
}
