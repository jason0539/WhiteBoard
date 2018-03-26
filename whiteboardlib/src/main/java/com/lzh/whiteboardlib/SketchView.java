/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.lzh.whiteboardlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.lzh.whiteboardlib.bean.SketchData;
import com.lzh.whiteboardlib.bean.StrokePath;
import com.lzh.whiteboardlib.bean.StrokeRecord;
import com.lzh.whiteboardlib.utils.BitmapUtils;
import com.lzh.whiteboardlib.utils.DensityUtil;
import com.lzh.whiteboardlib.utils.MLog;
import com.lzh.whiteboardlib.utils.MathUtil;
import com.lzh.whiteboardlib.utils.PaintUtils;

import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;


public class SketchView extends View {

    public static final int EDIT_STROKE = 1;
    public static final int DEFAULT_STROKE_SIZE = 3;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;
    public static final float TOUCH_TOLERANCE = 10;
    public static final int ACTION_NONE = 0;
    public static float SCALE_MAX = 4.0f;
    public static float SCALE_MIN = 0.2f;
    public static float SCALE_MIN_LEN;
    public Paint boardPaint;

    public SketchData curSketchData;
    public Rect backgroundSrcRect = new Rect();
    public Rect backgroundDstRect = new Rect();
    public int defaultBgColor = Color.WHITE;
    public StrokeRecord curStrokeRecord;
    public int actionMode;
    public TextWindowCallback textWindowCallback;
    public float strokeSize = DEFAULT_STROKE_SIZE;
    public int strokeRealColor = Color.BLACK;//画笔实际颜色
    public int strokeColor = Color.BLACK;//画笔颜色
    public int strokeAlpha = 255;//画笔透明度
    public float eraserSize = DEFAULT_ERASER_SIZE;
    public StrokePath strokePath;
    public Paint strokePaint;
    public float downX, downY, preX, preY, curX, curY;
    public static int mWidth, mHeight;
    public Context mContext;
    public boolean needCheckTolerance = true;//每次down事件都要检查是否滑动距离超过阈值，超过才绘制
    public OnDrawChangedListener onDrawChangedListener;
    public OnStrokeRecordFinishListener onStrokeRecordFinishListener;

