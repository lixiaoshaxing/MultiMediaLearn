#include <jni.h>
#include <string.h>
#include <android/log.h>
#include "stdio.h"
#include "stdlib.h"
#include <unistd.h>

#include <android/native_window_jni.h>
#include <android/native_window.h>
#include "inc/libyuv.h"
#include "inc/libyuv/convert_from.h"
#include "inc/ffmpeg/libavutil/time.h"
#include "pthread.h"


//封装格式
#include "inc/ffmpeg/libavformat/avformat.h"
//解码
#include "inc/ffmpeg/libavcodec/avcodec.h"
//缩放
#include "inc/ffmpeg/libswscale/swscale.h"
//重采样
#include "inc/ffmpeg/libswresample/swresample.h"

//格式转换
#include "inc/libyuv.h"

//存放Avpacket的队列
#include "queue.h"

//播放视频包括两个流：视频流，音频流，字幕流（这里不处理）
#define MAX_STREAM 2
#define PACKET_QUEUE_SIZE 50
#define MAX_AUDIO_FRME_SIZE 48000 * 4

#define MIN_SLEEP_TIME_US 1000ll
#define AUDIO_TIME_ADJUST_US -200000ll

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"sys.out",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"sys.out",FORMAT,##__VA_ARGS__);

/**
 * 解决音频、视频同步的问题
 * 1. 创建队列，用来存储AvPacket, 一个生产者（），两个消费者（）
 */
/**
 * 定义player结构体，存储用来传输的变量
 */
typedef struct Player {
    JavaVM *javaVM; //查找java层方法id使用

    AVFormatContext *input_format_ctx;  //封装格式上下文
    int video_stream_idx; //视频流索引位置
    int audio_stream_idx; //音频流索引位置
    AVCodecContext *input_codec_ctx[MAX_STREAM]; //解码器上下文数组，包括视频，音频解码器
    pthread_t decode_threads[MAX_STREAM]; //播放音视频的线程id

    ANativeWindow *nativeWindow; //播放视频的窗口

    int capture_streams_no; //流的总个数

    SwrContext *swr_ctx; //音频播放需要的参数
    //输入的采样格式
    enum AVSampleFormat in_sample_fmt;
    //输出采样格式16bit PCM
    enum AVSampleFormat out_sample_fmt;
    //输入采样率
    int in_sample_rate;
    //输出采样率
    int out_sample_rate;
    //输出的声道个数
    int out_channel_nb;

    //JNI
    jobject audio_track;
    jmethodID audio_track_write_mid;

    pthread_t thread_read_from_stream;
    //音频，视频读取队列，有两个队列，还可以加入字幕队列
    Queue *packets[MAX_STREAM];

    //互斥锁
    pthread_mutex_t mutex;
    //条件变量
    pthread_cond_t cond;

    //视频开始播放的时间
    int64_t start_time;

    int64_t audio_clock;

} Player;

//解码数据
typedef struct _DecoderData {
    Player *player;
    int stream_index; //在解码的时候，通过该索引拿到对应的队列
} DecoderData;

void decode_video(Player *pPlayer, AVPacket *pPacket);

void decode_audio_prepare(Player *pPlayer);

/**
 * 初始化工作，主要找到音频，视频的索引位置
 */
void init_input_format_ctx(Player *player, const char *input_cstr) {
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
    int i = 0;
    player->capture_streams_no = pFormatCtx->nb_streams;
    for (; i < player->capture_streams_no; i++) {
        //根据类型判断，是否是视频流,音频流
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            player->video_stream_idx = i;
        } else if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            player->audio_stream_idx = i;
        }
    }
    player->input_format_ctx = pFormatCtx;
}

/**
 * 根据获取视频，音频解码器，初始化解码器上下文
 * @param player
 * @param stream_idx
 */
