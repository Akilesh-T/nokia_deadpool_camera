package com.hmdglobal.app.camera.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.filmstrip.FilmstripContentPanel;
import com.hmdglobal.app.camera.filmstrip.FilmstripController;
import com.hmdglobal.app.camera.ui.FilmstripGestureRecognizer;
import com.hmdglobal.app.camera.ui.FilmstripGestureRecognizer.Listener;

public class FilmstripLayout extends FrameLayout implements FilmstripContentPanel {
    private static final long DEFAULT_DURATION_MS = 200;
    private MyBackgroundDrawable mBackgroundDrawable;
    private final ValueAnimator mFilmstripAnimator = ValueAnimator.ofFloat(null);
    private AnimatorListener mFilmstripAnimatorListener = new AnimatorListener() {
        private boolean mCanceled;

        public void onAnimationStart(Animator animator) {
            this.mCanceled = false;
        }

        public void onAnimationEnd(Animator animator) {
            if (!this.mCanceled) {
                if (FilmstripLayout.this.mFilmstripContentTranslationProgress != 0.0f) {
                    FilmstripLayout.this.mFilmstripView.getController().goToFilmstrip();
                    FilmstripLayout.this.setVisibility(4);
                    return;
                }
                FilmstripLayout.this.notifyShown();
            }
        }

        public void onAnimationCancel(Animator animator) {
            this.mCanceled = true;
        }

        public void onAnimationRepeat(Animator animator) {
        }
    };
    private AnimatorUpdateListener mFilmstripAnimatorUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            FilmstripLayout.this.translateContentLayout(((Float) valueAnimator.getAnimatedValue()).floatValue());
            FilmstripLayout.this.mBackgroundDrawable.invalidateSelf();
        }
    };
    private FrameLayout mFilmstripContentLayout;
    private float mFilmstripContentTranslationProgress;
    private Listener mFilmstripGestureListener;
    private FilmstripView mFilmstripView;
    private FilmstripGestureRecognizer mGestureRecognizer;
    private Handler mHandler;
    private FilmstripContentPanel.Listener mListener;
    private int mSwipeTrend;

    private class MyBackgroundDrawable extends Drawable {
        private int mOffset;
        private Paint mPaint = new Paint();

        public MyBackgroundDrawable() {
            this.mPaint.setAntiAlias(true);
            this.mPaint.setColor(FilmstripLayout.this.getResources().getColor(R.color.filmstrip_background));
            this.mPaint.setAlpha(255);
        }

        public void setOffset(int offset) {
            this.mOffset = offset;
        }

        public void setAlpha(int i) {
            this.mPaint.setAlpha(i);
        }

        private void setAlpha(float a) {
            setAlpha((int) (255.0f * a));
        }

        public void setColorFilter(ColorFilter colorFilter) {
            this.mPaint.setColorFilter(colorFilter);
        }

        public int getOpacity() {
            return -3;
        }

        public void draw(Canvas canvas) {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() - ((float) this.mOffset) != ((float) (FilmstripLayout.this.getMeasuredWidth() - this.mOffset))) {
                setAlpha(1.0f - FilmstripLayout.this.mFilmstripContentTranslationProgress);
                canvas.drawRect(0.0f, 0.0f, (float) FilmstripLayout.this.getMeasuredWidth(), (float) FilmstripLayout.this.getMeasuredHeight(), this.mPaint);
            }
        }
    }

    private class MyGestureListener implements Listener {
        private MyGestureListener() {
        }

        /* synthetic */ MyGestureListener(FilmstripLayout x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onScroll(float x, float y, float dx, float dy) {
            if (FilmstripLayout.this.mFilmstripView.getController().getCurrentId() == -1 || FilmstripLayout.this.mFilmstripAnimator.isRunning()) {
                return true;
            }
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f && FilmstripLayout.this.mFilmstripGestureListener.onScroll(x, y, dx, dy)) {
                return true;
            }
            FilmstripLayout.this.mSwipeTrend = (((int) dx) >> 1) + (FilmstripLayout.this.mSwipeTrend >> 1);
            if (dx < 0.0f && FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                FilmstripLayout.this.mBackgroundDrawable.setOffset(0);
                FilmstripLayout.this.onSwipeOutBegin();
            }
            if (dx > 0.0f && FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == ((float) FilmstripLayout.this.getMeasuredWidth())) {
                int currentItemLeft = FilmstripLayout.this.mFilmstripView.getCurrentItemLeft();
                dx = (float) currentItemLeft;
                FilmstripLayout.this.mBackgroundDrawable.setOffset(currentItemLeft);
            }
            float translate = FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() - dx;
            if (translate < 0.0f) {
                translate = 0.0f;
            } else if (translate > ((float) FilmstripLayout.this.getMeasuredWidth())) {
                translate = (float) FilmstripLayout.this.getMeasuredWidth();
            }
            FilmstripLayout.this.translateContentLayoutByPixel(translate);
            if (translate == 0.0f && dx > 0.0f) {
                FilmstripLayout.this.mFilmstripAnimatorListener.onAnimationEnd(FilmstripLayout.this.mFilmstripAnimator);
            }
            FilmstripLayout.this.mBackgroundDrawable.invalidateSelf();
            return true;
        }

        public boolean onSingleTapUp(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onSingleTapUp(x, y);
            }
            return false;
        }

        public boolean onDoubleTap(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onDoubleTap(x, y);
            }
            return false;
        }

        public boolean onFling(float velocityX, float velocityY) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onFling(velocityX, velocityY);
            }
            return false;
        }

        public boolean onScaleBegin(float focusX, float focusY) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onScaleBegin(focusX, focusY);
            }
            return false;
        }

        public boolean onScale(float focusX, float focusY, float scale) {
            if (FilmstripLayout.this.mFilmstripContentTranslationProgress == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onScale(focusX, focusY, scale);
            }
            return false;
        }

        public boolean onDown(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onDown(x, y);
            }
            return false;
        }

        public boolean onUp(float x, float y) {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                return FilmstripLayout.this.mFilmstripGestureListener.onUp(x, y);
            }
            if (FilmstripLayout.this.mSwipeTrend < 0) {
                FilmstripLayout.this.hideFilmstrip();
                FilmstripLayout.this.onSwipeOut();
            } else if (FilmstripLayout.this.mSwipeTrend > 0) {
                FilmstripLayout.this.showFilmstrip();
            } else if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() >= ((float) (FilmstripLayout.this.getMeasuredWidth() / 2))) {
                FilmstripLayout.this.hideFilmstrip();
                FilmstripLayout.this.onSwipeOut();
            } else {
                FilmstripLayout.this.showFilmstrip();
            }
            FilmstripLayout.this.mSwipeTrend = 0;
            return false;
        }

        public void onLongPress(float x, float y) {
            FilmstripLayout.this.mFilmstripGestureListener.onLongPress(x, y);
        }

        public void onScaleEnd() {
            if (FilmstripLayout.this.mFilmstripContentLayout.getTranslationX() == 0.0f) {
                FilmstripLayout.this.mFilmstripGestureListener.onScaleEnd();
            }
        }
    }

    public FilmstripLayout(Context context) {
        super(context);
        init(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FilmstripLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.mGestureRecognizer = new FilmstripGestureRecognizer(context, new MyGestureListener(this, null));
        this.mFilmstripAnimator.setDuration(DEFAULT_DURATION_MS);
        this.mFilmstripAnimator.addUpdateListener(this.mFilmstripAnimatorUpdateListener);
        this.mFilmstripAnimator.addListener(this.mFilmstripAnimatorListener);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mBackgroundDrawable = new MyBackgroundDrawable();
        this.mBackgroundDrawable.setCallback(new Callback() {
            public void invalidateDrawable(Drawable drawable) {
                FilmstripLayout.this.invalidate();
            }

            public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {
                FilmstripLayout.this.mHandler.postAtTime(runnable, drawable, l);
            }

            public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
                FilmstripLayout.this.mHandler.removeCallbacks(runnable, drawable);
            }
        });
        setBackground(this.mBackgroundDrawable);
    }

    public void setFilmstripListener(FilmstripContentPanel.Listener listener) {
        this.mListener = listener;
        if (getVisibility() == 0 && this.mFilmstripContentTranslationProgress == 0.0f) {
            notifyShown();
        } else if (getVisibility() != 0) {
            notifyHidden();
        }
        this.mFilmstripView.getController().setListener(listener);
    }

    public void hide() {
        translateContentLayout(1.0f);
        this.mFilmstripAnimatorListener.onAnimationEnd(this.mFilmstripAnimator);
    }

    public void show() {
        translateContentLayout(0.0f);
        this.mFilmstripAnimatorListener.onAnimationEnd(this.mFilmstripAnimator);
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != 0) {
            notifyHidden();
        }
    }

    private void notifyHidden() {
        if (this.mListener != null) {
            this.mListener.onFilmstripHidden();
        }
    }

    private void notifyShown() {
        if (this.mListener != null) {
            this.mListener.onFilmstripShown();
            this.mFilmstripView.zoomAtIndexChanged();
            FilmstripController controller = this.mFilmstripView.getController();
            int currentId = controller.getCurrentId();
            if (controller.inFilmstrip()) {
                this.mListener.onEnterFilmstrip(currentId);
            } else if (controller.inFullScreen()) {
                this.mListener.onEnterFullScreenUiShown(currentId);
            }
        }
    }

    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && this.mFilmstripView != null && getVisibility() == 4) {
            hide();
        } else {
            translateContentLayout(this.mFilmstripContentTranslationProgress);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mGestureRecognizer.onTouchEvent(ev);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == 0) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return false;
    }

    public void onFinishInflate() {
        this.mFilmstripView = (FilmstripView) findViewById(R.id.filmstrip_view);
        this.mFilmstripView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionEvent) {
                motionEvent.setLocation(motionEvent.getX() + FilmstripLayout.this.mFilmstripContentLayout.getX(), motionEvent.getY() + FilmstripLayout.this.mFilmstripContentLayout.getY());
                FilmstripLayout.this.mGestureRecognizer.onTouchEvent(motionEvent);
                return true;
            }
        });
        this.mFilmstripGestureListener = this.mFilmstripView.getGestureListener();
        this.mFilmstripContentLayout = (FrameLayout) findViewById(R.id.camera_filmstrip_content_layout);
    }

    public boolean onBackPressed() {
        return animateHide();
    }

    public boolean animateHide() {
        if (getVisibility() != 0) {
            return false;
        }
        if (!this.mFilmstripAnimator.isRunning()) {
            hideFilmstrip();
        }
        return true;
    }

    public void hideFilmstrip() {
        onSwipeOutBegin();
        runAnimation(this.mFilmstripContentTranslationProgress, 1.0f);
    }

    public void showFilmstrip() {
        setVisibility(0);
        runAnimation(this.mFilmstripContentTranslationProgress, 0.0f);
    }

    private void runAnimation(float begin, float end) {
        if (!this.mFilmstripAnimator.isRunning()) {
            if (begin == end) {
                this.mFilmstripAnimatorListener.onAnimationEnd(this.mFilmstripAnimator);
                return;
            }
            this.mFilmstripAnimator.setFloatValues(new float[]{begin, end});
            this.mFilmstripAnimator.start();
        }
    }

    private void translateContentLayout(float fraction) {
        this.mFilmstripContentTranslationProgress = fraction;
        this.mFilmstripContentLayout.setTranslationX(((float) getMeasuredWidth()) * fraction);
    }

    private void translateContentLayoutByPixel(float pixel) {
        this.mFilmstripContentLayout.setTranslationX(pixel);
        this.mFilmstripContentTranslationProgress = pixel / ((float) getMeasuredWidth());
    }

    private void onSwipeOut() {
        if (this.mListener != null) {
            this.mListener.onSwipeOut();
        }
    }

    private void onSwipeOutBegin() {
        if (this.mListener != null) {
            this.mListener.onSwipeOutBegin();
        }
    }
}
