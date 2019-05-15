package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class FilmstripGestureRecognizer {
    private static final Tag TAG = new Tag("FStripGestureRecog");
    private final GestureDetector mGestureDetector;
    private final Listener mListener;
    private final ScaleGestureDetector mScaleDetector;

    public interface Listener {
        boolean onDoubleTap(float f, float f2);

        boolean onDown(float f, float f2);

        boolean onFling(float f, float f2);

        void onLongPress(float f, float f2);

        boolean onScale(float f, float f2, float f3);

        boolean onScaleBegin(float f, float f2);

        void onScaleEnd();

        boolean onScroll(float f, float f2, float f3, float f4);

        boolean onSingleTapUp(float f, float f2);

        boolean onUp(float f, float f2);
    }

    private class MyDoubleTapListener implements OnDoubleTapListener {
        private MyDoubleTapListener() {
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            return FilmstripGestureRecognizer.this.mListener.onSingleTapUp(e.getX(), e.getY());
        }

        public boolean onDoubleTap(MotionEvent e) {
            return FilmstripGestureRecognizer.this.mListener.onDoubleTap(e.getX(), e.getY());
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            return true;
        }
    }

    private class MyGestureListener extends SimpleOnGestureListener {
        private MyGestureListener() {
        }

        public void onLongPress(MotionEvent e) {
            FilmstripGestureRecognizer.this.mListener.onLongPress(e.getX(), e.getY());
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return FilmstripGestureRecognizer.this.mListener.onScroll(e2.getX(), e2.getY(), dx, dy);
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return FilmstripGestureRecognizer.this.mListener.onFling(velocityX, velocityY);
        }

        public boolean onDown(MotionEvent e) {
            FilmstripGestureRecognizer.this.mListener.onDown(e.getX(), e.getY());
            return super.onDown(e);
        }
    }

    private class MyScaleListener extends SimpleOnScaleGestureListener {
        private MyScaleListener() {
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return FilmstripGestureRecognizer.this.mListener.onScaleBegin(detector.getFocusX(), detector.getFocusY());
        }

        public boolean onScale(ScaleGestureDetector detector) {
            return FilmstripGestureRecognizer.this.mListener.onScale(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            FilmstripGestureRecognizer.this.mListener.onScaleEnd();
        }
    }

    public FilmstripGestureRecognizer(Context context, Listener listener) {
        this.mListener = listener;
        this.mGestureDetector = new GestureDetector(context, new MyGestureListener(), null, true);
        this.mGestureDetector.setOnDoubleTapListener(new MyDoubleTapListener());
        this.mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean gestureProcessed = this.mGestureDetector.onTouchEvent(event);
        boolean scaleProcessed = this.mScaleDetector.onTouchEvent(event);
        if (event.getAction() == 1) {
            this.mListener.onUp(event.getX(), event.getY());
        }
        return gestureProcessed | scaleProcessed;
    }
}
