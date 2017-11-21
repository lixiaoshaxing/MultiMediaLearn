package com.lx.multimedialearn.openglstudy.particles.render;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.openglstudy.hockey.data.Geometry;
import com.lx.multimedialearn.openglstudy.particles.data.HeightMap;
import com.lx.multimedialearn.openglstudy.particles.data.ParticleShooter;
import com.lx.multimedialearn.openglstudy.particles.data.ParticleSystem;
import com.lx.multimedialearn.openglstudy.particles.data.SkyBox;
import com.lx.multimedialearn.openglstudy.particles.program.HeightmapShaderProgram;
import com.lx.multimedialearn.openglstudy.particles.program.ParticleShaderProgram;
import com.lx.multimedialearn.openglstudy.particles.program.SkyBoxShaderProgram;
import com.lx.multimedialearn.utils.GlUtil;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.scaleM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.transposeM;
import static com.lx.multimedialearn.R.drawable.heightmap;

/**
 * 粒子系统渲染器
 *
 * @author lixiao
 * @since 2017-10-18 21:12
 */
public class ParticlesRender implements GLSurfaceView.Renderer {

    private Context context;

    //画高度图
    private float[] modelMatrix = new float[16];
    private float[] tempMatrix = new float[16];
    private float[] modelViewProjectionMatrix = new float[16];

    //加入点光源，使用如下矩阵替换
    private float[] modelViewMatrix = new float[16];
    private float[] it_modelViewMatrix = new float[16];

    private float[] viewMatrixForSkybox = new float[16];

    private float[] projectionMatrix = new float[16]; //做投影变换
    private float[] viewMatrix = new float[16];
    private float[] viewProjectionMatrix = new float[16];

    private ParticleShaderProgram particleShaderProgram;
    private ParticleSystem particleSystem;
    private ParticleShooter redParticleShooter;
    private ParticleShooter greenParticleShooter;
    private ParticleShooter blueParticleShooter;
    private long globalStartTime; //开始的时间

    private float angleVarianceInDegree = 5f; //粒子喷泉的方向
    private float speedVariance = 1f;

    private int particleTexture; //加载纹理

    private SkyBoxShaderProgram skyboxProgram;
    private SkyBox skybox;
    private int skyboxTexture;

    //高度图
    private HeightmapShaderProgram heightmapProgram;
    private HeightMap heightMap;
    //定义光照（太阳）的方向，获取太阳位置可以通过打日志，获取，相当于方向光
    //private final Geometry.Vector vectorToLight = new Geometry.Vector(0.61f, 0.64f, -0.47f).normalize(); //白天
    //private final Geometry.Vector vectorToLight = new Geometry.Vector(0.30f, 0.35f, -0.89f).normalize(); //黑夜中月亮的位置
    private float[] vectorToLight = {0.30f, 0.35f, -0.89f, 0f};
    private float[] pointLightPositions = new float[]{
            -1f, 1f, 0f, 1f,
            0f, 1f, 0f, 1f,
            1f, 1f, 0f, 1f};
    private float[] pointLightColors = new float[]{
            1.00f, 0.20f, 0.02f,
            0.02f, 0.25f, 0.02f,
            0.02f, 0.20f, 1.00f
    };


    private float xRoatation, yRotation; //旋转角度，绕x轴上下看，绕y轴左右看


