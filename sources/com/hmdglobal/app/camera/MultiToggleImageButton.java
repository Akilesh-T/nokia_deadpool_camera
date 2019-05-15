package com.hmdglobal.app.camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView.ScaleType;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.RotateImageView;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.Gusterpolator;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class MultiToggleImageButton extends RotateImageView {
    public static final int ANIM_DIRECTION_HORIZONTAL = 1;
    public static final int ANIM_DIRECTION_VERTICAL = 0;
    private static final int ANIM_DURATION_MS = 250;
    private static final int UNSET = -1;
    Tag TAG = new Tag("MultiTogButton");
    private final boolean isOptimizeSwitchCamera = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_OPTIMIZE_SWITCH, false);
    private int mAnimDirection;
    private ValueAnimator mAnimator;
    private OnCancelSelectionMenuListener mCancelSelectionMenuListener;
    private boolean mClickEnabled = true;
    private int[] mDescIds;
    private int[] mImageIds;
    private int mLevel;
    private Matrix mMatrix = new Matrix();
    private boolean mNeedSuperDraw = true;
    private OnStateChangeListener mOnStateChangeListener;
    private OnTouchListener mOnTouchListener;
    private OnUnhandledClickListener mOnUnhandledClickListener;
    private int mOrientation = 0;
    private int mParentSize;
    private OnClickListener mSelectionModeClickListener = null;
    private int mState = -1;
    private boolean mWaitForTouchDown = false;

    public interface OnStateChangeListener {
        void stateChanged(View view, int i);
    }

    public interface OnTouchListener {
        void onTouchDown();

        void onTouchUp();
    }

    public interface OnUnhandledClickListener {
        void unhandledClick();
    }

    public MultiToggleImageButton(Context context) {
        super(context);
        init();
    }

    public MultiToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        parseAttributes(context, attrs);
        setState(-1);
    }

    public MultiToggleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
        parseAttributes(context, attrs);
        setState(-1);
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.mOnStateChangeListener = onStateChangeListener;
    }

    public void setOnUnhandledClickListener(OnUnhandledClickListener onUnhandledClickListener) {
        this.mOnUnhandledClickListener = onUnhandledClickListener;
    }

    public void setOnCancelSelectionMenu(OnCancelSelectionMenuListener onCancelSelectionMenuListener) {
        this.mCancelSelectionMenuListener = onCancelSelectionMenuListener;
    }

    public void setSelectionModeClickListener(OnClickListener l) {
        this.mSelectionModeClickListener = l;
    }

    public int getState() {
        return this.mState;
    }

    public void setState(int state) {
        setState(state, true);
    }

    public void setState(int state, boolean callListener) {
        setState(state, callListener, false);
    }

    public void setState(int state, boolean callListener, boolean animate) {
        setStateAnimatedInternal(state, callListener, animate);
    }

    private void setStateAnimatedInternal(final int state, final boolean callListener, boolean animate) {
        if (state != -1) {
            if (!(animate || this.mAnimator == null || !this.mAnimator.isRunning())) {
                this.mAnimator.end();
            }
            if (this.mState == state || this.mState == -1 || !animate) {
                setStateInternal(state, callListener);
            } else if (this.mImageIds != null) {
                if (this.isOptimizeSwitchCamera && callListener && this.mOnStateChangeListener != null) {
                    if (this.mWaitForTouchDown) {
                        if (this.mOnTouchListener != null) {
                            this.mOnTouchListener.onTouchUp();
                        }
                        this.mWaitForTouchDown = false;
                    }
                    this.mOnStateChangeListener.stateChanged(this, state);
                }
                new AsyncTask<Integer, Void, Bitmap>() {
                    /* Access modifiers changed, original: protected|varargs */
                    public Bitmap doInBackground(Integer... params) {
                        return MultiToggleImageButton.this.combine(params[0].intValue(), params[1].intValue());
                    }

                    /* Access modifiers changed, original: protected */
                    public void onPostExecute(Bitmap bitmap) {
                        if (bitmap == null) {
                            MultiToggleImageButton.this.setStateInternal(state, callListener);
                        } else {
                            int offset;
                            MultiToggleImageButton.this.setImageBitmap(bitmap);
                            Tag tag = MultiToggleImageButton.this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Bitmap width is ");
                            stringBuilder.append(bitmap.getWidth());
                            stringBuilder.append(" bitmap height is ");
                            stringBuilder.append(bitmap.getHeight());
                            Log.w(tag, stringBuilder.toString());
                            int rotation = -MultiToggleImageButton.this.mOrientation;
                            if (MultiToggleImageButton.this.mAnimDirection == 0) {
                                offset = (MultiToggleImageButton.this.mParentSize + MultiToggleImageButton.this.getHeight()) / 2;
                                MultiToggleImageButton.this.mAnimator.setFloatValues(new float[]{(float) (-offset), 0.0f});
                            } else if (MultiToggleImageButton.this.mAnimDirection == 1) {
                                offset = (MultiToggleImageButton.this.mParentSize + MultiToggleImageButton.this.getWidth()) / 2;
                                if ((rotation + 360) % 360 != MediaProviderUtils.ROTATION_270) {
                                    MultiToggleImageButton.this.mAnimator.setFloatValues(new float[]{0.0f, (float) (-offset)});
                                } else {
                                    MultiToggleImageButton.this.mAnimator.setFloatValues(new float[]{(float) (-offset), 0.0f});
                                }
                            } else {
                                return;
                            }
                            Tag tag2 = MultiToggleImageButton.this.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("offset is ");
                            stringBuilder2.append(offset);
                            Log.w(tag2, stringBuilder2.toString());
                            AnimatorSet s = new AnimatorSet();
                            s.play(MultiToggleImageButton.this.mAnimator);
                            s.addListener(new AnimatorListenerAdapter() {
                                public void onAnimationStart(Animator animation) {
                                    MultiToggleImageButton.this.mNeedSuperDraw = false;
                                    MultiToggleImageButton.this.setClickEnabled(false);
                                }

                                public void onAnimationEnd(Animator animation) {
                                    MultiToggleImageButton.this.setStateInternal(state, callListener);
                                    MultiToggleImageButton.this.setClickEnabled(true);
                                    MultiToggleImageButton.this.mNeedSuperDraw = true;
                                }

                                public void onAnimationCancel(Animator animation) {
                                    if (MultiToggleImageButton.this.mWaitForTouchDown && MultiToggleImageButton.this.mOnTouchListener != null) {
                                        MultiToggleImageButton.this.mOnTouchListener.onTouchUp();
                                    }
                                    super.onAnimationCancel(animation);
                                    MultiToggleImageButton.this.mNeedSuperDraw = true;
                                }
                            });
                            s.start();
                        }
                    }
                }.execute(new Integer[]{Integer.valueOf(this.mState), Integer.valueOf(state)});
                if (this.isOptimizeSwitchCamera) {
                    this.mState = state;
                }
            }
        }
    }

    public void setClickEnabled(boolean enabled) {
        this.mClickEnabled = enabled;
    }

    private void setStateInternal(int state, boolean callListener) {
        this.mState = state;
        if (this.mImageIds != null) {
            setImageByState(this.mState);
        }
        if (this.mDescIds != null) {
            String oldContentDescription = String.valueOf(getContentDescription());
            String newContentDescription = getResources().getString(this.mDescIds[this.mState]);
            if (!(oldContentDescription == null || oldContentDescription.isEmpty() || oldContentDescription.equals(newContentDescription))) {
                setContentDescription(newContentDescription);
                announceForAccessibility(getResources().getString(R.string.button_change_announcement, new Object[]{newContentDescription}));
            }
        }
        super.setImageLevel(this.mLevel);
        if (!this.isOptimizeSwitchCamera) {
            this.mState = state;
            if (callListener && this.mOnStateChangeListener != null) {
                if (this.mWaitForTouchDown) {
                    if (this.mOnTouchListener != null) {
                        this.mOnTouchListener.onTouchUp();
                    }
                    this.mWaitForTouchDown = false;
                }
                this.mOnStateChangeListener.stateChanged(this, getState());
            }
        }
    }

    private void nextState() {
        int state = this.mState + 1;
        if (state >= this.mImageIds.length) {
            state = 0;
        }
        setState(state);
    }

    public boolean onTouchEvent(MotionEvent event) {
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[MultiToggleImageButton] onTouchEvent isLocked = ");
        stringBuilder.append(isLocked());
        stringBuilder.append(" action = ");
        stringBuilder.append(event.getAction());
        stringBuilder.append(" isIgnoreLock = ");
        stringBuilder.append(isIgnoreLock());
        Log.d(tag, stringBuilder.toString());
        if (isLocked()) {
            if (isIgnoreLock() && event.getAction() == 1 && this.mCancelSelectionMenuListener != null) {
                this.mCancelSelectionMenuListener.onCancelSelectionMenu();
            }
            return super.onTouchEvent(event);
        } else if (isEnabled() || getId() != R.id.flash_toggle_button) {
            if (event.getAction() != 0) {
            }
            return super.onTouchEvent(event);
        } else {
            if (event.getAction() == 1 && this.mOnUnhandledClickListener != null) {
                this.mOnUnhandledClickListener.unhandledClick();
            }
            return true;
        }
    }

    public void setOnTouchListener(OnTouchListener listener) {
        this.mOnTouchListener = listener;
    }

    /* Access modifiers changed, original: protected */
    public void init() {
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (MultiToggleImageButton.this.mClickEnabled) {
                    if (MultiToggleImageButton.this.mOnTouchListener != null) {
                        MultiToggleImageButton.this.mOnTouchListener.onTouchDown();
                    }
                    MultiToggleImageButton.this.mWaitForTouchDown = true;
                    if (MultiToggleImageButton.this.mSelectionModeClickListener != null) {
                        MultiToggleImageButton.this.mSelectionModeClickListener.onClick(v);
                    } else {
                        MultiToggleImageButton.this.nextState();
                    }
                }
            }
        });
        setScaleType(ScaleType.MATRIX);
        this.mAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 0.0f});
        this.mAnimator.setDuration(250);
        this.mAnimator.setInterpolator(Gusterpolator.INSTANCE);
        this.mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                MultiToggleImageButton.this.mMatrix.reset();
                if (MultiToggleImageButton.this.mAnimDirection == 0) {
                    MultiToggleImageButton.this.mMatrix.setTranslate(0.0f, ((Float) animation.getAnimatedValue()).floatValue());
                } else if (MultiToggleImageButton.this.mAnimDirection == 1) {
                    MultiToggleImageButton.this.mMatrix.setTranslate(((Float) animation.getAnimatedValue()).floatValue(), 0.0f);
                }
                MultiToggleImageButton.this.setImageMatrix(MultiToggleImageButton.this.mMatrix);
                MultiToggleImageButton.this.invalidate();
            }
        });
    }

    /* Access modifiers changed, original: protected */
    public boolean needSuperDrawable() {
        return this.mNeedSuperDraw;
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MultiToggleImageButton, 0, 0);
        int imageIds = a.getResourceId(1, 0);
        if (imageIds > 0) {
            overrideImageIds(imageIds);
        }
        int descIds = a.getResourceId(0, 0);
        if (descIds > 0) {
            overrideContentDescriptions(descIds);
        }
        a.recycle();
    }

    public void overrideImageIds(int resId) {
        TypedArray ids = null;
        try {
            ids = getResources().obtainTypedArray(resId);
            int[] tmpImageIds = new int[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                tmpImageIds[i] = ids.getResourceId(i, 0);
            }
            this.mImageIds = tmpImageIds;
            if (this.mState >= 0 && this.mState < this.mImageIds.length) {
                setImageByState(this.mState);
            }
        } finally {
            if (ids != null) {
                ids.recycle();
            }
        }
    }

    public void overrideContentDescriptions(int resId) {
        TypedArray ids = null;
        try {
            ids = getResources().obtainTypedArray(resId);
            this.mDescIds = new int[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                this.mDescIds[i] = ids.getResourceId(i, 0);
            }
        } finally {
            if (ids != null) {
                ids.recycle();
            }
        }
    }

    public void setParentSize(int s) {
        this.mParentSize = s;
    }

    public void setAnimDirection(int d) {
        this.mAnimDirection = d;
    }

    public void setImageLevel(int level) {
        super.setImageLevel(level);
        this.mLevel = level;
    }

    private void setImageByState(int state) {
        if (this.mImageIds != null) {
            setImageResource(this.mImageIds[state]);
        }
        super.setImageLevel(this.mLevel);
    }

    public void setOrientation(int degree, boolean animation) {
        super.setOrientation(degree, animation);
        if (degree % MediaProviderUtils.ROTATION_180 == 0) {
            setAnimDirection(0);
        } else {
            setAnimDirection(1);
        }
        this.mOrientation = degree;
    }

    private Bitmap combine(int oldState, int newState) {
        int i = oldState;
        if (i >= this.mImageIds.length) {
            return null;
        }
        int width = getWidth();
        int height = getHeight();
        if (width <= 0) {
        } else if (height <= 0) {
            int i2 = width;
        } else {
            int[] enabledState = new int[]{16842910};
            Drawable newDrawable = getResources().getDrawable(this.mImageIds[newState]).mutate();
            newDrawable.setState(enabledState);
            Drawable oldDrawable = getResources().getDrawable(this.mImageIds[i]).mutate();
            oldDrawable.setState(enabledState);
            int oldBitmapOffset = ((this.mParentSize - height) / 2) + height;
            Bitmap bitmap = Bitmap.createBitmap(width, (height * 2) + ((this.mParentSize - height) / 2), Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            int newLeft = (width - newDrawable.getIntrinsicWidth()) / 2;
            int newTop = (height - newDrawable.getIntrinsicHeight()) / 2;
            newDrawable.setBounds(newLeft, newTop, newDrawable.getIntrinsicWidth() + newLeft, newDrawable.getIntrinsicHeight() + newTop);
            int oldLeft = (width - oldDrawable.getIntrinsicWidth()) / 2;
            int oldTop = (height - oldDrawable.getIntrinsicHeight()) / 2;
            oldDrawable.setBounds(oldLeft, oldBitmapOffset + oldTop, oldLeft + oldDrawable.getIntrinsicWidth(), (oldBitmapOffset + oldTop) + oldDrawable.getIntrinsicHeight());
            newDrawable.draw(canvas);
            oldDrawable.draw(canvas);
            if (this.mAnimDirection == 0) {
                int[] iArr = enabledState;
                int i3 = oldLeft;
                int i4 = newTop;
                int i5 = newLeft;
            } else {
                i = -this.mOrientation;
                width = this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("rotate orientation is");
                stringBuilder.append(-this.mOrientation);
                Log.w(width, stringBuilder.toString());
                width = new Matrix();
                width.setRotate((float) i, 0.0f, 0.0f);
                Bitmap bitmapToRecycle = bitmap;
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), width, true);
                bitmapToRecycle.recycle();
            }
            return bitmap;
        }
        return null;
    }
}
