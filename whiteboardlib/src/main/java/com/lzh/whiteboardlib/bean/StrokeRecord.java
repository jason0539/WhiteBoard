package com.lzh.whiteboardlib.bean;

import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

public class StrokeRecord {
    public static int ID_ID;

    public static final int STROKE_TYPE_ERASER = 1;//橡皮
    public static final int STROKE_TYPE_DRAW = 2;//曲线
    public static final int STROKE_TYPE_LINE = 3;//直线
    public static final int STROKE_TYPE_CIRCLE = 4;//圆形
    public static final int STROKE_TYPE_RECTANGLE = 5;//矩形
    public static final int STROKE_TYPE_TEXT = 6;//文字

    public long userid;//用户id
    public int id;//每个笔画赋予id
    public int type;//记录类型
    public Paint paint;//笔类
    public StrokePath path;//画笔路径数据
    public RectF rect; //圆、矩形区域
    public String text;//文字
    public TextPaint textPaint;//笔类

    public int textOffX;
    public int textOffY;
    public int textWidth;//文字位置

    /**
     * 初始化，指定会话人和笔画类型，id自增
     *
     * @param userid
     * @param type
     */
    public StrokeRecord(long userid, int type) {
        this.userid = userid;
        this.id = ID_ID++;
        this.type = type;
    }

    /**
     * 跨设备传输过来的，初始化指定id
     *
     * @param userid
     * @param type
     * @param id
     */
    public StrokeRecord(long userid, int type, int id) {
        this.userid = userid;
        this.type = type;
        this.id = id;
    }
}