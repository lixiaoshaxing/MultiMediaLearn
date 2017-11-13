package com.lx.multimedialearn.bmpstudy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.lx.multimedialearn.utils.ScreenUtils;

/**
 * 双缓冲的学习
 * 参考blog：https://stackoverflow.com/questions/4918079/android-drawing-a-canvas-to-an-imageview?answertab=votes
 * SurfaceView使用了双缓冲：http://blog.csdn.net/rabbit_in_android/article/details/50807765
 *
 * @author lixiao
 * @since 2017-09-05 22:04
 */
public class DoubleBufferView extends View {
    private int view_width = 0;
    private int view_height = 0;
    private float prevX = 0;
    private float prevY = 0;
    private Path mPath;
    private Bitmap mCacheBitmap;
    private Canvas mCacheCanvas;
    private Paint mPaint;

    public DoubleBufferView(Context context) {
        super(context);
        initView();
    }

    public DoubleBufferView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public DoubleBufferView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        //获取屏幕的宽高
        view_width = ScreenUtils.getScreenWidth(getContext());
        view_height = ScreenUtils.getScreenHeight(getContext());
        //创建双缓冲板
        mCacheBitmap = Bitmap.createBitmap(view_width, view_height, Bitmap.Config.ARGB_8888);//创建图片缓冲区
        mCacheCanvas = new Canvas(mCacheBitmap);//将该Canvas绘制到缓冲区中
        //创建Path
        mPath = new Path();
        mPaint = new Paint();
        mPaint.setStrokeWidth(3);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currentX = event.getX();
        float currentY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                prevX = currentX;
                prevY = currentY;
                mPath.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                prevX = currentX;
                prevY = currentY;
                mPath.quadTo(prevX, prevY, currentX, currentY);
                break;
            case MotionEvent.ACTION_UP:
                mPath.reset();
                break;
        }
        mCacheCanvas.drawPath(mPath, mPaint);
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        //真实的画布只用画一个bitmap
        canvas.drawBitmap(mCacheBitmap, 0, 0, paint);
    }
}
