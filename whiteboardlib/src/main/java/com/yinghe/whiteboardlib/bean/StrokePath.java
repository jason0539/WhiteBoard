package com.yinghe.whiteboardlib.bean;

import android.graphics.Path;

import com.yinghe.whiteboardlib.Utils.MLog;

import java.util.ArrayList;

/**
 * Created by liuzhenhui on 2018/3/13.
 */

public class StrokePath extends Path {
    public enum PathType {MOVE_TO, QUAD_TO, LINE_TO}

    private PathType pathType = PathType.MOVE_TO;
    private ArrayList<StrokePoint> pathPoints = new ArrayList<>();

    public StrokePath() {
    }

    @Override
    public void moveTo(float x, float y) {
        pathType = PathType.MOVE_TO;
        StrokePoint point = new StrokePoint(x, y);
        pathPoints.add(point);
        MLog.d(MLog.TAG_DRAW, "StrokePath->moveTo " + point);
        super.moveTo(x, y);
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {
        pathType = PathType.QUAD_TO;
        StrokePoint point1 = new StrokePoint(x1, y1);
        StrokePoint point2 = new StrokePoint(x2, y2);
        pathPoints.add(point1);
        MLog.d(MLog.TAG_DRAW, "StrokePath->quadTo " + point1 + ";" + point2);
        super.quadTo(x1, y1, x2, y2);
    }

    public void end(float x1, float y1) {
        StrokePoint point = new StrokePoint(x1, y1);
        pathPoints.add(point);
        MLog.d(MLog.TAG_DRAW, "StrokePath->end " + point);
    }

    public ArrayList<StrokePoint> getPathPoints() {
        return pathPoints;
    }

    public PathType getPathType() {
        return pathType;
    }
}
