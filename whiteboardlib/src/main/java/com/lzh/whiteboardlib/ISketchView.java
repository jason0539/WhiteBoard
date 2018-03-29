package com.lzh.whiteboardlib;

import com.lzh.whiteboardlib.bean.SketchData;
import com.lzh.whiteboardlib.bean.StrokeRecord;

import java.util.List;

public interface ISketchView {
    void setSketchData(SketchData sketchData);

    SketchData getSketchData();

    void addRecord(StrokeRecord strokeRecord);

    void deleteRecord(long uid, int sq);

    StrokeRecord undo();

    List<StrokeRecord> redo();

    void erase();

    int getRecordCount();

    int getRedoCount();
}
