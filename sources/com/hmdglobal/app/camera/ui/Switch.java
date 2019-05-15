package com.hmdglobal.app.camera.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CompoundButton;
import com.hmdglobal.app.camera.R;

public class Switch extends CompoundButton {
    private static final int[] CHECKED_STATE_SET = new int[]{16842912};
    private static final int TOUCH_MODE_DOWN = 1;
    private static final int TOUCH_MODE_DRAGGING = 2;
    private static final int TOUCH_MODE_IDLE = 0;
    private int mMinFlingVelocity;
    private Layout mOffLayout;
    private Layout mOnLayout;
    private int mSwitchBottom;
    private int mSwitchHeight;
    private int mSwitchLeft;
    private int mSwitchMinWidth;
    private int mSwitchPadding;
    private int mSwitchRight;
    private int mSwitchTextMaxWidth;
    private int mSwitchTop;
    private int mSwitchWidth;
    private final Rect mTempRect;
    private ColorStateList mTextColors;
    private CharSequence mTextOff;
    private CharSequence mTextOn;
    private TextPaint mTextPaint;
    private Drawable mThumbDrawable;
    private float mThumbPosition;
    private int mThumbTextPadding;
    private int mThumbWidth;
    private int mTouchMode;
    private int mTouchSlop;
    private float mTouchX;
    private float mTouchY;
    private Drawable mTrackDrawable;
    private VelocityTracker mVelocityTracker;

