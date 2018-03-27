package com.lzh.whiteboardlib;

/**
 * Created by liuzhenhui on 2018/3/14.
 * 持久化保存的白板数据结构
 */

public class WhiteBoardStroke {

    //用户id
    long userid;
    //笔画id
    int id;
    //笔画子id：一笔的坐标数据过长则需要切分为多段发送
    int sid;
    //坐标数组：用坐标内用逗号隔开,坐标间用分号隔开,例如:1,2;2,3
    String path;
    //线条宽度：px
    float width;
    //线条颜色：16进制
    String color;
    //线段类型：0曲线，1圆，2线段，3矩形，4橡皮擦
    int type;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }
}
