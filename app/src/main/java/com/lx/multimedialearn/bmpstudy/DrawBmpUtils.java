package com.lx.multimedialearn.bmpstudy;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import com.lx.multimedialearn.bmpstudy.stl.Model;
import com.lx.multimedialearn.bmpstudy.stl.Point;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * 画bitmap通用工具类
 *
 * @author lixiao
 * @since 2017-09-06 18:26
 */
public class DrawBmpUtils {
    public static boolean isSupportGLES20(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return manager.getDeviceConfigurationInfo().reqGlEsVersion >= 0x20000;
    }

    /**
     * 浮点数转换为FloatBuffer
     *
     * @return
     */
    public static FloatBuffer float2Buffer(float[] buffers) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buffers.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer result = byteBuffer.asFloatBuffer();
        result.put(buffers);
        result.position(0);
        return result;
    }

    /**
     * 把长度为4的字节数据转换为int
     *
     * @param bytes
     * @param offset 起始位置
     * @return
     */
    public static int byte4ToInt(byte[] bytes, int offset) {
        int b3 = bytes[offset + 3] & 0xFF;
        int b2 = bytes[offset + 2] & 0xFF;
        int b1 = bytes[offset + 1] & 0xFF;
        int b0 = bytes[offset + 0] & 0xFF;
        return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    /**
     * 字节数组转换为短整型
     *
     * @param bytes
     * @param offset
     * @return
     */
    public static short byte2ToShort(byte[] bytes, int offset) {
        int b1 = bytes[offset + 1] & 0xFF;
        int b0 = bytes[offset + 0] & 0xFF;
        return (short) ((b1 << 8) | b0);
    }

    /**
     * 字节数组转换为浮点型,从byte中读取float的方法
     */
    public static float byte4ToFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(byte4ToInt(bytes, offset));
    }

    /**
     * 获取多个model中的最大最小Point
     *
     * @param models
     * @param isMin  获取最小：true
     * @return
     */
    public static Point modelBorder(List<Model> models, boolean isMin) {
        Point p;
        if (isMin) {
            p = new Point(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        } else {
            p = new Point(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
        }
        for (Model model : models) {
            if (isMin) { //获取多个model中的最小值
                if (model.minX < p.x) {
                    p.x = model.minX;
                }
                if (model.minY < p.y) {
                    p.y = model.minY;
                }
                if (model.minZ < p.z) {
                    p.z = model.minZ;
                }
            } else {
                if (model.maxX > p.x)
                    p.x = model.maxX;
                if (model.maxY > p.y)
                    p.y = model.maxY;
                if (model.maxZ > p.z)
                    p.z = model.maxZ;
            }
        }
        return p;
    }

    /**
     * 通过多个model获取中心点
     *
     * @param models
     * @return
     */
    public static Point getCenter(List<Model> models) {
        Point min = modelBorder(models, true);
        Point max = modelBorder(models, false);
        float cx = min.x + (max.x - min.x) / 2;
        float cy = min.y + (max.y - min.y) / 2;
        float cz = min.z + (max.z - min.z) / 2;
        return new Point(cx, cy, cz);
    }

    /**
     * 获取多个model的最大半径，这样能包住整个图片
     *
     * @param models
     * @return
     */
    public static float getR(List<Model> models) {
        Point minP = modelBorder(models, true);
        Point maxP = modelBorder(models, false);
        float rx = maxP.x - minP.x;
        float ry = maxP.y - minP.y;
        float rz = maxP.z - minP.z;
        float r = (float) (Math.sqrt(rx * rx + ry * ry + rz * rz) / 2);
        return r;
    }

    /**
     * 读取显存中的内容，保存为图片，必须是显存中有内容
     * 把显存的内容读取到内存，推送出去，这个方法很重要
     *
     * @param width
     * @param height
     */
    public static void saveImage(int width, int height) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.position(0);
        long start = System.nanoTime();
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        long end = System.nanoTime();
        Log.i("sys.out", "glReadPixel时间：" + (end - start));
        saveRgb2Bitmap(byteBuffer, Environment.getExternalStorageDirectory().getAbsolutePath() + "/gl_dump_" + width + "_" + height + ".png", width, height);
    }

    /**
     * 保存图片
     *
     * @param byteBuffer
     * @param s
     * @param width
     * @param height
     */
    public static void saveRgb2Bitmap(ByteBuffer byteBuffer, String s, int width, int height) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(s));
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(byteBuffer);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
            bmp.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveBitmap(final Bitmap b, String path) {
        File folder = new File(path);
        if (!folder.exists() && !folder.mkdirs()) {
            Log.i("sys.out", "保存图片生成文件夹出错");
            return;
        }
        long dataTake = System.currentTimeMillis();
        final String jpegName = path + dataTake + ".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
