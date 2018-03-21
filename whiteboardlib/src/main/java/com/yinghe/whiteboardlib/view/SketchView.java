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

package com.yinghe.whiteboardlib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import com.yinghe.whiteboardlib.Utils.BitmapUtils;
import com.yinghe.whiteboardlib.Utils.MLog;
import com.yinghe.whiteboardlib.Utils.MathUtil;
import com.yinghe.whiteboardlib.Utils.PaintUtils;
import com.yinghe.whiteboardlib.Utils.ScreenUtils;
import com.yinghe.whiteboardlib.bean.PhotoRecord;
import com.yinghe.whiteboardlib.bean.SketchData;
import com.yinghe.whiteboardlib.bean.StrokePath;
import com.yinghe.whiteboardlib.bean.StrokeRecord;

import static com.yinghe.whiteboardlib.Utils.BitmapUtils.createBitmapThumbnail;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_CIRCLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_DRAW;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_ERASER;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_LINE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_RECTANGLE;
import static com.yinghe.whiteboardlib.bean.StrokeRecord.STROKE_TYPE_TEXT;


public class SketchView extends View {

    public static final int EDIT_STROKE = 1;
    public static final int DEFAULT_STROKE_SIZE = 3;
    public static final int DEFAULT_STROKE_ALPHA = 100;
    public static final int DEFAULT_ERASER_SIZE = 50;
    public static final float TOUCH_TOLERANCE = 4;
    public static final int ACTION_NONE = 0;
    //    public int curSketchData.editMode = EDIT_STROKE;
    public static float SCALE_MAX = 4.0f;
    public static float SCALE_MIN = 0.2f;
    public static float SCALE_MIN_LEN;
    public static float MULTI_POINTER_THRESH = 10;//两指间距阈值，低于该值认为是误触
    public final String TAG = getClass().getSimpleName();
    public Paint boardPaint;

    public SketchData curSketchData;
    //    public Bitmap curSketchData.backgroundBM;
    public Rect backgroundSrcRect = new Rect();
    public Rect backgroundDstRect = new Rect();
    public StrokeRecord curStrokeRecord;
    public PhotoRecord curPhotoRecord;
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
    public int mWidth, mHeight;
    //    public List<PhotoRecord> curSketchData.photoRecordList;
//    public List<StrokeRecord> curSketchData.strokeRecordList;
//    public List<StrokeRecord> curSketchData.strokeRedoList;
    public Context mContext;
    public int drawDensity = 1;//绘制密度,数值越高图像质量越低、性能越好
    public boolean needCheckThresh = true;//每次down事件都要检查是否滑动距离超过阈值，超过才绘制
    /**
     * 缩放手势
     */
    public ScaleGestureDetector mScaleGestureDetector = null;
    public OnDrawChangedListener onDrawChangedListener;
    public OnStrokeRecordFinishListener onStrokeRecordFinishListener;

    public SketchView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mContext = context;
//        setSketchData(new SketchData());
        initParams(context);
        if (isFocusable()) {
            mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    MLog.d(MLog.TAG_TOUCH,"SketchView->onScale ");
                    onScaleAction(detector);
                    return true;
                }


                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    MLog.d(MLog.TAG_TOUCH,"SketchView->onScaleBegin ");
                    return true;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                    MLog.d(MLog.TAG_TOUCH,"SketchView->onScaleEnd ");
                }
            });
        }
        invalidate();
    }

    public void setTextWindowCallback(TextWindowCallback textWindowCallback) {
        this.textWindowCallback = textWindowCallback;
    }

    public int getStrokeType() {
        return curSketchData.strokeType;
    }

