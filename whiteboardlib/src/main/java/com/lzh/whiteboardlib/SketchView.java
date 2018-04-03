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
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.lzh.whiteboardlib.bean.SketchData;
import com.lzh.whiteboardlib.bean.StrokePath;
import com.lzh.whiteboardlib.bean.StrokeRecord;
import com.lzh.whiteboardlib.utils.BitmapUtils;
import com.lzh.whiteboardlib.utils.DensityUtil;
import com.lzh.whiteboardlib.utils.MathUtil;
import com.lzh.whiteboardlib.utils.PaintUtils;
import com.lzh.whiteboardlib.utils.UtilBessel;

import java.util.List;
import java.util.UUID;

import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.lzh.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;


public class SketchView extends View {

    public static final long USER_ID = UUID.randomUUID().hashCode();

    public static final int MODE_VIEW = 0;//浏览模式
    public static final int MODE_STROKE = 1;//绘制模式

    public static final int DEFAULT_STROKE_SIZE = 3;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;
    public static final float TOUCH_TOLERANCE = 10;
    public static float SCALE_MIN_LEN;
    public Paint boardPaint;

    public int strokeType;
    public int editMode;
    public Bitmap backgroundBitmap;
    public SketchPainter mSketchPainter;
    public Rect backgroundSrcRect = new Rect();
    public Rect backgroundDstRect = new Rect();
    public int defaultBgColor = Color.WHITE;
    public StrokeRecord curStrokeRecord;
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
    public OnStrokeRecordChangeListener onStrokeRecordChangeListener;

    public SketchView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mContext = context;
        strokeType = StrokeRecord.STROKE_TYPE_DRAW;
        editMode = SketchView.MODE_STROKE;
        mSketchPainter = new SketchPainter(this);
        setSketchData(new SketchData());
        initParams(context);
        invalidate();
    }

    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    public int getStrokeType() {
        return strokeType;
    }

    public void setStrokeType(int strokeType) {
        this.strokeType = strokeType;
    }

    public void setSketchData(SketchData sketchData) {
        mSketchPainter.setSketchData(sketchData);
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
    private float mScaleX = 1;
    private float mScaleY = 1;

    private PointF mOffset = new PointF(0, 0);
    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        mScaleX = scaleX;
        updateCurrentStrokeRecordPathScale();
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);
        mScaleY = scaleY;
        updateCurrentStrokeRecordPathScale();
    }

    private void updateCurrentStrokeRecordPathScale() {
        if (curStrokeRecord != null && curStrokeRecord.path != null) {
            curStrokeRecord.path.setScale(mScaleX,mScaleY);
        }
    }

    public void setOffset(float offsetX, float offsetY) {
        mOffset.x = offsetX;
        mOffset.y = offsetY;
        if (curStrokeRecord != null && curStrokeRecord.path != null) {
            curStrokeRecord.path.setOffset(offsetX,offsetY);
        }
    }

    public float scaleOffsetX(float x){
        return  (x - mOffset.x)/mScaleX;
    }

    public float scaleOffsetY(float y){
        return  (y - mOffset.y)/mScaleY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (editMode == MODE_STROKE) {
            //根据缩放状态，计算触摸点在缩放后画布的对应坐标位置
            curX = scaleOffsetX(event.getX());
            curY = scaleOffsetY(event.getY());
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
//                    MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_POINTER_DOWN");
                    break;
                case MotionEvent.ACTION_DOWN:
//                    MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_DOWN");
                    touch_down();
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
//                    MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_MOVE");
                    touch_move(event);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
//                    MLog.d(MLog.TAG_TOUCH,"SketchView->onTouch ACTION_UP");
                    touch_up();
                    invalidate();
                    break;
            }
            preX = curX;
            preY = curY;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBackground(canvas);
        mSketchPainter.drawRecord(canvas,true);
        if (onDrawChangedListener != null) {
            onDrawChangedListener.onDrawChanged();
        }
    }

    public void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null) {
            Matrix matrix = new Matrix();
            float wScale = (float) canvas.getWidth() / backgroundBitmap.getWidth();
            float hScale = (float) canvas.getHeight() / backgroundBitmap.getHeight();
            matrix.postScale(wScale, hScale);
            canvas.drawBitmap(backgroundBitmap, matrix, null);
        } else {
            canvas.drawColor(defaultBgColor);
        }
    }

    public float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