void init_codec_context(Player *player, int stream_idx) {
    //获取视频解码器,音频解码器
    AVFormatContext *format_ctx = player->input_format_ctx;
    AVCodecContext *pCodeCtx = format_ctx->streams[stream_idx]->codec;

    AVCodec *pCodec = avcodec_find_decoder(pCodeCtx->codec_id);
    if (pCodec == NULL) {
        LOGE("%s", "无法解码");
        return;
    }
    //打开解码器
    if (avcodec_open2(pCodeCtx, pCodec, NULL) < 0) {
        LOGE("%s", "解码器无法打开");
        return;
    }
    player->input_codec_ctx[stream_idx] = pCodeCtx;
}

//准备播放视频的surfaceView，这时候获取env都是在主线程
void decode_video_prepare(JNIEnv *env, Player *player, jobject surface) {
    player->nativeWindow = ANativeWindow_fromSurface(env, surface);
}

//准备音频播放器
void decode_audio_prepare(Player *player) {
    AVCodecContext *codecCtx = player->input_codec_ctx[player->audio_stream_idx];
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

    //在player中保存初始化的变量
    player->in_sample_fmt = in_sample_fmt;
    player->out_sample_fmt = out_sample_fmt;
    player->in_sample_rate = in_sample_rate;
    player->out_sample_rate = out_sample_rate;
    player->out_channel_nb = out_channel_nb;
    player->swr_ctx = swrCtx;
}

void jni_audio_prepare(JNIEnv *env, jobject jthiz, Player *player) {
    //JNI begin------------------
    jclass player_class = (*env)->GetObjectClass(env, jthiz);

    //AudioTrack对象
    jmethodID create_audio_track_mid = (*env)->GetMethodID(env, player_class, "createAudioTrack",
                                                           "(II)Landroid/media/AudioTrack;");
    jobject audio_track = (*env)->CallObjectMethod(env, jthiz, create_audio_track_mid,
                                                   player->out_sample_rate, player->out_channel_nb);

    //调用AudioTrack.play方法
    jclass audio_track_class = (*env)->GetObjectClass(env, audio_track);
    jmethodID audio_track_play_mid = (*env)->GetMethodID(env, audio_track_class, "play", "()V");
    (*env)->CallVoidMethod(env, audio_track, audio_track_play_mid);

    //AudioTrack.write
    jmethodID audio_track_write_mid = (*env)->GetMethodID(env, audio_track_class, "write",
                                                          "([BII)I");

    //JNI end------------------
    player->audio_track = (*env)->NewGlobalRef(env, audio_track);
    //(*env)->DeleteGlobalRef
    player->audio_track_write_mid = audio_track_write_mid;
}

void *player_fill_packet() {
    AVPacket *packet = malloc(sizeof(AVPacket));
    return packet;
}

void player_alloc_queues(Player *player) {
    int i;
    //初始化两个队列
    for (i = 0; i < player->capture_streams_no; i++) {
        Queue *queue = queue_init(PACKET_QUEUE_SIZE, player_fill_packet);
        player->packets[i] = queue;
    }
}

void *packet_free_func(AVPacket *packet) {
    av_free_packet(packet);
    return 0;
}

/**
 * 获取视频当前播放时间
 */
int64_t player_get_current_video_time(Player *player) {
    int64_t current_time = av_gettime();
    return current_time - player->start_time;
}

/**
 * 延迟
 */
void player_wait_for_frame(Player *player, int64_t stream_time,
                           int stream_no) {
    pthread_mutex_lock(&player->mutex);
    for(;;){
        int64_t current_video_time = player_get_current_video_time(player);
        int64_t sleep_time = stream_time - current_video_time;
        if (sleep_time < -300000ll) {
            // 300 ms late
            int64_t new_value = player->start_time - sleep_time;
            LOGI("player_wait_for_frame[%d] correcting %f to %f because late",
                 stream_no, (av_gettime() - player->start_time) / 1000000.0,
                 (av_gettime() - new_value) / 1000000.0);

            player->start_time = new_value;
            pthread_cond_broadcast(&player->cond);
        }

        if (sleep_time <= MIN_SLEEP_TIME_US) {
            // We do not need to wait if time is slower then minimal sleep time
            break;
        }

        if (sleep_time > 500000ll) {
            // if sleep time is bigger then 500ms just sleep this 500ms
            // and check everything again
            sleep_time = 500000ll;
        }
        //等待指定时长
        int timeout_ret = pthread_cond_timeout_np(&player->cond,
                                                  &player->mutex, sleep_time/1000ll);

        // just go further
        LOGI("player_wait_for_frame[%d] finish", stream_no);
    }
    pthread_mutex_unlock(&player->mutex);
}


