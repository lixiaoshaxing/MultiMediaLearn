package com.lx.multimedialearn.opencvstudy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.lx.multimedialearn.R;

/**
 * OpenCV学习
 * 1. 导入OpenCV需要的包
 * 2. 对静态图片进行识别
 * 3. 通过边缘检测找到方框
 * 4. 剪裁图像
 */
public class OpenCVActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_utils");
    }

    MaskImageView img;
    Button mBtnScan;
    Button mBtnCrop;
    Point[] points;
    Bitmap mBmpSrc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_cv);
        img = (MaskImageView) findViewById(R.id.img_open_cv_show);
        mBtnScan = (Button) findViewById(R.id.btn_open_cv_scan);
        mBtnCrop = (Button) findViewById(R.id.btn_open_cv_crop);
        img.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.wallet));
        mBtnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                points = new Point[4];
                mBmpSrc = BitmapFactory.decodeResource(getResources(), R.drawable.wallet);
                points = new Point[4];
                scan(mBmpSrc, points);
                img.setPoints(points);
            }
        });
        mBtnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (points != null) {
                    Point leftTop = points[0];
                    Point rightTop = points[1];
                    Point rightBottom = points[2];
                    Point leftBottom = points[3];
                    int cropWidth = (int) ((getPointsDistance(leftTop, rightTop)
                            + getPointsDistance(leftBottom, rightBottom)) / 2);
                    int cropHeight = (int) ((getPointsDistance(leftTop, leftBottom)
                            + getPointsDistance(rightTop, rightBottom)) / 2);
                    Bitmap cropBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888);
                    crop(mBmpSrc, points, cropBitmap);
                    img.setImageBitmap(cropBitmap);
                }
            }
        });
    }

    public static double getPointsDistance(Point p1, Point p2) {
        return getPointsDistance(p1.x, p1.y, p2.x, p2.y);
    }

    public static double getPointsDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    /**
     * 识别图片中的方框
     *
     * @param srcBitmap 输入图片
     * @param outPoint  方框四个角（左上，右上，右下，左下）
     */
    public static native void scan(Bitmap srcBitmap, Point[] outPoint);

    /**
     * 根据输入点对图片进行剪裁
     *
     * @param srcBitmap 输入图片
     * @param points    输入四个角对应点
     * @param outBitmap 输出Bitmap
     */
    public static native void crop(Bitmap srcBitmap, Point[] points, Bitmap outBitmap);
}
