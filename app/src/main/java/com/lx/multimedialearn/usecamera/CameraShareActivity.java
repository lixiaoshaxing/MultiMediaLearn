package com.lx.multimedialearn.usecamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.lx.multimedialearn.R;
import com.lx.multimedialearn.usecamera.render.CameraRender;
import com.lx.multimedialearn.utils.CameraUtils;
import com.lx.multimedialearn.utils.GlUtil;
import com.lx.multimedialearn.utils.ScreenUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 使用SurfaceTexture拿到Camera数据，放入队列，在SurfaceView，GLSurfaceView，TextureView中画出
 */
public class CameraShareActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView; //使用SurfaceView预览
    private TextureView mTextureView; //使用TextureView预览
    private GLSurfaceView mGlSurfaceView; //使用GlSurfaceView预览

    private Camera mCamera;
    private Camera.Parameters mParameters;
    private CameraRender mRender;
    private SurfaceTexture mSurfaceTexture;
    boolean isDrawing = false;

    private LinkedBlockingQueue<byte[]> mQueue; //共享camera中返回的预览数据队列，这是SurfaceView的一种方法，可以使用SurfaceTexture进行共享
    private Handler mHandler = new Handler(Looper.getMainLooper());
    Bitmap mBitmp;
    int textureBuffer[];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_share);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera_preview);
        mTextureView = (TextureView) findViewById(R.id.textureview_camera_preview);
        mGlSurfaceView = (GLSurfaceView) findViewById(R.id.glsurface_camera_preview);

        mQueue = new LinkedBlockingQueue<>();
        initGLSurfaceView(); //GLSurfaceView必须设置Render
    }

    @Override
    protected void onResume() {
        super.onResume();
        initCameraParameters(); //需要重新打开摄像头
        if (mGlSurfaceView != null) {
            mGlSurfaceView.onResume();
        }
        startPreview();
    }

    /**
     * GLSurfaceView展示后需要设置Render等其它参数，这里先绑定
     */
    private void initGLSurfaceView() {
        mSurfaceTexture = new SurfaceTexture(GlUtil.createTextureID());//（1)创建SurfaceTexture获取Camera数据，并标定一个id
        mSurfaceTexture.detachFromGLContext(); //创建SurfaceTexture后必须立刻detach OpenGL的上下文，在CameraRender中的onDrawFrame前再进行关联，这样能够共享这个SurfaceTexture
        mGlSurfaceView.setEGLContextClientVersion(2);
        mRender = new CameraRender(this, mSurfaceTexture, GlUtil.createTextureID());//绑定id，startPreviewOnGlSurfaceView()时进行预览，向SurfaceTexture中填充数据
        mGlSurfaceView.setRenderer(mRender);//(2)把数据提供给Render，使用Render在SurfaceView上绘画预览图像
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY); //可以使用dirty进行手动触发更新界面
    }

    private void startPreview() {

        SurfaceHolder holder = mSurfaceView.getHolder();  // SurfaceView从队列中拿数据手动绘制
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                isDrawing = true;
            }

            @Override
            public void surfaceChanged(final SurfaceHolder holder, int format, int width, int height) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Camera.Size size = mCamera.getParameters().getPreviewSize();
                        while (isDrawing) {
                            try {
                                byte[] data = mQueue.take();
                                //data：yuv-》jpeg，
                                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, os);
                                Canvas canvas = holder.lockCanvas();
                                Bitmap bm = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.size());
                                if (bm != null && !bm.isRecycled() && canvas != null)
                                    canvas.drawBitmap(bm, 0, 0, null);
                                holder.unlockCanvasAndPost(canvas);
                                os.close();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                isDrawing = false;
            }
        });

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {  //在TextureView上手动绘制预览图
            @Override
            public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isDrawing) {
                            if (!mBitmp.isRecycled()) {
                                Camera.Size size = mCamera.getParameters().getPreviewSize();
                                while (isDrawing) {
                                    synchronized (mQueue) {
                                        try {
                                            if (!mQueue.isEmpty()) {
                                                byte[] data = mQueue.element();
                                                //data：yuv-》jpeg，
                                                YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                                                ByteArrayOutputStream os = new ByteArrayOutputStream();
                                                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, os);
                                                Canvas canvas = mTextureView.lockCanvas();
                                                Bitmap bm = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.size());
                                                if (bm != null && !bm.isRecycled() && canvas != null)
                                                    canvas.drawBitmap(bm, 0, 0, null);
                                                mTextureView.unlockCanvasAndPost(canvas);
                                                os.close();
                                            }
                                        } catch (IOException | NoSuchElementException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                isDrawing = true;
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                isDrawing = false;
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

    }

    /**
     * 打开camera，设置camera的相关参数，在OnResume时进行设置，如果打开另外申请摄像头的Activity，回来后该摄像头需要重新设置才能生效
     */
    private void initCameraParameters() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); //这里可以进行前后置摄像头
        mParameters = mCamera.getParameters();
        Camera.Size supportPreviewSize = CameraUtils.getSupportPreviewSize(mCamera, ScreenUtils.getScreenWidth(CameraShareActivity.this) / 2 - 50);
        final Camera.Size supportPictureSize = CameraUtils.getSupportPicSize(mCamera, ScreenUtils.getScreenWidth(CameraShareActivity.this) / 2 - 50);
        mParameters.setPreviewSize(supportPreviewSize.width, supportPreviewSize.height);
        mParameters.setPictureSize(supportPictureSize.width, supportPictureSize.height);
        mParameters.setPreviewFormat(ImageFormat.NV21);
        mParameters.setPictureFormat(ImageFormat.JPEG);
        mParameters.setFocusMode(CameraUtils.getSupportFocusMode(mCamera)); //对焦模式需要优化
        mCamera.setParameters(mParameters);
        CameraUtils.setCameraDisplayOrientation(CameraShareActivity.this, Camera.CameraInfo.CAMERA_FACING_FRONT, mCamera);
        int bitsPerPixel = ImageFormat.getBitsPerPixel(mParameters.getPreviewFormat()); //这里需要搞清楚WithBuffer和直接callback的区别
        final byte[] buffers = new byte[supportPreviewSize.width * supportPreviewSize.height * bitsPerPixel / 8]; //官方建议这么设置
        mBitmp = Bitmap.createBitmap(supportPictureSize.width, supportPictureSize.height, Bitmap.Config.ARGB_8888); //创建bitmap
        textureBuffer = new int[supportPictureSize.width * supportPictureSize.height];
        mCamera.stopPreview();
        try {
            mCamera.setPreviewTexture(mSurfaceTexture); //SurfaceTexture获取Camera数据，camera的onPreviewFrame开始回调数据
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.addCallbackBuffer(buffers);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() { //设置回调几个方法的区别：http://blog.csdn.net/lb377463323/article/details/53338045
            @Override
            public void onPreviewFrame(final byte[] data, final Camera camera) {
                //这里能够接收到在预览界面上的数据，NV21格式即yuv420sp
                mCamera.addCallbackBuffer(buffers);
                //Camera的数据
                if (isDrawing) {
                    mQueue.add(data);
//                    for (int i = 0; i < textureBuffer.length; i++) { //并不会。。。。
//                        textureBuffer[i] = 0xff000000 | data[i];
//                    }
//                    mBitmp.setPixels(textureBuffer, 0, supportPictureSize.width, 0, 0, supportPictureSize.width, supportPictureSize.height);
                }
            }
        });
        mCamera.startPreview();//GLSurfaceView直接从surfaceTexture中拿数据并预览
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGlSurfaceView != null) {
            mGlSurfaceView.onPause();
        }
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }
}


