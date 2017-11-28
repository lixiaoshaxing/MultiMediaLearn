//
// Created by 李晓 on 17/7/5.
//
#include <jni.h>
#include "inc/fmod.hpp"
#include "stdlib.h"
#include "unistd.h"

using namespace FMOD;

extern "C"
JNIEXPORT void JNICALL
Java_com_lixiao_voicechanger_VoiceFix_voiceFix(JNIEnv *env, jobject instance, jstring filePath_,
                                               jint index) {
    const char *filePath = env->GetStringUTFChars(filePath_, 0);
    System *system;
    Sound *sound;
    Channel *channel;
    DSP *dsp;
    bool playing = true;
    float frequency = 0;
    try {
        //初始化
        System_Create(&system);
        system->init(32, FMOD_INIT_NORMAL, NULL);
        //创建声音
        system->createSound(filePath, FMOD_DEFAULT, NULL, &sound);
        switch (index) {
            case 1:
                //修改音轨，音调等
                system->playSound(sound, 0, false, &channel);
                //播放声音
                break;
            case 2:
                //提升音调
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 2.5);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp); //在chanel中添加dsp改变音质
                break;
            case 3:
                //拉长降低音调
                system->createDSPByType(FMOD_DSP_TYPE_TREMOLO, &dsp);
                dsp->setParameterFloat(FMOD_DSP_TREMOLO_SKEW, 0.5);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp);
                break;
            case 4:
                //降低音调
                system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
                dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 0.8);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp);
                break;
            case 5:
                //提高语速
                system->playSound(sound, 0, false, &channel);
                channel->getFrequency(&frequency);
                frequency *= 1.6;
                channel->setFrequency(frequency);
                break;
            case 6:
                //回音
                system->createDSPByType(FMOD_DSP_TYPE_ECHO, &dsp);
                dsp->setParameterFloat(FMOD_DSP_ECHO_DELAY, 300);
                dsp->setParameterFloat(FMOD_DSP_ECHO_FEEDBACK, 20);
                system->playSound(sound, 0, false, &channel);
                channel->addDSP(0, dsp);
                break;
        }
    } catch (...) {
        goto end;
    }
    system->update();
    while (playing) {
        channel->isPlaying(&playing);
        usleep(1000 * 1000);
    }
    goto end;
    end:
    env->ReleaseStringUTFChars(filePath_, filePath);
    sound->release();
    system->close();
    system->release();
}
