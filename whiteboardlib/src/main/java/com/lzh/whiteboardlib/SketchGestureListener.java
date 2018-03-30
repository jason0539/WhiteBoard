package com.lzh.whiteboardlib;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class SketchGestureListener {
    private GestureDetector gestureDetector;
    private OnListener onListener;
    public SketchGestureListener(Context context,OnListener listener) {
        onListener = listener;
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
                if (onListener != null) {
                    onListener.onScroll(e1,e2,distanceX, distanceY);
                }
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
                if (onListener != null) {
                    onListener.onDoubleTap(e);
                }
                return false;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        });
    }

    public void onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
    }

    public interface OnListener{
        boolean onDoubleTap(MotionEvent e);
        boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
    }

}
