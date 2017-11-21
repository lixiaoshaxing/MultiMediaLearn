package com.lx.multimedialearn.openglstudy.wallpaper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.lx.multimedialearn.openglstudy.particles.render.ParticlesRender;

/**
 * 动态壁纸服务
 * 1. 内部类的使用
 * 2. 动态壁纸服务加载的方法
 * 3. 降低opengl渲染帧率
 * 4. 动态壁纸设置界面
 */
public class GLWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new GLEngine();
    }

    public class GLEngine extends Engine {

        private WallpaperGLSurfaceView glSurfaceView;
        private ParticlesRender particlesRenderer;
        private boolean rendererSet;


        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            glSurfaceView = new WallpaperGLSurfaceView(GLWallpaperService.this);

            ActivityManager activityManager =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo configurationInfo = activityManager
                    .getDeviceConfigurationInfo();

            final boolean supportsEs2 =
                    configurationInfo.reqGlEsVersion >= 0x20000
                            // Check for emulator.
                            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                            && (Build.FINGERPRINT.startsWith("generic")
                            || Build.FINGERPRINT.startsWith("unknown")
                            || Build.MODEL.contains("google_sdk")
                            || Build.MODEL.contains("Emulator")
                            || Build.MODEL.contains("Android SDK built for x86")));

            particlesRenderer = new ParticlesRender(GLWallpaperService.this);

            if (supportsEs2) {
                glSurfaceView.setEGLContextClientVersion(2);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { //保留egl上下文，加快重启速度
                    glSurfaceView.setPreserveEGLContextOnPause(true);
                }
                glSurfaceView.setRenderer(particlesRenderer);
                rendererSet = true;
            } else {
                Toast.makeText(GLWallpaperService.this,
                        "This device does not support OpenGL ES 2.0.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (rendererSet) {
                if (visible) {
                    glSurfaceView.onResume();
                } else {
                    glSurfaceView.onPause();
                }
            }
        }

        @Override
        public void onOffsetsChanged(final float xOffset, final float yOffset,
                                     float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            glSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    particlesRenderer.handleOffsetsChanged(xOffset, yOffset);
                }
            });
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            glSurfaceView.onWallpaperDestroy();
        }

        class WallpaperGLSurfaceView extends GLSurfaceView {

            WallpaperGLSurfaceView(Context context) {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            public void onWallpaperDestroy() { //重写清理方法
                super.onDetachedFromWindow();
            }
        }
    }
}

