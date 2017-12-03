#include <jni.h>  //基础
#include <string.h>
#include <android/log.h>
#include "stdio.h"
#include "stdlib.h"
#include <unistd.h>
#include "native_live.h"

#include <android/native_window_jni.h> //ndk库
#include <android/native_window.h>
#include "inc/libyuv.h"
#include "pthread.h"

#include "queue1.h" //自有库

#include "inc/x264/include/x264.h"  //第三方库
#include "inc/faac/include/faac.h"
#include "inc/faac/include/faaccfg.h"
#include "inc/libyuv/rotate.h"
#include "inc/libyuv/convert.h"

#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"sys.out",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"sys.out",FORMAT,##__VA_ARGS__);
#ifndef TRUE
#define TRUE	1
#define FALSE	0
#endif

//x264编码输入图像YUV420P
x264_picture_t pic_in;
x264_picture_t pic_out;
//YUV个数
int y_len, u_len, v_len;
//采集到的图像的宽高
int mWidth, mHeight;
//x264编码处理器
x264_t *video_encode_handle;

unsigned int start_time;
//线程处理
pthread_mutex_t mutex; //互斥锁
pthread_cond_t cond; //条件变量

//rtmp流媒体地址
char *rtmp_path;


//是否直播
int is_pushing = FALSE;
//faac音频编码处理器
faacEncHandle audio_encode_handle;

unsigned long nInputSamples; //输入的采样个数，处理数据一次输入的样本数，根据编码器的设置生成
unsigned long nMaxOutputBytes; //编码输出之后的字节数


/**
 * 加入RTMPPacket队列，等待消费者消费，这个队列需要学习
 * @param packet
 */
void add_rtmp_packet(RTMPPacket *packet) {
    pthread_mutex_lock(&mutex);
    if (is_pushing) {
        queue_append_last(packet);
    }
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&mutex);
}


void *push_thread(void *arg) {
    //建立RTMP连接
    RTMP *rtmp = RTMP_Alloc();
    if (!rtmp) {
        LOGE("rtmp初始化失败");
        goto end;
    }
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 5; //连接超时的时间
    //设置流媒体地址
    RTMP_SetupURL(rtmp, rtmp_path);
    //发布rtmp数据流
    RTMP_EnableWrite(rtmp);
    //建立连接
    if (!RTMP_Connect(rtmp, NULL)) {
        LOGE("%s", "RTMP 连接失败");
        goto end;
    }
    LOGE("%s", "RTMP 链接成功");
    //计时
    start_time = RTMP_GetTime();
    if (!RTMP_ConnectStream(rtmp, 0)) { //连接流
        goto end;
    }
    is_pushing = TRUE;
    add_aac_sequence_header();
    while (is_pushing) {
        //发送
        pthread_mutex_lock(&mutex);
        pthread_cond_wait(&cond, &mutex);
        //取出队列中的RTMPPacket
        RTMPPacket *packet = queue_get_first();
        if (packet) {
            queue_delete_first(); //移除
            packet->m_nInfoField2 = rtmp->m_stream_id; //RTMP协议，stream_id数据
            int i = RTMP_SendPacket(rtmp, packet, TRUE); //TRUE放入librtmp队列中，并不是立即发送
            if (!i) {
                LOGE("RTMP 断开");
                RTMPPacket_Free(packet);
                pthread_mutex_unlock(&mutex);
                goto end;
            }
            RTMPPacket_Free(packet);
        }

        pthread_mutex_unlock(&mutex);
    }
    end:
    free(rtmp_path);
    LOGI("%s", "释放资源");
    RTMP_Close(rtmp);
    RTMP_Free(rtmp);
    //todo 释放资源
    return 0;
}

