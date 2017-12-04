#include <jni.h>
#include <string>
#include <android/log.h>
#include "stdio.h"
#include "stdlib.h"
#include <unistd.h>

#include <android/native_window_jni.h>
#include <android/native_window.h>
#include "pthread.h"

extern "C"
{
//封装格式
#include "ffmpeg/libavformat/avformat.h"
//解码
#include "ffmpeg/libavcodec/avcodec.h"
//缩放
#include "ffmpeg/libswscale/swscale.h"
//重采样
#include "ffmpeg/libswresample/swresample.h"
//

#include "libyuv.h"
}

#define MAX_AUDIO_FRME_SIZE 48000 * 4

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"sys.out",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"sys.out",FORMAT,##__VA_ARGS__);

//ffmpeg转音频码，转为pcm
extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_ffmpeg_1decode(JNIEnv *env, jobject instance,
                                                       jstring input_jstr, jstring output_jstr) {
    const char *input_cstr = (env)->GetStringUTFChars(input_jstr, 0);
    const char *output_cstr = (env)->GetStringUTFChars(output_jstr, 0);
    //打开文件，读入文件-读取音频信息-读取音频流位置-打开输出通道，配置输出通道参数-循环输出
    //注册组件
    av_register_all();
    //申请文件空间，读取文件
    AVFormatContext *pFormatCtx = avformat_alloc_context();
    //输出本地空间的线程id
    pid_t pid = getpid();
    pthread_t t = gettid();
    LOGE("jni层当前进程id：%u", (unsigned int) pid);
    LOGE("jni层当前线程id：%u", (unsigned int) t);
    //打开音频文件
    if (avformat_open_input(&pFormatCtx, input_cstr, NULL, NULL) != 0) {
        LOGE("无法打开音频流文件");
        return;
    }
    //获取输入文件信息
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("获取文件信息出错");
        return;
    }
    //获取音频流索引位置，一个文件有视频，音频等流
    int i = 0, audio_stream_idx = -1;
    for (; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_idx = i;
            break;
        }
    }
    //获取解码器，进行正确解码，打开文件
    AVCodecContext *codecCtx = pFormatCtx->streams[audio_stream_idx]->codec;
    AVCodec *codec = avcodec_find_decoder(codecCtx->codec_id);
    if (codec == NULL) {
        LOGE("获取解码器失败");
        return;
    }
    //打开解码器
    if (avcodec_open2(codecCtx, codec, NULL) < 0) {
        LOGE("打开解码器失败");
        return;
    }
    //知道了输入文件格式，信息，音频所在位置，初始化输出文件
    //压缩数据
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));
    //解压缩数据，申请输出空间
    AVFrame *frame = av_frame_alloc();
    //设置音频格式，采样率
    SwrContext *swrCtx = swr_alloc();

    //初始化输出参数
    //输入的采样格式，设置输出格式
    enum AVSampleFormat in_sample_fmt = codecCtx->sample_fmt;
    //输出采样格式16bit PCM
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    //输入采样率
    int in_sample_rate = codecCtx->sample_rate;
    //输出采样率
    int out_sample_rate = 44100;
    //获取输入的声道布局
    //根据声道个数获取默认的声道布局（2个声道，默认立体声stereo）
    //av_get_default_channel_layout(codecCtx->channels);
    uint64_t in_ch_layout = codecCtx->channel_layout;
    //输出的声道布局（立体声）
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    swr_alloc_set_opts(swrCtx,
                       out_ch_layout, out_sample_fmt, out_sample_rate,
                       in_ch_layout, in_sample_fmt, in_sample_rate,
                       0, NULL);
    swr_init(swrCtx);
    //输出的声道个数
    int out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);
    //重采样设置参数-------------end

    //16bit 44100 PCM 数据
    uint8_t *out_buffer = (uint8_t *) av_malloc(MAX_AUDIO_FRME_SIZE);
    FILE *fp_pcm = fopen(output_cstr, "wb");
    int got_frame = 0, index = 0, ret;
    //循环读取然后输出到缓冲区，一个一个packet的写入
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        //解码
        ret = avcodec_decode_audio4(codecCtx, frame, &got_frame, packet);
        if (ret < 0) {
            LOGI("解码完毕");
        }
        //解码成功收输出
        if (got_frame > 0) {
           // LOGI("解码：%d", index++);
            swr_convert(swrCtx, &out_buffer, MAX_AUDIO_FRME_SIZE, (const uint8_t **) frame->data,
                        frame->nb_samples);
            int out_buffer_size = av_samples_get_buffer_size(NULL, out_channel_nb,
                                                             frame->nb_samples, out_sample_fmt, 1);
            fwrite(out_buffer, 1, out_buffer_size, fp_pcm);
        }
        av_free_packet(packet);
    }

    fclose(fp_pcm);
    av_frame_free(&frame);
    av_free(out_buffer);

    swr_free(&swrCtx);
    avcodec_close(codecCtx);
    avformat_close_input(&pFormatCtx);

    (env)->ReleaseStringUTFChars(input_jstr, input_cstr);
    (env)->ReleaseStringUTFChars(output_jstr, output_cstr);
}

