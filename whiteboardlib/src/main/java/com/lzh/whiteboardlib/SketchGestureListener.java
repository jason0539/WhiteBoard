package com.lzh.whiteboardlib;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.lzh.whiteboardlib.utils.MLog;

public class SketchGestureListener {
    private GestureDetector gestureDetector;

    public SketchGestureListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->onScroll x = " + distanceX + ", y = " + distanceY);
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        });
        gestureDetector.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                MLog.d(MLog.TAG_TOUCH, "ScaleSketchView->onDoubleTap ");
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
    }

    public void onTouchEvent(MotionEvent event) {

    }

}
