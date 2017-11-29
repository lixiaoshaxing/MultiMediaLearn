package com.lx.multimedialearn.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 文件的读取，输出
 *
 * @author lixiao
 * @since 2017-09-20 10:45
 */
public class FileUtils {

    /**
     * 根据后缀创建音视频地址
     *
     * @param suffix aac,mp4,pcm等
     * @param type   0: 视频 1：音频
     * @return
     */
    public static String createFilePath(String suffix, int type) {
        String path;
        if (type == 0) {
            path = createCommonDir() + "VID_" + new SimpleDateFormat("HH_mm_ss").format(new Date()) + "." + suffix;
        } else {
            path = createCommonDir() + "AUD_" + new SimpleDateFormat("HH_mm_ss").format(new Date()) + "." + suffix;
        }
        return path;
    }

    /**
     * 创建通用文件夹，并返回通用文件夹地址
     */
    public static String createCommonDir() {
        File file = new File(Environment.getExternalStorageDirectory(), "/abc/");
        if (!file.exists()) {
            file.mkdir();
        }
        if (file.listFiles().length > 10) { //如果超过10个文件，清空
            File[] lists = file.listFiles();
            for (File temp : lists) {
                temp.delete();
            }
        }
        return file.getAbsolutePath() + "/";
    }

    /**
     * 保存Bitmap的byte数组到文件中
     *
     * @param fileName
     * @param data
     */
    public static void convertBitmap2File(Context context, String fileName, byte[] data) {
        FileOutputStream fos = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            byte[] newData = null; //要对图片进行转向
            Bitmap oldBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix matrix = new Matrix();
            matrix.setRotate(270);
            Bitmap newBmp = Bitmap.createBitmap(oldBmp, 0, 0, oldBmp.getWidth(), oldBmp.getHeight(), matrix, true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            newBmp.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            newData = bos.toByteArray();
            fos.write(newData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 把jpeg文件转换为Bitmap
     *
     * @param context
     * @param fileName
     * @param width
     * @param height
     * @return
     */
    public static Bitmap convertFile2Bitmap(Context context, String fileName, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);
        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;
        int inSampleSize = 1;
        if (srcWidth > width || srcHeight > height) {
            inSampleSize = Math.max((int) (srcWidth / width), (int) (srcHeight / height));
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap bm = BitmapFactory.decodeFile(fileName, options);
        return bm;
    }

    /**
     * @param context
     * @param oldPath asset文件下的地址：/aa
     * @param newPath
     */
    public static void copyFileFromAssets(Context context, String oldPath, String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                File file = new File(newPath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFileFromAssets(context, oldPath + "/" + fileName, newPath + "/" + fileName); //使用递归进行复制
                }
            } else {//如果是文件
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取资源文件夹中的glsl文件为字符串
     *
     * @param context
     * @param resourceId
     * @return
     */
    public static String readTextFileFromResource(Context context, int resourceId) {
        StringBuilder sb = new StringBuilder();
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            is = context.getResources().openRawResource(resourceId);
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String nextLine;
            while ((nextLine = br.readLine()) != null) {
                sb.append(nextLine);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("sys.out", "glsl文件读取出错");
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
