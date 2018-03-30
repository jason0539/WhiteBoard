package com.lzh.whiteboardlib;

import android.content.Context;
import android.graphics.Bitmap;
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

    private static final float MAX_SCALE = 10.0F;
    private static final float MIN_SCALE = 1f;
    private float mBorderX, mBorderY;
    private float[] mMatrixValus = new float[9];
    private SketchView pathView;
    private boolean isDragAndTranslate;

    private float mOldDistance;
    private PointF mOldPointer;

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
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->onScroll x = " + distanceX + ", y = " + distanceY);
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
                    mOldPointer = TouchEventUtil.middleOfTwoFinger(ev);
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
                    scaleFactor = checkingScale(pathView.getScaleX(), scaleFactor);
                    pathView.setScaleX(pathView.getScaleX() * scaleFactor);
                    pathView.setScaleY(pathView.getScaleY() * scaleFactor);
                    mOldDistance = newDistance;

                    PointF newPointer = TouchEventUtil.middleOfTwoFinger(ev);
                    pathView.setX(pathView.getX() + newPointer.x - mOldPointer.x);
                    pathView.setY(pathView.getY() + newPointer.y - mOldPointer.y);
                    mOldPointer = newPointer;
                    checkingBorder();
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
                pathView.getMatrix().getValues(mMatrixValus);
                pathView.setScaleAndOffset(pathView.getScaleX(), pathView.getScaleY(), mMatrixValus[2], mMatrixValus[5]);
                isDragAndTranslate = false;
                break;
        }

        return true;

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
        pathView.setX(pathView.getX() + offset.x);
        pathView.setY(pathView.getY() + offset.y);
        if (pathView.getScaleX() == 1) {
            pathView.setX(0);
            pathView.setY(0);
        }
    }

    private PointF offsetBorder() {
        PointF offset = new PointF(0, 0);
        if (pathView.getScaleX() > 1) {
            pathView.getMatrix().getValues(mMatrixValus);
            if (mMatrixValus[2] > -(mBorderX * (pathView.getScaleX() - 1))) {
                offset.x = -(mMatrixValus[2] + mBorderX * (pathView.getScaleX() - 1));
            }

            if (mMatrixValus[2] + pathView.getWidth() * pathView.getScaleX() - mBorderX * (pathView.getScaleX() - 1) < getWidth()) {
                offset.x = getWidth() - (mMatrixValus[2] + pathView.getWidth() * pathView.getScaleX() - mBorderX * (pathView.getScaleX() - 1));
            }

            if (mMatrixValus[5] > -(mBorderY * (pathView.getScaleY() - 1))) {
                System.out.println("offsetY:" + mMatrixValus[5] + " borderY:" + mBorderY + " scale:" + getScaleY() + " scaleOffset:" + mBorderY * (getScaleY() - 1));
                offset.y = -(mMatrixValus[5] + mBorderY * (pathView.getScaleY() - 1));
            }

            if (mMatrixValus[5] + pathView.getHeight() * pathView.getScaleY() - mBorderY * (pathView.getScaleY() - 1) < getHeight()) {
                offset.y = getHeight() - (mMatrixValus[5] + pathView.getHeight() * pathView.getScaleY() - mBorderY * (pathView.getScaleY() - 1));
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
            pathView.setScaleAndOffset(pathView.getScaleX(), pathView.getScaleY(),mMatrixValus[2], mMatrixValus[5]);

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
}
