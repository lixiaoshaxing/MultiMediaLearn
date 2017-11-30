cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)

set(lib_src_DIR ${CMAKE_CURRENT_SOURCE_DIR})

set(lib_build_DIR $ENV{HOME}/tmp)
file(MAKE_DIRECTORY ${lib_build_DIR})

add_subdirectory(${lib_src_DIR}/multimedia ${lib_build_DIR}/multimedia) #音视频处理添加子cmakeLists
add_subdirectory(${lib_src_DIR}/voicechange ${lib_build_DIR}/voicechange) #fmod，音频处理
add_subdirectory(${lib_src_DIR}/libyuv ${lib_build_DIR}/libyuv) #yuv处理，图像处理