/**
 * 开始推送，建立握手，网络连接的过程
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_startPush(JNIEnv *env, jobject instance, jstring url_) {
    //进行初始化操作，创建队列，初始化锁，线程，编码的过程是在子线程中
    const char *url_cstr = (*env)->GetStringUTFChars(env, url_, NULL);
    //对推流地址进行赋值
    rtmp_path = (char *) malloc(strlen(url_cstr) + 1);

    memset(rtmp_path, 0, strlen(url_cstr) + 1);
    memcpy(rtmp_path, url_cstr, strlen(url_cstr));

    //初始化互斥锁和条件变量
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&cond, NULL);
    //创建队列
    create_queue();
    //启动消费者线程，不断从队列中拉去RTMPPacket进行消费，推送到nginx
    pthread_t push_thread_id;

    pthread_create(&push_thread_id, NULL, push_thread, NULL);

    (*env)->ReleaseStringUTFChars(env, url_, url_cstr);
}

/**
 * 停止推送
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_stopPush(JNIEnv *env, jobject instance) {
    is_pushing = FALSE;
}

/**
 * 释放资源
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_release(JNIEnv *env, jobject instance) {

}


/**
 * 发送h264 SPS与PPS参数集
 */
void add_264_sequence_header(unsigned char *pps, unsigned char *sps, int pps_len, int sps_len) {
    int body_size = 16 + sps_len + pps_len; //按照H264标准配置SPS和PPS，共使用了16字节
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    //RTMPPacket初始化
    RTMPPacket_Alloc(packet, body_size);
    RTMPPacket_Reset(packet);

    unsigned char *body = packet->m_body;
    int i = 0;
    //二进制表示：00010111
    body[i++] = 0x17;//VideoHeaderTag:FrameType(1=key frame)+CodecID(7=AVC)
    body[i++] = 0x00;//AVCPacketType = 0表示设置AVCDecoderConfigurationRecord
    //composition time 0x000000 24bit ?
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    /*AVCDecoderConfigurationRecord*/
    body[i++] = 0x01;//configurationVersion，版本为1
    body[i++] = sps[1];//AVCProfileIndication
    body[i++] = sps[2];//profile_compatibility
    body[i++] = sps[3];//AVCLevelIndication
    //?
    body[i++] = 0xFF;//lengthSizeMinusOne,H264 视频中 NALU的长度，计算方法是 1 + (lengthSizeMinusOne & 3),实际测试时发现总为FF，计算结果为4.

    /*sps*/
    body[i++] = 0xE1;//numOfSequenceParameterSets:SPS的个数，计算方法是 numOfSequenceParameterSets & 0x1F,实际测试时发现总为E1，计算结果为1.
    body[i++] = (sps_len >> 8) & 0xff;//sequenceParameterSetLength:SPS的长度
    body[i++] = sps_len & 0xff;//sequenceParameterSetNALUnits
    memcpy(&body[i], sps, sps_len);
    i += sps_len;

    /*pps*/
    body[i++] = 0x01;//numOfPictureParameterSets:PPS 的个数,计算方法是 numOfPictureParameterSets & 0x1F,实际测试时发现总为E1，计算结果为1.
    body[i++] = (pps_len >> 8) & 0xff;//pictureParameterSetLength:PPS的长度
    body[i++] = (pps_len) & 0xff;//PPS
    memcpy(&body[i], pps, pps_len);
    i += pps_len;

    //Message Type，RTMP_PACKET_TYPE_VIDEO：0x09
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    //Payload Length
    packet->m_nBodySize = body_size;
    //Time Stamp：4字节
    //记录了每一个tag相对于第一个tag（File Header）的相对时间。
    //以毫秒为单位。而File Header的time stamp永远为0。
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nChannel = 0x04; //Channel ID，Audio和Vidio通道
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM; //?
    //将RTMPPacket加入队列
    add_rtmp_packet(packet);

}

/**
 * 发送h264帧信息
 */
