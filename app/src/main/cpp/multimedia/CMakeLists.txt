cmake_minimum_required(VERSION 3.4.1)

#set(CMAKE_VERBOSE_MAKEFILE on)

add_library(multi_media SHARED multi_media.cpp)



find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(GLESv2-lib GLESv2)

target_link_libraries(multi_media ${log-lib} ${jnigraphics-lib} ${GLESv2-lib})