package com.lx.multimedialearn.mediastudy.voicechange;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lx.multimedialearn.R;

import org.fmod.FMOD;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 变声的学习
 * 1. Fmod库使用
 * （1）导入fmod.jar，放入libs，gradle引入
 * （2）导入libfmod.so，libfmodL.so，cmake中引入
 * （3）添加fmod头文件，cmake中引入
 * （4）voice_changer.cpp中调用
 * （5）FMOD.init(); //在调用前初始化
 * 2. 多层CMake.txt联合编译
 * （1）顶层cmake.txt，关联内部项目，最终编译为.so
 * （2）内层cmake.txt，最终编译为.a，供顶层引入
 * cmake blog: 很全面：http://www.jianshu.com/p/6332418b12b1
 */
public class VoiceChangeActivity extends AppCompatActivity implements View.OnClickListener {

    private String mPath = "file:///android_asset/wave.mp3";

    static {
        try {
            System.loadLibrary("voice_change");
        } catch (UnsatisfiedLinkError e) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_change);
        FMOD.init(this); //初始化！！！
        findViewById(R.id.btn_voice_1).setOnClickListener(this);
        findViewById(R.id.btn_voice_2).setOnClickListener(this);
        findViewById(R.id.btn_voice_3).setOnClickListener(this);
        findViewById(R.id.btn_voice_4).setOnClickListener(this);
        findViewById(R.id.btn_voice_5).setOnClickListener(this);
        findViewById(R.id.btn_voice_6).setOnClickListener(this);
    }

    /**
     * 对声音进行修改
     *
     * @param filePath
     * @param index    修改类型
     */
    public native void voiceFix(String filePath, int index);

    //线程总结：http://www.jianshu.com/p/b8197dd2934c
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_voice_1:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        voiceFix(mPath, 1); //后续待添加同时只能有一个音频在播放，使用线程池
                    }
                });
                break;
            case R.id.btn_voice_2:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        voiceFix(mPath, 2); //后续待添加同时只能有一个音频在播放，使用线程池
                    }
                });
                break;
            case R.id.btn_voice_3:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        voiceFix(mPath, 3); //后续待添加同时只能有一个音频在播放，使用线程池
                    }
                });
                break;
            case R.id.btn_voice_4:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        voiceFix(mPath, 4); //后续待添加同时只能有一个音频在播放，使用线程池
                    }
                });
                break;
            case R.id.btn_voice_5:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        voiceFix(mPath, 5); //后续待添加同时只能有一个音频在播放，使用线程池
                    }
                });
                break;
            case R.id.btn_voice_6:
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        voiceFix(mPath, 6); //后续待添加同时只能有一个音频在播放，使用线程池
                    }
                });
                break;
        }
    }
}
