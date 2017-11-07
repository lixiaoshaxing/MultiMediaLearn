package com.lx.multimedialearn.drawbmp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.lx.multimedialearn.R;

/**
 * 自定义View画bitmap
 * 参考blog：http://blog.csdn.net/nizhuanxingheit/article/details/48527639
 *
 * @author lixiao
 * @since 2017-09-06 16:23
 */
public class CustomBmpView extends View {
    private Bitmap mBmpTemp; //进行绘画的bitmap

    public CustomBmpView(Context context) {
        super(context);
        init();
    }

    public CustomBmpView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomBmpView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBmpTemp = BitmapFactory.decodeResource(getResources(), R.drawable.p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBmpTemp, 0, 0, null);
    }
}