    public Switch(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.switchStyle);
    }

    public Switch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mVelocityTracker = VelocityTracker.obtain();
        this.mTempRect = new Rect();
        this.mTextPaint = new TextPaint(1);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        this.mTextPaint.density = dm.density;
        this.mThumbDrawable = res.getDrawable(R.drawable.switch_inner_holo_dark);
        this.mTrackDrawable = res.getDrawable(R.drawable.switch_track_holo_dark);
        this.mTextOn = res.getString(R.string.capital_on);
        this.mTextOff = res.getString(R.string.capital_off);
        this.mThumbTextPadding = res.getDimensionPixelSize(R.dimen.thumb_text_padding);
        this.mSwitchMinWidth = res.getDimensionPixelSize(R.dimen.switch_min_width);
        this.mSwitchTextMaxWidth = res.getDimensionPixelSize(R.dimen.switch_text_max_width);
        this.mSwitchPadding = res.getDimensionPixelSize(R.dimen.switch_padding);
        setSwitchTextAppearance(context, 16974081);
        ViewConfiguration config = ViewConfiguration.get(context);
        this.mTouchSlop = config.getScaledTouchSlop();
        this.mMinFlingVelocity = config.getScaledMinimumFlingVelocity();
        refreshDrawableState();
        setChecked(isChecked());
    }

    public void setSwitchTextAppearance(Context context, int resid) {
        Resources res = getResources();
        this.mTextColors = getTextColors();
        int ts = res.getDimensionPixelSize(R.dimen.thumb_text_size);
        if (((float) ts) != this.mTextPaint.getTextSize()) {
            this.mTextPaint.setTextSize((float) ts);
            requestLayout();
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOnLayout == null) {
            this.mOnLayout = makeLayout(this.mTextOn, this.mSwitchTextMaxWidth);
        }
        if (this.mOffLayout == null) {
            this.mOffLayout = makeLayout(this.mTextOff, this.mSwitchTextMaxWidth);
        }
        this.mTrackDrawable.getPadding(this.mTempRect);
        int maxTextWidth = Math.min(this.mSwitchTextMaxWidth, Math.max(this.mOnLayout.getWidth(), this.mOffLayout.getWidth()));
        int switchWidth = Math.max(this.mSwitchMinWidth, (((maxTextWidth * 2) + (this.mThumbTextPadding * 4)) + this.mTempRect.left) + this.mTempRect.right);
        int switchHeight = this.mTrackDrawable.getIntrinsicHeight();
        this.mThumbWidth = (this.mThumbTextPadding * 2) + maxTextWidth;
        this.mSwitchWidth = switchWidth;
        this.mSwitchHeight = switchHeight;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();
        if (measuredHeight < switchHeight) {
            setMeasuredDimension(measuredWidth, switchHeight);
        }
    }

    @TargetApi(14)
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        CharSequence text = (isChecked() ? this.mOnLayout : this.mOffLayout).getText();
        if (!TextUtils.isEmpty(text)) {
            event.getText().add(text);
        }
    }

    private Layout makeLayout(CharSequence text, int maxWidth) {
        CharSequence charSequence = text;
        int actual_width = (int) Math.ceil((double) Layout.getDesiredWidth(charSequence, this.mTextPaint));
        return new StaticLayout(charSequence, 0, text.length(), this.mTextPaint, actual_width, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true, TruncateAt.END, Math.min(actual_width, maxWidth));
    }

    private boolean hitThumb(float x, float y) {
        this.mThumbDrawable.getPadding(this.mTempRect);
        int thumbLeft = (this.mSwitchLeft + ((int) (this.mThumbPosition + 0.5f))) - this.mTouchSlop;
        return x > ((float) thumbLeft) && x < ((float) ((((this.mThumbWidth + thumbLeft) + this.mTempRect.left) + this.mTempRect.right) + this.mTouchSlop)) && y > ((float) (this.mSwitchTop - this.mTouchSlop)) && y < ((float) (this.mSwitchBottom + this.mTouchSlop));
    }

    public boolean onTouchEvent(MotionEvent ev) {
        this.mVelocityTracker.addMovement(ev);
        float x;
        float y;
        switch (ev.getActionMasked()) {
            case 0:
                x = ev.getX();
                y = ev.getY();
                if (isEnabled() && hitThumb(x, y)) {
                    this.mTouchMode = 1;
                    this.mTouchX = x;
                    this.mTouchY = y;
                    break;
                }
            case 1:
            case 3:
                if (this.mTouchMode != 2) {
                    this.mTouchMode = 0;
                    this.mVelocityTracker.clear();
                    break;
                }
                stopDrag(ev);
                return true;
            case 2:
                float y2;
                switch (this.mTouchMode) {
                    case 1:
                        y = ev.getX();
                        y2 = ev.getY();
                        if (Math.abs(y - this.mTouchX) > ((float) this.mTouchSlop) || Math.abs(y2 - this.mTouchY) > ((float) this.mTouchSlop)) {
                            this.mTouchMode = 2;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            this.mTouchX = y;
                            this.mTouchY = y2;
                            return true;
                        }
                    case 2:
                        x = ev.getX();
                        y2 = Math.max(0.0f, Math.min(this.mThumbPosition + (x - this.mTouchX), (float) getThumbScrollRange()));
                        if (y2 != this.mThumbPosition) {
                            this.mThumbPosition = y2;
                            this.mTouchX = x;
                            invalidate();
                        }
                        return true;
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private void cancelSuperTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(3);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    private void stopDrag(MotionEvent ev) {
        boolean newState = false;
        this.mTouchMode = 0;
        boolean commitChange = ev.getAction() == 1 && isEnabled();
        cancelSuperTouch(ev);
        if (commitChange) {
            this.mVelocityTracker.computeCurrentVelocity(1000);
            float xvel = this.mVelocityTracker.getXVelocity();
            if (Math.abs(xvel) <= ((float) this.mMinFlingVelocity)) {
                newState = getTargetCheckedState();
            } else if (xvel > 0.0f) {
                newState = true;
            }
            animateThumbToCheckedState(newState);
            return;
        }
        animateThumbToCheckedState(isChecked());
    }

    private void animateThumbToCheckedState(boolean newCheckedState) {
        setChecked(newCheckedState);
    }

    private boolean getTargetCheckedState() {
        return this.mThumbPosition >= ((float) (getThumbScrollRange() / 2));
    }

    private void setThumbPosition(boolean checked) {
        this.mThumbPosition = checked ? (float) getThumbScrollRange() : 0.0f;
    }

    public void setChecked(boolean checked) {
        super.setChecked(checked);
        setThumbPosition(checked);
        invalidate();
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int switchTop;
        super.onLayout(changed, left, top, right, bottom);
        setThumbPosition(isChecked());
        int switchRight = getWidth() - getPaddingRight();
        int switchLeft = switchRight - this.mSwitchWidth;
        int gravity = getGravity() & 112;
        if (gravity == 16) {
            switchTop = (((getPaddingTop() + getHeight()) - getPaddingBottom()) / 2) - (this.mSwitchHeight / 2);
            gravity = this.mSwitchHeight + switchTop;
        } else if (gravity != 80) {
            switchTop = getPaddingTop();
            gravity = this.mSwitchHeight + switchTop;
        } else {
            gravity = getHeight() - getPaddingBottom();
            switchTop = gravity - this.mSwitchHeight;
        }
        this.mSwitchLeft = switchLeft;
        this.mSwitchTop = switchTop;
        this.mSwitchBottom = gravity;
        this.mSwitchRight = switchRight;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        int switchLeft = this.mSwitchLeft;
        int switchTop = this.mSwitchTop;
        int switchRight = this.mSwitchRight;
        int switchBottom = this.mSwitchBottom;
        this.mTrackDrawable.setBounds(switchLeft, switchTop, switchRight, switchBottom);
        this.mTrackDrawable.draw(canvas2);
        canvas.save();
        this.mTrackDrawable.getPadding(this.mTempRect);
        int switchInnerLeft = this.mTempRect.left + switchLeft;
        int switchInnerTop = this.mTempRect.top + switchTop;
        int switchInnerBottom = switchBottom - this.mTempRect.bottom;
        canvas2.clipRect(switchInnerLeft, switchTop, switchRight - this.mTempRect.right, switchBottom);
        this.mThumbDrawable.getPadding(this.mTempRect);
        int thumbPos = (int) (this.mThumbPosition + 1056964608);
        int thumbLeft = (switchInnerLeft - this.mTempRect.left) + thumbPos;
        int thumbRight = ((switchInnerLeft + thumbPos) + this.mThumbWidth) + this.mTempRect.right;
        this.mThumbDrawable.setBounds(thumbLeft, switchTop, thumbRight, switchBottom);
        this.mThumbDrawable.draw(canvas2);
        if (this.mTextColors != null) {
            this.mTextPaint.setColor(this.mTextColors.getColorForState(getDrawableState(), this.mTextColors.getDefaultColor()));
        }
        this.mTextPaint.drawableState = getDrawableState();
        Layout switchText = getTargetCheckedState() ? this.mOnLayout : this.mOffLayout;
        canvas2.translate((float) (((thumbLeft + thumbRight) / 2) - (switchText.getEllipsizedWidth() / 2)), (float) (((switchInnerTop + switchInnerBottom) / 2) - (switchText.getHeight() / 2)));
        switchText.draw(canvas2);
        canvas.restore();
    }

    public int getCompoundPaddingRight() {
        int padding = super.getCompoundPaddingRight() + this.mSwitchWidth;
        if (TextUtils.isEmpty(getText())) {
            return padding;
        }
        return padding + this.mSwitchPadding;
    }

    private int getThumbScrollRange() {
        if (this.mTrackDrawable == null) {
            return 0;
        }
        this.mTrackDrawable.getPadding(this.mTempRect);
        return ((this.mSwitchWidth - this.mThumbWidth) - this.mTempRect.left) - this.mTempRect.right;
    }

    /* Access modifiers changed, original: protected */
    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    /* Access modifiers changed, original: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        int[] myDrawableState = getDrawableState();
        if (this.mThumbDrawable != null) {
            this.mThumbDrawable.setState(myDrawableState);
        }
        if (this.mTrackDrawable != null) {
            this.mTrackDrawable.setState(myDrawableState);
        }
        invalidate();
    }

    /* Access modifiers changed, original: protected */
    public boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mThumbDrawable || who == this.mTrackDrawable;
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        this.mThumbDrawable.jumpToCurrentState();
        this.mTrackDrawable.jumpToCurrentState();
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(Switch.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Switch.class.getName());
        CharSequence switchText = isChecked() ? this.mTextOn : this.mTextOff;
        if (!TextUtils.isEmpty(switchText)) {
            CharSequence oldText = info.getText();
            if (TextUtils.isEmpty(oldText)) {
                info.setText(switchText);
                return;
            }
            StringBuilder newText = new StringBuilder();
            newText.append(oldText);
            newText.append(' ');
            newText.append(switchText);
            info.setText(newText);
        }
    }
}
