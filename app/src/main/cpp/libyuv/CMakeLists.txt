cmake_minimum_required(VERSION 3.4.1)

set(distribution_DIR ${CMAKE_SOURCE_DIR})

add_library(yuv_utils SHARED yuv_utils.cpp)
include_directories(src/main/cpp/libyuv/inc/)

add_library(yuv SHARED IMPORTED)
set_target_properties(yuv PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/libyuv/lib/${ANDROID_ABI}/libyuv.so)

find_library(log-lib log)

target_link_libraries(yuv_utils ${log-lib} yuv)