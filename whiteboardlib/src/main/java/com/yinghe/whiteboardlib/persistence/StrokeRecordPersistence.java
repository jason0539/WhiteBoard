package com.yinghe.whiteboardlib.persistence;

/**
 * Created by liuzhenhui on 2018/3/14.
 */

public class StrokeRecordPersistence {

    //坐标数组，用坐标内用逗号隔开,坐标间用分号隔开,例如:1,2;2,3
    String path;
    //线条宽度
    float width;
    //线条颜色，二进制
    int color;
    //线段类型,0曲线，1圆，2线段，3矩形
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
