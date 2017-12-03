#include <stdio.h>
#include <jni.h>
#include <android/log.h>
#include "opensl_io.h"

#define LOG(...) __android_log_print(ANDROID_LOG_DEBUG,"AudioDemo-JNI",__VA_ARGS__)

#define SAMPLERATE 44100
#define CHANNELS 1
#define PERIOD_TIME 20 //ms
#define FRAME_SIZE SAMPLERATE*PERIOD_TIME/1000
#define BUFFER_SIZE FRAME_SIZE*CHANNELS
#define TEST_CAPTURE_FILE_PATH "/sdcard/audio.pcm"

static volatile int g_loop_exit = 0;


/**
 * 使用opensl进行声音的录制和播放
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_startCapture(JNIEnv *env, jobject instance) {
    //1. 拿到文件
    FILE *fp = fopen(TEST_CAPTURE_FILE_PATH, "wb");
    if (fp == NULL) {
        LOG("open file: (%s)\n failed", TEST_CAPTURE_FILE_PATH);
        return;
    }
    //2. 创建record对象， 打开一个输入流
    OPENSL_STREAM *stream = android_OpenAudioDevice(SAMPLERATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        LOG("open record stream fail");
        return;
    }
    //3. 进行录制
    int samples;
    short buffer[BUFFER_SIZE]; //存储缓冲区
    g_loop_exit = 0;
    while (!g_loop_exit) {
        //获取数据
        samples = android_AudioIn(stream, buffer, BUFFER_SIZE);
        if (samples < 0) {
            LOG("recording is failed, please check!");
            break;
        }
        //写入数据
        if (fwrite((unsigned char *) buffer, samples * sizeof(short), 1, fp) != 1) {
            LOG("record data failed");
            break;
        }
        LOG("record %d samples !\n ", samples);
    }
    //清理资源
    android_CloseAudioDevice(stream);
    fclose(fp);
    LOG("record success");
    return;
}


JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_stopCapture(JNIEnv *env, jobject instance) {
    g_loop_exit = 1;
    return;
}

JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_startPlay(JNIEnv *env, jobject instance) {
    FILE * fp = fopen(TEST_CAPTURE_FILE_PATH, "rb");
    if( fp == NULL ) {
        LOG("cannot open file (%s) !\n",TEST_CAPTURE_FILE_PATH);
        return;
    }

    //打开流
    OPENSL_STREAM* stream = android_OpenAudioDevice(SAMPLERATE, CHANNELS, CHANNELS, FRAME_SIZE);
    if (stream == NULL) {
        fclose(fp);
        LOG("failed to open audio device ! \n");
        return;
    }

    int samples;
    short buffer[BUFFER_SIZE];
    g_loop_exit = 0;
    while (!g_loop_exit && !feof(fp)) {
        //读文件
        if (fread((unsigned char *)buffer, BUFFER_SIZE*2, 1, fp) != 1) {
            LOG("failed to read data \n ");
            break;
        }
        //播放
        samples = android_AudioOut(stream, buffer, BUFFER_SIZE);
        if (samples < 0) {
            LOG("android_AudioOut failed !\n");
        }
        LOG("playback %d samples !\n", samples);
    }
    android_CloseAudioDevice(stream);
    fclose(fp);

    LOG("nativeStartPlayback completed !");

    return;
}

JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_stopPlay(JNIEnv *env, jobject instance) {
    g_loop_exit = 1;
    return;
}