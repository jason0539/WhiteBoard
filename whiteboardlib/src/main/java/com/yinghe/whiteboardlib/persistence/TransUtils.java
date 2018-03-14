package com.yinghe.whiteboardlib.persistence;

import android.graphics.Paint;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yinghe.whiteboardlib.Utils.PaintUtils;
import com.yinghe.whiteboardlib.bean.SketchData;
import com.yinghe.whiteboardlib.bean.StrokePath;
import com.yinghe.whiteboardlib.bean.StrokeRecord;
import com.yinghe.whiteboardlib.bean.StrokePoint;

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
        StrokeRecordPersistence strokeRecordPersistence = new StrokeRecordPersistence();
        //笔迹类型
        int type = strokeRecord.type;
        strokeRecordPersistence.setType(type);
        //画笔
        Paint paint = strokeRecord.paint;
        strokeRecordPersistence.setColor(paint.getColor());
        strokeRecordPersistence.setWidth(paint.getStrokeWidth());
        //路线
        StrokePath path = strokeRecord.path;
        String strokePathString = transStrokePathToString(path);
        strokeRecordPersistence.setPath(strokePathString);

        return JSONObject.toJSONString(strokeRecordPersistence);
    }

    /**
     * 从string恢复一笔轨迹
     */
    public static StrokeRecord transStringToStrokeRecord(String recordString) {
        StrokeRecordPersistence recordPersistenceBean = JSON.parseObject(recordString, StrokeRecordPersistence.class);
        //笔迹类型
        StrokeRecord strokeRecord = new StrokeRecord(recordPersistenceBean.getType());
        //画笔
        Paint paint = PaintUtils.createDefaultStrokePaint();
        paint.setColor(recordPersistenceBean.getColor());
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
        return String.valueOf(point.getX() + "," + point.getY());
    }

    /**
     * 把一个string转成坐标
     */
    public static final StrokePoint transStringToSketchPoint(String pointString) {
        String[] pointsString = pointString.split(",");
        float x = Float.valueOf(pointsString[0]);
        float y = Float.valueOf(pointsString[1]);
        return new StrokePoint(x, y);
    }
}
