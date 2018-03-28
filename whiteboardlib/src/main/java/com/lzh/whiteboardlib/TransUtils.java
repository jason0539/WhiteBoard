package com.lzh.whiteboardlib;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lzh.whiteboardlib.bean.StrokePath;
import com.lzh.whiteboardlib.bean.StrokePoint;
import com.lzh.whiteboardlib.bean.StrokeRecord;
import com.lzh.whiteboardlib.utils.PaintUtils;
import com.lzh.whiteboardlib.utils.UtilBessel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuzhenhui on 2018/3/14.
 */

public class TransUtils {

    //===========================   String <-> StrokeRecord List 持久化保存到文本使用  =================================

    /**
     * 把一张白板所有轨迹数据转成string
     */
    public static final String transStrokeRecordList(List<StrokeRecord> strokeRecordList) {
        List<String> strokeRecordStringList = new ArrayList<>();
        for (StrokeRecord strokeRecord : strokeRecordList) {
            strokeRecordStringList.add(transStrokeRecord(strokeRecord));
        }
        String sketchDataString = JSONObject.toJSONString(strokeRecordStringList);
        return sketchDataString;
    }

    /**
     * 从string恢复一张白板的所有轨迹数据
     */
    public static final List<StrokeRecord> resumeStrokeRecordList(String stringSketchData) {
        List<String> strokeRecordListString = JSON.parseArray(stringSketchData, String.class);
        List<StrokeRecord> strokeRecordList = new ArrayList<>();
        for (String strokeRecordString : strokeRecordListString) {
            strokeRecordList.add(resumeStrokeRecord(strokeRecordString));
        }
        return strokeRecordList;
    }

    //===========================   String <-> StrokeRecord 单条消息使用  =================================

    /**
     * 把一笔轨迹转成string
     */
    public static final String transStrokeRecord(StrokeRecord strokeRecord) {
        long uid = strokeRecord.userid;
        int sq = strokeRecord.id;
        int type = strokeRecord.type;
        Paint paint = strokeRecord.paint;
        String c = "#"+Integer.toHexString(paint.getColor());
        float w = paint.getStrokeWidth();
        String p = new String();
        if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE || type == StrokeRecord.STROKE_TYPE_ERASER) {
            StrokePath strokePath = strokeRecord.path;
            StringBuilder pathBuilder = new StringBuilder();
            switch (strokePath.getPathType()) {
                case QUAD_TO: case LINE_TO:
                    List<StrokePoint> points = strokePath.getPathPoints();
                    for (StrokePoint point : points) {
                        pathBuilder.append(StrokePoint.transferPositionString(point)).append(";");
                    }
            }
            p = pathBuilder.toString();
        }else if (type == StrokeRecord.STROKE_TYPE_CIRCLE || type == StrokeRecord.STROKE_TYPE_RECTANGLE) {
            RectF rectF = strokeRecord.rect;
            StrokePoint strokePointLeftTop = new StrokePoint(rectF.left,rectF.top);
            StrokePoint strokePointRightBottom = new StrokePoint(rectF.right,rectF.bottom);
            p = StrokePoint.transferPositionString(strokePointLeftTop)+";"+ StrokePoint.transferPositionString(strokePointRightBottom);
        }else if (type == StrokeRecord.STROKE_TYPE_TEXT) {

        }
        WhiteBoardStroke whiteBoardStroke = new WhiteBoardStroke(uid,sq,p,w,c,type);
        return JSONObject.toJSONString(whiteBoardStroke);
    }

    /**
     * 从string恢复一笔轨迹
     */
    public static StrokeRecord resumeStrokeRecord(String recordString) {
        WhiteBoardStroke wbStroke = JSON.parseObject(recordString, WhiteBoardStroke.class);
        StrokeRecord strokeRecord = new StrokeRecord(wbStroke.uid,wbStroke.type,wbStroke.sq);
        int type = strokeRecord.type;
        Paint paint = PaintUtils.createDefaultStrokePaint();
        paint.setColor(Color.parseColor(wbStroke.c));
        paint.setStrokeWidth(wbStroke.w);
        if (type == StrokeRecord.STROKE_TYPE_ERASER) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码
        }
        strokeRecord.paint = paint;

        //路线
        String wbPath = wbStroke.p;
        if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE || type == StrokeRecord.STROKE_TYPE_ERASER) {
            StrokePath strokePath = new StrokePath();
            if (!TextUtils.isEmpty(wbPath)) {
                String[] pointStringArray = wbPath.split(";");
                List<StrokePoint> strokePointList = new ArrayList<>();
                for (String pointString : pointStringArray) {
                    strokePointList.add(StrokePoint.resumePosition(pointString));
                }
                int preIndex = 0;
                StrokePoint firstPoint = strokePointList.get(preIndex);
                strokePath.moveTo(firstPoint.getX(), firstPoint.getY());

                if (type == StrokeRecord.STROKE_TYPE_LINE) {
                    preIndex++;
                    strokePath.lineTo(strokePointList.get(preIndex).getX(), strokePointList.get(preIndex).getY());
                } else {
                    StrokePoint prePoint;
                    StrokePoint currPoint;
                    int sizeTotal = strokePointList.size();
                    for (int currIndex = 1; currIndex < sizeTotal; currIndex++) {
                        prePoint = strokePointList.get(preIndex);
                        currPoint = strokePointList.get(currIndex);
                        float preX = prePoint.getX();
                        float curX = currPoint.getX();
                        float preY = prePoint.getY();
                        float curY = currPoint.getY();
                        strokePath.quadTo(UtilBessel.ctrlX(preX, curX), UtilBessel.ctrlY(preY, curY), UtilBessel.endX(preX, curX), UtilBessel.endY(preY, curY));
                        preIndex = currIndex;
                        if (currIndex == sizeTotal - 1) {
                            //最后一个，end
                            strokePath.end(currPoint.getX(), currPoint.getY());
                        }
                    }
                }
            }
            strokeRecord.path = strokePath;
        }else if (type == StrokeRecord.STROKE_TYPE_CIRCLE || type == StrokeRecord.STROKE_TYPE_RECTANGLE) {
            String[] pointStringArray = wbPath.split(";");
            StrokePoint strokePointLeftTop = StrokePoint.resumePosition(pointStringArray[0]);
            StrokePoint strokePointRightBottom = StrokePoint.resumePosition(pointStringArray[1]);
            strokeRecord.rect = new RectF(strokePointLeftTop.getX(),strokePointLeftTop.getY(), strokePointRightBottom.getX(), strokePointRightBottom.getY());
        }else if (type == StrokeRecord.STROKE_TYPE_TEXT) {

        }

        return strokeRecord;
    }

}
