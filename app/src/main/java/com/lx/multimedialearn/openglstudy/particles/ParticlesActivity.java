package com.lx.multimedialearn.openglstudy.particles;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.lx.multimedialearn.openglstudy.particles.render.ParticlesRender;
import com.lx.multimedialearn.openglstudy.wallpaper.GLWallpaperService;
import com.lx.multimedialearn.utils.GlUtil;

/**
 * 显示粒子
 * 1. 粒子系统
 * 2. 动态桌面
 */
public class ParticlesActivity extends AppCompatActivity implements SensorEventListener {

    private static final float NS2S = 1.0f / 1000000000.0f;
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false; //是否设置渲染器
    private ParticlesRender render;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //注册陀螺仪
        initSensor();
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        glSurfaceView = new GLSurfaceView(this); //初始化GLSurfaceView
        if (GlUtil.checkGLEsVersion_2(this)) {//检查系统是否支持OpenGL 2.0
            glSurfaceView.setEGLContextClientVersion(2);
            render = new ParticlesRender(this);
            glSurfaceView.setRenderer(render);
            glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
                float previousX, previousY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event != null) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            previousX = event.getX();
                            previousY = event.getY();
                        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            final float deletaX = event.getX() - previousX;
                            final float deletaY = event.getY() - previousY;
                            previousX = event.getX();
                            previousY = event.getY();

                            glSurfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    render.handleTouchDrag(deletaX, deletaY);
                                }
                            });
                        }
                        return true;
                    }
                    return false;
                }
            });
            rendererSet = true;
        } else {
            Toast.makeText(this, "不支持OpenGL 2.0", Toast.LENGTH_SHORT).show();
            finish();
        }

        frameLayout.addView(glSurfaceView);
        Button button = new Button(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        button.setLayoutParams(params);
        button.setText("设置动态壁纸");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(ParticlesActivity.this, GLWallpaperService.class));
                ParticlesActivity.this.startActivity(intent);
            }
        });
        frameLayout.addView(button, params);
        setContentView(frameLayout);
    }

    /**
     * 初始化陀螺仪
     */
    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    private long timeStamp; //时间间隔
    private float[] angle = new float[3]; //三个方向上旋转的角度

    @Override
    public void onSensorChanged(SensorEvent event) {
        //处理转动
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (timeStamp != 0) { //时间戳
                float dT = (event.timestamp - timeStamp) * NS2S;
                angle[0] += event.values[0] * dT;
                angle[1] += event.values[1] * dT;
                angle[2] += event.values[2] * dT;
                final float anglex = (float) Math.toDegrees(angle[0]);
                final float angley = (float) Math.toDegrees(angle[1]);
                final float anglez = (float) Math.toDegrees(angle[2]);
                glSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        render.handleSensor(angley, anglex); //这里把x轴和y轴反一下？
                    }
                });
            }
            timeStamp = event.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onPause() {
        super.onPause();  //控制GLSurfaceView的生命周期，只有正确的暂停和继续后台渲染进程，申请和释放OpenGL上下文，才能保证程序不崩溃
        if (rendererSet) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rendererSet) {
            glSurfaceView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}