/**
 * 对视频数据进行解码
 * @param player
 * @param packet
 */
void decode_video(Player *player, AVPacket *packet) {
    AVFormatContext *input_format_ctx = player->input_format_ctx;
    AVStream *stream = input_format_ctx->streams[player->video_stream_idx];

    //像素数据（解码数据）
    AVFrame *yuv_frame = av_frame_alloc();
    AVFrame *rgb_frame = av_frame_alloc();
    //绘制时的缓冲区
    ANativeWindow_Buffer outBuffer;
    AVCodecContext *pCodeCtx = player->input_codec_ctx[player->video_stream_idx];
    int got_frame;

    //解码AVPacket->AVFrame
    avcodec_decode_video2(pCodeCtx, yuv_frame, &got_frame, packet);

    //非零，正在解码
    if (got_frame) {
        //lock
        //设置缓冲区的属性（宽、高、像素格式）
        ANativeWindow_setBuffersGeometry(player->nativeWindow, pCodeCtx->width, pCodeCtx->height,
                                         WINDOW_FORMAT_RGBA_8888);
        ANativeWindow_lock(player->nativeWindow, &outBuffer, NULL);

        //设置rgb_frame的属性（像素格式、宽高）和缓冲区
        //rgb_frame缓冲区与outBuffer.bits是同一块内存
        avpicture_fill((AVPicture *) rgb_frame, (const uint8_t *) outBuffer.bits, PIX_FMT_RGBA,
                       pCodeCtx->width, pCodeCtx->height);

        //YUV->RGBA_8888
        I420ToARGB(yuv_frame->data[0], yuv_frame->linesize[0],
                   yuv_frame->data[2], yuv_frame->linesize[2],
                   yuv_frame->data[1], yuv_frame->linesize[1],
                   rgb_frame->data[0], rgb_frame->linesize[0],
                   pCodeCtx->width, pCodeCtx->height);

        //计算延迟
        int64_t pts = av_frame_get_best_effort_timestamp(yuv_frame);
        //转换（不同时间基时间转换）
        int64_t time = av_rescale_q(pts,stream->time_base,AV_TIME_BASE_Q);

        player_wait_for_frame(player,time,player->video_stream_idx);

        //unlock
        ANativeWindow_unlockAndPost(player->nativeWindow);
        usleep(1000 * 16);
    }
    av_frame_free(&yuv_frame);
    av_frame_free(&rgb_frame);
}

/**
 * 音频解码
 */
