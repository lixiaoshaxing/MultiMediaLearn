package com.lx.multimedialearn.utils;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

/**
 * 日志工具类
 */
public class LOGUtils {

    public static boolean SHOW_IN_LOGCAT = true;
    public static final int INFO = 0;
    public static final int DEBUG = 1;
    public static final int ERROR = 2;
    public static final int ALL = -1;

    public static boolean IS_DEBUG = true;

    public static int CURRENT_STATE = IS_DEBUG ? ALL : ERROR;
    private static int sLogCount = 0;

    /**
     * Informational message
     *
     * @param msg the print log
     */
    public static void logI(String msg) {
        switch (CURRENT_STATE) {
            case ALL:
            case INFO:
                logInternal(INFO, msg);
        }
    }

    /**
     * debug message
     *
     * @param msg the print log
     */
    public static void logD(String msg) {
        switch (CURRENT_STATE) {
            case ALL:
            case INFO:
            case DEBUG:
                logInternal(DEBUG, msg);
        }
    }

    /**
     * Error message, would always show in logcat; use with care
     *
     * @param msg the print log
     */
    public static void logE(String msg) {
        switch (CURRENT_STATE) {
            case ALL:
            case INFO:
            case DEBUG:
            case ERROR:
                logInternal(ERROR, msg);
        }
    }


    /**
     * Informational message
     *
     * @param msg the print log
     * @param tr  the print stack
     */
    public static void logI(String msg, Throwable tr) {
        switch (CURRENT_STATE) {
            case ALL:
            case INFO:
                logInternal(INFO, msg + "\n" + getStackTraceString(tr));
        }

    }

    /**
     * debug message
     *
     * @param msg the print log
     * @param tr
     */
    public static void logD(String msg, Throwable tr) {
        switch (CURRENT_STATE) {
            case ALL:
            case INFO:
            case DEBUG:
                logInternal(DEBUG, msg + "\n" + getStackTraceString(tr));
        }
    }

    /**
     * Error message, would always show in logcat; use with care
     * 返回错误message
     *
     * @param msg the print log
     * @param tr  the print stack
     */
    public static void logE(String msg, Throwable tr) {
        switch (CURRENT_STATE) {
            case ALL:
            case INFO:
            case DEBUG:
            case ERROR:
                logInternal(ERROR, msg + "\n" + getStackTraceString(tr));
        }
    }

    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();

        try {
            PrintWriter pw = new PrintWriter(sw);
            tr.printStackTrace(pw);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return sw.toString();
    }

    private static void logInternal(int logLevel, String msg) {
        final Throwable t = IS_DEBUG ? new Throwable() : null;
        final StackTraceElement[] elements = t != null ? t.getStackTrace() : null;


        String callerClassName = (elements != null && elements.length > 2) ? elements[2].getClassName() : "N/A";
        String callerMethodName = (elements != null && elements.length > 2) ? elements[2].getMethodName() : "N/A";

        if (IS_DEBUG) {
            int pos = callerClassName.lastIndexOf('.');
            if (pos >= 0) {
                callerClassName = callerClassName.substring(pos + 1);
            }
        }

        final String tag = callerClassName + "[Log]";
        final String message;

        StringBuffer logBuffer = new StringBuffer();


        logBuffer.setLength(0);
        logBuffer.append("[").append(sLogCount).append("][").append(callerMethodName).append("] ").append(msg);
        message = logBuffer.toString();
        sLogCount++;

        if (SHOW_IN_LOGCAT) {
            switch (logLevel) {
                case INFO:
                    Log.i(tag, message);
                    break;
                case DEBUG:
                    Log.d(tag, message);
                    break;
                case ERROR:
                    Log.e(tag, message);
                    break;
            }
        }
    }
}
