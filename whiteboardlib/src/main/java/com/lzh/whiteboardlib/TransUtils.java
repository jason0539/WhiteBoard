package com.lzh.whiteboardlib;

import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzh.whiteboardlib.utils.PaintUtils;
import com.lzh.whiteboardlib.bean.SketchData;
import com.lzh.whiteboardlib.bean.StrokePath;
import com.lzh.whiteboardlib.bean.StrokePoint;
import com.lzh.whiteboardlib.bean.StrokeRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuzhenhui on 2018/3/14.
 */

public class TransUtils {

    /**
     * 把一张白板所有轨迹数据转成string
     */
    public static final String transSketchDataToString(SketchData sketchData) {
        List<String> strokeRecordStringList = new ArrayList<>();
        String tempStrokeRecordString = null;
        for (StrokeRecord strokeRecord : sketchData.strokeRecordList) {
            tempStrokeRecordString = transStrokeRecordToString(strokeRecord);
            strokeRecordStringList.add(tempStrokeRecordString);
        }
        String sketchDataString = JSONObject.toJSONString(strokeRecordStringList);
        return sketchDataString;
    }

    /**
     * 从string恢复一张白板的所有轨迹数据
     */
    public static final SketchData transStringToSketchData(String stringSketchData) {
        List<String> strokeRecordStringList = JSON.parseArray(stringSketchData, String.class);
        List<StrokeRecord> strokeRecordList = new ArrayList<>();
        StrokeRecord tempStrokeRecord = null;
        for (String strokeRecordString : strokeRecordStringList) {
            tempStrokeRecord = TransUtils.transStringToStrokeRecord(strokeRecordString);
            strokeRecordList.add(tempStrokeRecord);
        }
        SketchData sketchData = new SketchData();
        sketchData.strokeRecordList = strokeRecordList;
        return sketchData;
    }

    /**
     * 把一笔轨迹转成string
     */
    public static final String transStrokeRecordToString(StrokeRecord strokeRecord) {
        WhiteBoardData whiteBoardData = new WhiteBoardData();
        //笔迹类型
        int type = strokeRecord.type;
        whiteBoardData.setType(type);
        //画笔
        Paint paint = strokeRecord.paint;
        whiteBoardData.setColor("#"+Integer.toHexString(paint.getColor()));
        whiteBoardData.setWidth(paint.getStrokeWidth());
        //路线
        StrokePath path = strokeRecord.path;
        String strokePathString = transStrokePathToString(path);
        whiteBoardData.setPath(strokePathString);

        return JSONObject.toJSONString(whiteBoardData);
    }

    /**
     * 从string恢复一笔轨迹
     */
    public static StrokeRecord transStringToStrokeRecord(String recordString) {
        WhiteBoardData recordPersistenceBean = JSON.parseObject(recordString, WhiteBoardData.class);
        //笔迹类型
        StrokeRecord strokeRecord = new StrokeRecord(recordPersistenceBean.getType());
        //画笔
        Paint paint = PaintUtils.createDefaultStrokePaint();
        paint.setColor(Color.parseColor(recordPersistenceBean.getColor()));
        paint.setStrokeWidth(recordPersistenceBean.getWidth());
        strokeRecord.paint = paint;
        //路线
        StrokePath path = transStringToSketchPath(recordPersistenceBean.getPath());
        strokeRecord.path = path;

        return strokeRecord;
    }

    /**
     * 把一笔轨迹上的路径坐标点转换成string
     */
    public static String transStrokePathToString(StrokePath strokePath) {
        StringBuilder pathBuilder = new StringBuilder();
        switch (strokePath.getPathType()) {
            case QUAD_TO:
                List<StrokePoint> points = strokePath.getPathPoints();
                for (StrokePoint point : points) {
                    pathBuilder.append(transSketchPointToString(point)).append(";");
                }
        }
        return pathBuilder.toString();
    }

    /**
     * 从string恢复一笔轨迹的所有坐标点
     */
    public static StrokePath transStringToSketchPath(String position) {
        StrokePath strokePath = new StrokePath();
        if (TextUtils.isEmpty(position)) {
            return strokePath;
        }
        String[] pointStringArray = position.split(";");
        List<StrokePoint> strokePointList = new ArrayList<>();
        for (String pointString : pointStringArray) {
            strokePointList.add(transStringToSketchPoint(pointString));
        }
        int preIndex = 0;
        StrokePoint firstPoint = strokePointList.get(preIndex);
        strokePath.moveTo(firstPoint.getX(), firstPoint.getY());

        StrokePoint prePoint;
        StrokePoint currPoint;
        int sizeTotal = strokePointList.size();
        for (int currIndex = 1; currIndex < sizeTotal; currIndex++) {
            prePoint = strokePointList.get(preIndex);
            currPoint = strokePointList.get(currIndex);
            strokePath.quadTo(prePoint.getX(), prePoint.getY(), (prePoint.getX() + currPoint.getX()) / 2, (prePoint.getY() + currPoint.getY()) / 2);
            preIndex = currIndex;
            if (currIndex == sizeTotal - 1) {
                //最后一个，end
                strokePath.end(currPoint.getX(), currPoint.getY());
            }
        }
        return strokePath;
    }

    /**
     * 把一个坐标转换成string
     */
    public static final String transSketchPointToString(StrokePoint point) {
        return String.valueOf(point.getX() / SketchView.getSketchWidth() + "," + point.getY() / SketchView.getSketchHeight());
    }

    /**
     * 把一个string转成坐标
     */
    public static final StrokePoint transStringToSketchPoint(String pointString) {
        String[] pointsString = pointString.split(",");
        float x = Float.valueOf(pointsString[0]) * SketchView.getSketchWidth();
        float y = Float.valueOf(pointsString[1]) * SketchView.getSketchHeight();
        return new StrokePoint(x, y);
    }
}