void add_264_body(unsigned char *buf, int len) {
    //去掉起始码(界定符)
    if (buf[2] == 0x00) {  //00 00 00 01
        buf += 4;
        len -= 4;
    } else if (buf[2] == 0x01) { // 00 00 01
        buf += 3;
        len -= 3;
    }
    int body_size = len + 9;
    RTMPPacket *packet = malloc(sizeof(RTMPPacket));
    RTMPPacket_Alloc(packet, body_size);

    unsigned char *body = packet->m_body;
    //当NAL头信息中，type（5位）等于5，说明这是关键帧NAL单元
    //buf[0] NAL Header与运算，获取type，根据type判断关键帧和普通帧
    //00000101 & 00011111(0x1f) = 00000101
    int type = buf[0] & 0x1f;
    //Inter Frame 帧间压缩
    body[0] = 0x27;//VideoHeaderTag:FrameType(2=Inter Frame)+CodecID(7=AVC)
    //IDR I帧图像
    if (type == NAL_SLICE_IDR) {
        body[0] = 0x17;//VideoHeaderTag:FrameType(1=key frame)+CodecID(7=AVC)
    }
    //AVCPacketType = 1
    body[1] = 0x01; /*nal unit,NALUs（AVCPacketType == 1)*/
    body[2] = 0x00; //composition time 0x000000 24bit
    body[3] = 0x00;
    body[4] = 0x00;

    //写入NALU信息，右移8位，一个字节的读取？
    body[5] = (len >> 24) & 0xff;
    body[6] = (len >> 16) & 0xff;
    body[7] = (len >> 8) & 0xff;
    body[8] = (len) & 0xff;

    /*copy data*/
    memcpy(&body[9], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = body_size;
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;//当前packet的类型：Video
    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
//	packet->m_nTimeStamp = -1;
    packet->m_nTimeStamp = RTMP_GetTime() - start_time;//记录了每一个tag相对于第一个tag（File Header）的相对时间
    add_rtmp_packet(packet);

}

/**
 * 设置视频参数
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_setVideoOptions(JNIEnv *env, jobject instance, jint width,
                                                  jint height, jint bitrate, jint fps) {
    x264_param_t param;
    //设置默认
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    //编码输入的像素格式是YUV420P
    param.i_csp = X264_CSP_I420;
    //480(宽) * 320 // 320*240
    param.i_width = height; // 320 //推送出去的图像是240*320
    param.i_height = width; //480

    y_len = width * height;
    u_len = y_len / 4;
    v_len = u_len;
    mWidth = width; //来的图像还是320*240
    mHeight = height;

    param.i_keyint_min = 100; //设置gop最小间距
    //参数i_rc_method表示码率控制，CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    //恒定码率，会尽量控制在固定码率
    param.rc.i_rc_method = X264_RC_CRF;
    param.rc.i_bitrate = bitrate / 1000; //* 码率(比特率,单位Kbps)
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2; //瞬时最大码率

    //码率控制不通过timebase和timestamp，而是fps
    param.b_vfr_input = 0;
    param.i_fps_num = fps; //* 帧率分子
    param.i_fps_den = 1; //* 帧率分母
    param.i_timebase_den = param.i_fps_num;
    param.i_timebase_num = param.i_fps_den;
    param.i_threads = 1;//并行编码线程数量，0默认为多线程

    //是否把SPS和PPS放入每一个关键帧
    //SPS Sequence Parameter Set 序列参数集，PPS Picture Parameter Set 图像参数集
    //为了提高图像的纠错能力
    param.b_repeat_headers = 1;
    //设置Level级别
    param.i_level_idc = 51;
    //设置Profile档次
    //baseline级别，没有B帧
    x264_param_apply_profile(&param, "baseline");

    //x264_picture_t（输入图像）初始化
    x264_picture_alloc(&pic_in, param.i_csp, param.i_width, param.i_height);
    pic_in.i_pts = 0;
    //打开编码器
    video_encode_handle = x264_encoder_open(&param);

    if (video_encode_handle) {
        LOGI("打开编码器成功...");
    }
}


/**
 * 推送视频，解析数据后向队列中添加数据，供消费者队列进行推流消费
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_fireVideo(JNIEnv *env, jclass instance, jbyteArray data_,
                                            jstring output_path, jint cameraId) {
    //把这里的数组数据转换为YUM420p
    //buffer->nv21->YUM420p，这是一帧画面？， buffer中的数据在这里可以进行美颜，人脸识别的处理
    //nv21->yuv420p,yyyyyyyy vuvu交叉，I420效果更好
    const jchar *output_p = (*env)->GetStringUTFChars(env, output_path, NULL);
    jbyte *nv21_buffer = (*env)->GetByteArrayElements(env, data_, NULL);
    jbyte *u = pic_in.img.plane[1];
    jbyte *v = pic_in.img.plane[2];
    //   jsize len = (*env)->GetArrayLength(env, data_);
    //nv21 4:2:0 Formats, 12 Bits per Pixel
    //nv21与yuv420p-I420，y个数一致，uv位置对调
    //nv21转yuv420p  y = w*h,u/v=w*h/4
    //nv21 = yvu yuv420p=yuv y=y u=y+1+1 v=y+1，一帧换面的处理
    //在这里对数据进行转方向
    //1. 得到这里的数组数据是什么？转为yuv图片，使用libyuv进行转换
    //2. 使用libyuv进行转向
    //3. 对生成的图片在使用h264编码推送
    //h264编码得到NALU数组
    //const uint8* src, int src_stride,
    //uint8* dst, int dst_stride,
    //int src_width, int src_height, enum RotationMode mode
    //  jbyte *dst = (jbyte *) malloc(len * sizeof(jbyte));
    //  memcpy(nv21_buffer, dst, len);
    // 1. nv21->i420 2. i420->rotate 270 3. i420 -> nv21 4.

//    src_frame, src_size,
//    dst_y, dst_stride_y,
//    dst_u, dst_stride_u,
//    dst_v, dst_stride_v,
//    crop_x, crop_y,
//    src_width, src_height,
//    crop_width, crop_height,
//    RotationMode rotation,
//    format
    //从nv21(yyyyVUVU)经过剪切，转向转换为I420(yyyyUUVV)
    //申请dst空间 320*240
    int size = mWidth * mHeight * 3 / 2;
    uint8 *dst = (uint8 *) malloc(size);
    //先90度转向
    if (cameraId == 1) {
        //如果是前置摄像头
        ConvertToI420(nv21_buffer, size,
                      dst, mHeight,
                      dst + mWidth * mHeight, mHeight / 2,
                      dst + mWidth * mHeight * 5 / 4, mHeight / 2,
                      0, 0, // 以左上角为原点，裁剪起始点
                      mWidth, mHeight, //320 * 240
                      mWidth, mHeight, //240 * 320
                      kRotate270,
                      FOURCC_NV21);
        //进行镜像变换

    } else {
        //如果是后置摄像头
        ConvertToI420(nv21_buffer, size,
                      dst, mHeight,
                      dst + mWidth * mHeight, mHeight / 2,
                      dst + mWidth * mHeight * 5 / 4, mHeight / 2,
                      0, 0, // 以左上角为原点，裁剪起始点
                      mWidth, mHeight, //320 * 240
                      mWidth, mHeight, //240 * 320
                      kRotate90,
                      FOURCC_NV21);
    }

//    //这里录制视频，验明，把dst转为rgb，然后保存到本地
//    uint8 *dst_argb = (uint8 *) malloc(size);
//    ConvertToARGB(dst, size,
//                  dst_argb, mHeight / 2,
//                  0, 0,
//                  mHeight, mWidth,
//                  mHeight, mWidth,
//                  0,
//                  FOURCC_I420);
//    //保存图片到本地,保存的图片是错的，只有1*1px。。。。
//    FILE *file = fopen(output_p, "w+");
//    if(file != NULL){
//        fputs(dst_argb, file);
//        fflush(file);
//        fclose(file);
//    }

    uint8 *dst2 = (uint8 *) malloc(size);
//    I420Mirror(dst, mWidth,
//               dst + mWidth * mHeight * 4 / 4, mHeight / 2,
//               dst + mWidth * mHeight * 5 / 4, mHeight / 2,
//               dst2, mWidth,
//               dst2 + mWidth * mHeight * 4 / 4, mHeight / 2,
//               dst2 + mWidth * mHeight * 5 / 4, mHeight / 2,
//               mWidth, mHeight);
    //                  yyyyvuvu
    //这时候dst已经是i420，yyyyUUVV
    memcpy(pic_in.img.plane[0], dst, y_len);
    int i;
    for (i = 0; i < u_len; i++) {
        *(u + i) = *(dst + y_len + i);
        *(v + i) = *(dst + y_len + u_len + i);
    }
//    for (i = 0; i < u_len; i++) {
//        *(u + i) = *(nv21_buffer + y_len + i * 2 + 1);
//        *(v + i) = *(nv21_buffer + y_len + i * 2);
//    }
    x264_nal_t *nal = NULL; //NAL
    int n_nal = -1; //NALU的个数
    //进行h264编码，对yum420编码为h264
    if (x264_encoder_encode(video_encode_handle, &nal, &n_nal, &pic_in, &pic_out) < 0) {
        LOGE("%s", "编码失败");
        return;
    }
    //这里是加入队列
    //使用rtmp协议将h264编码的视频数据发送给流媒体服务器
    //帧分为关键帧和普通帧，为了提高画面的纠错率，关键帧应包含SPS和PPS数据
    int sps_len, pps_len;
    unsigned char sps[100];
    unsigned char pps[100];
    memset(sps, 0, 100);
    memset(pps, 0, 100);
    pic_in.i_pts += 1; //顺序累加
    //遍历NALU数组，根据NALU的类型判断
    LOGE("一帧有%d个nal单元", n_nal);
    for (i = 0; i < n_nal; i++) {
        if (nal[i].i_type == NAL_SPS) {
            //复制SPS数据
            sps_len = nal[i].i_payload - 4;
            memcpy(sps, nal[i].p_payload + 4, sps_len); //不复制四字节起始码
            //todo 这里如果没有怎么添加？
        } else if (nal[i].i_type == NAL_PPS) {
            //复制PPS数据
            pps_len = nal[i].i_payload - 4;
            memcpy(pps, nal[i].p_payload + 4, pps_len); //不复制四字节起始码

            //发送序列信息
            //h264关键帧会包含SPS和PPS数据
            LOGE("来了一个新的图片组", n_nal);
            add_264_sequence_header(pps, sps, pps_len, sps_len);

        } else {
            //发送帧信息
            add_264_body(nal[i].p_payload, nal[i].i_payload);
        }
    }
    free(dst);
    free(dst2);
    (*env)->ReleaseStringUTFChars(env, output_path, output_p);
    (*env)->ReleaseByteArrayElements(env, data_, nv21_buffer, NULL);
}


/**
 * 设置音频参数
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_setAudioOptions(JNIEnv *env, jobject instance,
                                                  jint sampleRateInHz, jint channel) {
    audio_encode_handle = faacEncOpen(sampleRateInHz, channel, &nInputSamples, &nMaxOutputBytes);
    if (!audio_encode_handle) {
        LOGE("音频编码器打开失败");
        return;
    }
    //设置音频编码参数，初始化一段空间
    faacEncConfigurationPtr p_config = faacEncGetCurrentConfiguration(audio_encode_handle);
    p_config->mpegVersion = MPEG4;
    p_config->allowMidside = 1;
    p_config->aacObjectType = LOW;
    p_config->outputFormat = 0; //输出是否包含ADTS头
    p_config->useTns = 1; //时域噪音控制,大概就是消爆音
    p_config->useLfe = 0;
    p_config->inputFormat = FAAC_INPUT_16BIT; //如果直接pcm转换acc，需要加上这一行
    p_config->quantqual = 100;
    p_config->bandWidth = 0; //频宽
    p_config->shortctl = SHORTCTL_NORMAL;

    if (!faacEncSetConfiguration(audio_encode_handle, p_config)) {
        LOGE("%s", "音频编码器配置失败..");
        return;
    }

    LOGI("%s", "音频编码器配置成功");
}

/**
 * 添加AAC头信息
 */
void add_aac_sequence_header() {
    //获取aac头信息的长度
    unsigned char *buf;
    unsigned long len; //长度
    faacEncGetDecoderSpecificInfo(audio_encode_handle, &buf, &len);
    int body_size = 2 + len;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    //RTMPPacket初始化
    RTMPPacket_Alloc(packet, body_size);
    RTMPPacket_Reset(packet);
    unsigned char *body = packet->m_body;
    //头信息配置
    /*AF 00 + AAC RAW data*/
    body[0] = 0xAF;//10 5 SoundFormat(4bits):10=AAC,SoundRate(2bits):3=44kHz,SoundSize(1bit):1=16-bit samples,SoundType(1bit):1=Stereo sound
    body[1] = 0x00;//AACPacketType:0表示AAC sequence header
    memcpy(&body[2], buf, len); /*spec_buf是AAC sequence header数据*/
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    add_rtmp_packet(packet);
    free(buf);

}

/**
 * 添加AAC rtmp packet
 */
void add_aac_body(unsigned char *buf, int len) {
    int body_size = 2 + len;
    RTMPPacket *packet = (RTMPPacket *) malloc(sizeof(RTMPPacket));
    //RTMPPacket初始化
    RTMPPacket_Alloc(packet, body_size);
    RTMPPacket_Reset(packet);
    unsigned char *body = packet->m_body;
    //头信息配置
    /*AF 00 + AAC RAW data*/
    body[0] = 0xAF;//10 5 SoundFormat(4bits):10=AAC,SoundRate(2bits):3=44kHz,SoundSize(1bit):1=16-bit samples,SoundType(1bit):1=Stereo sound
    body[1] = 0x01;//AACPacketType:1表示AAC raw
    memcpy(&body[2], buf, len); /*spec_buf是AAC raw数据*/
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = body_size;
    //TODO 这里是0x04
    packet->m_nChannel = 0x04;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nTimeStamp = RTMP_GetTime() - start_time;
    add_rtmp_packet(packet);
}

/**
 * 推送音频， pcm数据转换为aac数据
 */
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_ffmpegstudy_FFmpegUtils_fireAudio(JNIEnv *env, jobject instance, jbyteArray buffer,
                                            jint len_mic) {
    jbyte *buffer_from_mic = (*env)->GetByteArrayElements(env, buffer, 0);

//    //使用下边六行的方法也能达到推送，设置时需要设置inputSample = 16bit， 这里是int32，所以会过一会就缓冲，不知道为什么
//    jbyte *aac_buffer = (jbyte *) malloc(nMaxOutputBytes * sizeof(jbyte));
//    int bytesLen = faacEncEncode(audio_encode_handle, (int32_t *) buffer_from_mic, nInputSamples,
//                                 aac_buffer,
//                                 nMaxOutputBytes);
//    add_aac_body(aac_buffer, bytesLen);
//    free(aac_buffer);

    //对mic输入的pcm数据进行位转换，根据aac一次能够处理样本的大小，进行采样, int = 4 byte， 32位
    int16_t *buffer_pcm_input_to_aac = (int16_t *) malloc(
            nInputSamples * sizeof(int16_t)); //一次输入的样本大小

    //输出的acc流buff
    unsigned char *buffer_aac_output = (unsigned char *) malloc(
            nMaxOutputBytes * sizeof(unsigned char));

    int nByteCount = 0; //
    unsigned int nBufferSize = (unsigned int) len_mic / 2; //每次读取一半，从8位变到16位，长度应该是变为之前的一半
    unsigned short *buffer_from_mic_copy = (unsigned short *) buffer_from_mic; //输入的pcm总数据


    while (nByteCount < nBufferSize) { //这里为什么是小于长度的一半？两个声道的问题？4
        int audioLength = nInputSamples;

        //nInputSamples为2000， nBufferSize = 3200，开始处理一部分，再处理一部分，这里是处理剩余的那一部分
        if ((nByteCount + nInputSamples) >= nBufferSize) {
            audioLength = nBufferSize - nByteCount;

        }

        int i;
        //对一次能够处理的pcm做赋值，audioLength= 2000
        for (i = 0; i < audioLength; i++) {//每次从实时的pcm音频队列中读出量化位数为8的pcm数据。
            int16_t *s = ((int16_t *) buffer_from_mic_copy +
                          nByteCount)[i]; //按照16位的int类型读取，short，取16位
            buffer_pcm_input_to_aac[i] = s; //按16位，两个字节进行取值，然后赋值，进行转换，之前<<8，就是瞎写
        }

        nByteCount += nInputSamples;
        //利用FAAC进行编码，pcmbuf为转换后的pcm流数据，audioLength为调用faacEncOpen时得到的输入采样数，bitbuf为编码后的数据buff，nMaxOutputBytes为调用faacEncOpen时得到的最大输出字节数
        int byteslen = faacEncEncode(audio_encode_handle, buffer_pcm_input_to_aac, audioLength,
                                     buffer_aac_output, nMaxOutputBytes);
        if (byteslen < 1) {
            continue;
        }
        add_aac_body(buffer_aac_output, byteslen);//从bitbuf中得到编码后的aac数据流，放到数据队列
    }

    if (buffer_aac_output)
        free(buffer_aac_output);
    if (buffer_pcm_input_to_aac)
        free(buffer_pcm_input_to_aac);
    (*env)->ReleaseByteArrayElements(env, buffer, buffer_from_mic, NULL);
}