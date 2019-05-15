package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.ImageView.ScaleType;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.LockUtils;
import com.hmdglobal.app.camera.util.LockUtils.Lock;
import com.hmdglobal.app.camera.util.LockUtils.LockType;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class RotateImageView extends TwoStateImageView implements Rotatable, Lockable {
    private static final int ANIMATION_SPEED = 270;
    private static final Tag TAG = new Tag("RotateImageView");
    private long mAnimationEndTime = 0;
    private long mAnimationStartTime = 0;
    private boolean mClockwise = false;
    protected int mCurrentDegree = 0;
    private boolean mEnableAnimation = true;
    private Lock mMultiLock = LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK);
    private int mStartDegree = 0;
    private int mTargetDegree = 0;
    private Bitmap mThumb;
    private TransitionDrawable mThumbTransition;
    private Drawable[] mThumbs;

    public RotateImageView(Context context) {
        super(context);
    }

    public RotateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotateImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context);
    }

    /* Access modifiers changed, original: protected */
    public int getDegree() {
        return this.mTargetDegree;
    }

    public void setOrientation(int degree, boolean animation) {
        this.mEnableAnimation = animation;
        degree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
        if (degree != this.mTargetDegree) {
            this.mTargetDegree = degree;
            if (this.mEnableAnimation) {
                this.mStartDegree = this.mCurrentDegree;
                this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
                int diff = this.mTargetDegree - this.mCurrentDegree;
                int diff2 = diff >= 0 ? diff : 360 + diff;
                diff2 = diff2 > MediaProviderUtils.ROTATION_180 ? diff2 - 360 : diff2;
                this.mClockwise = diff2 >= 0;
                this.mAnimationEndTime = this.mAnimationStartTime + ((long) ((Math.abs(diff2) * 1000) / 270));
            } else {
                this.mCurrentDegree = this.mTargetDegree;
            }
            invalidate();
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean needSuperDrawable() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        Canvas canvas2 = canvas;
        if (needSuperDrawable()) {
            Drawable drawable = getDrawable();
            if (drawable != null) {
                Rect bounds = drawable.getBounds();
                int w = bounds.right - bounds.left;
                int h = bounds.bottom - bounds.top;
                Rect rect;
                if (w == 0) {
                } else if (h == 0) {
                    rect = bounds;
                } else {
                    if (this.mCurrentDegree != this.mTargetDegree) {
                        long time = AnimationUtils.currentAnimationTimeMillis();
                        if (time < this.mAnimationEndTime) {
                            int deltaTime = (int) (time - this.mAnimationStartTime);
                            int degree = this.mStartDegree + ((270 * (this.mClockwise ? deltaTime : -deltaTime)) / 1000);
                            this.mCurrentDegree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
                            invalidate();
                        } else {
                            this.mCurrentDegree = this.mTargetDegree;
                        }
                    }
                    int left = getPaddingLeft();
                    int top = getPaddingTop();
                    int width = (getWidth() - left) - getPaddingRight();
                    int height = (getHeight() - top) - getPaddingBottom();
                    int saveCount = canvas.getSaveCount();
                    if (getScaleType() != ScaleType.FIT_CENTER) {
                    } else if (width < w || height < h) {
                        float ratio = Math.min(((float) width) / ((float) w), ((float) height) / ((float) h));
                        canvas2.scale(ratio, ratio, ((float) width) / 2.0f, ((float) height) / 2.0f);
                    } else {
                        rect = bounds;
                    }
                    canvas2.translate((float) ((width / 2) + left), (float) ((height / 2) + top));
                    canvas2.rotate((float) (-this.mCurrentDegree));
                    canvas2.translate((float) ((-w) / 2), (float) ((-h) / 2));
                    drawable.draw(canvas2);
                    canvas2.restoreToCount(saveCount);
                    return;
                }
                return;
            }
            return;
        }
        super.onDraw(canvas);
    }

    /* Access modifiers changed, original: protected */
    public boolean needTranslation() {
        return true;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            this.mThumb = null;
            this.mThumbs = null;
            setImageDrawable(null);
            setVisibility(8);
            return;
        }
        LayoutParams param = getLayoutParams();
        this.mThumb = ThumbnailUtils.extractThumbnail(bitmap, (param.width - getPaddingLeft()) - getPaddingRight(), (param.height - getPaddingTop()) - getPaddingBottom());
        if (this.mThumbs != null && this.mEnableAnimation && needTranslation()) {
            this.mThumbs[0] = this.mThumbs[1];
            this.mThumbs[1] = new BitmapDrawable(getContext().getResources(), this.mThumb);
            this.mThumbTransition = new TransitionDrawable(this.mThumbs);
            setImageDrawable(this.mThumbTransition);
            this.mThumbTransition.startTransition(500);
        } else {
            this.mThumbs = new Drawable[2];
            this.mThumbs[1] = new BitmapDrawable(getContext().getResources(), this.mThumb);
            setImageDrawable(this.mThumbs[1]);
        }
        setVisibility(0);
    }

    public boolean onTouchEvent(MotionEvent event) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onTouchEvent  ");
        stringBuilder.append(isLocked());
        stringBuilder.append(",  ");
        stringBuilder.append(event.getAction());
        Log.d(tag, stringBuilder.toString());
        if (isLocked()) {
            MotionEvent newEvent = MotionEvent.obtain(event);
            newEvent.setAction(3);
            super.onTouchEvent(newEvent);
            newEvent.recycle();
            return true;
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("RotateImageView isLocked , instance is ");
        stringBuilder.append(toString());
        Log.e(tag, stringBuilder.toString());
        return super.onTouchEvent(event);
    }

    public void lockSelf() {
        this.mMultiLock.aquireLock(hashCode());
    }

    public void unLockSelf() {
        this.mMultiLock.unlockWithToken(Integer.valueOf(hashCode()));
    }

    public boolean isLocked() {
        return this.mMultiLock.isLocked();
    }

    public int lock() {
        return this.mMultiLock.aquireLock().intValue();
    }

    public boolean unlockWithToken(int token) {
        return this.mMultiLock.unlockWithToken(Integer.valueOf(token));
    }

    public boolean isIgnoreLock() {
        return this.mMultiLock.isIgnoreLock();
    }
}
