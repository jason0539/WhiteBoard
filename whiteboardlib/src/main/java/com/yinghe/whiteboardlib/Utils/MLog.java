package com.yinghe.whiteboardlib.Utils;

import android.util.Log;

/**
 * Created by liuzhenhui on 2018/3/13.
 */

public class MLog {
    public static final String TAG = "LZH_SKETCH";

    private static boolean DEBUG = true;

    public static final String TAG_SOCKET = "TAG_SOCKET";
    public static final String TAG_DRAW = "TAG_DRAW";
    public static final String TAG_TOUCH = "TAG_TOUCH";
    public static final String TAG_SCALE = "TAG_SCALE";

    private static boolean isDebug() {
        return DEBUG;
    }

    public static final void d(String subTag, String msg) {
        if (isDebug()) {
            Log.d(TAG, subTag + "-->" + msg);
        }
    }

    public static final void e(String subTag, String msg) {
        if (isDebug()) {
            Log.e(TAG, subTag + "-->" + msg);
        }
    }

    public static final void v(String subTag, String msg) {
        if (isDebug()) {
            Log.v(TAG, subTag + "-->" + msg);
        }
    }

}