/*****************************以下为测试代码*******************************************/
//处理共享Camera的方法
//        testHolder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                isDrawing = true;
//            }
//
//            @Override
//            public void surfaceChanged(final SurfaceHolder holder, int format, final int width, final int height) {
//                if (thread == null) {
//                    thread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Camera.Size size = mCamera.getParameters().getPreviewSize();
//                            while (isDrawing && !thread.isInterrupted()) {
//                                try {
//                                    byte[] data = mQueue.take();
//                                    //data：yuv-》jpeg，
//                                    YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//                                    ByteArrayOutputStream os = new ByteArrayOutputStream();
//                                    image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, os);
//                                    Canvas canvas = holder.lockCanvas();
//                                    Bitmap bm = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.size());
//                                    if (bm != null && !bm.isRecycled() && canvas != null)
//                                        canvas.drawBitmap(bm, 0, 0, null);
//                                    holder.unlockCanvasAndPost(canvas);
//                                    os.close();
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    });
//                    thread.start();
//                }
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                isDrawing = false;
//                if (thread != null) {
//                    try {
//                        thread.join(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    thread = null;
//                }
//            }
//        });

//    /**
//     * 初始化GLSurfaceView，画一幅bitmap
//     */
//    private void initGLSurfaceView() {
//        mGlSurfaceView.setEGLContextClientVersion(2);
//        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
//        mGlSurfaceView.setRenderer(new BmpRender(this));
//        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        mGlSurfaceView.requestRender();
//    }
