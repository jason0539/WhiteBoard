package com.lzh.whiteboardlib.utils;

import android.graphics.RectF;

/**
 * Created by liuzhenhui on 2018/3/15.
 */

public class MathUtil {

    /**
     * 计算矩形对角线的长度
     *
     * @param rectF
     * @return
     */
    public static final float rectDiagonal(RectF rectF) {
        return rectDiagonal(rectF.width(), rectF.height());
    }

    /**
     * 计算矩形对角线长度
     *
     * @param rectWidth  矩形宽
     * @param rectHeight 矩形高
     * @return
     */
    public static final float rectDiagonal(float rectWidth, float rectHeight) {
        return (float) Math.sqrt(Math.pow(rectWidth, 2) + Math.pow(rectHeight, 2));
    }
}
