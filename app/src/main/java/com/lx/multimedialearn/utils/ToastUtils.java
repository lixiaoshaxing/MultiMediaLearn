package com.lx.multimedialearn.utils;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * 吐司工具类
 *
 * @author lixiao
 * @since 2017-10-24 23:56
 */
public class ToastUtils {

    public static void show(Context context, String des) {
        if (!TextUtils.isEmpty(des)) {
            Toast.makeText(context, des, Toast.LENGTH_SHORT).show();
        }
    }
}
