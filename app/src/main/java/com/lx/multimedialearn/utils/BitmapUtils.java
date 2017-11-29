package com.lx.multimedialearn.utils;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * 处理bitmap的相关工具类
 *
 * @author lixiao
 * @since 2017-11-06 14:05
 */
public class BitmapUtils {
    /**
     * 把缓冲区中的数据转换为bitmap
     *
     * @param width
     * @param height
     * @param ib
     * @return
     */
    public static Bitmap frame2Bitmap(int width, int height, IntBuffer ib) {
        int pixs[] = ib.array();
        // 扫描转置(OpenGl:左上->右下 Bitmap:左下->右上)
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width; x++) {
                int pos1 = y * width + x;
                int pos2 = (height - 1 - y) * width + x;

                int tmp = pixs[pos1];
                pixs[pos1] = (pixs[pos2] & 0xFF00FF00) | ((pixs[pos2] >> 16) & 0xff) | ((pixs[pos2] << 16) & 0x00ff0000); // ABGR->ARGB
                pixs[pos2] = (tmp & 0xFF00FF00) | ((tmp >> 16) & 0xff) | ((tmp << 16) & 0x00ff0000);
            }
        }
        if (height % 2 == 1) { // 中间一行
            for (int x = 0; x < width; x++) {
                int pos = (height / 2 + 1) * width + x;
                pixs[pos] = (pixs[pos] & 0xFF00FF00) | ((pixs[pos] >> 16) & 0xff) | ((pixs[pos] << 16) & 0x00ff0000);
            }
        }

        return Bitmap.createBitmap(pixs, width, height, Bitmap.Config.ARGB_8888);
    }

    //图片保存
    public static void saveBitmap(Bitmap b) {
        String path = Environment.getExternalStorageDirectory() + "/abc/";
        File folder = new File(path);
        if (!folder.exists() && !folder.mkdirs()) {
            //runOnUiThread,保存图片一般在子线程
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
        BitmapUtils.saveRgb2Bitmap(byteBuffer, Environment.getExternalStorageDirectory().getAbsolutePath() + "/gl_dump_" + width + "_" + height + ".png", width, height);
    }

    /**
     * 从gpu中读取bitmap
     *
     * @param width
     * @param height
     * @return
     */
    public static Bitmap getBitmap(int width, int height) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        byteBuffer.position(0);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer);
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(byteBuffer);
        return bmp;
    }

    /**
     * java层rgba->yuv，MediaCodec编码时需要yuv数据，从SurfaceView，Camera回调中都是rgba数据
     *
     * @param rgba
     * @param width
     * @param height
     * @param yuv
     */
    public static void rgbaToYuv(byte[] rgba, int width, int height, byte[] yuv) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + frameSize / 4;

        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                index = j * width + i;
                if (rgba[index * 4] > 127 || rgba[index * 4] < -128) {
                    Log.e("color", "-->" + rgba[index * 4]);
                }
                R = rgba[index * 4] & 0xFF;
                G = rgba[index * 4 + 1] & 0xFF;
                B = rgba[index * 4 + 2] & 0xFF;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }
            }
        }
    }
}
