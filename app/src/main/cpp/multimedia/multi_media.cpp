#include <jni.h>
#include <GLES2/gl2.h>

extern "C"
JNIEXPORT void JNICALL
Java_com_lx_multimedialearn_openglstudy_OpenGLJniUtils_glReadPixels(JNIEnv *env, jclass type_,
                                                                    jint x, jint y, jint width,
                                                                    jint height, jint format,
                                                                    jint type) {
    glReadPixels(x, y, width, height, format, type, 0);
}