//
// Created by 李晓 on 17/11/30.
//
#include <jni.h>
#include "inc/libyuv.h"
#include "inc/libyuv/convert_argb.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_utils_YuvUtils_rgba_1mirror(JNIEnv *env, jclass type, jbyteArray src_,
                                                        jbyteArray dst_,
                                                        jint width, jint height) {
    jbyte *src = env->GetByteArrayElements(src_, NULL);
    jbyte *dst = env->GetByteArrayElements(dst_, NULL);

    //src: 原始是rgba，则调用需要使用abgr->mirror,只有argbMirror，则需要abgr->argb->mirror
    uint8 *dst2 = (uint8 *) malloc(width * height * 4); //每个像素4个byte
    //libyuv::RGBAToARGB((const uint8 *) src, width, dst2, width, width, height);
    //libyuv::ARGBMirror(dst2, width, (uint8 *) dst, width, width, height);
    libyuv::ABGRToARGB((uint8 *) src, width * 4, (uint8 *) dst2, width * 4, width, height);
    libyuv::ARGBMirror((uint8 *) dst2, width * 4, (uint8 *) dst, width * 4, width, height);
    free(dst2);
    env->ReleaseByteArrayElements(src_, src, 0);
    env->ReleaseByteArrayElements(dst_, dst, 0);
}

//libyuv中argb顺序与java层传过来的相反，如果要转rgba，则在libyuv中找abgr相关的方法
extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_utils_YuvUtils_rgba_1nv21(JNIEnv *env, jclass type, jbyteArray src_,
                                                      jbyteArray dst_, jint width, jint height) {
    jbyte *src = env->GetByteArrayElements(src_, NULL);
    jbyte *dst = env->GetByteArrayElements(dst_, NULL);
    //采集到的是rgba，使用libyuv，应该是abgr，转换为nv21，因为没有abgr2nv21,只有abgr2I420,转换后，i420->nv21
    libyuv::ABGRToI420((uint8 *) src, width * 4, //要I420就够了
                       (uint8 *) dst, width,
                       (uint8 *) (dst + width * height), width / 2,
                       (uint8 *) (dst + width * height + width * height / 4), width / 2,
                       width, height);

    uint8 *dst2 = (uint8 *) malloc(width * height * 3 / 2);
//    libyuv::ABGRToI420((uint8 *) src, width * 4, //注释的这是转nv21，同时要把上边转i420的注释了
//                       dst2, width,
//                       (dst2 + width * height), width / 2,
//                       (dst2 + width * height + width * height / 4), width / 2,
//                       width, height);
//    libyuv::I420ToNV21(dst2, width,
//                       dst2 + width * height, width / 2,
//                       dst2 + width * height + width * height / 4, width / 2,
//                       (uint8 *) dst, width,
//                       (uint8 *) (dst + width * height), width / 2,
//                       width, height);
    env->ReleaseByteArrayElements(src_, src, 0);
    env->ReleaseByteArrayElements(dst_, dst, 0);
    free(dst2);
}