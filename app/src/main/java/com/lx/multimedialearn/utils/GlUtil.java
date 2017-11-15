package com.lx.multimedialearn.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;
import static android.opengl.GLUtils.texImage2D;


/**
 * GLUtil对源码GLUtil进行封装
 *
 * @author lixiao
 * @since 2017-09-17 16:59
 */
public class GlUtil {
    /**
     * Identity matrix for general use.  Don't modify or life will get weird.
     */
    public static final float[] IDENTITY_MATRIX;

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_SHORT = 2;

    private GlUtil() {
    }     // do not instantiate

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();// create empty OpenGL ES Program
        checkGlError("glCreateProgram");
        if (program == 0) {
            LOGUtils.logE("Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);// add the vertex shader to program
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);// add the fragment shader to program
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);// creates OpenGL ES program executables
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LOGUtils.logE("Could not link program: ");
            LOGUtils.logE(GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     * 编译顶点着色器
     *
     * @param shaderCode
     * @return
     */
    public static int compileVertexShader(String shaderCode) {
        return compileShader(GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * 编译片段着色器
     *
     * @param shaderCode
     * @return
     */
    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GL_FRAGMENT_SHADER, shaderCode);
    }

    /**
     * 真正的编译
     *
     * @return
     */
    public static int compileShader(int type, String shaderCode) {
        int shaderObjectId = glCreateShader(type);//1. 创建着色器，返回对象id，如果为0，则失败
        if (shaderObjectId == 0) {
            Log.e("sys.out", "创建着色器对象失败");
        }
        glShaderSource(shaderObjectId, shaderCode); //2. 关联着色器对象和源码，这里通过id传输
        glCompileShader(shaderObjectId); //3. 编译上边上传的源码
        int[] compileStatus = new int[1];  // 4. 检查编译状态，使用数组获取值
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) { //如果编译失败
            glDeleteShader(shaderObjectId);
            Log.e("sys.out", "着色器编译失败");
            return 0;
        }
        return shaderObjectId;
    }

