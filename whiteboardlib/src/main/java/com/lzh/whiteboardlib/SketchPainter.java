package com.lzh.whiteboardlib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.view.View;

import com.lzh.whiteboardlib.bean.SketchData;
import com.lzh.whiteboardlib.bean.StrokeRecord;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;

public class SketchPainter {
    public View mView;
    public SketchData curSketchData;

    public Bitmap tempBitmap;//临时绘制的bitmap
    public Canvas tempCanvas;
    public Bitmap tempHoldBitmap;//保存已固化的笔画bitmap
    public Canvas tempHoldCanvas;

    public SketchPainter(View view) {
        mView = view;
    }

    public void setSketchData(SketchData sketchData) {
        this.curSketchData = sketchData;
    }

    public SketchData getSketchData() {
        return curSketchData;
    }

    public void addStrokeRecord(StrokeRecord record) {
        curSketchData.strokeRecordList.add(record);
        mView.invalidate();
    }

    public void drawRecord(Canvas canvas, boolean isDrawBoard) {
        if (curSketchData != null) {
            //新建一个临时画布，以便橡皮擦生效
            if (tempBitmap == null) {
                tempBitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.ARGB_4444);
                tempCanvas = new Canvas(tempBitmap);
            }
            //新建一个临时画布，以便保存过多的画笔
            if (tempHoldBitmap == null) {
                tempHoldBitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(), Bitmap.Config.ARGB_4444);
                tempHoldCanvas = new Canvas(tempHoldBitmap);
            }
            //节省性能把10笔以前的全都画(保存)进固化层(tempHoldBitmap),移除record历史
            //从而每次10笔以前的一次性从tempHoldBitmap绘制过来，其他重绘最多10笔，
            while (curSketchData.strokeRecordList.size() > Integer.MAX_VALUE) {
                StrokeRecord record = curSketchData.strokeRecordList.remove(0);
                drawRecordToCanvas(tempHoldCanvas, record);
            }
            //清空画布
            Paint p = new Paint();
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            tempCanvas.drawPaint(p);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            //绘制
            tempCanvas.drawColor(Color.TRANSPARENT);
            tempCanvas.drawBitmap(tempHoldBitmap, new Rect(0, 0, tempHoldBitmap.getWidth(), tempHoldBitmap.getHeight()), new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), null);
            for (StrokeRecord record : curSketchData.strokeRecordList) {
                drawRecordToCanvas(tempCanvas, record);
            }
            canvas.drawBitmap(tempBitmap, new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), null);
        }
    }

    private void drawRecordToCanvas(Canvas canvas, StrokeRecord record) {
        int type = record.type;
        if (type == StrokeRecord.STROKE_TYPE_ERASER) {//橡皮擦需要在固化层也绘制
            canvas.drawPath(record.path, record.paint);
        } else if (type == StrokeRecord.STROKE_TYPE_DRAW || type == StrokeRecord.STROKE_TYPE_LINE) {
            canvas.drawPath(record.path, record.paint);
        } else if (type == STROKE_TYPE_CIRCLE) {
            canvas.drawOval(record.rect, record.paint);
        } else if (type == STROKE_TYPE_RECTANGLE) {
            canvas.drawRect(record.rect, record.paint);
        } else if (type == STROKE_TYPE_TEXT) {
            if (record.text != null) {
                StaticLayout layout = new StaticLayout(record.text, record.textPaint, record.textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                canvas.translate(record.textOffX, record.textOffY);
                layout.draw(canvas);
                canvas.translate(-record.textOffX, -record.textOffY);
            }
        }
    }

    /**
     * 删除某人的某笔
     *
     * @param uid
     * @param sq
     */
    public List<StrokeRecord> deleteRecord(long uid, int sq) {
        List<StrokeRecord> strokeRecordList = curSketchData.strokeRecordList;
        List<StrokeRecord> deletedRecordList = new ArrayList<>();
        Iterator<StrokeRecord> iterator = strokeRecordList.iterator();
        while (iterator.hasNext()) {
            StrokeRecord strokeRecord = iterator.next();
            if (strokeRecord.id == sq && strokeRecord.userid == uid) {
                iterator.remove();
                curSketchData.strokeRedoList.add(strokeRecord);
                deletedRecordList.add(strokeRecord);
            }
        }
        mView.invalidate();
        return deletedRecordList;
    }

    /*
     * 删除一笔
     */
    public List<StrokeRecord> undo() {
        List<StrokeRecord> strokeRecordList = curSketchData.strokeRecordList;
        int recordSize = strokeRecordList.size();
        if (recordSize > 0) {
            StrokeRecord lastRecord = strokeRecordList.get(recordSize - 1);
            return deleteRecord(lastRecord.userid, lastRecord.id);
        }
        return new ArrayList<>();
    }

    /*
     * 撤销删除
     */
    public List<StrokeRecord> redo() {
        List<StrokeRecord> strokeRedoList = curSketchData.strokeRedoList;
        List<StrokeRecord> redodStrokeList = new ArrayList<>();
        int redoSize = strokeRedoList.size();
        if (redoSize > 0) {
            StrokeRecord redoRecord = strokeRedoList.get(redoSize - 1);
            long uid = redoRecord.userid;
            int sq = redoRecord.id;
            Iterator<StrokeRecord> iterator = strokeRedoList.iterator();
            while (iterator.hasNext()) {
                StrokeRecord next = iterator.next();
                if (next.userid == uid && next.id == sq) {
                    iterator.remove();
                    curSketchData.strokeRecordList.add(next);
                    redodStrokeList.add(next);
                }
            }
            mView.invalidate();
        }
        return redodStrokeList;
    }

    public void erase() {
        curSketchData.strokeRecordList.clear();
        curSketchData.strokeRedoList.clear();

        tempCanvas = null;
        if (tempBitmap != null) {
            tempBitmap.recycle();
            tempBitmap = null;
        }
        tempHoldCanvas = null;
        if (tempHoldBitmap != null) {
            tempHoldBitmap.recycle();
            tempHoldBitmap = null;
        }
        System.gc();
        mView.invalidate();
    }

    public int getRedoCount() {
        return curSketchData.strokeRedoList != null ? curSketchData.strokeRedoList.size() : 0;
    }

    public int getRecordCount() {
        return (curSketchData.strokeRecordList != null) ? curSketchData.strokeRecordList.size() : 0;
    }

}
