package com.lzh.whiteboardlib.bean;

import com.lzh.whiteboardlib.SketchView;

/**
 * Created by liuzhenhui on 2018/3/14.
 */

public class StrokePoint {
    float x;
    float y;

    public StrokePoint(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public static final String transferPositionString(StrokePoint point) {
        return String.valueOf(point.getX() / SketchView.getSketchWidth() + "," + point.getY() / SketchView.getSketchHeight());
    }

    public static final StrokePoint resumePosition(String pointString) {
        String[] pointsString = pointString.split(",");
        float x = Float.valueOf(pointsString[0]) * SketchView.getSketchWidth();
        float y = Float.valueOf(pointsString[1]) * SketchView.getSketchHeight();
        return new StrokePoint(x, y);
    }
}
