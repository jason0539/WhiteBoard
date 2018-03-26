package com.lzh.whiteboardlib.bean;

import android.graphics.Path;

import com.lzh.whiteboardlib.utils.MLog;

import java.util.ArrayList;

/**
 * Created by liuzhenhui on 2018/3/13.
 */

public class StrokePath extends Path {
    public enum PathType {MOVE_TO, QUAD_TO, LINE_TO}

    private PathType pathType = PathType.MOVE_TO;
    private ArrayList<StrokePoint> pathPoints = new ArrayList<>();

    //缩放参数
    //触摸坐标绘制时要根据缩放参数做坐标转换
    //每次绘制时，可能都做过缩放，所以记录属于这次绘制的缩放参数，用于恢复坐标，保存原始坐标
    private float scaleFactor = 1;
    private float offsetX = 0;
    private float offsetY = 0;

    public StrokePath() {
    }

    @Override
    public void moveTo(float x, float y) {
        pathType = PathType.MOVE_TO;
        StrokePoint point = new StrokePoint(recoverXPosition(x), recoverYPosition(y));
        pathPoints.add(point);
        MLog.d(MLog.TAG_DRAW, "StrokePath->moveTo " + point);
        super.moveTo(x, y);
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {
        pathType = PathType.QUAD_TO;
        StrokePoint point1 = new StrokePoint(recoverXPosition(x1), recoverYPosition(y1));
        StrokePoint point2 = new StrokePoint(x2, y2);
        pathPoints.add(point1);
        MLog.d(MLog.TAG_DRAW, "StrokePath->quadTo " + point1 + ";" + point2);
        super.quadTo(x1, y1, x2, y2);
    }

    public void end(float x, float y) {
        StrokePoint point = new StrokePoint(recoverXPosition(x), recoverYPosition(y));
        pathPoints.add(point);
        MLog.d(MLog.TAG_DRAW, "StrokePath->end " + point);
    }

    private float recoverXPosition(float x) {
        return x * scaleFactor + offsetX;
    }

    private float recoverYPosition(float y) {
        return y * scaleFactor + offsetY;
    }

    private float translateXPosition(float x) {
        return (x - offsetX) / scaleFactor;
    }

    private float translateYPosition(float y) {
        return (y - offsetY) / scaleFactor;
    }

    public void setScaleAndOffset(float scaleFactor, float offsetX, float offsetY) {
        this.scaleFactor = scaleFactor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public ArrayList<StrokePoint> getPathPoints() {
        return pathPoints;
    }

    public PathType getPathType() {
        return pathType;
    }
}
