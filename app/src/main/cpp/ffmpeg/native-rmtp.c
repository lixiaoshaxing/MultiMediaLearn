//
// Created by 李晓 on 17/8/1.
//
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "stdio.h"
#include "stdlib.h"
#include <unistd.h>

#include <android/native_window_jni.h>
#include <android/native_window.h>
#include "inc/libyuv.h"
#include "inc/ffmpeg/libavutil/time.h"
#include "inc/libyuv/convert_from.h"


#include "pthread.h"
//封装格式
#include "inc/ffmpeg/libavformat/avformat.h"
//解码
#include "inc/ffmpeg/libavcodec/avcodec.h"
//缩放
#include "inc/ffmpeg/libswscale/swscale.h"
//重采样
#include "inc/ffmpeg/libswresample/swresample.h"


#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"sys.out",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"sys.out",FORMAT,##__VA_ARGS__);

JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_push(JNIEnv *env, jclass instance,
                                                         jstring inputUrl_,
                                                         jstring outputUrl_) {
    const char *input_cstr = (*env)->GetStringUTFChars(env, inputUrl_, 0);
    const char *output_cstr = (*env)->GetStringUTFChars(env, outputUrl_, 0);
    //变量初始化
    AVFormatContext *inFmtCtx = NULL, *outFmtCtx = NULL;
    int ret;
    //注册组件
    av_register_all();
    //初始化网络
    avformat_network_init();

    //打开输入文件
    if ((ret = avformat_open_input(&inFmtCtx, input_cstr, 0, 0)) < 0) {
        LOGE("无法打开文件");
        goto end;
    }
    //获取文件信息
    if ((ret = avformat_find_stream_info(inFmtCtx, 0)) < 0) {
        LOGE("无法获取文件信息");
        goto end;
    }
    //输出的封装格式上下文，使用RTMP协议推送flv封装格式的流
    avformat_alloc_output_context2(&outFmtCtx, NULL, "flv", output_cstr); //RTMP
    //avformat_alloc_output_context2(&ofmt_ctx, NULL, "mpegts", output_str);//UDP

    int i = 0;
    for (; i < inFmtCtx->nb_streams; i++) {
        //根据输入封装格式中的AVStream流，来创建输出封装格式的AVStream流
        //解码器，解码器上下文都要一致
        AVStream *in_stream = inFmtCtx->streams[i];
        AVStream *out_stream = avformat_new_stream(outFmtCtx, in_stream->codec->codec);
        //复制解码器上下文
        ret = avcodec_copy_context(out_stream->codec, in_stream->codec);
        //全局头
        out_stream->codec->codec_tag = 0;
        if (outFmtCtx->oformat->flags == AVFMT_GLOBALHEADER) {
            out_stream->codec->flags = CODEC_FLAG_GLOBAL_HEADER;
        }
    }

    //打开输出的AVIOContext IO流上下文
    AVOutputFormat *ofmt = outFmtCtx->oformat;
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&outFmtCtx->pb, output_cstr, AVIO_FLAG_WRITE);
    }

    //先写一个头
    ret = avformat_write_header(outFmtCtx, NULL);
    if (ret < 0) {
        LOGE("推流发生错误\n");
        goto end;
    }
    //获取视频流的索引位置
    int videoindex = -1;
    for (i = 0; i < inFmtCtx->nb_streams; i++) {
        if (inFmtCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoindex = i;
            break;
        }
    }
    int frame_index = 0;
    int64_t start_time = av_gettime();
    AVPacket pkt;
    while (1) {
        AVStream *in_stream, *out_stream;
        //读取AVPacket
        ret = av_read_frame(inFmtCtx, &pkt);
        if (ret < 0)
            break;
        //没有封装格式的裸流（例如H.264裸流）是不包含PTS、DTS这些参数的。在发送这种数据的时候，需要自己计算并写入AVPacket的PTS，DTS，duration等参数
        //PTS：Presentation Time Stamp。PTS主要用于度量解码后的视频帧什么时候被显示出来
        //DTS：Decode Time Stamp。DTS主要是标识读入内存中的流在什么时候开始送入解码器中进行解码
        if (pkt.pts == AV_NOPTS_VALUE) {
            //Write PTS
            AVRational time_base1 = inFmtCtx->streams[videoindex]->time_base;
            //Duration between 2 frames (us)
            int64_t calc_duration =
                    (double) AV_TIME_BASE / av_q2d(inFmtCtx->streams[videoindex]->r_frame_rate);
            //Parameters
            pkt.pts = (double) (frame_index * calc_duration) /
                      (double) (av_q2d(time_base1) * AV_TIME_BASE);
            pkt.dts = pkt.pts;
            pkt.duration = (double) calc_duration / (double) (av_q2d(time_base1) * AV_TIME_BASE);
        }

        if (pkt.stream_index == videoindex) {
            //FFmpeg处理数据速度很快，瞬间就能把所有的数据发送出去，流媒体服务器是接受不了
            //这里采用av_usleep()函数休眠的方式来延迟发送，延时时间根据帧率与时间基准计算得到
            AVRational time_base = inFmtCtx->streams[videoindex]->time_base;
            AVRational time_base_q = {1, AV_TIME_BASE};
            int64_t pts_time = av_rescale_q(pkt.dts, time_base, time_base_q);
            int64_t now_time = av_gettime() - start_time;
            if (pts_time > now_time) {
                av_usleep(pts_time - now_time);
            }
        }

        in_stream = inFmtCtx->streams[pkt.stream_index];
        out_stream = outFmtCtx->streams[pkt.stream_index];
        /* copy packet */
        //Convert PTS/DTS
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                                   AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                                   AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;
        //输出进度
        if (pkt.stream_index == videoindex) {
            LOGI("第%d帧", frame_index);
            frame_index++;
        }
        //推送
        ret = av_interleaved_write_frame(outFmtCtx, &pkt);

        if (ret < 0) {
            LOGE("Error muxing packet");
            break;
        }
        av_free_packet(&pkt);

    }
    //输出结尾
    av_write_trailer(outFmtCtx);
    end:
    //释放资源
    avformat_free_context(inFmtCtx);
    avio_close(outFmtCtx->pb);
    avformat_free_context(outFmtCtx);
    (*env)->ReleaseStringUTFChars(env, inputUrl_, input_cstr);
    (*env)->ReleaseStringUTFChars(env, outputUrl_, output_cstr);
}