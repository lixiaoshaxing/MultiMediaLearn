cmake_minimum_required(VERSION 3.4.1)

add_library(multi_media SHARED src/main/cpp/multi_media.cpp )

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)
find_library(GLESv2-lib GLESv2)

target_link_libraries(multi_media
                        ${log-lib}
                        ${jnigraphics-lib}
                        ${GLESv2-lib})