void decode_audio(struct Player *player, AVPacket *packet) {
    AVFormatContext *input_format_ctx = player->input_format_ctx;
    AVStream *stream = input_format_ctx->streams[player->audio_stream_idx];

    AVCodecContext *codec_ctx = player->input_codec_ctx[player->audio_stream_idx];
    LOGI("%s", "decode_audio");
    //解压缩数据
    AVFrame *frame = av_frame_alloc();
    int got_frame;
    avcodec_decode_audio4(codec_ctx, frame, &got_frame, packet);

    //16bit 44100 PCM 数据（重采样缓冲区）
    uint8_t *out_buffer = (uint8_t *) av_malloc(MAX_AUDIO_FRME_SIZE);
    //解码一帧成功
    if (got_frame > 0) {
        swr_convert(player->swr_ctx, &out_buffer, MAX_AUDIO_FRME_SIZE,
                    (const uint8_t **) frame->data, frame->nb_samples);
        //获取sample的size
        int out_buffer_size = av_samples_get_buffer_size(NULL, player->out_channel_nb,
                                                         frame->nb_samples, player->out_sample_fmt,
                                                         1);

        int64_t pts = packet->pts;
        if (pts != AV_NOPTS_VALUE) {
            player->audio_clock = av_rescale_q(pts, stream->time_base, AV_TIME_BASE_Q);
            //				av_q2d(stream->time_base) * pts;
            LOGI("player_write_audio - read from pts");
            player_wait_for_frame(player,
                                  player->audio_clock + AUDIO_TIME_ADJUST_US, player->audio_stream_idx);
        }

        //关联当前线程的JNIEnv
        JavaVM *javaVM = player->javaVM;
        JNIEnv *env;
        (*javaVM)->AttachCurrentThread(javaVM, &env, NULL);

        //out_buffer缓冲区数据，转成byte数组
        jbyteArray audio_sample_array = (*env)->NewByteArray(env, out_buffer_size);
        jbyte *sample_bytep = (*env)->GetByteArrayElements(env, audio_sample_array, NULL);
        //out_buffer的数据复制到sampe_bytep
        memcpy(sample_bytep, out_buffer, out_buffer_size);
        //同步
        (*env)->ReleaseByteArrayElements(env, audio_sample_array, sample_bytep, 0);

        //AudioTrack.write PCM数据
        (*env)->CallIntMethod(env, player->audio_track, player->audio_track_write_mid,
                              audio_sample_array, 0, out_buffer_size);
        //释放局部引用
        (*env)->DeleteLocalRef(env, audio_sample_array);

        (*javaVM)->DetachCurrentThread(javaVM);

        usleep(1000 * 16);
    }

    av_frame_free(&frame);
}

/**
 * 两个消费者从对应的队列中拿内容进行设置，这里要从队列中拿数据，然后消费
 * @param arg
 * @return
 */
void *decode_data(void *arg) {
    DecoderData *decoderData = (DecoderData *) arg;
    Player *player = decoderData->player;
    int stream_index = decoderData->stream_index;
    Queue *queue = player->packets[stream_index];

    AVFormatContext *format_ctx = player->input_format_ctx;

    //编码数据
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    int video_frame_count = 0, audio_frame_count = 0;
    for (;;) {
        if (queue != NULL) {
            //音频和视频进行消费
            pthread_mutex_lock(&player->mutex); //在这里进行加锁控制，保证不会在读的时候进行覆盖？？？？
            AVPacket *packet = (AVPacket *) queue_pop(queue, &player->mutex, &player->cond);
            pthread_mutex_unlock(&player->mutex);
            if (stream_index == player->video_stream_idx) {
                decode_video(player, packet);
                LOGI("video_frame_count:%d", video_frame_count++);
            } else if (stream_index == player->audio_stream_idx) {
                decode_audio(player, packet);
                LOGI("audio_frame_count:%d", audio_frame_count++);
            }
        } else {
            break;
        }
    }

//    //一帧一帧读取压缩的视频数据AVPacket
//    while (av_read_frame(format_ctx, packet) >= 0) {
//        /*if (packet->stream_index == player->video_stream_idx) {
//            decode_video(player, packet);
//            LOGI("解码%d帧", framecount++);
//        } else */if (packet->stream_index == player->audio_stream_idx) {
//            decode_audio(player, packet);
//        }
//        av_free_packet(packet);
//    }
}

/**
 * 生产者队列：从流中读出数据放入两个队列中，音频队列，视频队列
 * @param player
 * @return
 */
void *player_read_from_stream(Player *player) {
    int index = 0;
    int ret;
    //拿到AvPacket，判断类型，对对应的队列中元素进行赋值
    AVPacket packet, *pkt = &packet;
    //要一直进行读取
    for (;;) {
        ret = av_read_frame(player->input_format_ctx, pkt);
        if (ret < 0) {
            break;
        }
        //根据序号拿到对应的队列
        Queue *queue = player->packets[pkt->stream_index];
        //在入队列的时候加锁，赋值完成后，解锁，保证不能对正在读的数据进行修改
        pthread_mutex_lock(&player->mutex);
        //找到队列的顶部，拿到它的地址
        AVPacket *packet_data = queue_push(queue, &player->mutex, &player->cond);
        *packet_data = packet; //深拷贝，赋值
        pthread_mutex_unlock(&player->mutex);
        //释放 todo:内存的释放需要修改
        //queue_free(queue, packet_free_func);

    }
}

