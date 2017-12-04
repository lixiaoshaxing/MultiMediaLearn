cmake_minimum_required(VERSION 3.4.1)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/../../../../distribution)

add_library(ffmpeg_utils SHARED #生成libffmpeg_utils.so
            ffmpeg_play.cpp #ffmpeg转码：解析pcm，播放pcm，解析mp4
            queue.c #队列，供ffmpeg使用
            native-player.c #ffmpeg音视频同步播放
            native-rmtp.c #rtmp推流本地视频
            native-live.c #直播
            queue1.c #直播用的队列
            )
include_directories(${distribution_DIR}/ffmpeg/include) #包含所有头文件

add_library(native-video SHARED native-video.c opensl_io.c)  #opensl

add_library(avcodec-56 SHARED IMPORTED)
set_target_properties(avcodec-56 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libavcodec-56.so)  #设置路径

add_library(avdevice-56 SHARED IMPORTED)
set_target_properties(avdevice-56 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libavdevice-56.so)  #设置路径

add_library(avfilter-5 SHARED IMPORTED)
set_target_properties(avfilter-5 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libavfilter-5.so)  #设置路径

add_library(avformat-56 SHARED IMPORTED)
set_target_properties(avformat-56 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libavformat-56.so)  #设置路径

add_library(avutil-54 SHARED IMPORTED)
set_target_properties(avutil-54 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libavutil-54.so)  #设置路径

add_library(postproc-53 SHARED IMPORTED)
set_target_properties(postproc-53 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libpostproc-53.so)  #设置路径

add_library(swresample-1 SHARED IMPORTED)
set_target_properties(swresample-1 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libswresample-1.so)  #设置路径

add_library(swscale-3 SHARED IMPORTED)
set_target_properties(swscale-3 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libswscale-3.so)  #设置路径

add_library(yuv SHARED IMPORTED)
set_target_properties(yuv PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libffmpeg_yuv.so)


add_library(x264 STATIC IMPORTED)
set_target_properties(x264 PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libx264.a)  #引入静态链接库

add_library(faac STATIC IMPORTED)
set_target_properties(faac PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/libfaac.a)  #引入静态链接库

add_library(rtmpdump STATIC IMPORTED)
set_target_properties(rtmpdump PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/ffmpeg/lib/${ANDROID_ABI}/librtmp.a)  #引入静态链接库

find_library(log-lib log)

set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

target_link_libraries(ffmpeg_utils android ${log-lib} Threads::Threads avcodec-56 avdevice-56 avfilter-5 avformat-56 avutil-54 postproc-53 swresample-1 swscale-3 yuv x264 faac rtmpdump)

target_link_libraries(native-video android ${log-lib} OpenSLES)
