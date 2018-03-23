package com.yinghe.whiteboardlib.utils;

import android.graphics.PointF;
import android.view.MotionEvent;

/**
 * Created by liuzhenhui on 2018/3/16.
 */

public class TouchEventUtil {

    public static final boolean isTwoFingerEvent(MotionEvent event) {
        if (event != null) {
            return fingersOfEvent(event) == 2;
        }
        return false;
    }

    public static final int fingersOfEvent(MotionEvent event) {
        if (event != null) {
            return event.getPointerCount();
        }
        return 0;
    }
    /**
     * 计算两个触控点之间的距离
     *
     * @param event 触控事件
     * @return 触控点之间的距离
     */
    public static float spacingOfTwoFinger(MotionEvent event) {
        if (event.getPointerCount() != 2) {
            return 0;
        }
        double dx = event.getX(0) - event.getX(1);
        double dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两个触控点形成的角度
     *
     * @param event 触控事件
     * @return 角度值
     */
    public static float rotationDegreeOfTwoFinger(MotionEvent event) {
        if (event.getPointerCount() != 2) {
            return 0;
        }
        double dx = (event.getX(0) - event.getX(1));
        double dy = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(dy, dx);
        return (float) Math.toDegrees(radians);
    }

    /**
     * 获得触控事件的坐标点
     *
     * @param event 触控事件
     * @return 坐标点浮点类
     */
    public static PointF getPointFFromEvent(MotionEvent event) {
        return new PointF(event.getX(), event.getY());
    }

    /**
     * 计算两个手指的中点
     *
     * @param event
     * @return
     */
    public static PointF middleOfTwoFinger(MotionEvent event) {
        if (!isTwoFingerEvent(event)) {
            return new PointF();
        }
        float mx = (event.getX(0) + event.getX(1)) / 2;
        float my = (event.getY(0) + event.getY(1)) / 2;
        PointF middle = new PointF(mx, my);
        return middle;
    }
}
