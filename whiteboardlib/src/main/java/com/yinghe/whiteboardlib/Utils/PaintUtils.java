package com.yinghe.whiteboardlib.Utils;

import android.graphics.Paint;

/**
 * Created by liuzhenhui on 2018/3/14.
 */

public class PaintUtils {
    /**
     * 创建默认画笔
     */
    public static final Paint createDefaultStrokePaint() {
        Paint strokePaint = new Paint();
        strokePaint.setAntiAlias(true);
        strokePaint.setDither(true);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        return strokePaint;
    }
}
