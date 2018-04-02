package com.lzh.whiteboardlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.lzh.whiteboardlib.bean.SketchData;
import com.lzh.whiteboardlib.bean.StrokeRecord;
import com.lzh.whiteboardlib.utils.BitmapUtils;
import com.lzh.whiteboardlib.utils.MLog;
import com.lzh.whiteboardlib.utils.TouchEventUtil;

/**
 * Created by liuzhenhui on 2018/3/16.
 */

public class ScaleSketchView extends RelativeLayout {

    private static final float MAX_SCALE = 5f;
    private static final float MIN_SCALE = 1f;
    private float[] mMatrixValus = new float[9];
    private SketchView pathView;
    private boolean isDragAndTranslate;

    private float mOldDistance;

    private boolean isAutoScale;
    private int doubleScale = 3;
    private int scaleDelay = 16;

    private SketchGestureListener mGestureListener;

    public ScaleSketchView(Context context, AttributeSet attributeSet) {
        super(context);
        RelativeLayout.LayoutParams pathViewParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        pathView = new SketchView(getContext(), attributeSet);
        addView(pathView, pathViewParams);
        mGestureListener = new SketchGestureListener(context,onListener);
    }

    SketchGestureListener.OnListener onListener = new SketchGestureListener.OnListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->onDoubleTap ");
            if (pathView.getEditMode() == SketchView.MODE_VIEW && !isAutoScale) {
                float cx = e.getX();
                float cy = e.getY();
                if (pathView.getScaleX() < doubleScale) {
                    post(new AutoScaleRunnable(doubleScale, cx, cy));
                } else {
                    post(new AutoScaleRunnable(1, cx, cy));
                }
                isAutoScale = true;
            }
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            MLog.d(MLog.TAG_OFFSET, "ScaleSketchView->onScroll x = " + distanceX + ", y = " + distanceY);
            if (pathView.getEditMode() == SketchView.MODE_VIEW || e2.getPointerCount() > 1) {
                float newX = pathView.getX() - distanceX;
                float newY = pathView.getY() - distanceY;
                pathView.setX(newX);
                pathView.setY(newY);
                pathView.getMatrix().getValues(mMatrixValus);
                pathView.setOffset( mMatrixValus[Matrix.MTRANS_X], mMatrixValus[Matrix.MTRANS_Y]);
                checkingBorder();
            }
            return false;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mGestureListener.onTouchEvent(ev);
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
//                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->dispatchTouchEvent ACTION_DOWN");
                return pathView.onTouchEvent(ev);
            case MotionEvent.ACTION_POINTER_DOWN:
                //两指时自动进入缩放/拖拽模式
                isDragAndTranslate = TouchEventUtil.isTwoFingerEvent(ev);
                if (isDragAndTranslate) {
                    mOldDistance = TouchEventUtil.spacingOfTwoFinger(ev);
                }
//                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->dispatchTouchEvent ACTION_POINTER_DOWN");
                break;
            case MotionEvent.ACTION_MOVE:
//                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->dispatchTouchEvent ACTION_MOVE");
                if (!isDragAndTranslate && TouchEventUtil.fingersOfEvent(ev) == 1) {
                    return pathView.onTouchEvent(ev);
                }
                if (TouchEventUtil.isTwoFingerEvent(ev)) {
                    float newDistance = TouchEventUtil.spacingOfTwoFinger(ev);
                    float scaleFactor = newDistance / mOldDistance;
                    PointF centerPoint = TouchEventUtil.middleOfTwoFinger(ev);
                    scaleSketchView(scaleFactor, -1,-1);
                    mOldDistance = newDistance;
                }else if (TouchEventUtil.fingersOfEvent(ev) > 2) {
                    isDragAndTranslate = false;
                }
            case MotionEvent.ACTION_POINTER_UP:
//                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->dispatchTouchEvent ACTION_POINTER_UP");
                break;
            case MotionEvent.ACTION_UP:
//                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->dispatchTouchEvent ACTION_UP");
                if (!isDragAndTranslate) {
                    return pathView.onTouchEvent(ev);
                }
                isDragAndTranslate = false;
                break;
        }

        return true;

    }

    private void scaleSketchView(float scaleFactor,float cx,float cy) {
        MLog.d(MLog.TAG_SCALE,"ScaleSketchView->scaleSketchView cx = " + cx + ",cy = " + cy);
        scaleFactor = checkingScale(pathView.getScaleX(), scaleFactor);
        if (cx > -1 && cy > -1) {
            pathView.setPivotX(cx);
            pathView.setPivotY(cy);
        }
        pathView.setScaleX(pathView.getScaleX() * scaleFactor);
        pathView.setScaleY(pathView.getScaleY() * scaleFactor);
        checkingBorder();
    }

    private float checkingScale(float scale, float scaleFactor) {
        if ((scale <= MAX_SCALE && scaleFactor > 1.0) || (scale >= MIN_SCALE && scaleFactor < 1.0)) {
            if (scale * scaleFactor < MIN_SCALE) {
                scaleFactor = MIN_SCALE / scale;
            }

            if (scale * scaleFactor > MAX_SCALE) {
                scaleFactor = MAX_SCALE / scale;
            }

        }

        return scaleFactor;
    }

    private void checkingBorder() {
        PointF offset = offsetBorder();
        MLog.d(MLog.TAG_TOUCH,"ScaleSketchView->checkingBorder x = " + offset.x + ",y = " + offset.y);
        MLog.d(MLog.TAG_TOUCH,"ScaleSketchView->checkingBorder =========================================================================================================");
        pathView.setX(pathView.getX() + offset.x);
        pathView.setY(pathView.getY() + offset.y);
    }

    private PointF offsetBorder() {
        PointF offset = new PointF(0, 0);
        if (pathView.getScaleX() >= 1 || pathView.getScaleY() >= 1) {
            MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder 当前缩放x="+pathView.getScaleX()+",y="+pathView.getScaleY());
            MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder pathView.getWidth()="+pathView.getWidth()+",pathView.getWidth()*pathView.getScaleX()="+pathView.getScaleX()*pathView.getWidth());
            MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder pathView.getHeight()="+pathView.getHeight()+",pathView.getHeight()*pathView.getScaleY()="+pathView.getScaleY()*pathView.getHeight());
            MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder getWidth = " + getWidth() + ",getHeight()=" + getHeight());
            pathView.getMatrix().getValues(mMatrixValus);
            MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder 当前位移x="+mMatrixValus[Matrix.MTRANS_X]+",y="+mMatrixValus[Matrix.MTRANS_Y]);
            //左边界
            if (mMatrixValus[Matrix.MTRANS_X] > 0) {
                offset.x = -(mMatrixValus[Matrix.MTRANS_X]);
                MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder 到达左边界，纠正offset.x="+offset.x);
            }
            //右边界
            if (mMatrixValus[Matrix.MTRANS_X] < -(pathView.getWidth() * pathView.getScaleX() - getWidth())) {
                offset.x = getWidth() - (mMatrixValus[Matrix.MTRANS_X] + pathView.getWidth() * pathView.getScaleX());
                MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder 到达右边界，纠正offset.x="+offset.x);
            }
            //上边界
            if (mMatrixValus[Matrix.MTRANS_Y] > 0) {
                offset.y = -(mMatrixValus[Matrix.MTRANS_Y]);
                MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder 到达上边界，纠正offset.y="+offset.y);
            }
            //下边界
            if (mMatrixValus[Matrix.MTRANS_Y] < -(pathView.getHeight() * pathView.getScaleY() - getHeight())) {
                offset.y = getHeight() - (mMatrixValus[Matrix.MTRANS_Y] + pathView.getHeight() * pathView.getScaleY());
                MLog.d(MLog.TAG_OFFSET,"ScaleSketchView->offsetBorder 到达下边界，纠正offset.y="+offset.y);
            }
        }

        return offset;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }


    public void setSketchData(SketchData newSketchData) {
        pathView.setSketchData(newSketchData);
    }

    public void setEditMode(int editMode) {
        pathView.setEditMode(editMode);
    }

    public void setStrokeAlpha(int alpha) {
        pathView.setStrokeAlpha(alpha);
    }

    public void setOnDrawChangedListener(SketchView.OnDrawChangedListener whiteBoardFragment) {
        pathView.setOnDrawChangedListener(whiteBoardFragment);
    }

    public void setTextWindowCallback(SketchView.TextWindowCallback textWindowCallback) {
        pathView.setTextWindowCallback(textWindowCallback);
    }

    public void erase(boolean manual) {
        pathView.erase(manual);
    }

    public void redo() {
        pathView.redo();
    }

    public void undo() {
        pathView.undo();
    }

    public void setStrokeType(int strokeType) {
        pathView.setStrokeType(strokeType);
    }

    public int getRecordCount() {
        return pathView.getRecordCount();
    }

    public int getEditMode() {
        return pathView.getEditMode();
    }

    public int getStrokeType() {
        return pathView.getStrokeType();
    }

    public void setBackgroundByPath(String path) {
        Bitmap sampleBM = BitmapUtils.getSampleBitMap(getContext(),path);
        if (sampleBM != null) {
            int bgHeight = sampleBM.getHeight();
            int bgWidth = sampleBM.getWidth();
            int viewHeight = pathView.getHeight();
            int viewWidth = pathView.getWidth();

            float ratio = 1f * bgHeight / bgWidth;
            //宽度填满，高度缩放
            int newHeight = (int) (viewWidth * ratio);
            float scale = 1f * newHeight/viewHeight;

            pathView.setScaleX(1);
            pathView.setScaleY(scale);
            pathView.getMatrix().getValues(mMatrixValus);
            pathView.setOffset(mMatrixValus[2], mMatrixValus[5]);

            pathView.setBackgroundByBitmap(sampleBM);
        } else {
            Toast.makeText(getContext(), "图片文件路径有误！", Toast.LENGTH_SHORT).show();
        }
    }

    public void addStrokeRecord(StrokeRecord record) {
        pathView.addRecord(record);
    }

    public Bitmap getResultBitmap() {
        return pathView.getResultBitmap();
    }

    public SketchData getSketchData() {
        return pathView.getSketchData();
    }

    public void setSize(int newSize, int drawMode) {
        pathView.setSize(newSize, drawMode);
    }

    public int getRedoCount() {
        return pathView.getRedoCount();
    }

    public void setStrokeColor(int color) {
        pathView.setStrokeColor(color);
    }

    public SketchView getSketchView() {
        return pathView;
    }

    /**
     * 自动放大与缩小
     */
    private class AutoScaleRunnable implements Runnable {
        private float mTargetScale;
        // 缩放的中心点
        private float x;
        private float y;

        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        private float tmpScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if (pathView.getScaleX() < mTargetScale) {
                tmpScale = BIGGER;
            }
            if (pathView.getScaleX() > mTargetScale) {
                tmpScale = SMALL;
            }
        }

        @Override
        public void run() {
            //进行缩放
            scaleSketchView(tmpScale,x,y);

            float currentScale = pathView.getScaleX();

            if ((tmpScale >1.0f && currentScale < mTargetScale) ||(tmpScale<1.0f &&currentScale>mTargetScale)) {
                //这个方法是重新调用run()方法
                postDelayed(this, scaleDelay);
            }else{
                //设置为我们的目标值
                float scale = mTargetScale/currentScale;
                scaleSketchView(scale,x,y);
                isAutoScale = false;
            }
        }
    }
}
