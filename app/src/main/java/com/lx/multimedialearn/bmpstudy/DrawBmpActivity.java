package com.lx.multimedialearn.bmpstudy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.Toast;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.bmpstudy.render.BmpRender;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ScreenUtils;

import static com.lx.multimedialearn.R.drawable.p;

/**
 * task1：在Android平台绘制一张图片，使用不同的API，ImageView，SurfaceView，自定义View，GLSurfaceView, TextureView
 */
public class DrawBmpActivity extends AppCompatActivity {
    private ImageView mImgPic;
    private SurfaceView mSurfaceView;
    private CustomBmpView mCustomBmpView;
    private GLSurfaceView mGlSurfaceView; //画bitmap
    private TextureView mTextureView;

    private volatile boolean isDrawing = false; //surface绘画使用
    private Thread mThread;  //suface绘图需要在子线程
    private volatile Bitmap mBmpTemp; //公用bitmap
    private BmpRender mBmpRender; //使用GlSurfaceView画图的自定义渲染器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!GlUtil.checkGLEsVersion_2(this)) {
            Toast.makeText(this, "不支持open gl es 2.0", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_draw_bmp);
        mImgPic = (ImageView) findViewById(R.id.img_draw_bmp_imageview);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_draw_bmp_surfaceview);
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_draw_bmp_glsurfaceview);
        mTextureView = (TextureView) findViewById(R.id.textureview_draw_bmp_textureview);
        mBmpTemp = BitmapFactory.decodeResource(getResources(), p, null);
        //1. 在ImageView上画图，使用双缓冲
        drawBmpOnImageView();
        //2. 在SurfaceView上画图，定时旋转
        drawBmpOnSurfaceView();
        //3. 使用自定义View画图，CustomBmpView.java
        //4. 在GlSurfaceView上画图
        //SurfaceView,GLSurefaceView,TextureView不同：http://www.jianshu.com/p/e8da1bf61bc7
        drawBmpOnGlSurfaceView();
        //5. 在TextureView上画图
        drawBmpOnTextureView();
    }

    /**
     * 在TextureView上画bmp
     * 1. 参考blog：http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2014/1213/2153.html
     */
    private void drawBmpOnTextureView() {
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //surface中获取纹理图像，可以进行处理
                Canvas canvas = mTextureView.lockCanvas();
                canvas.drawBitmap(mBmpTemp, 0, 0, null);
                mTextureView.unlockCanvasAndPost(canvas);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    /**
     * 在GlSurfaceView上画Bitmap
     * 1. 参考blog：https://blog.piasy.com/2016/06/07/Open-gl-es-android-2-part-1/
     */
    private void drawBmpOnGlSurfaceView() {
        //设置GlsurfaceView参数
        mGlSurfaceView.setEGLContextClientVersion(2);
        //int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mBmpRender = new BmpRender(this);
        mGlSurfaceView.setRenderer(mBmpRender);
        //两种模式：when_dirty:懒惰渲染，需要调用requestRender(), 当前为不停渲染
        //mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGlSurfaceView.requestRender();
    }

    /**
     * 使用SurfaceView画bitmap
     * SurfaceView通过Surface替代Canvas可以在后台线程绘制，解决gui线程阻塞的问题
     * 1. 参考blog: http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2012/1201/656.html
     * 2. Canvas和SurfaceView：http://tbfungeek.github.io/2016/04/12/Android-%E8%BF%9B%E9%98%B6%E4%B9%8BAndroid-%E7%BB%98%E5%9B%BE-%E4%B8%89-%E7%94%BB%E5%B8%83Canvs%E4%BB%A5%E5%8F%8ASurfaceView/
     */
    private void drawBmpOnSurfaceView() {
        //是否可以进行绘画
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //从create到destroy之间可以获取canvas进行绘画
                isDrawing = true;
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, final int width, final int height) {
                if (mThread == null) {
                    mThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //画一个旋转的三角形
                            float rotate = 0;
                            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            paint.setFilterBitmap(true);
                            paint.setDither(true);
                            paint.setColor(Color.RED);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(5);
                            while (isDrawing) {
                                //只对脏区域进行更新，画在了中间位置，正方形，宽高都为200px
                                Rect rect = new Rect(width / 2 - 100, height / 2 - 100, width / 2 + 100, height / 2 + 100);
                                Canvas c = holder.lockCanvas();
                                //这里坐标系根据canvas左上角为原点
                                //在子线程获取画布canvas，在canvas上绘bitmap
                                c.drawBitmap(mBmpTemp, 0, 0, null);
                                c.save();
                                c.rotate((rotate += 30) % 360, width / 2, height / 2);
                                c.drawRect(rect, paint);
                                c.restore();
                                holder.unlockCanvasAndPost(c);
                                try {
                                    Thread.sleep(300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
                mThread.start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                isDrawing = false;
                if (mThread != null) {
                    try {
                        mThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mThread = null;
                }
            }
        });
    }

    /**
     * 使用ImageView画bitmap
     */
    private void drawBmpOnImageView() {
        // 使用ImageView画bitmap：1. 加载bitmap 2. 画bitmap
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), p);
        //1. 直接设置bitmap绘制 Bitmap加载注意：http://www.jianshu.com/p/c66816a83080
        //mImgPic.setImageBitmap(bmp);
        //2. 使用bitmapDrawable
        //BitmapDrawable bmpDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.p);
        //mImgPic.setImageDrawable(bmpDrawable);
        //3. 创建bitmap+canvas，绘制bitmap，渲染到imageview上
        int width = ScreenUtils.getScreenWidth(this);
        int height = ScreenUtils.dp2px(this, 200);
        Bitmap bmpBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas();
        canvas.setBitmap(bmpBuffer);
        canvas.drawBitmap(bmp, 0, 0, null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawCircle(ScreenUtils.getScreenWidth(this) / 2, 300, 100, paint);
        mImgPic.setImageBitmap(bmpBuffer); //这里可以学习双缓冲技术，先把所有要画的内容画在bitmapBuffer上，然后一并画在目标imageView上
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGlSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGlSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBmpRender.destory();
    }
}
