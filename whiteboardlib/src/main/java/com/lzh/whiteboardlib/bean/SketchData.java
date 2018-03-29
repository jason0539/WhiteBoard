package com.lzh.whiteboardlib.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ChiEr on 16/6/16.
 */
public class SketchData {
    public List<StrokeRecord> strokeRecordList;
    public List<StrokeRecord> strokeRedoList;

    public SketchData() {
        strokeRecordList = new ArrayList<>();
        strokeRedoList = new ArrayList<>();
    }

}