    public SketchView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mContext = context;
        setSketchData(new SketchData());
        initParams(context);
        invalidate();
    }

    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    public int getStrokeType() {
        return curSketchData.strokeType;
    }

    public void setStrokeType(int strokeType) {
        this.curSketchData.strokeType = strokeType;
    }

    public void setSketchData(SketchData sketchData) {
        this.curSketchData = sketchData;
    }

    public void addStrokePath(StrokeRecord strokeRecord) {
        curSketchData.strokeRecordList.add(strokeRecord);
        invalidate();
    }

    public void initParams(Context context) {
        setBackgroundColor(Color.WHITE);

        strokePaint = PaintUtils.createDefaultStrokePaint();
        strokePaint.setColor(strokeRealColor);
        strokePaint.setStrokeWidth(strokeSize);

        boardPaint = new Paint();
        boardPaint.setColor(Color.GRAY);
        boardPaint.setStrokeWidth(DensityUtil.dip2px(mContext, 0.8f));
        boardPaint.setStyle(Paint.Style.STROKE);

        SCALE_MIN_LEN = DensityUtil.dip2px(context, 20);
    }

    public void setStrokeAlpha(int mAlpha) {
        this.strokeAlpha = mAlpha;
        calculColor();
        strokePaint.setStrokeWidth(strokeSize);
    }

    public void setStrokeColor(int color) {
        strokeColor = color;
        calculColor();
        strokePaint.setColor(strokeRealColor);
    }

    public void calculColor() {
        strokeRealColor = Color.argb(strokeAlpha, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(mWidth, mHeight);
    }

    //缩放参数
    private float mScale = 1;
    private PointF mOffset = new PointF(0, 0);
    public void setScaleAndOffset(float scaleX, float mMatrixValus, float mMatrixValus1) {
        mScale = scaleX;
        mOffset.x = mMatrixValus;
        mOffset.y = mMatrixValus1;
        //绘制形状时path为空
        if (curStrokeRecord.path != null) {
            curStrokeRecord.path.setScaleAndOffset(mScale,mOffset.x,mOffset.y);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //根据缩放状态，计算触摸点在缩放后画布的对应坐标位置
        curX = (event.getX() - mOffset.x)/mScale;
        curY = (event.getY() - mOffset.y)/mScale;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_POINTER_DOWN");
                break;
            case MotionEvent.ACTION_DOWN:
                MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_DOWN");
                touch_down();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_MOVE");
                touch_move(event);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_UP");
                touch_up();
                invalidate();
                break;
        }
        preX = curX;
        preY = curY;
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        drawRecord(canvas,true);
        if (onDrawChangedListener != null) {
            onDrawChangedListener.onDrawChanged();
        }
    }

    public void drawBackground(Canvas canvas) {
        if (curSketchData.backgroundBitmap != null) {
            Matrix matrix = new Matrix();
            float wScale = (float) canvas.getWidth() / curSketchData.backgroundBitmap.getWidth();
            float hScale = (float) canvas.getHeight() / curSketchData.backgroundBitmap.getHeight();
            matrix.postScale(wScale, hScale);
            canvas.drawBitmap(curSketchData.backgroundBitmap, matrix, null);
        } else {
            canvas.drawColor(defaultBgColor);
        }
    }

    public Bitmap tempBitmap;//临时绘制的bitmap
    public Canvas tempCanvas;
    public Bitmap tempHoldBitmap;//保存已固化的笔画bitmap
    public Canvas tempHoldCanvas;

    public void drawRecord(Canvas canvas, boolean isDrawBoard) {
        if (curSketchData != null) {
            //新建一个临时画布，以便橡皮擦生效
            if (tempBitmap == null) {
                tempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_4444);
                tempCanvas = new Canvas(tempBitmap);
            }
            //新建一个临时画布，以便保存过多的画笔
            if (tempHoldBitmap == null) {
                tempHoldBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_4444);
                tempHoldCanvas = new Canvas(tempHoldBitmap);
            }
            //节省性能把10笔以前的全都画(保存)进固化层(tempHoldBitmap),移除record历史
            //从而每次10笔以前的一次性从tempHoldBitmap绘制过来，其他重绘最多10笔，
            while (curSketchData.strokeRecordList.size() > Integer.MAX_VALUE) {
                StrokeRecord record = curSketchData.strokeRecordList.remove(0);
                drawRecordToCanvas(tempHoldCanvas,record);
            }
            clearCanvas(tempCanvas);//清空画布
            tempCanvas.drawColor(Color.TRANSPARENT);
            tempCanvas.drawBitmap(tempHoldBitmap, new Rect(0, 0, tempHoldBitmap.getWidth(), tempHoldBitmap.getHeight()), new Rect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight()), null);
            for (StrokeRecord record : curSketchData.strokeRecordList) {
                drawRecordToCanvas(tempCanvas,record);
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
     * 清理画布canvas
     *
     * @param temptCanvas
     */
    public void clearCanvas(Canvas temptCanvas) {
        Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        temptCanvas.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    public float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
//        SCALE_MIN = SCALE_MAX / 5;
    }

    public void addStrokeRecord(StrokeRecord record) {
        curSketchData.strokeRecordList.add(record);
        invalidate();
    }

    public void touch_down() {
        downX = curX;
        downY = curY;
        needCheckTolerance = true;
        if (curSketchData.editMode == EDIT_STROKE) {
            //进行新的绘制时，清空redo栈（如果要保留，注释这行即可）
            curSketchData.strokeRedoList.clear();
            curStrokeRecord = new StrokeRecord(curSketchData.strokeType);
            strokePaint.setAntiAlias(true);//由于降低密度绘制，所以需要抗锯齿
            if (curSketchData.strokeType == StrokeRecord.STROKE_TYPE_ERASER) {
                strokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码
            } else {
                strokePaint.setXfermode(null);//关键代码
            }
            if (curSketchData.strokeType == STROKE_TYPE_ERASER) {
                strokePath = new StrokePath();
                strokePath.moveTo(downX, downY);
                strokePaint.setColor(Color.WHITE);
                strokePaint.setStrokeWidth(eraserSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
                curStrokeRecord.path = strokePath;
            } else if (curSketchData.strokeType == STROKE_TYPE_DRAW || curSketchData.strokeType == STROKE_TYPE_LINE) {
                strokePath = new StrokePath();
                strokePath.moveTo(downX, downY);
                curStrokeRecord.path = strokePath;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (curSketchData.strokeType == STROKE_TYPE_CIRCLE || curSketchData.strokeType == STROKE_TYPE_RECTANGLE) {
                RectF rect = new RectF(downX, downY, downX, downY);
                curStrokeRecord.rect = rect;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (curSketchData.strokeType == STROKE_TYPE_TEXT) {
                curStrokeRecord.textOffX = (int) downX;
                curStrokeRecord.textOffY = (int) downY;
                TextPaint tp = new TextPaint();
                tp.setColor(strokeRealColor);
                curStrokeRecord.textPaint = tp; // Clones the mPaint object
                textWindowCallback.onText(this, curStrokeRecord);
                return;
            }
            curSketchData.strokeRecordList.add(curStrokeRecord);
        }
    }

    public void touch_move(MotionEvent event) {
        if (!needCheckTolerance ||
                (needCheckTolerance && MathUtil.rectDiagonal(curX-downX, curY- downY) > TOUCH_TOLERANCE)) {
            needCheckTolerance = false;
            if (curSketchData.editMode == EDIT_STROKE) {
                if (curSketchData.strokeType == STROKE_TYPE_ERASER) {
                    strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
                } else if (curSketchData.strokeType == STROKE_TYPE_DRAW) {
                    strokePath.quadTo(preX, preY, (curX + preX) / 2, (curY + preY) / 2);
                } else if (curSketchData.strokeType == STROKE_TYPE_LINE) {
                    strokePath.reset();
                    strokePath.moveTo(downX, downY);
                    strokePath.lineTo(curX, curY);
                } else if (curSketchData.strokeType == STROKE_TYPE_CIRCLE || curSketchData.strokeType == STROKE_TYPE_RECTANGLE) {
                    curStrokeRecord.rect.set(downX < curX ? downX : curX, downY < curY ? downY : curY, downX > curX ? downX : curX, downY > curY ? downY : curY);
                } else if (curSketchData.strokeType == STROKE_TYPE_TEXT) {

                }
            }
        }
        preX = curX;
        preY = curY;
    }

    public void touch_up() {
        strokePath.end(curX, curY);
        //先只同步DRAW类型
        if (strokePath.getPathType() == StrokePath.PathType.QUAD_TO) {
            if (onStrokeRecordFinishListener != null) {
                onStrokeRecordFinishListener.onPathDrawFinish(curStrokeRecord);
            }
        }
    }

    @NonNull
    public Bitmap getResultBitmap() {
        return getResultBitmap(null);
    }

    public SketchData getSketchData() {
        return curSketchData;
    }

    @NonNull
    public Bitmap getResultBitmap(Bitmap addBitmap) {
        Bitmap newBM = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
//        Bitmap newBM = Bitmap.createBitmap(1280, 800, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(newBM);
//        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));//抗锯齿
        //绘制背景
        drawBackground(canvas);
        drawRecord(canvas, false);

        if (addBitmap != null) {
            canvas.drawBitmap(addBitmap, 0, 0, null);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
//        return newBM;
        Bitmap bitmap = BitmapUtils.createBitmapThumbnail(newBM, true, 800, 1280);
        return bitmap;
    }


    /*
     * 删除一笔
     */
    public void undo() {
        if (curSketchData.strokeRecordList.size() > 0) {
            curSketchData.strokeRedoList.add(curSketchData.strokeRecordList.get(curSketchData.strokeRecordList.size() - 1));
            curSketchData.strokeRecordList.remove(curSketchData.strokeRecordList.size() - 1);
            invalidate();
        }
    }

    /*
     * 撤销
     */
    public void redo() {
        if (curSketchData.strokeRedoList.size() > 0) {
            curSketchData.strokeRecordList.add(curSketchData.strokeRedoList.get(curSketchData.strokeRedoList.size() - 1));
            curSketchData.strokeRedoList.remove(curSketchData.strokeRedoList.size() - 1);
        }
        invalidate();
    }

    public int getRedoCount() {
        return curSketchData.strokeRedoList != null ? curSketchData.strokeRedoList.size() : 0;
    }

    public int getRecordCount() {
        return (curSketchData.strokeRecordList != null ) ? curSketchData.strokeRecordList.size() : 0;
    }

    public int getStrokeRecordCount() {
        return curSketchData.strokeRecordList != null ? curSketchData.strokeRecordList.size() : 0;
    }

    public int getStrokeSize() {
        return Math.round(this.strokeSize);
    }

    public void setSize(int size, int eraserOrStroke) {
        switch (eraserOrStroke) {
            case STROKE_TYPE_DRAW:
                strokeSize = size;
                break;
            case STROKE_TYPE_ERASER:
                eraserSize = size;
                break;
        }

    }

    public void erase() {
        if (curSketchData.backgroundBitmap != null && !curSketchData.backgroundBitmap.isRecycled()) {
            // 回收并且置为null
            curSketchData.backgroundBitmap.recycle();
            curSketchData.backgroundBitmap = null;
        }
        curSketchData.strokeRecordList.clear();
        curSketchData.strokeRedoList.clear();

        tempCanvas = null;
        tempBitmap.recycle();
        tempBitmap = null;
        tempHoldCanvas = null;
        tempHoldBitmap.recycle();
        tempHoldBitmap = null;
        System.gc();
        invalidate();
    }

    public void setOnDrawChangedListener(OnDrawChangedListener listener) {
        this.onDrawChangedListener = listener;
    }

    public void setBackgroundByPath(Bitmap bm) {
        setBackgroundByBitmap(bm);
    }

    public void setBackgroundByPath(String path) {
        Bitmap sampleBM = BitmapUtils.getSampleBitMap(mContext,path);
        if (sampleBM != null) {
            setBackgroundByBitmap(sampleBM);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void setBackgroundByBitmap(Bitmap sampleBM) {
        curSketchData.backgroundBitmap = sampleBM;
        backgroundSrcRect = new Rect(0, 0, curSketchData.backgroundBitmap.getWidth(), curSketchData.backgroundBitmap.getHeight());
        backgroundDstRect = new Rect(0, 0, mWidth, mHeight);
        invalidate();
    }

    public int getEditMode() {
        return curSketchData.editMode;
    }

    public void setEditMode(int editMode) {
        this.curSketchData.editMode = editMode;
        invalidate();
    }

    public static final int getSketchWidth(){
        return mWidth;
    }

    public static final int getSketchHeight() {
        return mHeight;
    }

    public void setOnStrokeRecordFinishListener(OnStrokeRecordFinishListener listener){
        onStrokeRecordFinishListener = listener;
    }

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }

    public interface OnDrawChangedListener {
        void onDrawChanged();
    }

    public interface OnStrokeRecordFinishListener {
        void onPathDrawFinish(StrokeRecord strokeRecord);
    }
}