package com.lzh.whiteboardlib.bean;

import android.graphics.Bitmap;

import com.lzh.whiteboardlib.SketchView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ChiEr on 16/6/16.
 */
public class SketchData {
    public List<StrokeRecord> strokeRecordList;
    public List<StrokeRecord> strokeRedoList;
    public Bitmap backgroundBitmap;
    public int strokeType;
    public int editMode;

    public SketchData() {
        strokeRecordList = new ArrayList<>();
        strokeRedoList = new ArrayList<>();
        backgroundBitmap = null;
        strokeType = StrokeRecord.STROKE_TYPE_DRAW;
        editMode = SketchView.MODE_STROKE;
    }

}
