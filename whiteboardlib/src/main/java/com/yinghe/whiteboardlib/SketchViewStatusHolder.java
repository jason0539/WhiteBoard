package com.yinghe.whiteboardlib;

/**
 * Created by liuzhenhui on 2018/3/15.
 * 用于画板view的信息保存
 * 保存坐标用于转换跨设备坐标比例
 */

public class SketchViewStatusHolder {
    private static int mSketchViewWidth = 1;
    private static int mSketchViewHeight = 1;

//    private SketchViewStatusHolder() {
//
//    }
//
//    private static class LazyHolder {
//        public static final SketchViewStatusHolder INSTANCE = new SketchViewStatusHolder();
//    }
//
//    public static final SketchViewStatusHolder getInstance() {
//        return LazyHolder.INSTANCE;
//    }

    public static final void setSize(int width, int height) {
        mSketchViewHeight = height;
        mSketchViewWidth = width;
    }

    public static final int getSketchViewWidth() {
        return mSketchViewWidth;
    }

    public static final int getSketchViewHeight() {
        return mSketchViewHeight;
    }
}
