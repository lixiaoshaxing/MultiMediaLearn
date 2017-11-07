package com.lx.multimedialearn.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * 画布使用工具类
 *
 * @author lixiao
 * @since 2017-09-20 10:42
 */
public class CanvasUtils {

    /**
     * 对画布清空，并画一幅Bitmap
     *
     * @param canvas 要清空的画布
     * @param bitmap 要画的Bitmap
     */
    public static void clearCanvas(Canvas canvas, Bitmap bitmap, float left, float top) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint p = new Paint();
        canvas.drawBitmap(bitmap, left, top, p);
    }

    /**
     * 对画布清空方法2，并画一幅Bitmap
     *
     * @param canvas 要清空的画布
     * @param bitmap 要画的Bitmap
     */
    public static void clearCanvas2(Canvas canvas, Bitmap bitmap, float left, float top) {
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        canvas.drawBitmap(bitmap, left, top, p);
    }
}