//    public int curSketchData.strokeType = StrokeRecord.STROKE_TYPE_DRAW;

    public void setStrokeType(int strokeType) {
        this.curSketchData.strokeType = strokeType;
    }

    public void setSketchData(SketchData sketchData) {
        this.curSketchData = sketchData;
        curPhotoRecord = null;
    }

    public void updateSketchData(SketchData sketchData) {
        if (curSketchData != null){
            curSketchData.thumbnailBM = getThumbnailResultBitmap();//更新数据前先保存上一份数据的缩略图
        }
        setSketchData(sketchData);
    }

    public void addStrokePath(StrokeRecord strokeRecord) {
        curSketchData.strokeRecordList.add(strokeRecord);
        invalidate();
    }

    public void initParams(Context context) {

//        setFocusable(true);
//        setFocusableInTouchMode(true);
        setBackgroundColor(Color.WHITE);

        strokePaint = PaintUtils.createDefaultStrokePaint();
        strokePaint.setColor(strokeRealColor);
        strokePaint.setStrokeWidth(strokeSize);

        boardPaint = new Paint();
        boardPaint.setColor(Color.GRAY);
        boardPaint.setStrokeWidth(ScreenUtils.dip2px(mContext, 0.8f));
        boardPaint.setStyle(Paint.Style.STROKE);

        SCALE_MIN_LEN = ScreenUtils.dip2px(context, 20);
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
        SketchViewStatusHolder.setSize(mWidth, mHeight);
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
        curX = ((event.getX()) / drawDensity - mOffset.x)/mScale;
        curY = ((event.getY()) / drawDensity - mOffset.y)/mScale;
        int toolType = event.getToolType(0);
//        //检测到手指点击自动进入拖动图片模式
//        if (toolType == MotionEvent.TOOL_TYPE_FINGER&&curSketchData.editMode == EDIT_STROKE) {
//            curSketchData.editMode = EDIT_PHOTO;
//        } else if (toolType == MotionEvent.TOOL_TYPE_STYLUS){//检测到手写板开始绘画则自动进入绘画模式
//            curSketchData.editMode = EDIT_STROKE;
//        }
//        Log.d(getClass().getSimpleName(), "onTouch======" + toolType);
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
        if (curSketchData.backgroundBM != null) {
//            Rect dstRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
//            canvas.drawBitmap(curSketchData.backgroundBM, backgroundSrcRect, backgroundDstRect, null);
            Matrix matrix = new Matrix();
            float wScale = (float) canvas.getWidth() / curSketchData.backgroundBM.getWidth();
            float hScale = (float) canvas.getHeight() / curSketchData.backgroundBM.getHeight();
            matrix.postScale(wScale, hScale);
            canvas.drawBitmap(curSketchData.backgroundBM, matrix, null);
//            canvas.drawBitmap(curSketchData.backgroundBM, backgroundSrcRect, dstRect, null);
            Log.d(TAG, "drawBackground:src= " + backgroundSrcRect.toString() + ";dst=" + backgroundDstRect.toString());
        } else {
//            try {
//                setBackgroundByPath("background/bg_yellow_board.png");
//            canvas.drawColor(Color.rgb(246, 246, 246));
//            } catch (Exception e) {
//                e.printStackTrace();
//            canvas.drawColor(Color.rgb(246, 246, 246));
            canvas.drawColor(Color.rgb(239, 234, 224));
//            }
        }
    }

    public Bitmap tempBitmap;//临时绘制的bitmap
    public Canvas tempCanvas;
    public Bitmap tempHoldBitmap;//保存已固化的笔画bitmap
    public Canvas tempHoldCanvas;

    public void drawRecord(Canvas canvas, boolean isDrawBoard) {
        if (curSketchData != null) {
            for (PhotoRecord record : curSketchData.photoRecordList) {
                if (record != null) {
                    Log.d(getClass().getSimpleName(), "drawRecord" + record.bitmap.toString());
                    canvas.drawBitmap(record.bitmap, record.matrix, null);
                }
            }
            //新建一个临时画布，以便橡皮擦生效
            if (tempBitmap == null) {
                tempBitmap = Bitmap.createBitmap(getWidth() / drawDensity, getHeight() / drawDensity, Bitmap.Config.ARGB_4444);
                tempCanvas = new Canvas(tempBitmap);
            }
            //新建一个临时画布，以便保存过多的画笔
            if (tempHoldBitmap == null) {
                tempHoldBitmap = Bitmap.createBitmap(getWidth() / drawDensity, getHeight() / drawDensity, Bitmap.Config.ARGB_4444);
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

    //绘制图像边线（由于图形旋转或不一定是矩形，所以用Path绘制边线）
    public void drawBoard(Canvas canvas, float[] photoCorners) {
        Path photoBorderPath = new Path();
        photoBorderPath.moveTo(photoCorners[0], photoCorners[1]);
        photoBorderPath.lineTo(photoCorners[2], photoCorners[3]);
        photoBorderPath.lineTo(photoCorners[4], photoCorners[5]);
        photoBorderPath.lineTo(photoCorners[6], photoCorners[7]);
        photoBorderPath.lineTo(photoCorners[0], photoCorners[1]);
        canvas.drawPath(photoBorderPath, boardPaint);
    }

    public float[] calculateCorners(PhotoRecord record) {
        float[] photoCornersSrc = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        float[] photoCorners = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        RectF rectF = record.photoRectSrc;
        photoCornersSrc[0] = rectF.left;
        photoCornersSrc[1] = rectF.top;
        photoCornersSrc[2] = rectF.right;
        photoCornersSrc[3] = rectF.top;
        photoCornersSrc[4] = rectF.right;
        photoCornersSrc[5] = rectF.bottom;
        photoCornersSrc[6] = rectF.left;
        photoCornersSrc[7] = rectF.bottom;
        photoCornersSrc[8] = rectF.centerX();
        photoCornersSrc[9] = rectF.centerY();
        curPhotoRecord.matrix.mapPoints(photoCorners, photoCornersSrc);
        return photoCorners;
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
        needCheckThresh = true;
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
        if (!needCheckThresh ||
                (needCheckThresh && MathUtil.rectDiagonal(curX-downX, curY- downY) > MULTI_POINTER_THRESH)) {
            needCheckThresh = false;
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

    public void onScaleAction(ScaleGestureDetector detector) {
        float[] photoCorners = calculateCorners(curPhotoRecord);
        //目前图片对角线长度
        float len = (float) Math.sqrt(Math.pow(photoCorners[0] - photoCorners[4], 2) + Math.pow(photoCorners[1] - photoCorners[5], 2));
        double photoLen = Math.sqrt(Math.pow(curPhotoRecord.photoRectSrc.width(), 2) + Math.pow(curPhotoRecord.photoRectSrc.height(), 2));
        float scaleFactor = detector.getScaleFactor();
        //设置Matrix缩放参数
        if ((scaleFactor < 1 && len >= photoLen * SCALE_MIN && len >= SCALE_MIN_LEN) || (scaleFactor > 1 && len <= photoLen * SCALE_MAX)) {
            Log.e(scaleFactor + "", scaleFactor + "");
            curPhotoRecord.matrix.postScale(scaleFactor, scaleFactor, photoCorners[8], photoCorners[9]);
        }
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

    @NonNull
    public void createCurThumbnailBM() {
        curSketchData.thumbnailBM = getThumbnailResultBitmap();
    }

    @NonNull
    public Bitmap getThumbnailResultBitmap() {
        return createBitmapThumbnail(getResultBitmap(), true, ScreenUtils.dip2px(mContext, 200), ScreenUtils.dip2px(mContext, 200));
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
        return (curSketchData.strokeRecordList != null && curSketchData.photoRecordList != null) ? curSketchData.strokeRecordList.size() + curSketchData.photoRecordList.size() : 0;
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
        // 先判断是否已经回收
        for (PhotoRecord record : curSketchData.photoRecordList) {
            if (record != null && record.bitmap != null && !record.bitmap.isRecycled()) {
                record.bitmap.recycle();
                record.bitmap = null;
            }
        }
        if (curSketchData.backgroundBM != null && !curSketchData.backgroundBM.isRecycled()) {
            // 回收并且置为null
            curSketchData.backgroundBM.recycle();
            curSketchData.backgroundBM = null;
        }
        curSketchData.strokeRecordList.clear();
        curSketchData.photoRecordList.clear();
        curSketchData.strokeRedoList.clear();
        curPhotoRecord = null;

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

    public void addPhotoByPath(String path) {
        Bitmap sampleBM = BitmapUtils.getSampleBitMap(mContext,path);
        addPhotoByBitmap(sampleBM);
    }

    public void addPhotoByBitmap(Bitmap sampleBM) {
        if (sampleBM != null) {
            PhotoRecord newRecord = initPhotoRecord(sampleBM);
            setCurPhotoRecord(newRecord);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void addPhotoByBitmap(Bitmap sampleBM, int[] position) {
        if (sampleBM != null) {
            PhotoRecord newRecord = initPhotoRecord(sampleBM, position);
            setCurPhotoRecord(newRecord);
        } else {
            Toast.makeText(mContext, "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void removeCurrentPhotoRecord() {
        curSketchData.photoRecordList.remove(curPhotoRecord);
        setCurPhotoRecord(null);
        actionMode = ACTION_NONE;
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
        curSketchData.backgroundBM = sampleBM;
        backgroundSrcRect = new Rect(0, 0, curSketchData.backgroundBM.getWidth(), curSketchData.backgroundBM.getHeight());
        backgroundDstRect = new Rect(0, 0, mWidth, mHeight);
        invalidate();
    }

    @NonNull
    public PhotoRecord initPhotoRecord(Bitmap bitmap) {
        PhotoRecord newRecord = new PhotoRecord();
        newRecord.bitmap = bitmap;
        newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
        newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
        newRecord.matrix = new Matrix();
        newRecord.matrix.postTranslate(getWidth() / 2 - bitmap.getWidth() / 2, getHeight() / 2 - bitmap.getHeight() / 2);
        return newRecord;
    }

    @NonNull
    public PhotoRecord initPhotoRecord(Bitmap bitmap, int[] position) {
        PhotoRecord newRecord = new PhotoRecord();
        newRecord.bitmap = bitmap;
        newRecord.photoRectSrc = new RectF(0, 0, newRecord.bitmap.getWidth(), newRecord.bitmap.getHeight());
        newRecord.scaleMax = getMaxScale(newRecord.photoRectSrc);//放大倍数
        newRecord.matrix = new Matrix();
        newRecord.matrix.postTranslate(position[0], position[1]);
        return newRecord;
    }

    public void setCurPhotoRecord(PhotoRecord record) {
        curSketchData.photoRecordList.remove(record);
        curSketchData.photoRecordList.add(record);
        curPhotoRecord = record;
        invalidate();
    }

    public int getEditMode() {
        return curSketchData.editMode;
    }

    public void setEditMode(int editMode) {
        this.curSketchData.editMode = editMode;
        invalidate();
    }

    public void setOnStrokeRecordFinishListener(OnStrokeRecordFinishListener listener){
        onStrokeRecordFinishListener = listener;
    }

    public interface TextWindowCallback {
        void onText(View view, StrokeRecord record);
    }

    public interface OnDrawChangedListener {

        public void onDrawChanged();
    }

    public interface OnStrokeRecordFinishListener {
        void onPathDrawFinish(StrokeRecord strokeRecord);
    }
}