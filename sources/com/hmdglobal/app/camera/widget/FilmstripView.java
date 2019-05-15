package com.hmdglobal.app.camera.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;
import com.google.common.primitives.Ints;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.filmstrip.DataAdapter;
import com.hmdglobal.app.camera.filmstrip.DataAdapter.UpdateReporter;
import com.hmdglobal.app.camera.filmstrip.FilmstripController;
import com.hmdglobal.app.camera.filmstrip.FilmstripController.FilmstripListener;
import com.hmdglobal.app.camera.filmstrip.ImageData;
import com.hmdglobal.app.camera.ui.FilmstripGestureRecognizer;
import com.hmdglobal.app.camera.ui.FilmstripGestureRecognizer.Listener;
import com.hmdglobal.app.camera.ui.ZoomView;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class FilmstripView extends ViewGroup {
    private static final int BUFFER_SIZE = 5;
    private static final int CAMERA_PREVIEW_SWIPE_THRESHOLD = 300;
    private static final int DECELERATION_FACTOR = 4;
    private static final float FILM_STRIP_SCALE = 0.7f;
    private static final float FLING_COASTING_DURATION_S = 0.05f;
    private static final float FULL_SCREEN_SCALE = 1.0f;
    private static final int GEOMETRY_ADJUST_TIME_MS = 400;
    private static final float PROMOTE_HEIGHT_RATIO = 0.5f;
    private static final float PROMOTE_VELOCITY = 3.5f;
    private static final int SNAP_IN_CENTER_TIME_MS = 600;
    private static final int SWIPE_TIME_OUT = 500;
    private static final Tag TAG = new Tag("FilmstripView");
    private static final float TOLERANCE = 0.1f;
    private static final float VELOCITY_PROMOTE_HEIGHT_RATIO = 0.1f;
    private static final int ZOOM_ANIMATION_DURATION_MS = 200;
    private ActionCallback mActionCallback;
    private CameraActivity mActivity;
    private int mCenterX;
    private boolean mCheckToIntercept;
    private MyController mController;
    private final int mCurrentItem;
    private DataAdapter mDataAdapter;
    private int mDataIdOnUserScrolling;
    private MotionEvent mDown;
    private final Rect mDrawArea;
    private boolean mFullScreenUIHidden;
    private Listener mGestureListener;
    private FilmstripGestureRecognizer mGestureRecognizer;
    private boolean mIsUserScrolling;
    private FilmstripListener mListener;
    private float mOverScaleFactor;
    private float mScale;
    private int mSlop;
    private TimeInterpolator mViewAnimInterpolator;
    private int mViewGapInPixel;
    private final ViewItem[] mViewItem;
    private ZoomView mZoomView;
    private final SparseArray<Queue<View>> recycledViews;

    private static class MyScroller {
        private final Handler mHandler;
        private final Listener mListener;
        private final Runnable mScrollChecker = new Runnable() {
            public void run() {
                if (MyScroller.this.mScroller.computeScrollOffset()) {
                    MyScroller.this.mListener.onScrollUpdate(MyScroller.this.mScroller.getCurrX(), MyScroller.this.mScroller.getCurrY());
                    MyScroller.this.mHandler.removeCallbacks(this);
                    MyScroller.this.mHandler.post(this);
                    return;
                }
                MyScroller.this.mListener.onScrollEnd();
            }
        };
        private final Scroller mScroller;
        private final ValueAnimator mXScrollAnimator;
        private final AnimatorListener mXScrollAnimatorListener = new AnimatorListener() {
            public void onAnimationCancel(Animator animation) {
            }

            public void onAnimationEnd(Animator animation) {
                MyScroller.this.mListener.onScrollEnd();
            }

            public void onAnimationRepeat(Animator animation) {
            }

            public void onAnimationStart(Animator animation) {
            }
        };
        private final AnimatorUpdateListener mXScrollAnimatorUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MyScroller.this.mListener.onScrollUpdate(((Integer) animation.getAnimatedValue()).intValue(), 0);
            }
        };

        public interface Listener {
            void onScrollEnd();

            void onScrollUpdate(int i, int i2);
        }

        public MyScroller(Context ctx, Handler handler, Listener listener, TimeInterpolator interpolator) {
            this.mHandler = handler;
            this.mListener = listener;
            this.mScroller = new Scroller(ctx);
            this.mXScrollAnimator = new ValueAnimator();
            this.mXScrollAnimator.addUpdateListener(this.mXScrollAnimatorUpdateListener);
            this.mXScrollAnimator.addListener(this.mXScrollAnimatorListener);
            this.mXScrollAnimator.setInterpolator(interpolator);
        }

        public void fling(int startX, int startY, int velocityX, int velocityY, int minX, int maxX, int minY, int maxY) {
            this.mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
            runChecker();
        }

        public void startScroll(int startX, int startY, int dx, int dy) {
            this.mScroller.startScroll(startX, startY, dx, dy);
            runChecker();
        }

        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            this.mXScrollAnimator.cancel();
            this.mXScrollAnimator.setDuration((long) duration);
            this.mXScrollAnimator.setIntValues(new int[]{startX, startX + dx});
            this.mXScrollAnimator.start();
        }

        public boolean isFinished() {
            return this.mScroller.isFinished() && !this.mXScrollAnimator.isRunning();
        }

        public void forceFinished(boolean finished) {
            this.mScroller.forceFinished(finished);
            if (finished) {
                this.mXScrollAnimator.cancel();
            }
        }

        private void runChecker() {
            if (this.mHandler != null && this.mListener != null) {
                this.mHandler.removeCallbacks(this.mScrollChecker);
                this.mHandler.post(this.mScrollChecker);
            }
        }
    }

    private class ViewItem {
        private ValueAnimator mAlphaAnimator;
        private final ImageData mData;
        private int mDataId;
        private int mLeftPosition = -1;
        private boolean mMaximumBitmapRequested = false;
        private ValueAnimator mTranslationXAnimator;
        private ValueAnimator mTranslationYAnimator;
        private final View mView;
        private final RectF mViewArea = new RectF();

        public ViewItem(int id, View v, ImageData data) {
            v.setPivotX(0.0f);
            v.setPivotY(0.0f);
            this.mDataId = id;
            this.mData = data;
            this.mView = v;
        }

        public boolean isMaximumBitmapRequested() {
            return this.mMaximumBitmapRequested;
        }

        public void setMaximumBitmapRequested() {
            this.mMaximumBitmapRequested = true;
        }

        public int getId() {
            return this.mDataId;
        }

        public void setId(int id) {
            this.mDataId = id;
        }

        public void setLeftPosition(int pos) {
            this.mLeftPosition = pos;
        }

        public int getLeftPosition() {
            return this.mLeftPosition;
        }

        public float getTranslationY() {
            return this.mView.getTranslationY() / FilmstripView.this.mScale;
        }

        public float getTranslationX() {
            return this.mView.getTranslationX() / FilmstripView.this.mScale;
        }

        public void setTranslationY(float transY) {
            this.mView.setTranslationY(FilmstripView.this.mScale * transY);
        }

        public void setTranslationX(float transX) {
            this.mView.setTranslationX(FilmstripView.this.mScale * transX);
        }

        public void setAlpha(float alpha) {
            this.mView.setAlpha(alpha);
        }

        public float getAlpha() {
            return this.mView.getAlpha();
        }

        public int getMeasuredWidth() {
            return this.mView.getMeasuredWidth();
        }

        public void animateTranslationX(float targetX, long duration_ms, TimeInterpolator interpolator) {
            if (this.mTranslationXAnimator == null) {
                this.mTranslationXAnimator = new ValueAnimator();
                this.mTranslationXAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        FilmstripView.this.invalidate();
                    }
                });
            }
            runAnimation(this.mTranslationXAnimator, getTranslationX(), targetX, duration_ms, interpolator);
        }

        public void animateTranslationY(float targetY, long duration_ms, TimeInterpolator interpolator) {
            if (this.mTranslationYAnimator == null) {
                this.mTranslationYAnimator = new ValueAnimator();
                this.mTranslationYAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            runAnimation(this.mTranslationYAnimator, getTranslationY(), targetY, duration_ms, interpolator);
        }

        public void animateAlpha(float targetAlpha, long duration_ms, TimeInterpolator interpolator) {
            if (this.mAlphaAnimator == null) {
                this.mAlphaAnimator = new ValueAnimator();
                this.mAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            runAnimation(this.mAlphaAnimator, getAlpha(), targetAlpha, duration_ms, interpolator);
        }

        private void runAnimation(ValueAnimator animator, float startValue, float targetValue, long duration_ms, TimeInterpolator interpolator) {
            if (startValue != targetValue) {
                animator.setInterpolator(interpolator);
                animator.setDuration(duration_ms);
                animator.setFloatValues(new float[]{startValue, targetValue});
                animator.start();
            }
        }

        public void translateXScaledBy(float transX) {
            setTranslationX(getTranslationX() + (FilmstripView.this.mScale * transX));
        }

        public void getHitRect(Rect rect) {
            this.mView.getHitRect(rect);
        }

        public int getCenterX() {
            return this.mLeftPosition + (this.mView.getMeasuredWidth() / 2);
        }

        public int getVisibility() {
            return this.mView.getVisibility();
        }

        public void setVisibility(int visibility) {
            this.mView.setVisibility(visibility);
        }

        public void resizeView(Context context, int w, int h) {
            FilmstripView.this.mDataAdapter.resizeView(context, this.mDataId, this.mView, w, h);
        }

        public void addViewToHierarchy() {
            if (FilmstripView.this.indexOfChild(this.mView) < 0) {
                this.mData.prepare();
                FilmstripView.this.addView(this.mView);
            }
            setVisibility(0);
            setAlpha(1.0f);
            setTranslationX(0.0f);
            setTranslationY(0.0f);
        }

        public void removeViewFromHierarchy(boolean force) {
            if (force || this.mData.getViewType() != 1) {
                FilmstripView.this.removeView(this.mView);
                this.mData.recycle(this.mView);
                FilmstripView.this.recycleView(this.mView, this.mDataId);
                return;
            }
            setVisibility(4);
        }

        public void bringViewToFront() {
            FilmstripView.this.bringChildToFront(this.mView);
        }

        public float getX() {
            return this.mView.getX();
        }

        public float getY() {
            return this.mView.getY();
        }

        public void measure(int widthSpec, int heightSpec) {
            this.mView.measure(widthSpec, heightSpec);
        }

        private void layoutAt(int left, int top) {
            this.mView.layout(left, top, this.mView.getMeasuredWidth() + left, this.mView.getMeasuredHeight() + top);
        }

        public RectF getViewRect() {
            RectF r = new RectF();
            r.left = this.mView.getX();
            r.top = this.mView.getY();
            r.right = r.left + (((float) this.mView.getWidth()) * this.mView.getScaleX());
            r.bottom = r.top + (((float) this.mView.getHeight()) * this.mView.getScaleY());
            return r;
        }

        private View getView() {
            return this.mView;
        }

        public void layoutWithTranslationX(Rect drawArea, int refCenter, float scale) {
            float translationX = (this.mTranslationXAnimator == null || !this.mTranslationXAnimator.isRunning()) ? 0.0f : ((Float) this.mTranslationXAnimator.getAnimatedValue()).floatValue();
            layoutAt((int) (((float) drawArea.centerX()) + ((((float) (this.mLeftPosition - refCenter)) + translationX) * scale)), (int) (((float) drawArea.centerY()) - (((float) (this.mView.getMeasuredHeight() / 2)) * scale)));
            this.mView.setScaleX(scale);
            this.mView.setScaleY(scale);
            int l = this.mView.getLeft();
            int t = this.mView.getTop();
            this.mViewArea.set((float) l, (float) t, ((float) l) + (((float) this.mView.getMeasuredWidth()) * scale), ((float) t) + (((float) this.mView.getMeasuredHeight()) * scale));
        }

        public boolean areaContains(float x, float y) {
            return this.mViewArea.contains(x, y);
        }

        public int getWidth() {
            return this.mView.getWidth();
        }

        public int getDrawAreaLeft() {
            return Math.round(this.mViewArea.left);
        }

        public void copyAttributes(ViewItem item) {
            setLeftPosition(item.getLeftPosition());
            setTranslationX(item.getTranslationX());
            if (item.mTranslationXAnimator != null) {
                this.mTranslationXAnimator = item.mTranslationXAnimator;
                this.mTranslationXAnimator.removeAllUpdateListeners();
                this.mTranslationXAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        FilmstripView.this.invalidate();
                    }
                });
            }
            setTranslationY(item.getTranslationY());
            if (item.mTranslationYAnimator != null) {
                this.mTranslationYAnimator = item.mTranslationYAnimator;
                this.mTranslationYAnimator.removeAllUpdateListeners();
                this.mTranslationYAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
            setAlpha(item.getAlpha());
            if (item.mAlphaAnimator != null) {
                this.mAlphaAnimator = item.mAlphaAnimator;
                this.mAlphaAnimator.removeAllUpdateListeners();
                this.mAlphaAnimator.addUpdateListener(new AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        ViewItem.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                    }
                });
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void postScale(float focusX, float focusY, float postScale, int viewportWidth, int viewportHeight) {
            float transX = this.mView.getTranslationX();
            updateTransform(transX - ((focusX - getX()) * (postScale - 1.0f)), this.mView.getTranslationY() - ((focusY - getY()) * (postScale - 1.0f)), this.mView.getScaleX() * postScale, this.mView.getScaleY() * postScale, viewportWidth, viewportHeight);
        }

        /* Access modifiers changed, original: 0000 */
        public void updateTransform(float transX, float transY, float scaleX, float scaleY, int viewportWidth, int viewportHeight) {
            float left = ((float) this.mView.getLeft()) + transX;
            float top = ((float) this.mView.getTop()) + transY;
            RectF r = ZoomView.adjustToFitInBounds(new RectF(left, top, (((float) this.mView.getWidth()) * scaleX) + left, (((float) this.mView.getHeight()) * scaleY) + top), viewportWidth, viewportHeight);
            this.mView.setScaleX(scaleX);
            this.mView.setScaleY(scaleY);
            transX = r.top - ((float) this.mView.getTop());
            this.mView.setTranslationX(r.left - ((float) this.mView.getLeft()));
            this.mView.setTranslationY(transX);
        }

        /* Access modifiers changed, original: 0000 */
        public void resetTransform() {
            this.mView.setScaleX(1.0f);
            this.mView.setScaleY(1.0f);
            this.mView.setTranslationX(0.0f);
            this.mView.setTranslationY(0.0f);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DataID = ");
            stringBuilder.append(this.mDataId);
            stringBuilder.append("\n\t left = ");
            stringBuilder.append(this.mLeftPosition);
            stringBuilder.append("\n\t viewArea = ");
            stringBuilder.append(this.mViewArea);
            stringBuilder.append("\n\t centerX = ");
            stringBuilder.append(getCenterX());
            stringBuilder.append("\n\t view MeasuredSize = ");
            stringBuilder.append(this.mView.getMeasuredWidth());
            stringBuilder.append(',');
            stringBuilder.append(this.mView.getMeasuredHeight());
            stringBuilder.append("\n\t view Size = ");
            stringBuilder.append(this.mView.getWidth());
            stringBuilder.append(',');
            stringBuilder.append(this.mView.getHeight());
            stringBuilder.append("\n\t view scale = ");
            stringBuilder.append(this.mView.getScaleX());
            return stringBuilder.toString();
        }
    }

    public static class ActionCallbackImpl implements ActionCallback {
        private final WeakReference<Activity> mActivity;

        public ActionCallbackImpl(Activity activity) {
            this.mActivity = new WeakReference(activity);
        }

        public void playVideo(Uri uri, String title) {
            Activity activity = (Activity) this.mActivity.get();
            if (activity != null) {
                CameraUtil.playVideo(activity, uri, title);
            }
        }
    }

    private class MyController implements FilmstripController {
        private boolean mCanStopScroll;
        private AnimatorSet mFlingAnimator;
        private final ValueAnimator mScaleAnimator;
        private final AnimatorUpdateListener mScaleAnimatorUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                if (FilmstripView.this.mViewItem[2] != null) {
                    FilmstripView.this.mScale = ((Float) animation.getAnimatedValue()).floatValue();
                    FilmstripView.this.invalidate();
                }
            }
        };
        private final MyScroller mScroller;
        private final Listener mScrollerListener = new Listener() {
            public void onScrollUpdate(int currX, int currY) {
                FilmstripView.this.mCenterX = currX;
                if (FilmstripView.this.clampCenterX()) {
                    FilmstripView.this.mController.stopScrolling(true);
                }
                FilmstripView.this.invalidate();
            }

            public void onScrollEnd() {
                MyController.this.mCanStopScroll = true;
                if (FilmstripView.this.mViewItem[2] != null) {
                    FilmstripView.this.snapInCenter();
                    if (FilmstripView.this.isCurrentItemCentered() && FilmstripView.this.isViewTypeSticky(FilmstripView.this.mViewItem[2])) {
                        MyController.this.goToFullScreen();
                    }
                }
            }
        };
        private ValueAnimator mZoomAnimator;

        MyController(Context context) {
            TimeInterpolator decelerateInterpolator = new DecelerateInterpolator(1.5f);
            this.mScroller = new MyScroller(FilmstripView.this.mActivity.getAndroidContext(), new Handler(FilmstripView.this.mActivity.getMainLooper()), this.mScrollerListener, decelerateInterpolator);
            this.mCanStopScroll = true;
            this.mScaleAnimator = new ValueAnimator();
            this.mScaleAnimator.addUpdateListener(this.mScaleAnimatorUpdateListener);
            this.mScaleAnimator.setInterpolator(decelerateInterpolator);
            this.mScaleAnimator.addListener(new AnimatorListener(FilmstripView.this) {
                public void onAnimationStart(Animator animator) {
                    if (FilmstripView.this.mScale == 1.0f) {
                        FilmstripView.this.onLeaveFullScreen();
                    } else if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onLeaveFilmstrip();
                    }
                }

                public void onAnimationEnd(Animator animator) {
                    if (FilmstripView.this.mScale == 1.0f) {
                        FilmstripView.this.onEnterFullScreen();
                    } else if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onEnterFilmstrip();
                    }
                    FilmstripView.this.zoomAtIndexChanged();
                }

                public void onAnimationCancel(Animator animator) {
                }

                public void onAnimationRepeat(Animator animator) {
                }
            });
        }

        public void setImageGap(int imageGap) {
            FilmstripView.this.setViewGap(imageGap);
        }

        public int getCurrentId() {
            return FilmstripView.this.getCurrentId();
        }

        public void setDataAdapter(DataAdapter adapter) {
            FilmstripView.this.setDataAdapter(adapter);
        }

        public boolean inFilmstrip() {
            return FilmstripView.this.inFilmstrip();
        }

        public boolean inFullScreen() {
            return FilmstripView.this.inFullScreen();
        }

        public boolean isCameraPreview() {
            return FilmstripView.this.isCameraPreview();
        }

        public boolean inCameraFullscreen() {
            return FilmstripView.this.inCameraFullscreen();
        }

        public void setListener(FilmstripListener l) {
            FilmstripView.this.setListener(l);
        }

        public boolean isScrolling() {
            return this.mScroller.isFinished() ^ 1;
        }

        public boolean isScaling() {
            return this.mScaleAnimator.isRunning();
        }

        private int estimateMinX(int dataID, int leftPos, int viewWidth) {
            return leftPos - ((dataID + 100) * (FilmstripView.this.mViewGapInPixel + viewWidth));
        }

        private int estimateMaxX(int dataID, int leftPos, int viewWidth) {
            return (((FilmstripView.this.mDataAdapter.getTotalNumber() - dataID) + 100) * (FilmstripView.this.mViewGapInPixel + viewWidth)) + leftPos;
        }

        private void zoomAt(final ViewItem current, final float focusX, final float focusY) {
            if (this.mZoomAnimator != null) {
                this.mZoomAnimator.end();
            }
            float maxScale = getCurrentDataMaxScale(false);
            float endScale = FilmstripView.this.mScale < maxScale - (0.1f * maxScale) ? maxScale : 1.0f;
            this.mZoomAnimator = new ValueAnimator();
            this.mZoomAnimator.setFloatValues(new float[]{FilmstripView.this.mScale, endScale});
            this.mZoomAnimator.setDuration(200);
            final float f = endScale;
            final ViewItem viewItem = current;
            final float f2 = focusX;
            final float f3 = focusY;
            this.mZoomAnimator.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                    if (FilmstripView.this.mScale == 1.0f) {
                        if (FilmstripView.this.mFullScreenUIHidden) {
                            FilmstripView.this.onLeaveFullScreenUiHidden();
                        } else {
                            FilmstripView.this.onLeaveFullScreen();
                        }
                        MyController.this.setSurroundingViewsVisible(false);
                    } else if (MyController.this.inZoomView()) {
                        FilmstripView.this.onLeaveZoomView();
                    }
                    MyController.this.cancelLoadingZoomedImage();
                }

                public void onAnimationEnd(Animator animation) {
                    if (FilmstripView.this.mScale != f) {
                        viewItem.postScale(f2, f3, f / FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                        FilmstripView.this.mScale = f;
                    }
                    if (MyController.this.inFullScreen()) {
                        MyController.this.setSurroundingViewsVisible(true);
                        FilmstripView.this.mZoomView.setVisibility(8);
                        viewItem.resetTransform();
                        FilmstripView.this.onEnterFullScreenUiHidden();
                    } else {
                        FilmstripView.this.mController.loadZoomedImage();
                        FilmstripView.this.onEnterZoomView();
                    }
                    MyController.this.mZoomAnimator = null;
                    FilmstripView.this.zoomAtIndexChanged();
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            });
            this.mZoomAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    float newScale = ((Float) animation.getAnimatedValue()).floatValue();
                    float postScale = newScale / FilmstripView.this.mScale;
                    FilmstripView.this.mScale = newScale;
                    current.postScale(focusX, focusY, postScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                }
            });
            this.mZoomAnimator.start();
        }

        public void scroll(float deltaX) {
            if (stopScrolling(false)) {
                FilmstripView.access$1916(FilmstripView.this, deltaX);
                if (FilmstripView.this.clampCenterX()) {
                    FilmstripView.this.mController.stopScrolling(true);
                }
                FilmstripView.this.invalidate();
            }
        }

        public void fling(float velocityX) {
            if (stopScrolling(false)) {
                ViewItem item = FilmstripView.this.mViewItem[2];
                if (item != null) {
                    float scaledVelocityX = velocityX / FilmstripView.this.mScale;
                    if (inFullScreen() && FilmstripView.this.isViewTypeSticky(item) && scaledVelocityX < 0.0f) {
                        goToFilmstrip();
                    }
                    int w = FilmstripView.this.getWidth();
                    this.mScroller.fling(FilmstripView.this.mCenterX, 0, (int) (-velocityX), 0, estimateMinX(item.getId(), item.getLeftPosition(), w), estimateMaxX(item.getId(), item.getLeftPosition(), w), 0, 0);
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void flingInsideZoomView(float velocityX, float velocityY) {
            if (inZoomView()) {
                final ViewItem current = FilmstripView.this.mViewItem[2];
                if (current != null) {
                    float translationX = current.getTranslationX() * FilmstripView.this.mScale;
                    float translationY = current.getTranslationY() * FilmstripView.this.mScale;
                    final ValueAnimator decelerationX = ValueAnimator.ofFloat(new float[]{translationX, ((((float) (0.05000000074505806d * Math.pow((double) Math.max(Math.abs(velocityX), Math.abs(velocityY)), 0.3333333432674408d))) / 4.0f) * velocityX) + translationX});
                    final ValueAnimator decelerationY = ValueAnimator.ofFloat(new float[]{translationY, ((duration / 4.0f) * velocityY) + translationY});
                    decelerationY.addUpdateListener(new AnimatorUpdateListener() {
                        public void onAnimationUpdate(ValueAnimator animation) {
                            float transX = ((Float) decelerationX.getAnimatedValue()).floatValue();
                            float transY = ((Float) decelerationY.getAnimatedValue()).floatValue();
                            current.updateTransform(transX, transY, FilmstripView.this.mScale, FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                        }
                    });
                    this.mFlingAnimator = new AnimatorSet();
                    this.mFlingAnimator.play(decelerationX).with(decelerationY);
                    this.mFlingAnimator.setDuration((long) ((int) (1000.0f * duration)));
                    this.mFlingAnimator.setInterpolator(new TimeInterpolator() {
                        public float getInterpolation(float input) {
                            return (float) (1.0d - Math.pow((double) (1.0f - input), 4.0d));
                        }
                    });
                    this.mFlingAnimator.addListener(new AnimatorListener() {
                        private boolean mCancelled = false;

                        public void onAnimationStart(Animator animation) {
                        }

                        public void onAnimationEnd(Animator animation) {
                            if (!this.mCancelled) {
                                MyController.this.loadZoomedImage();
                            }
                            MyController.this.mFlingAnimator = null;
                        }

                        public void onAnimationCancel(Animator animation) {
                            this.mCancelled = true;
                        }

                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    this.mFlingAnimator.start();
                }
            }
        }

        public boolean stopScrolling(boolean forced) {
            if (!isScrolling()) {
                return true;
            }
            if (!this.mCanStopScroll && !forced) {
                return false;
            }
            this.mScroller.forceFinished(true);
            return true;
        }

        private void stopScale() {
            this.mScaleAnimator.cancel();
        }

        public void scrollToPosition(int position, int duration, boolean interruptible) {
            if (FilmstripView.this.mViewItem[2] != null) {
                this.mCanStopScroll = interruptible;
                this.mScroller.startScroll(FilmstripView.this.mCenterX, 0, position - FilmstripView.this.mCenterX, 0, duration);
            }
        }

        public boolean goToNextItem() {
            return goToItem(3);
        }

        public boolean goToPreviousItem() {
            return goToItem(1);
        }

        private boolean goToItem(int itemIndex) {
            ViewItem nextItem = FilmstripView.this.mViewItem[itemIndex];
            if (nextItem == null) {
                return false;
            }
            stopScrolling(true);
            scrollToPosition(nextItem.getCenterX(), 800, false);
            if (FilmstripView.this.isViewTypeSticky(FilmstripView.this.mViewItem[2])) {
                scaleTo(FilmstripView.FILM_STRIP_SCALE, 400);
            }
            return true;
        }

        private void scaleTo(float scale, int duration) {
            if (FilmstripView.this.mViewItem[2] != null) {
                stopScale();
                this.mScaleAnimator.setDuration((long) duration);
                this.mScaleAnimator.setFloatValues(new float[]{FilmstripView.this.mScale, scale});
                this.mScaleAnimator.start();
            }
        }

        public void goToFilmstrip() {
            if (FilmstripView.this.mViewItem[2] != null && FilmstripView.this.mScale != FilmstripView.FILM_STRIP_SCALE) {
                scaleTo(FilmstripView.FILM_STRIP_SCALE, 400);
                ViewItem currItem = FilmstripView.this.mViewItem[2];
                ViewItem nextItem = FilmstripView.this.mViewItem[3];
                if (currItem.getId() == 0 && FilmstripView.this.isViewTypeSticky(currItem) && nextItem != null) {
                    scrollToPosition(nextItem.getCenterX(), 400, false);
                }
                if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                    FilmstripView.this.onLeaveFilmstrip();
                }
            }
        }

        public void goToFullScreen() {
            if (!inFullScreen()) {
                scaleTo(1.0f, 400);
            }
        }

        private void cancelFlingAnimation() {
            if (isFlingAnimationRunning()) {
                this.mFlingAnimator.cancel();
            }
        }

        private void cancelZoomAnimation() {
            if (isZoomAnimationRunning()) {
                this.mZoomAnimator.cancel();
            }
        }

        private void setSurroundingViewsVisible(boolean visible) {
            int i = 0;
            while (i < 2) {
                if (!(i == 2 || FilmstripView.this.mViewItem[i] == null)) {
                    FilmstripView.this.mViewItem[i].setVisibility(visible ? 0 : 4);
                }
                i++;
            }
        }

        private Uri getCurrentUri() {
            ViewItem curr = FilmstripView.this.mViewItem[2];
            if (curr == null) {
                return Uri.EMPTY;
            }
            return FilmstripView.this.mDataAdapter.getImageData(curr.getId()).getUri();
        }

        private float getCurrentDataMaxScale(boolean allowOverScale) {
            ViewItem curr = FilmstripView.this.mViewItem[2];
            if (curr == null) {
                return 1.0f;
            }
            ImageData imageData = FilmstripView.this.mDataAdapter.getImageData(curr.getId());
            if (imageData == null || !imageData.isUIActionSupported(4)) {
                return 1.0f;
            }
            float imageWidth = (float) imageData.getWidth();
            if (imageData.getRotation() == 90 || imageData.getRotation() == MediaProviderUtils.ROTATION_270) {
                imageWidth = (float) imageData.getHeight();
            }
            float scale = imageWidth / ((float) curr.getWidth());
            if (allowOverScale) {
                scale *= FilmstripView.this.mOverScaleFactor;
            }
            return scale;
        }

        private void loadZoomedImage() {
            if (inZoomView()) {
                ViewItem curr = FilmstripView.this.mViewItem[2];
                if (curr != null) {
                    ImageData imageData = FilmstripView.this.mDataAdapter.getImageData(curr.getId());
                    if (imageData.isUIActionSupported(4)) {
                        Uri uri = getCurrentUri();
                        RectF viewRect = curr.getViewRect();
                        if (uri != null && uri != Uri.EMPTY) {
                            FilmstripView.this.mZoomView.loadBitmap(uri, imageData.getRotation(), viewRect);
                        }
                    }
                }
            }
        }

        private void cancelLoadingZoomedImage() {
            FilmstripView.this.mZoomView.cancelPartialDecodingTask();
        }

        public void goToFirstItem() {
            if (FilmstripView.this.mViewItem[2] != null) {
                FilmstripView.this.resetZoomView();
                FilmstripView.this.reload();
            }
        }

        public boolean inZoomView() {
            return FilmstripView.this.inZoomView();
        }

        public boolean isFlingAnimationRunning() {
            return this.mFlingAnimator != null && this.mFlingAnimator.isRunning();
        }

        public boolean isZoomAnimationRunning() {
            return this.mZoomAnimator != null && this.mZoomAnimator.isRunning();
        }
    }

    private class MyGestureReceiver implements Listener {
        private static final int SCROLL_DIR_HORIZONTAL = 2;
        private static final int SCROLL_DIR_NONE = 0;
        private static final int SCROLL_DIR_VERTICAL = 1;
        private long mLastDownTime;
        private float mLastDownY;
        private float mMaxScale;
        private float mScaleTrend;
        private int mScrollingDirection;

        private MyGestureReceiver() {
            this.mScrollingDirection = 0;
        }

        /* synthetic */ MyGestureReceiver(FilmstripView x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onSingleTapUp(float x, float y) {
            ViewItem centerItem = FilmstripView.this.mViewItem[2];
            if (FilmstripView.this.inFilmstrip()) {
                if (centerItem != null && centerItem.areaContains(x, y)) {
                    FilmstripView.this.mController.goToFullScreen();
                    return true;
                }
            } else if (FilmstripView.this.inFullScreen()) {
                if (FilmstripView.this.mFullScreenUIHidden) {
                    FilmstripView.this.onLeaveFullScreenUiHidden();
                    FilmstripView.this.onEnterFullScreen();
                } else {
                    FilmstripView.this.onLeaveFullScreen();
                    FilmstripView.this.onEnterFullScreenUiHidden();
                }
                return true;
            }
            return false;
        }

        /* JADX WARNING: Missing block: B:22:0x006d, code skipped:
            return false;
     */
        public boolean onDoubleTap(float r6, float r7) {
            /*
            r5 = this;
            r0 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r0 = r0.mViewItem;
            r1 = 2;
            r0 = r0[r1];
            r1 = 0;
            if (r0 != 0) goto L_0x000d;
        L_0x000c:
            return r1;
        L_0x000d:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.inFilmstrip();
            r3 = 1;
            if (r2 == 0) goto L_0x0020;
        L_0x0016:
            r1 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r1 = r1.mController;
            r1.goToFullScreen();
            return r3;
        L_0x0020:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.mScale;
            r4 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
            r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
            if (r2 < 0) goto L_0x006d;
        L_0x002c:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.inCameraFullscreen();
            if (r2 == 0) goto L_0x0035;
        L_0x0034:
            goto L_0x006d;
        L_0x0035:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.mController;
            r2 = r2.stopScrolling(r1);
            if (r2 != 0) goto L_0x0042;
        L_0x0041:
            return r1;
        L_0x0042:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.inFullScreen();
            if (r2 == 0) goto L_0x0059;
        L_0x004a:
            r1 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r1 = r1.mController;
            r1.zoomAt(r0, r6, r7);
            r1 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r1.checkItemAtMaxSize();
            return r3;
        L_0x0059:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.mScale;
            r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
            if (r2 <= 0) goto L_0x006c;
        L_0x0063:
            r2 = com.hmdglobal.app.camera.widget.FilmstripView.this;
            r2 = r2.mController;
            r2.zoomAt(r0, r6, r7);
        L_0x006c:
            return r1;
        L_0x006d:
            return r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.widget.FilmstripView$MyGestureReceiver.onDoubleTap(float, float):boolean");
        }

        public boolean onDown(float x, float y) {
            this.mLastDownTime = SystemClock.uptimeMillis();
            this.mLastDownY = y;
            FilmstripView.this.mController.cancelFlingAnimation();
            if (FilmstripView.this.mController.stopScrolling(false)) {
                return true;
            }
            return false;
        }

        public boolean onUp(float x, float y) {
            if (FilmstripView.this.mViewItem[2] == null || FilmstripView.this.mController.isZoomAnimationRunning() || FilmstripView.this.mController.isFlingAnimationRunning()) {
                return false;
            }
            if (FilmstripView.this.inZoomView()) {
                FilmstripView.this.mController.loadZoomedImage();
                return true;
            }
            float promoteHeight = ((float) FilmstripView.this.getHeight()) * FilmstripView.PROMOTE_HEIGHT_RATIO;
            float velocityPromoteHeight = ((float) FilmstripView.this.getHeight()) * 0.1f;
            FilmstripView.this.mIsUserScrolling = false;
            this.mScrollingDirection = 0;
            float speedY = Math.abs(y - this.mLastDownY) / ((float) (SystemClock.uptimeMillis() - this.mLastDownTime));
            for (int i = 0; i < 5; i++) {
                if (FilmstripView.this.mViewItem[i] != null) {
                    float transY = FilmstripView.this.mViewItem[i].getTranslationY();
                    if (transY != 0.0f) {
                        int id = FilmstripView.this.mViewItem[i].getId();
                        if (FilmstripView.this.mDataAdapter.getImageData(id).isUIActionSupported(2) && (transY > promoteHeight || (transY > velocityPromoteHeight && speedY > FilmstripView.PROMOTE_VELOCITY))) {
                            FilmstripView.this.demoteData(i, id);
                        } else if (!FilmstripView.this.mDataAdapter.getImageData(id).isUIActionSupported(1) || (transY >= (-promoteHeight) && (transY >= (-velocityPromoteHeight) || speedY <= FilmstripView.PROMOTE_VELOCITY))) {
                            FilmstripView.this.slideViewBack(FilmstripView.this.mViewItem[i]);
                        } else {
                            FilmstripView.this.promoteData(i, id);
                        }
                    }
                }
            }
            ViewItem currItem = FilmstripView.this.mViewItem[2];
            if (currItem == null) {
                return true;
            }
            int currId = currItem.getId();
            if (FilmstripView.this.mCenterX > currItem.getCenterX() + 300 && currId == 0 && FilmstripView.this.isViewTypeSticky(currItem) && FilmstripView.this.mDataIdOnUserScrolling == 0) {
                FilmstripView.this.mController.goToFilmstrip();
                if (FilmstripView.this.mViewItem[3] != null) {
                    FilmstripView.this.mController.scrollToPosition(FilmstripView.this.mViewItem[3].getCenterX(), 400, false);
                } else {
                    FilmstripView.this.snapInCenter();
                }
            }
            if (FilmstripView.this.isCurrentItemCentered() && currId == 0 && FilmstripView.this.isViewTypeSticky(currItem)) {
                FilmstripView.this.mController.goToFullScreen();
            } else {
                if (FilmstripView.this.mDataIdOnUserScrolling == 0 && currId != 0) {
                    FilmstripView.this.mController.goToFilmstrip();
                    FilmstripView.this.mDataIdOnUserScrolling = currId;
                }
                FilmstripView.this.snapInCenter();
            }
            return false;
        }

        public void onLongPress(float x, float y) {
            int dataId = FilmstripView.this.getCurrentId();
            if (dataId != -1) {
                FilmstripView.this.mListener.onFocusedDataLongPressed(dataId);
            }
        }

        public boolean onScroll(float x, float y, float dx, float dy) {
            ViewItem currItem = FilmstripView.this.mViewItem[2];
            if (currItem == null) {
                return false;
            }
            if (FilmstripView.this.inFullScreen() && !FilmstripView.this.mDataAdapter.canSwipeInFullScreen(currItem.getId())) {
                return false;
            }
            FilmstripView.this.hideZoomView();
            if (FilmstripView.this.inZoomView()) {
                ViewItem curr = FilmstripView.this.mViewItem[2];
                curr.updateTransform((curr.getTranslationX() * FilmstripView.this.mScale) - dx, (curr.getTranslationY() * FilmstripView.this.mScale) - dy, FilmstripView.this.mScale, FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                return true;
            }
            int deltaX = (int) (dx / FilmstripView.this.mScale);
            FilmstripView.this.mController.stopScrolling(true);
            if (!FilmstripView.this.mIsUserScrolling) {
                FilmstripView.this.mIsUserScrolling = true;
                FilmstripView.this.mDataIdOnUserScrolling = FilmstripView.this.mViewItem[2].getId();
            }
            float f;
            float f2;
            if (FilmstripView.this.inFilmstrip()) {
                if (this.mScrollingDirection == 0) {
                    this.mScrollingDirection = Math.abs(dx) > Math.abs(dy) ? 2 : 1;
                }
                if (this.mScrollingDirection != 2) {
                    int hit = 0;
                    Rect hitRect = new Rect();
                    while (hit < 5) {
                        if (FilmstripView.this.mViewItem[hit] == null) {
                            f = x;
                            f2 = y;
                        } else {
                            FilmstripView.this.mViewItem[hit].getHitRect(hitRect);
                            if (hitRect.contains((int) x, (int) y)) {
                                break;
                            }
                        }
                        hit++;
                    }
                    f = x;
                    f2 = y;
                    if (hit == 5) {
                        return true;
                    }
                    ImageData data = FilmstripView.this.mDataAdapter.getImageData(FilmstripView.this.mViewItem[hit].getId());
                    float transY = FilmstripView.this.mViewItem[hit].getTranslationY() - (dy / FilmstripView.this.mScale);
                    if (!data.isUIActionSupported(2) && transY > 0.0f) {
                        transY = 0.0f;
                    }
                    if (!data.isUIActionSupported(1) && transY < 0.0f) {
                        transY = 0.0f;
                    }
                    FilmstripView.this.mViewItem[hit].setTranslationY(transY);
                } else if (FilmstripView.this.mCenterX == currItem.getCenterX() && currItem.getId() == 0 && dx < 0.0f) {
                    FilmstripView.this.mIsUserScrolling = false;
                    this.mScrollingDirection = 0;
                    return false;
                } else {
                    FilmstripView.this.mController.scroll((float) deltaX);
                    f = x;
                    f2 = y;
                }
            } else {
                f = x;
                f2 = y;
                if (FilmstripView.this.inFullScreen()) {
                    if (FilmstripView.this.mViewItem[2] == null || (deltaX < 0 && FilmstripView.this.mCenterX <= currItem.getCenterX() && currItem.getId() == 0)) {
                        return false;
                    }
                    FilmstripView.this.mController.scroll((float) ((int) (((double) deltaX) * 1.2d)));
                }
            }
            FilmstripView.this.invalidate();
            return true;
        }

        public boolean onFling(float velocityX, float velocityY) {
            ViewItem currItem = FilmstripView.this.mViewItem[2];
            if (currItem == null || !FilmstripView.this.mDataAdapter.canSwipeInFullScreen(currItem.getId())) {
                return false;
            }
            if (FilmstripView.this.inZoomView()) {
                FilmstripView.this.mController.flingInsideZoomView(velocityX, velocityY);
                return true;
            } else if (Math.abs(velocityX) < Math.abs(velocityY)) {
                return true;
            } else {
                if (FilmstripView.this.mScale == 1.0f) {
                    int currItemCenterX = currItem.getCenterX();
                    ViewItem prevItem;
                    if (velocityX > 0.0f) {
                        if (FilmstripView.this.mCenterX > currItemCenterX) {
                            FilmstripView.this.mController.scrollToPosition(currItemCenterX, 400, true);
                            return true;
                        }
                        prevItem = FilmstripView.this.mViewItem[1];
                        if (prevItem == null) {
                            return false;
                        }
                        FilmstripView.this.mController.scrollToPosition(prevItem.getCenterX(), 400, true);
                    } else if (FilmstripView.this.mController.stopScrolling(false)) {
                        if (FilmstripView.this.mCenterX < currItemCenterX) {
                            FilmstripView.this.mController.scrollToPosition(currItemCenterX, 400, true);
                            return true;
                        }
                        prevItem = FilmstripView.this.mViewItem[3];
                        if (prevItem == null) {
                            return false;
                        }
                        FilmstripView.this.mController.scrollToPosition(prevItem.getCenterX(), 400, true);
                        if (FilmstripView.this.isViewTypeSticky(currItem)) {
                            FilmstripView.this.mController.goToFilmstrip();
                        }
                    }
                }
                if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                    FilmstripView.this.mController.fling(velocityX);
                }
                return true;
            }
        }

        public boolean onScaleBegin(float focusX, float focusY) {
            if (FilmstripView.this.inCameraFullscreen()) {
                return false;
            }
            FilmstripView.this.hideZoomView();
            this.mScaleTrend = 1.0f;
            this.mMaxScale = Math.max(FilmstripView.this.mController.getCurrentDataMaxScale(true), 1.0f);
            return true;
        }

        public boolean onScale(float focusX, float focusY, float scale) {
            if (FilmstripView.this.inCameraFullscreen()) {
                return false;
            }
            this.mScaleTrend = (this.mScaleTrend * 0.3f) + (scale * FilmstripView.FILM_STRIP_SCALE);
            float newScale = FilmstripView.this.mScale * scale;
            if (FilmstripView.this.mScale < 1.0f && newScale < 1.0f) {
                if (newScale <= FilmstripView.FILM_STRIP_SCALE) {
                    newScale = FilmstripView.FILM_STRIP_SCALE;
                }
                if (FilmstripView.this.mScale != newScale) {
                    if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onLeaveFilmstrip();
                    }
                    if (newScale == FilmstripView.FILM_STRIP_SCALE) {
                        FilmstripView.this.onEnterFilmstrip();
                    }
                }
                FilmstripView.this.mScale = newScale;
                FilmstripView.this.invalidate();
            } else if (FilmstripView.this.mScale < 1.0f && newScale >= 1.0f) {
                if (FilmstripView.this.mScale == FilmstripView.FILM_STRIP_SCALE) {
                    FilmstripView.this.onLeaveFilmstrip();
                }
                FilmstripView.this.mScale = 1.0f;
                FilmstripView.this.onEnterFullScreen();
                FilmstripView.this.mController.setSurroundingViewsVisible(false);
                FilmstripView.this.invalidate();
            } else if (FilmstripView.this.mScale < 1.0f || newScale >= 1.0f) {
                if (!FilmstripView.this.inZoomView()) {
                    FilmstripView.this.mController.setSurroundingViewsVisible(false);
                }
                ViewItem curr = FilmstripView.this.mViewItem[2];
                newScale = Math.min(newScale, this.mMaxScale);
                if (newScale == FilmstripView.this.mScale) {
                    return true;
                }
                curr.postScale(focusX, focusY, newScale / FilmstripView.this.mScale, FilmstripView.this.mDrawArea.width(), FilmstripView.this.mDrawArea.height());
                FilmstripView.this.mScale = newScale;
                if (FilmstripView.this.mScale == 1.0f) {
                    FilmstripView.this.onEnterFullScreen();
                } else {
                    FilmstripView.this.onEnterZoomView();
                }
                FilmstripView.this.checkItemAtMaxSize();
            } else {
                if (!FilmstripView.this.inFullScreen()) {
                    FilmstripView.this.onLeaveZoomView();
                } else if (FilmstripView.this.mFullScreenUIHidden) {
                    FilmstripView.this.onLeaveFullScreenUiHidden();
                } else {
                    FilmstripView.this.onLeaveFullScreen();
                }
                FilmstripView.this.mScale = newScale;
                FilmstripView.this.onEnterFilmstrip();
                FilmstripView.this.invalidate();
            }
            return true;
        }

        public void onScaleEnd() {
            FilmstripView.this.zoomAtIndexChanged();
            if (FilmstripView.this.mScale <= 1.1f) {
                FilmstripView.this.mController.setSurroundingViewsVisible(true);
                if (FilmstripView.this.mScale <= 0.8f) {
                    FilmstripView.this.mController.goToFilmstrip();
                } else if (this.mScaleTrend > 1.0f || FilmstripView.this.mScale > 0.9f) {
                    if (FilmstripView.this.inZoomView()) {
                        FilmstripView.this.mScale = 1.0f;
                        FilmstripView.this.resetZoomView();
                    }
                    FilmstripView.this.mController.goToFullScreen();
                } else {
                    FilmstripView.this.mController.goToFilmstrip();
                }
                this.mScaleTrend = 1.0f;
            }
        }
    }

    static /* synthetic */ int access$1916(FilmstripView x0, float x1) {
        int i = (int) (((float) x0.mCenterX) + x1);
        x0.mCenterX = i;
        return i;
    }

    public FilmstripView(Context context) {
        super(context);
        this.mDrawArea = new Rect();
        this.mCurrentItem = 2;
        this.mCenterX = -1;
        this.mViewItem = new ViewItem[5];
        this.mZoomView = null;
        this.mCheckToIntercept = true;
        this.mOverScaleFactor = 1.0f;
        this.mFullScreenUIHidden = false;
        this.recycledViews = new SparseArray();
        init((CameraActivity) context);
    }

    public FilmstripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDrawArea = new Rect();
        this.mCurrentItem = 2;
        this.mCenterX = -1;
        this.mViewItem = new ViewItem[5];
        this.mZoomView = null;
        this.mCheckToIntercept = true;
        this.mOverScaleFactor = 1.0f;
        this.mFullScreenUIHidden = false;
        this.recycledViews = new SparseArray();
        init((CameraActivity) context);
    }

    public FilmstripView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDrawArea = new Rect();
        this.mCurrentItem = 2;
        this.mCenterX = -1;
        this.mViewItem = new ViewItem[5];
        this.mZoomView = null;
        this.mCheckToIntercept = true;
        this.mOverScaleFactor = 1.0f;
        this.mFullScreenUIHidden = false;
        this.recycledViews = new SparseArray();
        init((CameraActivity) context);
    }

    private void init(CameraActivity cameraActivity) {
        setWillNotDraw(false);
        this.mActivity = cameraActivity;
        this.mActionCallback = new ActionCallbackImpl(this.mActivity);
        this.mScale = 1.0f;
        this.mDataIdOnUserScrolling = 0;
        this.mController = new MyController(cameraActivity);
        this.mViewAnimInterpolator = new DecelerateInterpolator();
        this.mZoomView = new ZoomView(cameraActivity);
        this.mZoomView.setVisibility(8);
        addView(this.mZoomView);
        this.mGestureListener = new MyGestureReceiver(this, null);
        this.mGestureRecognizer = new FilmstripGestureRecognizer(cameraActivity, this.mGestureListener);
        this.mSlop = (int) getContext().getResources().getDimension(R.dimen.pie_touch_slop);
        DisplayMetrics metrics = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.mOverScaleFactor = ((float) metrics.densityDpi) / 240.0f;
        if (this.mOverScaleFactor < 1.0f) {
            this.mOverScaleFactor = 1.0f;
        }
        setAccessibilityDelegate(new AccessibilityDelegate() {
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(FilmstripView.class.getName());
                info.setScrollable(true);
                info.addAction(4096);
                info.addAction(8192);
            }

            public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (!FilmstripView.this.mController.isScrolling()) {
                    if (action == 64) {
                        FilmstripView.this.mViewItem[2].getView().performAccessibilityAction(action, args);
                        return true;
                    } else if (action == 4096) {
                        FilmstripView.this.mController.goToNextItem();
                        return true;
                    } else if (action == 8192) {
                        if (!FilmstripView.this.mController.goToPreviousItem()) {
                            FilmstripView.this.mActivity.getCameraAppUI().hideFilmstrip();
                        }
                        return true;
                    }
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    private void recycleView(View view, int dataId) {
        int viewType = ((Integer) view.getTag(R.id.mediadata_tag_viewtype)).intValue();
        if (viewType > 0) {
            Queue<View> recycledViewsForType = (Queue) this.recycledViews.get(viewType);
            if (recycledViewsForType == null) {
                recycledViewsForType = new ArrayDeque();
                this.recycledViews.put(viewType, recycledViewsForType);
            }
            recycledViewsForType.offer(view);
        }
    }

    private View getRecycledView(int dataId) {
        Queue<View> recycledViewsForType = (Queue) this.recycledViews.get(this.mDataAdapter.getItemViewType(dataId));
        if (recycledViewsForType != null) {
            return (View) recycledViewsForType.poll();
        }
        return null;
    }

    public FilmstripController getController() {
        return this.mController;
    }

    public int getCurrentItemLeft() {
        return this.mViewItem[2].getDrawAreaLeft();
    }

    private void setListener(FilmstripListener l) {
        this.mListener = l;
    }

    private void setViewGap(int viewGap) {
        this.mViewGapInPixel = viewGap;
    }

    public void zoomAtIndexChanged() {
        if (this.mViewItem[2] != null) {
            this.mListener.onZoomAtIndexChanged(this.mViewItem[2].getId(), this.mScale);
        }
    }

    private boolean isDataAtCenter(int id) {
        if (this.mViewItem[2] != null && this.mViewItem[2].getId() == id && isCurrentItemCentered()) {
            return true;
        }
        return false;
    }

    private void measureViewItem(ViewItem item, int boundWidth, int boundHeight) {
        ImageData imageData = this.mDataAdapter.getImageData(item.getId());
        if (imageData == null) {
            Log.e(TAG, "trying to measure a null item");
            return;
        }
        Point dim = CameraUtil.resizeToFill(imageData.getWidth(), imageData.getHeight(), imageData.getRotation(), boundWidth, boundHeight);
        item.measure(MeasureSpec.makeMeasureSpec(dim.x, Ints.MAX_POWER_OF_TWO), MeasureSpec.makeMeasureSpec(dim.y, Ints.MAX_POWER_OF_TWO));
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int boundWidth = MeasureSpec.getSize(widthMeasureSpec);
        int boundHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (boundWidth != 0 && boundHeight != 0) {
            for (ViewItem item : this.mViewItem) {
                if (item != null) {
                    measureViewItem(item, boundWidth, boundHeight);
                }
            }
            clampCenterX();
            this.mZoomView.measure(MeasureSpec.makeMeasureSpec(widthMeasureSpec, Ints.MAX_POWER_OF_TWO), MeasureSpec.makeMeasureSpec(heightMeasureSpec, Ints.MAX_POWER_OF_TWO));
        }
    }

    private int findTheNearestView(int pointX) {
        int nearest = 0;
        while (nearest < 5 && (this.mViewItem[nearest] == null || this.mViewItem[nearest].getLeftPosition() == -1)) {
            nearest++;
        }
        if (nearest == 5) {
            return -1;
        }
        int min = Math.abs(pointX - this.mViewItem[nearest].getCenterX());
        int itemID = nearest + 1;
        while (itemID < 5 && this.mViewItem[itemID] != null) {
            if (this.mViewItem[itemID].getLeftPosition() != -1) {
                int dist = Math.abs(pointX - this.mViewItem[itemID].getCenterX());
                if (dist < min) {
                    min = dist;
                    nearest = itemID;
                }
            }
            itemID++;
        }
        return nearest;
    }

    private ViewItem buildItemFromData(int dataID) {
        if (this.mActivity.isDestroyed()) {
            Log.d(TAG, "Activity destroyed, don't load data");
            return null;
        }
        ImageData data = this.mDataAdapter.getImageData(dataID);
        if (data == null) {
            return null;
        }
        int width = Math.round(((float) getWidth()) * 1060320051);
        int height = Math.round(1060320051 * ((float) getHeight()));
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("suggesting item bounds: ");
        stringBuilder.append(width);
        stringBuilder.append("x");
        stringBuilder.append(height);
        Log.v(tag, stringBuilder.toString());
        this.mDataAdapter.suggestViewSizeBound(width, height);
        data.prepare();
        View v = this.mDataAdapter.getView(this.mActivity.getAndroidContext(), getRecycledView(dataID), dataID, this.mActionCallback);
        if (v == null) {
            return null;
        }
        ViewItem item = new ViewItem(dataID, v, data);
        item.addViewToHierarchy();
        return item;
    }

    private void checkItemAtMaxSize() {
        ViewItem item = this.mViewItem[2];
        if (!item.isMaximumBitmapRequested()) {
            item.setMaximumBitmapRequested();
            int id = item.getId();
            int h = this.mDataAdapter.getImageData(id).getHeight();
            item.resizeView(this.mActivity, this.mDataAdapter.getImageData(id).getWidth(), h);
        }
    }

    private void removeItem(int itemID) {
        if (itemID < this.mViewItem.length && this.mViewItem[itemID] != null) {
            if (this.mDataAdapter.getImageData(this.mViewItem[itemID].getId()) == null) {
                Log.e(TAG, "trying to remove a null item");
                return;
            }
            this.mViewItem[itemID].removeViewFromHierarchy(false);
            this.mViewItem[itemID] = null;
        }
    }

    private void stepIfNeeded() {
        if (inFilmstrip() || inFullScreen()) {
            int nearest = findTheNearestView(this.mCenterX);
            if (nearest != -1 && nearest != 2) {
                int k;
                int prevDataId = this.mViewItem[2] == null ? -1 : this.mViewItem[2].getId();
                int adjust = nearest - 2;
                int k2;
                if (adjust > 0) {
                    k = 0;
                    for (k2 = 0; k2 < adjust; k2++) {
                        removeItem(k2);
                    }
                    while (k + adjust < 5) {
                        this.mViewItem[k] = this.mViewItem[k + adjust];
                        k++;
                    }
                    for (k = 5 - adjust; k < 5; k++) {
                        this.mViewItem[k] = null;
                        if (this.mViewItem[k - 1] != null) {
                            this.mViewItem[k] = buildItemFromData(this.mViewItem[k - 1].getId() + 1);
                        }
                    }
                    adjustChildZOrder();
                } else {
                    k2 = 4;
                    for (int k3 = 4; k3 >= 5 + adjust; k3--) {
                        removeItem(k3);
                    }
                    while (true) {
                        int k4 = k2;
                        if (k4 + adjust < 0) {
                            break;
                        }
                        this.mViewItem[k4] = this.mViewItem[k4 + adjust];
                        k2 = k4 - 1;
                    }
                    for (k = -1 - adjust; k >= 0; k--) {
                        this.mViewItem[k] = null;
                        if (this.mViewItem[k + 1] != null) {
                            this.mViewItem[k] = buildItemFromData(this.mViewItem[k + 1].getId() - 1);
                        }
                    }
                }
                invalidate();
                if (this.mListener != null) {
                    this.mListener.onDataFocusChanged(prevDataId, this.mViewItem[2].getId());
                    k = this.mViewItem[2].getId() - 2;
                    this.mListener.onScroll(k, k + 5, this.mDataAdapter.getTotalNumber());
                }
                zoomAtIndexChanged();
            }
        }
    }

    private boolean clampCenterX() {
        ViewItem curr = this.mViewItem[2];
        if (curr == null) {
            return false;
        }
        boolean stopScroll = false;
        if (curr.getId() == 1 && this.mCenterX < curr.getCenterX() && this.mDataIdOnUserScrolling > 1 && this.mDataAdapter.getImageData(0).getViewType() == 1 && this.mController.isScrolling()) {
            stopScroll = true;
        } else if (curr.getId() == 0 && this.mCenterX < curr.getCenterX()) {
            stopScroll = true;
        }
        if (curr.getId() == this.mDataAdapter.getTotalNumber() - 1 && this.mCenterX > curr.getCenterX()) {
            stopScroll = true;
        }
        if (stopScroll) {
            this.mCenterX = curr.getCenterX();
        }
        return stopScroll;
    }

    private void adjustChildZOrder() {
        for (int i = 4; i >= 0; i--) {
            if (this.mViewItem[i] != null) {
                this.mViewItem[i].bringViewToFront();
            }
        }
        bringChildToFront(this.mZoomView);
        if (ApiHelper.isLOrHigher()) {
            setMaxElevation(this.mZoomView);
        }
    }

    @TargetApi(21)
    private void setMaxElevation(View v) {
        v.setElevation(Float.MAX_VALUE);
    }

    private int getCurrentId() {
        ViewItem current = this.mViewItem[2];
        if (current == null) {
            return -1;
        }
        return current.getId();
    }

    private void snapInCenter() {
        ViewItem currItem = this.mViewItem[2];
        if (currItem != null) {
            int currentViewCenter = currItem.getCenterX();
            if (!this.mController.isScrolling() && !this.mIsUserScrolling && !isCurrentItemCentered()) {
                this.mController.scrollToPosition(currentViewCenter, (int) ((600.0f * ((float) Math.abs(this.mCenterX - currentViewCenter))) / ((float) this.mDrawArea.width())), false);
                if (!(!isViewTypeSticky(currItem) || this.mController.isScaling() || this.mScale == 1.0f)) {
                    this.mController.goToFullScreen();
                }
            }
        }
    }

    private void translateLeftViewItem(int currItem, int drawAreaWidth, float scaleFraction) {
        if (currItem < 0 || currItem > 4) {
            Log.e(TAG, "currItem id out of bound.");
            return;
        }
        ViewItem curr = this.mViewItem[currItem];
        ViewItem next = this.mViewItem[currItem + 1];
        if (curr == null || next == null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid view item (curr or next == null). curr = ");
            stringBuilder.append(currItem);
            Log.e(tag, stringBuilder.toString());
            return;
        }
        int currCenterX = curr.getCenterX();
        int nextCenterX = next.getCenterX();
        int translate = (int) (((float) ((nextCenterX - drawAreaWidth) - currCenterX)) * scaleFraction);
        curr.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
        curr.setAlpha(1.0f);
        curr.setVisibility(0);
        if (inFullScreen()) {
            curr.setTranslationX((float) (((this.mCenterX - currCenterX) * translate) / (nextCenterX - currCenterX)));
        } else {
            curr.setTranslationX((float) translate);
        }
    }

    private void fadeAndScaleRightViewItem(int currItemId) {
        if (currItemId < 1 || currItemId > 5) {
            Log.e(TAG, "currItem id out of bound.");
            return;
        }
        ViewItem currItem = this.mViewItem[currItemId];
        ViewItem prevItem = this.mViewItem[currItemId - 1];
        if (currItem == null || prevItem == null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid view item (curr or prev == null). curr = ");
            stringBuilder.append(currItemId);
            Log.e(tag, stringBuilder.toString());
        } else if (currItemId > 3) {
            currItem.setVisibility(4);
        } else {
            int prevCenterX = prevItem.getCenterX();
            if (this.mCenterX <= prevCenterX) {
                currItem.setVisibility(4);
                return;
            }
            int currCenterX = currItem.getCenterX();
            float fadeDownFraction = (((float) this.mCenterX) - ((float) prevCenterX)) / ((float) (currCenterX - prevCenterX));
            currItem.layoutWithTranslationX(this.mDrawArea, currCenterX, FILM_STRIP_SCALE + (0.3f * fadeDownFraction));
            currItem.setAlpha(fadeDownFraction);
            currItem.setTranslationX(0.0f);
            currItem.setVisibility(0);
        }
    }

    private void layoutViewItems(boolean layoutChanged) {
        if (this.mViewItem[2] != null && this.mDrawArea.width() != 0 && this.mDrawArea.height() != 0) {
            if (layoutChanged) {
                this.mViewItem[2].setLeftPosition(this.mCenterX - (this.mViewItem[2].getMeasuredWidth() / 2));
            }
            if (!inZoomView()) {
                ViewItem curr;
                ViewItem currItem;
                float scaleFraction = this.mViewAnimInterpolator.getInterpolation((this.mScale - FILM_STRIP_SCALE) / 0.3f);
                int fullScreenWidth = this.mDrawArea.width() + this.mViewGapInPixel;
                for (int itemID = 1; itemID >= 0; itemID--) {
                    ViewItem curr2 = this.mViewItem[itemID];
                    if (curr2 == null) {
                        break;
                    }
                    curr2.setLeftPosition((this.mViewItem[itemID + 1].getLeftPosition() - curr2.getMeasuredWidth()) - this.mViewGapInPixel);
                }
                for (int itemID2 = 3; itemID2 < 5; itemID2++) {
                    curr = this.mViewItem[itemID2];
                    if (curr == null) {
                        break;
                    }
                    ViewItem prev = this.mViewItem[itemID2 - 1];
                    curr.setLeftPosition((prev.getLeftPosition() + prev.getMeasuredWidth()) + this.mViewGapInPixel);
                }
                boolean immediateRight = this.mViewItem[2].getId() == 1 && this.mDataAdapter.getImageData(0).getViewType() == 1;
                int currCenterX;
                if (immediateRight) {
                    currItem = this.mViewItem[2];
                    currItem.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                    currItem.setTranslationX(0.0f);
                    currItem.setAlpha(1.0f);
                } else if (scaleFraction == 1.0f) {
                    currItem = this.mViewItem[2];
                    currCenterX = currItem.getCenterX();
                    if (this.mCenterX < currCenterX) {
                        fadeAndScaleRightViewItem(2);
                    } else if (this.mCenterX > currCenterX) {
                        translateLeftViewItem(2, fullScreenWidth, scaleFraction);
                    } else {
                        currItem.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                        currItem.setTranslationX(0.0f);
                        currItem.setAlpha(1.0f);
                    }
                } else {
                    curr = this.mViewItem[2];
                    curr.setTranslationX(curr.getTranslationX() * scaleFraction);
                    curr.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                    if (this.mViewItem[1] == null) {
                        curr.setAlpha(1.0f);
                    } else {
                        int currCenterX2 = curr.getCenterX();
                        currCenterX = this.mViewItem[1].getCenterX();
                        float fadeDownFraction = (((float) this.mCenterX) - ((float) currCenterX)) / ((float) (currCenterX2 - currCenterX));
                        curr.setAlpha(((1.0f - fadeDownFraction) * (1.0f - scaleFraction)) + fadeDownFraction);
                    }
                }
                int itemID3 = 1;
                while (itemID3 >= 0 && this.mViewItem[itemID3] != null) {
                    translateLeftViewItem(itemID3, fullScreenWidth, scaleFraction);
                    itemID3--;
                }
                for (itemID3 = 3; itemID3 < 5; itemID3++) {
                    currItem = this.mViewItem[itemID3];
                    if (currItem == null) {
                        break;
                    }
                    currItem.layoutWithTranslationX(this.mDrawArea, this.mCenterX, this.mScale);
                    if (currItem.getId() == 1 && isViewTypeSticky(currItem)) {
                        currItem.setAlpha(1.0f);
                    } else if (scaleFraction == 1.0f) {
                        fadeAndScaleRightViewItem(itemID3);
                    } else {
                        boolean setToVisible = currItem.getVisibility() == 4;
                        if (itemID3 == 3) {
                            currItem.setAlpha(1.0f - scaleFraction);
                        } else if (scaleFraction == 0.0f) {
                            currItem.setAlpha(1.0f);
                        } else {
                            setToVisible = false;
                        }
                        if (setToVisible) {
                            currItem.setVisibility(0);
                        }
                        currItem.setTranslationX(((float) (this.mViewItem[2].getLeftPosition() - currItem.getLeftPosition())) * scaleFraction);
                    }
                }
                stepIfNeeded();
            }
        }
    }

    private boolean isViewTypeSticky(ViewItem item) {
        boolean z = false;
        if (item == null) {
            return false;
        }
        if (this.mDataAdapter.getImageData(item.getId()).getViewType() == 1) {
            z = true;
        }
        return z;
    }

    public void onDraw(Canvas c) {
        layoutViewItems(false);
        super.onDraw(c);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        this.mDrawArea.left = 0;
        this.mDrawArea.top = 0;
        this.mDrawArea.right = r - l;
        this.mDrawArea.bottom = b - t;
        this.mZoomView.layout(this.mDrawArea.left, this.mDrawArea.top, this.mDrawArea.right, this.mDrawArea.bottom);
        if (!inZoomView() || changed) {
            resetZoomView();
            layoutViewItems(changed);
        }
    }

    private void resetZoomView() {
        if (inZoomView()) {
            ViewItem current = this.mViewItem[2];
            if (current != null) {
                this.mScale = 1.0f;
                this.mController.cancelZoomAnimation();
                this.mController.cancelFlingAnimation();
                current.resetTransform();
                this.mController.cancelLoadingZoomedImage();
                this.mZoomView.setVisibility(8);
                this.mController.setSurroundingViewsVisible(true);
            }
        }
    }

    private void hideZoomView() {
        if (inZoomView()) {
            this.mController.cancelLoadingZoomedImage();
            this.mZoomView.setVisibility(8);
        }
    }

    private void slideViewBack(ViewItem item) {
        item.animateTranslationX(0.0f, 400, this.mViewAnimInterpolator);
        item.animateTranslationY(0.0f, 400, this.mViewAnimInterpolator);
        item.animateAlpha(1.0f, 400, this.mViewAnimInterpolator);
    }

    private void animateItemRemoval(int dataID, ImageData data) {
        if (this.mScale > 1.0f) {
            resetZoomView();
        }
        int removedItemId = findItemByDataID(dataID);
        int i = 0;
        int i2 = 0;
        while (i2 < 5) {
            if (this.mViewItem[i2] != null && this.mViewItem[i2].getId() > dataID) {
                this.mViewItem[i2].setId(this.mViewItem[i2].getId() - 1);
            }
            i2++;
        }
        if (removedItemId != -1) {
            int i3;
            final ViewItem removedItem = this.mViewItem[removedItemId];
            int offsetX = removedItem.getMeasuredWidth() + this.mViewGapInPixel;
            for (int i4 = removedItemId + 1; i4 < 5; i4++) {
                if (this.mViewItem[i4] != null) {
                    this.mViewItem[i4].setLeftPosition(this.mViewItem[i4].getLeftPosition() - offsetX);
                }
            }
            int next;
            int i5;
            if (removedItemId < 2 || this.mViewItem[removedItemId].getId() >= this.mDataAdapter.getTotalNumber()) {
                this.mCenterX -= offsetX;
                for (i3 = removedItemId; i3 > 0; i3--) {
                    this.mViewItem[i3] = this.mViewItem[i3 - 1];
                }
                next = 0 + 1;
                if (this.mViewItem[next] != null) {
                    this.mViewItem[0] = buildItemFromData(this.mViewItem[next].getId() - 1);
                }
                for (i5 = removedItemId; i5 >= 0; i5--) {
                    if (this.mViewItem[i5] != null) {
                        this.mViewItem[i5].setTranslationX((float) (-offsetX));
                    }
                }
            } else {
                for (i3 = removedItemId; i3 < 4; i3++) {
                    this.mViewItem[i3] = this.mViewItem[i3 + 1];
                }
                i5 = 4 - 1;
                if (this.mViewItem[i5] != null) {
                    this.mViewItem[4] = buildItemFromData(this.mViewItem[i5].getId() + 1);
                }
                if (inFullScreen()) {
                    this.mViewItem[2].setVisibility(0);
                    ViewItem nextItem = this.mViewItem[3];
                    if (nextItem != null) {
                        nextItem.setVisibility(4);
                    }
                }
                for (next = removedItemId; next < 5; next++) {
                    if (this.mViewItem[next] != null) {
                        this.mViewItem[next].setTranslationX((float) offsetX);
                    }
                }
                ViewItem currItem = this.mViewItem[2];
                if (currItem == null) {
                    Log.w(TAG, "Caught invalid update in removal animation.");
                } else if (currItem.getId() == this.mDataAdapter.getTotalNumber() - 1 && this.mCenterX > currItem.getCenterX()) {
                    int adjustDiff = currItem.getCenterX() - this.mCenterX;
                    this.mCenterX = currItem.getCenterX();
                    for (int i6 = 0; i6 < 5; i6++) {
                        if (this.mViewItem[i6] != null) {
                            this.mViewItem[i6].translateXScaledBy((float) adjustDiff);
                        }
                    }
                }
            }
            i3 = getHeight() / 8;
            if (removedItem.getTranslationY() < 0.0f) {
                i3 = -i3;
            }
            removedItem.animateTranslationY(removedItem.getTranslationY() + ((float) i3), 400, this.mViewAnimInterpolator);
            removedItem.animateAlpha(0.0f, 400, this.mViewAnimInterpolator);
            postDelayed(new Runnable() {
                public void run() {
                    removedItem.removeViewFromHierarchy(false);
                }
            }, 400);
            adjustChildZOrder();
            invalidate();
            if (this.mViewItem[2] != null) {
                while (i < 5) {
                    if (!(this.mViewItem[i] == null || this.mViewItem[i].getTranslationX() == 0.0f)) {
                        slideViewBack(this.mViewItem[i]);
                    }
                    i++;
                }
                if (isCurrentItemCentered() && isViewTypeSticky(this.mViewItem[2])) {
                    this.mController.goToFullScreen();
                }
            }
        }
    }

    private int findItemByDataID(int dataID) {
        int i = 0;
        while (i < 5) {
            if (this.mViewItem[i] != null && this.mViewItem[i].getId() == dataID) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void updateInsertion(int dataID) {
        int insertedItemId = findItemByDataID(dataID);
        int i = 4;
        int i2 = 1;
        if (insertedItemId == -1 && dataID == this.mDataAdapter.getTotalNumber() - 1) {
            int prev = findItemByDataID(dataID - 1);
            if (prev >= 0 && prev < 4) {
                insertedItemId = prev + 1;
            }
        }
        int i3 = 0;
        while (i3 < 5) {
            if (this.mViewItem[i3] != null && this.mViewItem[i3].getId() >= dataID) {
                this.mViewItem[i3].setId(this.mViewItem[i3].getId() + 1);
            }
            i3++;
        }
        if (insertedItemId != -1) {
            ImageData data = this.mDataAdapter.getImageData(dataID);
            int offsetX = CameraUtil.resizeToFill(data.getWidth(), data.getHeight(), data.getRotation(), getMeasuredWidth(), getMeasuredHeight()).x + this.mViewGapInPixel;
            ViewItem viewItem = buildItemFromData(dataID);
            if (viewItem == null) {
                Log.w(TAG, "unable to build inserted item from data");
                return;
            }
            if (insertedItemId < 2) {
                insertedItemId--;
                if (insertedItemId >= 0) {
                    removeItem(0);
                    while (true) {
                        i = i2;
                        if (i > insertedItemId) {
                            break;
                        }
                        if (this.mViewItem[i] != null) {
                            this.mViewItem[i].setTranslationX((float) offsetX);
                            slideViewBack(this.mViewItem[i]);
                            this.mViewItem[i - 1] = this.mViewItem[i];
                        }
                        i2 = i + 1;
                    }
                } else {
                    return;
                }
            }
            if (insertedItemId == 2) {
                viewItem.setLeftPosition(this.mViewItem[2].getLeftPosition());
            }
            removeItem(4);
            while (i > insertedItemId) {
                this.mViewItem[i] = this.mViewItem[i - 1];
                if (this.mViewItem[i] != null) {
                    this.mViewItem[i].setTranslationX((float) (-offsetX));
                    slideViewBack(this.mViewItem[i]);
                }
                i--;
            }
            this.mViewItem[insertedItemId] = viewItem;
            viewItem.setAlpha(0.0f);
            viewItem.setTranslationY((float) (getHeight() / 8));
            slideViewBack(viewItem);
            adjustChildZOrder();
            invalidate();
        }
    }

    private void setDataAdapter(DataAdapter adapter) {
        this.mDataAdapter = adapter;
        int maxEdge = (int) (((float) Math.max(getHeight(), getWidth())) * 1060320051);
        this.mDataAdapter.suggestViewSizeBound(maxEdge, maxEdge);
        this.mDataAdapter.setListener(new DataAdapter.Listener() {
            public void onDataLoaded() {
                FilmstripView.this.reload();
            }

            public void onDataUpdated(UpdateReporter reporter) {
                FilmstripView.this.update(reporter);
            }

            public void onDataInserted(int dataId, ImageData data) {
                if (FilmstripView.this.mViewItem[2] == null) {
                    FilmstripView.this.reload();
                } else {
                    FilmstripView.this.updateInsertion(dataId);
                }
                if (FilmstripView.this.mListener != null) {
                    FilmstripView.this.mListener.onDataFocusChanged(dataId, FilmstripView.this.getCurrentId());
                }
            }

            public void onDataRemoved(int dataId, ImageData data) {
                FilmstripView.this.animateItemRemoval(dataId, data);
                if (FilmstripView.this.mListener != null) {
                    FilmstripView.this.mListener.onDataFocusChanged(dataId, FilmstripView.this.getCurrentId());
                }
            }
        });
    }

    private boolean inFilmstrip() {
        return this.mScale == FILM_STRIP_SCALE;
    }

    private boolean inFullScreen() {
        return this.mScale == 1.0f;
    }

    private boolean inZoomView() {
        return this.mScale > 1.0f;
    }

    private boolean isCameraPreview() {
        return isViewTypeSticky(this.mViewItem[2]);
    }

    private boolean inCameraFullscreen() {
        if (isDataAtCenter(0) && inFullScreen() && isViewTypeSticky(this.mViewItem[2])) {
            return true;
        }
        return false;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (this.mController.isScrolling()) {
            return true;
        }
        if (ev.getActionMasked() == 0) {
            this.mCheckToIntercept = true;
            this.mDown = MotionEvent.obtain(ev);
            ViewItem viewItem = this.mViewItem[2];
            if (!(viewItem == null || this.mDataAdapter.canSwipeInFullScreen(viewItem.getId()))) {
                this.mCheckToIntercept = false;
            }
            return false;
        } else if (ev.getActionMasked() == 5) {
            this.mCheckToIntercept = false;
            return false;
        } else if (!this.mCheckToIntercept || ev.getEventTime() - ev.getDownTime() > 500) {
            return false;
        } else {
            int deltaX = (int) (ev.getX() - this.mDown.getX());
            int deltaY = (int) (ev.getY() - this.mDown.getY());
            if (ev.getActionMasked() != 2 || deltaX >= this.mSlop * -1 || Math.abs(deltaX) < Math.abs(deltaY) * 2) {
                return false;
            }
            return true;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        return this.mGestureRecognizer.onTouchEvent(ev);
    }

    /* Access modifiers changed, original: 0000 */
    public Listener getGestureListener() {
        return this.mGestureListener;
    }

    private void updateViewItem(int itemID) {
        ViewItem item = this.mViewItem[itemID];
        if (item == null) {
            Log.e(TAG, "trying to update an null item");
            return;
        }
        item.removeViewFromHierarchy(true);
        ViewItem newItem = buildItemFromData(item.getId());
        if (newItem == null) {
            Log.e(TAG, "new item is null");
            item.addViewToHierarchy();
            return;
        }
        newItem.copyAttributes(item);
        this.mViewItem[itemID] = newItem;
        this.mZoomView.resetDecoder();
        if (clampCenterX()) {
            this.mController.stopScrolling(true);
        }
        adjustChildZOrder();
        invalidate();
        if (this.mListener != null) {
            this.mListener.onDataUpdated(newItem.getId());
        }
    }

    private void update(UpdateReporter reporter) {
        if (this.mViewItem[2] == null) {
            reload();
            return;
        }
        ViewItem curr = this.mViewItem[2];
        int dataId = curr.getId();
        if (reporter.isDataRemoved(dataId)) {
            reload();
            return;
        }
        int i;
        ViewItem curr2;
        int dataId2;
        ViewItem next;
        if (reporter.isDataUpdated(dataId)) {
            updateViewItem(2);
            ImageData data = this.mDataAdapter.getImageData(dataId);
            if (!(this.mIsUserScrolling || this.mController.isScrolling())) {
                this.mCenterX = curr.getLeftPosition() + (CameraUtil.resizeToFill(data.getWidth(), data.getHeight(), data.getRotation(), getMeasuredWidth(), getMeasuredHeight()).x / 2);
            }
        }
        for (i = 1; i >= 0; i--) {
            curr2 = this.mViewItem[i];
            if (curr2 != null) {
                dataId2 = curr2.getId();
                if (reporter.isDataRemoved(dataId2) || reporter.isDataUpdated(dataId2)) {
                    updateViewItem(i);
                }
            } else {
                next = this.mViewItem[i + 1];
                if (next != null) {
                    this.mViewItem[i] = buildItemFromData(next.getId() - 1);
                }
            }
        }
        for (i = 3; i < 5; i++) {
            curr2 = this.mViewItem[i];
            if (curr2 != null) {
                dataId2 = curr2.getId();
                if (reporter.isDataRemoved(dataId2) || reporter.isDataUpdated(dataId2)) {
                    updateViewItem(i);
                }
            } else {
                next = this.mViewItem[i - 1];
                if (next != null) {
                    this.mViewItem[i] = buildItemFromData(next.getId() + 1);
                }
            }
        }
        adjustChildZOrder();
        requestLayout();
    }

    private void reload() {
        this.mController.stopScrolling(true);
        this.mController.stopScale();
        this.mDataIdOnUserScrolling = 0;
        int prevId = -1;
        if (this.mViewItem[2] != null) {
            prevId = this.mViewItem[2].getId();
        }
        for (int i = 0; i < this.mViewItem.length; i++) {
            if (this.mViewItem[i] != null) {
                this.mViewItem[i].removeViewFromHierarchy(false);
            }
        }
        Arrays.fill(this.mViewItem, null);
        if (this.mDataAdapter.getTotalNumber() != 0) {
            this.mViewItem[2] = buildItemFromData(0);
            if (this.mViewItem[2] != null) {
                this.mViewItem[2].setLeftPosition(0);
                for (int i2 = 3; i2 < 5; i2++) {
                    this.mViewItem[i2] = buildItemFromData(this.mViewItem[i2 - 1].getId() + 1);
                    if (this.mViewItem[i2] == null) {
                        break;
                    }
                }
                this.mCenterX = -1;
                this.mScale = FILM_STRIP_SCALE;
                adjustChildZOrder();
                invalidate();
                if (this.mListener != null) {
                    this.mListener.onDataReloaded();
                    this.mListener.onDataFocusChanged(prevId, this.mViewItem[2].getId());
                }
            }
        }
    }

    private void promoteData(int itemID, int dataID) {
        if (this.mListener != null) {
            this.mListener.onFocusedDataPromoted(dataID);
        }
    }

    private void demoteData(int itemID, int dataID) {
        if (this.mListener != null) {
            this.mListener.onFocusedDataDemoted(dataID);
        }
    }

    private void onEnterFilmstrip() {
        if (this.mListener != null) {
            this.mListener.onEnterFilmstrip(getCurrentId());
        }
    }

    private void onLeaveFilmstrip() {
        if (this.mListener != null) {
            this.mListener.onLeaveFilmstrip(getCurrentId());
        }
    }

    private void onEnterFullScreen() {
        this.mFullScreenUIHidden = false;
        if (this.mListener != null) {
            this.mListener.onEnterFullScreenUiShown(getCurrentId());
        }
    }

    private void onLeaveFullScreen() {
        if (this.mListener != null) {
            this.mListener.onLeaveFullScreenUiShown(getCurrentId());
        }
    }

    private void onEnterFullScreenUiHidden() {
        this.mFullScreenUIHidden = true;
        if (this.mListener != null) {
            this.mListener.onEnterFullScreenUiHidden(getCurrentId());
        }
    }

    private void onLeaveFullScreenUiHidden() {
        this.mFullScreenUIHidden = false;
        if (this.mListener != null) {
            this.mListener.onLeaveFullScreenUiHidden(getCurrentId());
        }
    }

    private void onEnterZoomView() {
        if (this.mListener != null) {
            this.mListener.onEnterZoomView(getCurrentId());
        }
    }

    private void onLeaveZoomView() {
        this.mController.setSurroundingViewsVisible(true);
    }

    private boolean isCurrentItemCentered() {
        return this.mViewItem[2].getCenterX() == this.mCenterX;
    }
}
