package com.lzh.whiteboardlib.utils;

public class UtilBessel {

    public static final float ctrlX(float preX, float curX) {
//        return (preX + curX) / 2;
        return preX;
    }

    public static final float ctrlY(float preY, float curY) {
//        return (preY + curY) / 2;
        return preY;
    }

    public static final float endX(float preX, float curX) {
//        return curX;
        return (preX + curX) / 2;
    }

    public static final float endY(float preY, float curY) {
//        return curY;
        return (preY + curY) / 2;
    }
}
