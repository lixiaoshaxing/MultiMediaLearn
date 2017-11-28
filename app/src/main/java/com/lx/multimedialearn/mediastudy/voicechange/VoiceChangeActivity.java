package com.lx.multimedialearn.mediastudy.voicechange;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.lx.multimedialearn.R;

/**
 * 变声的学习
 * 1. Fmod库使用
 * （1）导入fmod.jar，放入libs，gradle引入
 * （2）导入libfmod.so，libfmodL.so，cmake中引入
 * （3）添加fmod头文件，cmake中引入
 * （4）voice_changer.cpp中调用
 * 2. 多层CMake.txt联合编译
 * （1）顶层cmake.txt，关联内部项目，最终编译为.so
 * （2）内层cmake.txt，最终编译为.a，供顶层引入
 * blog: 多层cmake：https://justchen.com/2016/12/17/cmake%E5%A4%9A%E4%B8%AA%E7%9B%AE%E5%BD%95%EF%BC%8C%E5%A4%9A%E4%B8%AA%E6%BA%90%E6%96%87%E4%BB%B6-%E5%B0%86%E5%AD%90%E7%9B%AE%E5%BD%95%E7%BC%96%E8%AF%91%E4%B8%BA%E5%BA%93.html
 */
public class VoiceChangeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_change);
    }
}
