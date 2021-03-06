package com.lzh.whiteboardlib;

import com.lzh.whiteboardlib.bean.StrokePath;
import com.lzh.whiteboardlib.bean.StrokePoint;
import com.lzh.whiteboardlib.bean.StrokeRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuzhenhui on 2018/3/14.
 * 持久化保存的白板数据结构
 */

public class WhiteBoardCmd {
    //总长度限制（x字节）- uid（8字节） - sq（4字节） - w（8字节） - type（4字节）- c（8个字符8字节）
    //除以每个坐标长度（4字节）得出每个WhiteBoardStroke能够容纳的最多StrokePoint数量
    public static final int MAX_LENGTH = (500 - 8 - 4 - 8 - 4 - 8) / 4;

    public static final int CMD_CLEAR = 0;
    public static final int CMD_DRAW = 1;
    public static final int CMD_DELETE = 2;

    //指令类型
    int cmd;
    //用户id
    long uid;
    //笔画id
    int sq;
    //坐标数组：用坐标内用逗号隔开,坐标间用分号隔开,例如:1,2;2,3
    String p;
    //线条宽度：px
    float w;
    //线条颜色：16进制
    String c;
    //线段类型：0曲线，1圆，2线段，3矩形，4橡皮擦
    int type;

    public WhiteBoardCmd() {

    }

    public WhiteBoardCmd(int cmd, long uid, int sq, String p, float w, String c, int type) {
        this.cmd = cmd;
        this.uid = uid;
        this.sq = sq;
        this.p = p;
        this.w = w;
        this.c = c;
        this.type = type;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public int getSq() {
        return sq;
    }

    public void setSq(int sq) {
        this.sq = sq;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    public float getW() {
        return w;
    }

    public void setW(float w) {
        this.w = w;
    }

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public static boolean isStrokeRecordNeedSplit(StrokeRecord strokeRecord) {
        return strokeRecord.path != null && strokeRecord.path.getPathPoints().size() > WhiteBoardCmd.MAX_LENGTH;
    }

    public static StrokeRecord[] splitStrokeRecord(StrokeRecord strokeRecord) {
        StrokePath[] strokePath = splitStrokePath(strokeRecord.path);
        StrokeRecord firstHalfRecord = strokeRecord.clone();
        StrokeRecord secondHalfRecord = strokeRecord.clone();
        firstHalfRecord.path = strokePath[0];
        secondHalfRecord.path = strokePath[1];
        return new StrokeRecord[]{firstHalfRecord, secondHalfRecord};
    }

    public static StrokePath[] splitStrokePath(StrokePath strokePath) {

        ArrayList<StrokePoint> pathPoints = strokePath.getPathPoints();
        int size = pathPoints.size();
        List<StrokePoint> firstHalf = pathPoints.subList(0, size / 2);
        List<StrokePoint> sencondHalf = new ArrayList<>(pathPoints.subList(size / 2, size));

        // 后半段补充一个前半段结尾的地方作为起点,防止断线
        StrokePoint prePoint = pathPoints.get(size / 2 - 2);
        StrokePoint currPoint = pathPoints.get(size / 2 - 1);
        sencondHalf.add(0,new StrokePoint((prePoint.getX()+currPoint.getX())/2,(prePoint.getY()+currPoint.getY())/2));

        //前半段
        StrokePath firstHalfPath = new StrokePath();
        firstHalfPath.setPathType(strokePath.getPathType());
        firstHalfPath.setPathPoints(new ArrayList(firstHalf));
        //后半段
        StrokePath secondHalfPath = new StrokePath();
        secondHalfPath.setPathType(strokePath.getPathType());
        secondHalfPath.setPathPoints(new ArrayList(sencondHalf));

        return new StrokePath[]{firstHalfPath, secondHalfPath};
    }
}