//音视频通过创建子线程进行播放
//1. 对播放器解码，上下文进行初始化，只能在主线程获取javaVm，供子线程获取上下文使用，对于getMethodid等需要在主线程中获取
//2. 线程都单独对应一个上下文
//3. 数据的播放
//1.找数据流-》找解码器-》解码
void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_videoPthreadPlay(JNIEnv *env, jobject instance,
                                                         jstring inputPath_, jobject surface) {
    const char *input_cstr = (*env)->GetStringUTFChars(env, inputPath_, 0);
    Player *player = (Player *) malloc(sizeof(Player));
    //对javaVm进行赋值
    (*env)->GetJavaVM(env, &(player->javaVM));
    init_input_format_ctx(player, input_cstr);

    //获取视频解码器,音频解码器
    int video_stream_index = player->video_stream_idx;
    int audio_stream_index = player->audio_stream_idx;
    init_codec_context(player, video_stream_index);
    init_codec_context(player, audio_stream_index);


    decode_video_prepare(env, player, surface);
    decode_audio_prepare(player);

    //准备audioTrack，在主线程中找到methodId，Class
    jni_audio_prepare(env, instance, player);

    //初始化队列，供生产者和消费者使用
    player_alloc_queues(player);

    //对互斥锁，条件变量初始化
    pthread_mutex_init(&player->mutex, NULL);
    pthread_cond_init(&player->cond, NULL);

    //创建线程，在子线程播放视频
//    pthread_create(&(player->decode_threads[player->video_stream_idx]), NULL, decode_data,
//                   (void *) player);

    //创建三个线程：生产者线程，两个消费者线程，生产者把AvPacket放入到队列中
    pthread_create(&(player->thread_read_from_stream), NULL, player_read_from_stream,
                   (void *) player);
    sleep(1);
    player->start_time = 0; //音视频同步，设置初始时间，有一个播放的时长，通过读取当前播放的时间，音频，或者视频进行等待，保证播放同步
    //在播放音视频的过程中进行等待处理
    //当前为什么不能播放，因为缓冲区大小是50，视频处理的比较慢，对缓冲区不断的覆盖，导致消费者进程读到的是错误的数据帧
    //通过加锁的方法，保证读完了，才会生产下一步

    DecoderData data1 = {player, video_stream_index}, *decode_data1 = &data1;
    //创建线程，在子线程中播放音频，这是两个消费者线程
    pthread_create(&(player->decode_threads[player->video_stream_idx]), NULL, decode_data,
                   (void *) decode_data1);

    DecoderData data2 = {player, audio_stream_index}, *decode_data2 = &data2;
    pthread_create(&(player->decode_threads[player->audio_stream_idx]), NULL, decode_data,
                   (void *) decode_data2);

    //todo 按了后退键，这里没有销毁，可以监听surface的状态，destroy时进行暂停，创建后，继续播放
    sleep(10);
    //todo: 这里join，会屏蔽其它按钮
//    pthread_join(player->thread_read_from_stream,NULL);
//    pthread_join(player->decode_threads[video_stream_index],NULL);
//    pthread_join(player->decode_threads[audio_stream_index],NULL);
//      void *p_result;
//    //todo: 在锁屏，退出的时候如何释放资源，这里需要重新处理，在本地方法的子线程，仍然不能连续播放！！！
//    通过pthread_join阻塞等待播放完毕，再释放资源，阻塞会导致后退键不起作用，这里需要再考虑
//      pthread_join(player->decode_threads[player->video_stream_idx], p_result);
//    释放资源
//    (*env)->ReleaseStringUTFChars(env, inputPath_, input_cstr);
//    avcodec_close(player->input_codec_ctx[player->video_stream_idx]);
//    avformat_free_context(player->input_format_ctx);
//    free(player);
}