    /**
     * 链接顶点着色器和片段着色器，生成program
     *
     * @return
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        int programObjectId = glCreateProgram(); //1. 创建对象
        if (programObjectId == 0) {
            Log.e("sys.out", "创建programObject失败");
        }
        glAttachShader(programObjectId, vertexShaderId); //2. 附着Shader对象
        glAttachShader(programObjectId, fragmentShaderId);

        glLinkProgram(programObjectId); //3. 链接
        int[] linkStatus = new int[1]; //4. 检查创建program创建
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e("sys.out", "链接Program失败");
            return 0;
        }
        return programObjectId;
    }

    /**
     * 判断当前程序能不能正常运行
     *
     * @param programObjectId
     * @return
     */
    public static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        if (validateStatus[0] == 0) {
            Log.e("sys.out", "program程序不能正常运行");
        }
        return validateStatus[0] != 0;
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            LOGUtils.logE("Could not compile shader " + shaderType + ":");
            LOGUtils.logE(" " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            LOGUtils.logE(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Creates a texture from raw data.
     *
     * @param data   Image data, in a "direct" ByteBuffer.
     * @param width  Texture width, in pixels (not bytes).
     * @param height Texture height, in pixels.
     * @param format Image data format (use constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        int textureHandle;

        GLES20.glGenTextures(1, textureHandles, 0);
        textureHandle = textureHandles[0];
        GlUtil.checkGlError("glGenTextures");

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("loadImageTexture");

        // Load the data from the buffer into the texture handle.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width, height, 0, format,
                GLES20.GL_UNSIGNED_BYTE, data);
        GlUtil.checkGlError("loadImageTexture");

        return textureHandle;
    }

    /**
     * 加载图像资源，生成纹理对象，返回纹理对象ID，宽高信息，长度为3
     * int[] 数组：0：存放生成的textureID， 1：存放bitmap的width， 2：存放bitmap的height，用来根据bitmap进行缩放处理
     * 注意：不能把bitmap传出去，因为bitmap可能会被回收
     *
     * @param context
     * @param resourceId
     * @return
     */
    public static int[] loadTexture(Context context, int resourceId) {
        int[] textureInfo = new int[3];
        GLES20.glGenTextures(1, textureInfo, 0); //生成纹理对象id
        if (textureInfo[0] == 0) {
            Log.e("sys.out", "生成纹理对象ID出错");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();//加载图片资源，生成Bitmap，OpenGL只能识别Bitmap
        options.inScaled = false;

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        if (bitmap == null) {
            Log.e("sys.out", "读取Bitmap出错");
            GLES20.glDeleteTextures(1, textureInfo, 0);
            return new int[]{0, 0, 0};
        }
        textureInfo[1] = bitmap.getWidth();
        textureInfo[2] = bitmap.getHeight();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInfo[0]); //告诉以后OpenGL空间中使用纹理都调用这个对象
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,  //对于纹理缩小，才有mip双线性插值过滤器
                GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, //对于纹理放大，采用线性插值过滤器
                GLES20.GL_LINEAR);
        texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0); //加载纹理到OpenGL中，OpenGL读入Bitmap位图，复制给当前绑定的对象
        bitmap.recycle(); //复制完后，生成了纹理，位图已经不需要

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D); //生成Mip贴图，供纹理放大缩小时使用
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0); //纹理已经加载到OpenGL空间中，绑定到0就是解除绑定
        return textureInfo;
    }

    /**
     * 从File中加载图片
     *
     * @param context
     * @param path
     * @return
     */
    public static int[] loadTexture(Context context, String path) {
        int[] textureInfo = new int[3];
        GLES20.glGenTextures(1, textureInfo, 0); //生成纹理对象id
        if (textureInfo[0] == 0) {
            Log.e("sys.out", "生成纹理对象ID出错");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();//加载图片资源，生成Bitmap，OpenGL只能识别Bitmap
        options.inScaled = false;

        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), path, options);
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        if (bitmap == null) {
            Log.e("sys.out", "读取Bitmap出错");
            GLES20.glDeleteTextures(1, textureInfo, 0);
            return new int[]{0, 0, 0};
        }
        textureInfo[1] = bitmap.getWidth();
        textureInfo[2] = bitmap.getHeight();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureInfo[0]); //告诉以后OpenGL空间中使用纹理都调用这个对象
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,  //对于纹理缩小，才有mip双线性插值过滤器
                GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, //对于纹理放大，采用线性插值过滤器
                GLES20.GL_LINEAR);
        texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0); //加载纹理到OpenGL中，OpenGL读入Bitmap位图，复制给当前绑定的对象
        bitmap.recycle(); //复制完后，生成了纹理，位图已经不需要

        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D); //生成Mip贴图，供纹理放大缩小时使用
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0); //纹理已经加载到OpenGL空间中，绑定到0就是解除绑定
        return textureInfo;
    }

    /**
     * 加载天空图，生成立体图形
     *
     * @param context
     * @param cubeResource 六个面的资源id：左，右，下，上，前，后
     * @return
     */
    public static int loadCubeMap(Context context, int[] cubeResource) {
        int[] textureObjectIds = new int[1];
        GLES20.glGenTextures(1, textureObjectIds, 0);
        if (textureObjectIds[0] == 0) {
            Log.w("sys.out", "加载天空图出错：生成纹理对象出错");
            return 0;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap[] cubeBitmap = new Bitmap[6]; //加载图片
        for (int i = 0; i < cubeResource.length; i++) {
            cubeBitmap[i] = BitmapFactory.decodeResource(context.getResources(), cubeResource[i], options);
            if (cubeBitmap[i] == null) {
                Log.w("sys.out", "生成天空图出错：加载图片失败");
                GLES20.glDeleteTextures(1, textureObjectIds, 0);
                return 0;
            }
        }
        //配置纹理过滤器
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureObjectIds[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR); //天空图都是从同一个观察点观察，不需要mip贴图技术了

        //绑定贴图和立方体的各面
        texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, cubeBitmap[0], 0);
        texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, cubeBitmap[1], 0);
        texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, cubeBitmap[2], 0);
        texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, cubeBitmap[3], 0);
        texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, cubeBitmap[4], 0);
        texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, cubeBitmap[5], 0); //使用的左手坐标系

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0); //解除绑定
        for (Bitmap bitmap : cubeBitmap) {
            bitmap.recycle();
        }
        return textureObjectIds[0];
    }

    /**
     * 创建一个相机使用的纹理ID
     *
     * @return
     */
    public static int createCameraTextureID() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);//把相机中的图像yuv转换为rgb
        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,  //设置剪裁参数
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    /**
     * 生成普通纹理ID，没有在纹理上绑定bitmap
     *
     * @return
     */
    public static int createTextureID() {
        int[] texture = new int[1];
        //生成纹理
        GLES20.glGenTextures(1, texture, 0);
        //生成纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Short数组转换为ShortBuffer
     *
     * @param coords
     * @return
     */
    public static ShortBuffer createShortBuffer(short[] coords) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(coords.length * SIZEOF_SHORT);
        byteBuffer.order(ByteOrder.nativeOrder());
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(coords);
        shortBuffer.position(0);
        return shortBuffer;
    }


    /**
     * Writes GL version info to the log.
     */
    public static void logVersionInfo() {
        LOGUtils.logE("vendor  : " + GLES20.glGetString(GLES20.GL_VENDOR));
        LOGUtils.logE("renderer: " + GLES20.glGetString(GLES20.GL_RENDERER));
        LOGUtils.logE("version : " + GLES20.glGetString(GLES20.GL_VERSION));

    }

    /**
     * 检查是否支持版本2.0
     *
     * @param context
     * @return
     */
    public static boolean checkGLEsVersion_2(Context context) {
        return checkGLEsVersion(context, 0x20000);
    }

    /**
     * 检查支持的OpenGL版本
     * 2.0->0x20000
     *
     * @return
     */
    public static boolean checkGLEsVersion(Context context, int version) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= version;
    }
}
