package com.lx.multimedialearn.ffmpegstudy.live.pusher;

/**
 * 推送
 *
 * @author lixiao
 * @since 2017-08-09 17:12
 */
public abstract class Pusher {
    public abstract void startPush();

    public abstract void stopPush();

    public abstract void release();
}
