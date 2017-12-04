cmake_minimum_required(VERSION 3.4.1)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/../../../../distribution)

add_library(opencv_utils SHARED OpencvUtils.cpp android_utils.cpp Scanner.cpp)
target_include_directories(opencv_utils PRIVATE
                           ${distribution_DIR}/opencv/include)

add_library(opencv_imgproc STATIC IMPORTED)
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/opencv/lib/${ANDROID_ABI}/libopencv_imgproc.a)

add_library(opencv_core STATIC IMPORTED)
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/opencv/lib/${ANDROID_ABI}/libopencv_core.a)

find_library( log-lib log )

find_library(jnigraphics-lib jnigraphics)

target_link_libraries(opencv_utils opencv_imgproc opencv_core ${log-lib} ${jnigraphics-lib})