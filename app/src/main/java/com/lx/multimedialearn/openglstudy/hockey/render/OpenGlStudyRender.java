package com.lx.multimedialearn.openglstudy.hockey.render;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.utils.FileUtils;
import com.lx.multimedialearn.utils.GlUtil;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * 学习使用OpenGL的渲染器, 不加载纹理，代码耦合在一起
 * 1. 声明顶点
 * 2. 编写着色器的glsl文件，加载，编译，链接，运行，就能显示出来桌子，中线，球
 *
 * @author lixiao
 * @since 2017-10-09 16:29
 */
public class OpenGlStudyRender implements GLSurfaceView.Renderer {
    private static final int POSITION_COMPONENT_COUNT = 2;//一个顶点由两个元素构成，即从属性中每次取出来两个值
    private static final int COLOR_COMPONENT_COUNT = 3; //颜色占三个数据
    private static final int BYTES_PER_FLOAT = 4;

    //private static final String U_COLOR = "u_Color"; //片段着色器中(不再需要)

    private static final String A_POSITION = "a_Position"; //顶点着色器中顶点的位置属性
    private static final String A_COLOR = "a_Color"; //顶点着色器中的顶点颜色属性
    private static final String U_MATRIX = "u_Matrix"; //顶点着色器中构建虚拟空间，即传入的点的坐标都会映射到虚拟空间中显示，不做以前的放大了

    private static final int STRIDE = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;

    private FloatBuffer vertexData;
    private Context context;

    int program;
    //  int uColorLocation; (片段着色器中的颜色属性，不再需要，使用平滑着色)
    int aPositionLocation;
    int aColorLocation; //顶点颜色的位置，对顶点进行赋值
    int uMatrixLocation; //做虚拟坐标变换的位置

    private final float[] projectionMatrix = new float[16]; //存储透视投影变换矩阵
    private final float[] modelMatrix = new float[16]; //存储模型矩阵移动


    public OpenGlStudyRender(Context context) {
        this.context = context;
        //这是所有的顶点集合，通过索引拿到当前要使用的顶点数据
        float[] tableVerticesWithTriangles = { //画长方形，需要两个三角形，这里声明三角形的顶点，按照逆时针方向，从左下角开始
//                0f, 0f,   //第一个三角形,这里的坐标系是假想的，OpenGL坐标系是[-1,1]从左到右,[-1,1]从上到下
//                9f, 14f,
//                0f, 14f,
//
//                0f, 0f, //第二个三角形，两个三角形构成了长方形的桌子
//                9f, 0f,
//                9f, 14f,
//
//                0f, 7f, //中线的左右两个坐标点
//                9f, 7f,
//
//                4.5f, 2f, //两个球门的坐标点
//                4.5f, 12f

//                -0.51f, -0.51f, //画桌子边缘
//                0.51f, 0.51f,
//                -0.51f, 0.51f,

//                -0.51f, -0.51f,
//                0.51f, -0.51f,
//                0.51f, 0.51f,

//                -0.5f, -0.5f, //通过绘制两个三角形绘制桌子的长方形
//                0.5f, 0.5f,
//                -0.5f, 0.5f,
//
//                -0.5f, -0.5f,
//                0.5f, -0.5f,
//                0.5f, 0.5f,

                //为了方便着色，让桌子中间亮，四周暗，引入三角扇形，画四个三角形
                //每个顶点有2个值确定位置
//                0f, 0f, //中心点
//                -0.5f, -0.5f, //相邻两个点+中心点构成一个三角形
//                0.5f, -0.5f,
//                0.5f, 0.5f,
//                -0.5f, 0.5f,
//                -0.5f, -0.5f, //为了闭合，最后重复第二个点， 画三角形时，要改GL_TRIANGLE_FAN
//
//                -0.5f, 0f, //中介线
//                0.5f, 0f,
//
//                0f, -0.25f, //球
//                0f, 0.25f,

                //为了方便着色，让桌子中间亮，四周暗，引入三角扇形，画四个三角形
                //给顶点添加颜色属性，进行平滑着色
                //每个顶点由5个值确定位置和当前顶点的颜色，直线通过两个顶点进行平滑着色，三角形根据三角形面积占比进行平滑着色
                0f, 0f, 1f, 1f, 1f, //中心点
                -0.5f, -0.8f, 0.7f, 0.7f, 0.7f, //相邻两个点+中心点构成一个三角形
                0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
                0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
                -0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
                -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,//为了闭合，最后重复第二个点， 画三角形时，要改GL_TRIANGLE_FAN

                -0.5f, 0f, 1f, 0f, 0f,//中介线
                0.5f, 0f, 0f, 0f, 1f,

                0f, -0.4f, 0f, 0f, 1f,//球
                0f, 0.4f, 1f, 0f, 0f
        };

        //OpenGL顶点着色器会拿FloatBuffer中的坐标（每次拿两个（通过设置））进行赋值
        vertexData = GlUtil.createFloatBuffer(tableVerticesWithTriangles); //使用FloatBuffer，实现OpenGL本地空间和DalVik空间的数据交换

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) { // 程序第一次运行时，即GLSurfaceView创建的时候调用，当Activity创建和销毁的时候也可能会回调
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f); //创建的时候设置初始色，R,G,B,A,其中GL10 gl这个参数是历史遗留问题