//ffmeeg转码为pcm，使用audiotrack进行播放
extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_ffmpeg_1play(JNIEnv *env, jobject instance,
                                                     jstring input_jstr,
                                                     jstring output_jstr) {
    const char *input_cstr = (env)->GetStringUTFChars(input_jstr, 0);
    const char *output_cstr = (env)->GetStringUTFChars(output_jstr, 0);
    jint j = 1 / 0;
    //打开文件，读入文件-读取音频信息-读取音频流位置-打开输出通道，配置输出通道参数-循环输出
    //注册组件
    av_register_all();
    //申请文件空间，读取文件
    AVFormatContext *pFormatCtx = avformat_alloc_context();
    //打开音频文件
    if (avformat_open_input(&pFormatCtx, input_cstr, NULL, NULL) != 0) {
        LOGE("无法打开音频流文件");
        return;
    }
    //获取输入文件信息
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("获取文件信息出错");
        return;
    }
    //获取音频流索引位置，一个文件有视频，音频等流
    int i = 0, audio_stream_idx = -1;
    for (; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_idx = i;
            break;
        }
    }
    //获取解码器，进行正确解码，打开文件
    AVCodecContext *codecCtx = pFormatCtx->streams[audio_stream_idx]->codec;
    AVCodec *codec = avcodec_find_decoder(codecCtx->codec_id);
    if (codec == NULL) {
        LOGE("获取解码器失败");
        return;
    }
    //打开解码器
    if (avcodec_open2(codecCtx, codec, NULL) < 0) {
        LOGE("打开解码器失败");
        return;
    }
    //知道了输入文件格式，信息，音频所在位置，初始化输出文件
    //压缩数据
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));
    //解压缩数据，申请输出空间
    AVFrame *frame = av_frame_alloc();
    //设置音频格式，采样率
    SwrContext *swrCtx = swr_alloc();

    //初始化输出参数
    //输入的采样格式，设置输出格式
    enum AVSampleFormat in_sample_fmt = codecCtx->sample_fmt;
    //输出采样格式16bit PCM
    enum AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_S16;
    //输入采样率
    int in_sample_rate = codecCtx->sample_rate;
    //输出采样率
    int out_sample_rate = 44100;
    //获取输入的声道布局
    //根据声道个数获取默认的声道布局（2个声道，默认立体声stereo）
    //av_get_default_channel_layout(codecCtx->channels);
    uint64_t in_ch_layout = codecCtx->channel_layout;
    //输出的声道布局（立体声）
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    swr_alloc_set_opts(swrCtx,
                       out_ch_layout, out_sample_fmt, out_sample_rate,
                       in_ch_layout, in_sample_fmt, in_sample_rate,
                       0, NULL);
    swr_init(swrCtx);
    //输出的声道个数
    int out_channel_nb = av_get_channel_layout_nb_channels(out_ch_layout);
    //重采样设置参数-------------end

    //16bit 44100 PCM 数据
    uint8_t *out_buffer = (uint8_t *) av_malloc(MAX_AUDIO_FRME_SIZE);
    FILE *fp_pcm = fopen(output_cstr, "wb");
    int got_frame = 0, index = 0, ret;
    //不在传出，从MainActivity中拿到AudioTrack，使用AudioTrack播放解析出来的pcm数据
    //JasonPlayer
    jclass player_class = env->GetObjectClass(instance);

    //AudioTrack对象
    jmethodID create_audio_track_mid = env->GetMethodID(player_class, "createAudioTrack",
                                                        "(II)Landroid/media/AudioTrack;");
    jobject audio_track = env->CallObjectMethod(instance, create_audio_track_mid, out_sample_rate,
                                                out_channel_nb);

    //调用AudioTrack.play方法
    jclass audio_track_class = env->GetObjectClass(audio_track);
    jmethodID audio_track_play_mid = env->GetMethodID(audio_track_class, "play", "()V");
    env->CallVoidMethod(audio_track, audio_track_play_mid);

    //AudioTrack.write
    jmethodID audio_track_write_mid = env->GetMethodID(audio_track_class, "write", "([BII)I");

    //循环读取然后输出到缓冲区，一个一个packet的写入
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        //解码
        ret = avcodec_decode_audio4(codecCtx, frame, &got_frame, packet);
        if (ret < 0) {
            LOGI("解码完毕");
        }
        //解码成功收输出
        if (got_frame > 0) {
            LOGI("解码：%d", index++);
            swr_convert(swrCtx, &out_buffer, MAX_AUDIO_FRME_SIZE, (const uint8_t **) frame->data,
                        frame->nb_samples);
            int out_buffer_size = av_samples_get_buffer_size(NULL, out_channel_nb,
                                                             frame->nb_samples, out_sample_fmt, 1);
            // fwrite(out_buffer, 1, out_buffer_size, fp_pcm);

            //out_buffer缓冲区数据，转成byte数组
            jbyteArray audio_sample_array = env->NewByteArray(out_buffer_size);
            jbyte *sample_bytep = env->GetByteArrayElements(audio_sample_array, NULL);
            //out_buffer的数据复制到sampe_bytep
            memcpy(sample_bytep, out_buffer, out_buffer_size);
            //同步
            env->ReleaseByteArrayElements(audio_sample_array, sample_bytep, 0);

            //AudioTrack.write PCM数据
            env->CallIntMethod(audio_track, audio_track_write_mid,
                               audio_sample_array, 0, out_buffer_size);
            //释放局部引用
            env->DeleteLocalRef(audio_sample_array);
            usleep(1000 * 16);
        }
        av_free_packet(packet);
    }

    fclose(fp_pcm);
    av_frame_free(&frame);
    av_free(out_buffer);

    swr_free(&swrCtx);
    avcodec_close(codecCtx);
    avformat_close_input(&pFormatCtx);

    (env)->ReleaseStringUTFChars(input_jstr, input_cstr);
    (env)->ReleaseStringUTFChars(output_jstr, output_cstr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_videoPlay(JNIEnv *env, jobject instance, jstring path_,
                                                  jobject surface) {
    const char *input_cstr = env->GetStringUTFChars(path_, 0);
    //1.注册组件
    av_register_all();

    //封装格式上下文
    AVFormatContext *pFormatCtx = avformat_alloc_context();

    //2.打开输入视频文件
    if (avformat_open_input(&pFormatCtx, input_cstr, NULL, NULL) != 0) {
        LOGE("%s", "打开输入视频文件失败");
        return;
    }
    //3.获取视频信息
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("%s", "获取视频信息失败");
        return;
    }

    //视频解码，需要找到视频对应的AVStream所在pFormatCtx->streams的索引位置
    int video_stream_idx = -1;
    int i = 0;
    for (; i < pFormatCtx->nb_streams; i++) {
        //根据类型判断，是否是视频流
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_idx = i;
            break;
        }
    }

    //4.获取视频解码器
    AVCodecContext *pCodeCtx = pFormatCtx->streams[video_stream_idx]->codec;
    AVCodec *pCodec = avcodec_find_decoder(pCodeCtx->codec_id);
    if (pCodec == NULL) {
        LOGE("%s", "无法解码");
        return;
    }

    //5.打开解码器
    if (avcodec_open2(pCodeCtx, pCodec, NULL) < 0) {
        LOGE("%s", "解码器无法打开");
        return;
    }

    //编码数据
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    //像素数据（解码数据）
    AVFrame *yuv_frame = av_frame_alloc();
    AVFrame *rgb_frame = av_frame_alloc();

    //native绘制
    //窗体
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    //绘制时的缓冲区
    ANativeWindow_Buffer outBuffer;

    int len, got_frame, framecount = 0;
    //6.一帧一帧读取压缩的视频数据AVPacket
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        //解码AVPacket->AVFrame
        len = avcodec_decode_video2(pCodeCtx, yuv_frame, &got_frame, packet);

        //Zero if no frame could be decompressed
        //非零，正在解码
        if (got_frame) {
            LOGI("解码%d帧", framecount++);
            //lock
            //设置缓冲区的属性（宽、高、像素格式）
            ANativeWindow_setBuffersGeometry(nativeWindow, pCodeCtx->width, pCodeCtx->height,
                                             WINDOW_FORMAT_RGBA_8888);
            ANativeWindow_lock(nativeWindow, &outBuffer, NULL);

            //设置rgb_frame的属性（像素格式、宽高）和缓冲区
            //rgb_frame缓冲区与outBuffer.bits是同一块内存
            avpicture_fill((AVPicture *) rgb_frame, (const uint8_t *) outBuffer.bits, PIX_FMT_RGBA,
                           pCodeCtx->width, pCodeCtx->height);

            //YUV->RGBA_8888
            libyuv::I420ToARGB(yuv_frame->data[0], yuv_frame->linesize[0],
                               yuv_frame->data[2], yuv_frame->linesize[2],
                               yuv_frame->data[1], yuv_frame->linesize[1],
                               rgb_frame->data[0], rgb_frame->linesize[0],
                               pCodeCtx->width, pCodeCtx->height);

            //unlock
            ANativeWindow_unlockAndPost(nativeWindow);

            usleep(1000 * 16);

        }

        av_free_packet(packet);
    }

    ANativeWindow_release(nativeWindow);
    av_frame_free(&yuv_frame);
    avcodec_close(pCodeCtx);
    avformat_free_context(pFormatCtx);

    env->ReleaseStringUTFChars(path_, input_cstr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_videoDecode(JNIEnv *env, jobject instance, jstring input_path,
                                                    jstring output_path) {
    char info[10000] = {0};
    const char *input_cstr = env->GetStringUTFChars(input_path, NULL);
    const char *output_cstr = env->GetStringUTFChars(output_path, NULL);

    //1.注册组件
    av_register_all();

    //封装格式上下文
    AVFormatContext *pFormatCtx = avformat_alloc_context();

    //2.打开输入视频文件
    if (avformat_open_input(&pFormatCtx, input_cstr, NULL, NULL) != 0) {
        LOGE("%s", "打开输入视频文件失败");
        return;
    }
    //3.获取视频信息
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("%s", "获取视频信息失败");
        return;
    }

    //视频解码，需要找到视频对应的AVStream所在pFormatCtx->streams的索引位置
    int video_stream_idx = -1;
    int i = 0;
    for (; i < pFormatCtx->nb_streams; i++) {
        //根据类型判断，是否是视频流
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_idx = i;
            break;
        }
    }

    //4.获取视频解码器
    AVCodecContext *pCodeCtx = pFormatCtx->streams[video_stream_idx]->codec;
    AVCodec *pCodec = avcodec_find_decoder(pCodeCtx->codec_id);
    if (pCodec == NULL) {
        LOGE("%s", "无法解码");
        return;
    }

    //5.打开解码器
    if (avcodec_open2(pCodeCtx, pCodec, NULL) < 0) {
        LOGE("%s", "解码器无法打开");
        return;
    }

    //编码数据
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    //像素数据（解码数据）
    AVFrame *frame = av_frame_alloc();
    AVFrame *yuvFrame = av_frame_alloc();

    //只有指定了AVFrame的像素格式、画面大小才能真正分配内存
    //缓冲区分配内存
    uint8_t *out_buffer = (uint8_t *) av_malloc(
            avpicture_get_size(AV_PIX_FMT_YUV420P, pCodeCtx->width, pCodeCtx->height));
    //初始化缓冲区
    avpicture_fill((AVPicture *) yuvFrame, out_buffer, AV_PIX_FMT_YUV420P, pCodeCtx->width,
                   pCodeCtx->height);


    //输出文件
    FILE *fp_yuv = fopen(output_cstr, "wb");

    //用于像素格式转换或者缩放
    struct SwsContext *sws_ctx = sws_getContext(
            pCodeCtx->width, pCodeCtx->height, pCodeCtx->pix_fmt,
            pCodeCtx->width, pCodeCtx->height, AV_PIX_FMT_YUV420P,
            SWS_BILINEAR, NULL, NULL, NULL);

    int len, got_frame, framecount = 0;
    //6.一帧一帧读取压缩的视频数据AVPacket
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        //解码AVPacket->AVFrame
        len = avcodec_decode_video2(pCodeCtx, frame, &got_frame, packet);

        //非零，正在解码
        if (got_frame) {
            //frame->yuvFrame (YUV420P)
            //转为指定的YUV420P像素帧
            //const uint8_t *const*
            sws_scale(sws_ctx,
                    // uint8_t *
                      (const uint8_t *const *) frame->data, frame->linesize, 0, frame->height,
                      yuvFrame->data, yuvFrame->linesize);

            //向YUV文件保存解码之后的帧数据
            //AVFrame->YUV
            //一个像素包含一个Y
            int y_size = pCodeCtx->width * pCodeCtx->height;
            fwrite(yuvFrame->data[0], 1, y_size, fp_yuv);
            fwrite(yuvFrame->data[1], 1, y_size / 4, fp_yuv);
            fwrite(yuvFrame->data[2], 1, y_size / 4, fp_yuv);

            LOGI("解码%d帧", framecount++);
        }

        av_free_packet(packet);
    }

    fclose(fp_yuv);

    av_frame_free(&frame);
    avcodec_close(pCodeCtx);
    avformat_free_context(pFormatCtx);

    env->ReleaseStringUTFChars(input_path, input_cstr);
    env->ReleaseStringUTFChars(output_path, output_cstr);
}
