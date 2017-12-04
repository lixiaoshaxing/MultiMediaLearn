package com.lx.multimedialearn.opencvstudy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * 根据OpenCV识别出的顶点，画蒙层
 *
 * @author lixiao
 * @since 2017-12-04 14:49
 */
@SuppressLint("AppCompatCustomView")
public class MaskImageView extends ImageView {
    private Path mMaskLinePath;
    private Paint mPaint;
    private int mPicWidth; //图片实际宽高
    private int mPicHeight;
    private int mPicLeft;  //图片实际距离左边的距离
    private int mPicTop;
    private float mScaleX; //图片x轴缩放比例，放入
    private float mScaleY;
    private float[] mMatrix = new float[9]; //图片在ImageView中缩放比例

    public MaskImageView(Context context) {
        super(context);
        init();
    }

    public MaskImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(10);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mMaskLinePath = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getDrawablePosition();
        if (mMaskLinePath != null) {
            canvas.drawPath(mMaskLinePath, mPaint);
        }
    }

    private void getDrawablePosition() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            getImageMatrix().getValues(mMatrix);
            mScaleX = mMatrix[Matrix.MSCALE_X];
            mScaleY = mMatrix[Matrix.MSCALE_Y];
            int origW = drawable.getIntrinsicWidth();
            int origH = drawable.getIntrinsicHeight();
            mPicWidth = Math.round(origW * mScaleX);
            mPicHeight = Math.round(origH * mScaleY);
            mPicLeft = (getWidth() - mPicWidth) / 2;
            mPicTop = (getHeight() - mPicHeight) / 2;
        }
    }

    public void setPoints(Point[] points) {
        if (points != null && points.length == 4) {
            mMaskLinePath = new Path();
            mMaskLinePath.reset();
            Point lt = points[0];
            Point rt = points[1];
            Point rb = points[2];
            Point lb = points[3];
            mMaskLinePath.moveTo(getViewPointX(lt), getViewPointY(lt));
            mMaskLinePath.lineTo(getViewPointX(rt), getViewPointY(rt));
            mMaskLinePath.lineTo(getViewPointX(rb), getViewPointY(rb));
            mMaskLinePath.lineTo(getViewPointX(lb), getViewPointY(lb));
            mMaskLinePath.close();
        }
        invalidate();
    }

    private int getViewPointX(Point point) {
        return (int) (point.x * mScaleX + mPicLeft);
    }

    private int getViewPointY(Point point) {
        return (int) (point.y * mScaleY + mPicTop);
    }
}