        //读入顶点着色器和片段着色器
        String vertexShaderSource = FileUtils.readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderSource = FileUtils.readTextFileFromResource(context, R.raw.simple_fragment_shader);

        //对着色器：编译，链接，创建program，运行
        int vertexShader = GlUtil.compileVertexShader(vertexShaderSource); //1. 编译顶点着色器和片段着色器
        int fragmentShader = GlUtil.compileFragmentShader(fragmentShaderSource);
        program = GlUtil.linkProgram(vertexShader, fragmentShader);//2. 链接顶点和片段，顶点着色器知道在哪里画，片段着色器知道线，三角形怎么画，什么颜色，链接后生成program对象
        GlUtil.validateProgram(program); //3.验证程序能不能用
        glUseProgram(program); //4. 告诉OpenGL，在屏幕上绘制内容时，必须使用这里创建的程序
        //uColorLocation = glGetUniformLocation(program, U_COLOR); //5. （平滑着色，不再需要）对program中的变量赋值，数据的绑定，从程序中拿出来变量， 找到颜色属性的位置
        aPositionLocation = glGetAttribLocation(program, A_POSITION); //找到属性的位置
        aColorLocation = glGetAttribLocation(program, A_COLOR); //获取属性中的颜色的位置
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX); //获取虚拟矩阵位置

        //告诉OpenGL去哪里找a_Position的数据，就是按照规格导入顶点位置数据
        glVertexAttribPointer(
                aPositionLocation, //获取位置
                POSITION_COMPONENT_COUNT, //每个顶点需要两个值进行确定，本身是ver4，没有赋值的，前三位为0， 后一位为1
                GL_FLOAT, //类型
                false,  //只有使用整型数据时才有意义
                STRIDE,  // (设置步长，每个步长取位置的两个值)一个数组存储多于一个属性时，才有意义，这里之存储了一个属性，就是顶点位置
                vertexData); // 存储数据的buffer，来这里找数据
        glEnableVertexAttribArray(aPositionLocation); //使用顶点数组

        //告诉OpenGl找a_Color的数据
        vertexData.position(POSITION_COMPONENT_COUNT); //向后移动两个位置，读取颜色值
        glVertexAttribPointer(
                aColorLocation,
                COLOR_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                STRIDE,
                vertexData);
        glEnableVertexAttribArray(aColorLocation);


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
                45f, //45度的视野
                (float) width / (float) height,
                1f, //视椎体的z值从-1到-10
                10f); //做透视投影矩阵变换，改变视角查看

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -2.6f); //在z轴上平移-2，需要把平移矩阵和投影矩阵相乘融入到投影矩阵中，是画图生效
        Matrix.rotateM(modelMatrix, 0, -60f, 1f, 0f, 0f); //绕x轴反向旋转60度

        float[] temp = new float[16];
        Matrix.multiplyMM(temp, 0, projectionMatrix, 0, modelMatrix, 0); //矩阵相乘，生成新的矩阵
        System.arraycopy(temp, 0, projectionMatrix, 0, temp.length);

    }

    @Override
    public void onDrawFrame(GL10 gl) { // 根据设备的刷新频率，进行更新调用，也可以使用setRenderMode(),在需要的时候进行渲染
        glClear(GL_COLOR_BUFFER_BIT); //清空屏幕，使用onSurfaceCreated中设置的初始色填充屏幕
        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0); //（正交投影）传递矩阵给着色器，着色器生成虚拟空间
        //在确定了程序，位置，绑定了数据后，进行绘制
//        glUniform4f(uColorLocation, 0.0f, 1.0f, 0.0f, 1.0f);      //绘制桌子边缘
//        glDrawArrays(GL_TRIANGLES, 0, 6);

//        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f); //(使用顶点的颜色属性进行赋值，进行平滑着色，这里不再需要)绘制桌子，对片段着色器变量的颜色进行赋值，颜色有红，绿，蓝，阿尔法四个分量
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6); //使用Buffer中从0-5六个顶点绘制三角形，每个顶点两个变量

//        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f); //绘制分割线
        glDrawArrays(GL_LINES, 6, 2);

//        glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f); //绘制点
        glDrawArrays(GL_POINTS, 8, 1);

//        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f); //绘制点
        glDrawArrays(GL_POINTS, 9, 1);
    }
}
