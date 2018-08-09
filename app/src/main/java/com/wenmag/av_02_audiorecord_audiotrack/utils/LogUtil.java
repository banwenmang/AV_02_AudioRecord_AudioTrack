package com.wenmag.av_02_audiorecord_audiotrack.utils;

import android.util.Log;

/**
 * desc: 日志输出
 * author: created by zhoujx on 2018/8/8 15:00
 */
public class LogUtil {
    public static final String TAG_PREFIX = "WDAudio_";
    public static boolean sIsDebug = true;

    public static void i(String tag, String msg) {
        if (sIsDebug)
            Log.i(TAG_PREFIX + tag, msg);
    }

    public static void d(String tag, String msg) {
        if (sIsDebug)
            Log.d(TAG_PREFIX + tag, msg);
    }

    public static void w(String tag, String msg) {
        if (sIsDebug)
            Log.w(TAG_PREFIX + tag, msg);
    }

    public static void e(String tag, String msg) {
        if (sIsDebug)
            Log.e(TAG_PREFIX + tag, msg);
    }
}