    public ParticlesRender(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); //设置黑色背景
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); //执行深度测试
        GLES20.glEnable(GLES20.GL_CULL_FACE); //关闭两面绘制，减少开销
        particleShaderProgram = new ParticleShaderProgram(context);
        particleSystem = new ParticleSystem(10000);
        globalStartTime = System.nanoTime();

        Geometry.Vector particleDirector = new Geometry.Vector(0f, 0.5f, 0f);
        redParticleShooter = new ParticleShooter(
                new Geometry.Point(-1f, 0f, 0f),
                particleDirector, Color.rgb(255, 50, 5), angleVarianceInDegree, speedVariance //增加5度的偏移角度
        );
        greenParticleShooter = new ParticleShooter(
                new Geometry.Point(0f, 0f, 0f),
                particleDirector, Color.rgb(25, 255, 25), angleVarianceInDegree, speedVariance);
        blueParticleShooter = new ParticleShooter(
                new Geometry.Point(1f, 0f, 0f),
                particleDirector, Color.rgb(5, 50, 255), angleVarianceInDegree, speedVariance);

        int[] temp = GlUtil.loadTexture(context, R.drawable.particle_texture);
        particleTexture = temp[0];

        //初始化天空图
        skyboxProgram = new SkyBoxShaderProgram(context);
        skybox = new SkyBox();
        skyboxTexture = GlUtil.loadCubeMap(context, new int[]{
//                R.drawable.left, R.drawable.right, //设置的白天
//                R.drawable.bottom, R.drawable.top,
//                R.drawable.front, R.drawable.back

                R.drawable.night_left, R.drawable.night_right,
                R.drawable.night_bottom, R.drawable.night_top,
                R.drawable.night_front, R.drawable.night_back
        });

        heightmapProgram = new HeightmapShaderProgram(context);
        heightMap = new HeightMap(((BitmapDrawable)
                context.getResources().getDrawable(heightmap))
                .getBitmap());

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        Matrix.perspectiveM(projectionMatrix, 0, 45f, (float) width / (float) height, 1f, 100f); //给投影矩阵更大的空间
        //加入天空后， 粒子的矩阵变换应该单独处理， 天空的矩阵变换也单独处理

        updateViewMatrices();
    }

    private long frameStartTimeMs;

    private void limitFrameRate(int framesPerSecond) {
        long elapsedFrameTimems = SystemClock.elapsedRealtime() - frameStartTimeMs;
        long expectedFrameTimes = 1000 / framesPerSecond; //期待每帧的时间
        long timeToSleepMs = expectedFrameTimes - elapsedFrameTimems; //过去了多长时间
        if (timeToSleepMs > 0) { //如果还没有到设定间隔
            SystemClock.sleep(timeToSleepMs);
        }
        frameStartTimeMs = SystemClock.elapsedRealtime();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //控制时间，限制帧率
        limitFrameRate(40);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        drawHeightmap();
        drawSkyBox();
        drawParticles();

    }

    private void updateViewMatrices() {
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        Matrix.rotateM(viewMatrix, 0, -xRoatation, 0f, 1f, 0f);
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.length);
        Matrix.translateM(viewMatrix, 0, 0f - xOffset, -1.5f - yOffset, -5f); //这里最后一个参数为15，可以从里边看，说明opengl是两面绘制的
    }


    private void updateMvpMatrix() {
//        multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0);
//        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
        multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        invertM(tempMatrix, 0, modelViewMatrix, 0);
        transposeM(it_modelViewMatrix, 0, tempMatrix, 0);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);
    }

    private void updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }

    /**
     * 处理触摸
     */
    public void handleTouchDrag(float deltaX, float deltaY) {
        xRoatation += deltaX / 16f;
        yRotation += deltaY / 16f; //降低灵敏度，累加，然后坐对应的矩阵变化

        if (yRotation < -90) {
            yRotation = -90;
        } else if (yRotation > 90) {
            yRotation = 90;
        }

        updateViewMatrices();
    }

    private float xOffset, yOffset; //动态壁纸使用

    public void handleOffsetsChanged(float xOffset, float yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        updateViewMatrices();
    }

    float previousX;
    float previousY;

    /**
     * 处理重力加速仪
     *
     * @param xRoatation
     * @param yRoatation
     */
    public void handleSensor(float xRoatation, float yRoatation) {
        float x = xRoatation;
        float y = yRoatation;
        float dx = x - previousX;
        float dy = y - previousY;

        this.xRoatation += dx * 2.0f;
        this.yRotation += dy * 0.5f;

        if (this.yRotation < -90) {
            this.yRotation = -90;
        } else if (this.yRotation > 90) {
            this.yRotation = 90;
        }
        previousX = xRoatation;
        previousY = yRoatation;

        updateViewMatrices();
    }

    private void drawHeightmap() {
//        setIdentityM(modelMatrix, 0);
//        scaleM(modelMatrix, 0, 100f, 10f, 100f);
//        updateMvpMatrix();
//        heightmapProgram.useProgram();
//        heightmapProgram.setUniforms(modelViewProjectionMatrix, vectorToLight); //加入光照方向
//        heightMap.bindData(heightmapProgram);
//        heightMap.draw();

        //加入单点光源后进行替换
        setIdentityM(modelMatrix, 0);

        scaleM(modelMatrix, 0, 100f, 10f, 100f);
        updateMvpMatrix();

        heightmapProgram.useProgram();

        final float[] vectorToLightInEyeSpace = new float[4];
        final float[] pointPositionsInEyeSpace = new float[12];
        multiplyMV(vectorToLightInEyeSpace, 0, viewMatrix, 0, vectorToLight, 0);
        multiplyMV(pointPositionsInEyeSpace, 0, viewMatrix, 0, pointLightPositions, 0);
        multiplyMV(pointPositionsInEyeSpace, 4, viewMatrix, 0, pointLightPositions, 4);
        multiplyMV(pointPositionsInEyeSpace, 8, viewMatrix, 0, pointLightPositions, 8);

        heightmapProgram.setUniforms(modelViewMatrix, it_modelViewMatrix,
                modelViewProjectionMatrix, vectorToLightInEyeSpace,
                pointPositionsInEyeSpace, pointLightColors);
        heightMap.bindData(heightmapProgram);
        heightMap.draw();
    }

    private void drawSkyBox() {
        //Matrix.setIdentityM(viewMatrix, 0); //这里公用了viewmatrix，会不会出问题？
        //Matrix.rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f); //绕x轴旋转y的角度
        //Matrix.rotateM(viewMatrix, 0, -xRoatation, 0f, 1f, 0f); //绕y轴旋转x的角度
        //multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        //调整深度测试算法
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();
        skyboxProgram.useProgram();
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture);
        skybox.bindData(skyboxProgram);
        skybox.draw();

        GLES20.glDepthFunc(GLES20.GL_LESS);
    }

    private void drawParticles() {
        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;
        redParticleShooter.addParticles(particleSystem, currentTime, 5);
        greenParticleShooter.addParticles(particleSystem, currentTime, 5);
        blueParticleShooter.addParticles(particleSystem, currentTime, 5);

        //Matrix.setIdentityM(viewMatrix, 0);
        //Matrix.rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        //Matrix.rotateM(viewMatrix, 0, -xRoatation, 0f, 1f, 0f);
        //Matrix.translateM(viewMatrix, 0, 0f, -1.5f, -5f);
        //multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);


        setIdentityM(modelMatrix, 0); //重置为标准矩阵
        updateMvpMatrix();

        //添加累加混合技术，让新渲染的粒子 + 缓冲区中已经渲染的粒子数据混合
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);


        //绘制粒子时保持深度测试功能同时禁用深度更新
        GLES20.glDepthMask(false);

        particleShaderProgram.useProgram();
        particleShaderProgram.setUniforms(modelViewProjectionMatrix, currentTime, particleTexture); //加载纹理
        particleSystem.bindData(particleShaderProgram);
        particleSystem.draw();

        GLES20.glDepthMask(true);

        GLES20.glDisable(GLES20.GL_BLEND);
    }
}
