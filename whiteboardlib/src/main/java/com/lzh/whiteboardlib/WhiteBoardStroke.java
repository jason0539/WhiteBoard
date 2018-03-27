package com.lzh.whiteboardlib;

/**
 * Created by liuzhenhui on 2018/3/14.
 * 持久化保存的白板数据结构
 */

public class WhiteBoardStroke {

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

    public WhiteBoardStroke() {

    }

    public WhiteBoardStroke(long uid, int sq, String p, float w, String c, int type) {
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
}