//        SCALE_MIN = SCALE_MAX / 5;
    }

    public void addRecord(StrokeRecord record) {
        mSketchPainter.addStrokeRecord(record);
    }

    public void touch_down() {
        downX = curX;
        downY = curY;
        needCheckTolerance = true;
        if (editMode == MODE_STROKE) {
            curStrokeRecord = new StrokeRecord(USER_ID,strokeType);
            strokePaint.setAntiAlias(true);//由于降低密度绘制，所以需要抗锯齿
            if (strokeType == StrokeRecord.STROKE_TYPE_ERASER) {
                strokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码
            } else {
                strokePaint.setXfermode(null);//关键代码
            }
            if (strokeType == STROKE_TYPE_ERASER) {
                strokePath = new StrokePath();
                strokePath.moveTo(downX, downY);
                strokePaint.setColor(Color.WHITE);
                strokePaint.setStrokeWidth(eraserSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
                curStrokeRecord.path = strokePath;
            } else if (strokeType == STROKE_TYPE_DRAW || strokeType == STROKE_TYPE_LINE) {
                strokePath = new StrokePath();
                strokePath.moveTo(downX, downY);
                curStrokeRecord.path = strokePath;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
                RectF rect = new RectF(downX, downY, downX, downY);
                curStrokeRecord.rect = rect;
                strokePaint.setColor(strokeRealColor);
                strokePaint.setStrokeWidth(strokeSize);
                curStrokeRecord.paint = new Paint(strokePaint); // Clones the mPaint object
            } else if (strokeType == STROKE_TYPE_TEXT) {
                curStrokeRecord.textOffX = (int) downX;
                curStrokeRecord.textOffY = (int) downY;
                TextPaint tp = new TextPaint();
                tp.setColor(strokeRealColor);
                curStrokeRecord.textPaint = tp; // Clones the mPaint object
                textWindowCallback.onText(this, curStrokeRecord);
                return;
            }
            mSketchPainter.addStrokeRecord(curStrokeRecord);
        }
    }

    public void touch_move(MotionEvent event) {
        if (!needCheckTolerance ||
                (needCheckTolerance && MathUtil.rectDiagonal(curX-downX, curY- downY) > TOUCH_TOLERANCE)) {
            needCheckTolerance = false;
            if (editMode == MODE_STROKE) {
                if (strokeType == STROKE_TYPE_ERASER) {
                    strokePath.quadTo(UtilBessel.ctrlX(preX, curX), UtilBessel.ctrlY(preY,curY), UtilBessel.endX(preX, curX), UtilBessel.endY(preY, curY));
                } else if (strokeType == STROKE_TYPE_DRAW) {
                    strokePath.quadTo(UtilBessel.ctrlX(preX, curX), UtilBessel.ctrlY(preY,curY), UtilBessel.endX(preX, curX), UtilBessel.endY(preY, curY));
                } else if (strokeType == STROKE_TYPE_LINE) {
                    strokePath.reset();
                    strokePath.moveTo(downX, downY);
                    strokePath.lineTo(curX, curY);
                } else if (strokeType == STROKE_TYPE_CIRCLE || strokeType == STROKE_TYPE_RECTANGLE) {
                    curStrokeRecord.rect.set(downX < curX ? downX : curX, downY < curY ? downY : curY, downX > curX ? downX : curX, downY > curY ? downY : curY);
                } else if (strokeType == STROKE_TYPE_TEXT) {

                }
            }
        }
        preX = curX;
        preY = curY;
    }

    public void touch_up() {
        if (strokePath != null) {
            //先只同步DRAW类型
            if (strokePath.getPathType() == StrokePath.PathType.QUAD_TO) {
                strokePath.end(curX, curY);
            }
        }
        notifyDrawListener(curStrokeRecord);
    }

    private void notifyDrawListener(StrokeRecord strokeRecord) {
        if (onStrokeRecordChangeListener != null) {
            onStrokeRecordChangeListener.onPathDrawFinish(strokeRecord);
        }
    }

    private void notifyDeleteListener(long uid,int sq){
        if (onStrokeRecordChangeListener != null) {
            onStrokeRecordChangeListener.onPathDeleted(uid, sq);
        }
    }

    private void notifyClearListener(){
        if (onStrokeRecordChangeListener != null) {
            onStrokeRecordChangeListener.onPathCleared();
        }
    }

    @NonNull
    public Bitmap getResultBitmap() {
        return getResultBitmap(null);
    }

    public SketchData getSketchData() {
        return mSketchPainter.getSketchData();
    }

    @NonNull
    public Bitmap getResultBitmap(Bitmap addBitmap) {
        Bitmap newBM = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.RGB_565);
//        Bitmap newBM = Bitmap.createBitmap(1280, 800, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(newBM);
//        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));//抗锯齿
        //绘制背景
        drawBackground(canvas);
        mSketchPainter.drawRecord(canvas, false);

        if (addBitmap != null) {
            canvas.drawBitmap(addBitmap, 0, 0, null);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
//        return newBM;
        Bitmap bitmap = BitmapUtils.createBitmapThumbnail(newBM, true, 800, 1280);
        return bitmap;
    }

    /**
     * 删除某人的某笔
     * @param uid
     * @param sq
     */
    public void deleteRecord(long uid, int sq, boolean manual) {
        mSketchPainter.deleteRecord(uid,sq);
        if (manual) {
            notifyDeleteListener(uid, sq);
        }
    }

    /*
     * 删除一笔
     */
    public void undo(){
        List<StrokeRecord> undo = mSketchPainter.undo();
        if (undo.size()>0) {
            deleteRecord(undo.get(0).userid, undo.get(0).id,true);
        }
    }

    /*
     * 撤销
     */
    public void redo() {
        List<StrokeRecord> redo = mSketchPainter.redo();
        for (StrokeRecord strokeRecord : redo) {
            notifyDrawListener(strokeRecord);
        }
    }

    public int getRedoCount() {
        return mSketchPainter.getRedoCount();
    }

    public int getRecordCount() {
        return mSketchPainter.getRecordCount();
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

    public void erase(boolean manual) {
        if (manual) {
            notifyClearListener();
        }
        mSketchPainter.erase();
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            // 回收并且置为null
            backgroundBitmap.recycle();
            backgroundBitmap = null;
        }
    }

    public void setOnDrawChangedListener(OnDrawChangedListener listener) {
        this.onDrawChangedListener = listener;
    }

    public void setBackgroundByBitmap(Bitmap sampleBM) {
        backgroundBitmap = sampleBM;
        backgroundSrcRect = new Rect(0, 0, backgroundBitmap.getWidth(), backgroundBitmap.getHeight());
        backgroundDstRect = new Rect(0, 0, mWidth, mHeight);
        invalidate();
    }

    public float getScaleWidth(){
        return getWidth() * getScaleX();
    }

    public float getScaleHeight(){
        return getHeight() * getScaleY();
    }

    public int getEditMode() {
        return editMode;
    }

    public void setEditMode(int editMode) {
        this.editMode = editMode;
    }

    public static final int getSketchWidth(){
        return mWidth;
    }

    public static final int getSketchHeight() {
        return mHeight;
    }

    public void setOnStrokeRecordChangeListener(OnStrokeRecordChangeListener listener){
        onStrokeRecordChangeListener = listener;
    }

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }

    public interface OnDrawChangedListener {
        void onDrawChanged();
    }

    public interface OnStrokeRecordChangeListener {
        void onPathDrawFinish(StrokeRecord strokeRecord);
        void onPathDeleted(long userid, int sq);
        void onPathCleared();
    }
}