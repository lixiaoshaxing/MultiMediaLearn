package com.lx.multimedialearn.openglstudy.hockey.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.lx.multimedialearn.openglstudy.hockey.render.OpenGlStudyRender2;

/**
 * 自定义GLSurfaceView，处理触摸事件
 *
 * @author lixiao
 * @since 2017-10-17 21:05
 */
public class CustomGLSurfaceView extends GLSurfaceView {
    OpenGlStudyRender2 renderer;

    public CustomGLSurfaceView(Context context) {
        super(context);
    }

    public CustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    //处理触摸事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null && renderer != null) {
            final float normalizedX = (event.getX() / (float) getWidth()) * 2 - 1; //触摸点计算为Opengl坐标系中的点
            final float normalizedY = -((event.getY() / (float) getHeight()) * 2 - 1);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.handleTouchPress(normalizedX, normalizedY);
                    }
                });
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        renderer.handleTouchDrag(normalizedX, normalizedY);
                    }
                });
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void setRenderer(Renderer renderer) {
        super.setRenderer(renderer);
        this.renderer = (OpenGlStudyRender2) renderer;
    }

    @Override
    public boolean performClick() {
        return true;
    }
}
