cmake_minimum_required(VERSION 3.4.1)

set(JNI_DIR ${PROJECT_SOURCE_DIR}/..) #设置jni路径
set(distribution_DIR ${CMAKE_SOURCE_DIR})

add_library( voice_change SHARED voice_changer.cpp)
include_directories(src/main/cpp/voicechange/inc/) #包含的头文件,依赖的so库都会提供头文件共使用，必须列出来

# 添加so库，注意so库存放位置，这些so库需要load，所以放在外部的jniLibs中
add_library( fmod SHARED IMPORTED)
#set_target_properties(fmod PROPERTIES IMPORTED_LOCATION ${JNI_DIR}/jniLibs/${ANDROID_ABI}/libfmod.so)  #设置路径
set_target_properties(fmod PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/voicechange/lib/${ANDROID_ABI}/libfmod.so)  #设置路径

#添加so库，注意so库存放位置
add_library( fmodL SHARED IMPORTED)
#set_target_properties(fmodL PROPERTIES IMPORTED_LOCATION ${JNI_DIR}/jniLibs/${ANDROID_ABI}/libfmodL.so)
set_target_properties(fmodL PROPERTIES IMPORTED_LOCATION ${distribution_DIR}/voicechange/lib/${ANDROID_ABI}/libfmodL.so)


#使用的ndk自带库，使用find
find_library( log-lib log )

#本地库依赖外来库，进行连接，要使用，必须链接，综合生成一个终极so，原so不能删除，为什么不能删除？
target_link_libraries(voice_change fmod fmodL)