package com.hmdglobal.app.camera.widget;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ui.Lockable;
import com.hmdglobal.app.camera.ui.ManualItem;
import com.hmdglobal.app.camera.util.LockUtils;
import com.hmdglobal.app.camera.util.LockUtils.Lock;
import com.hmdglobal.app.camera.util.LockUtils.LockType;

public class FloatingActionsMenu extends ViewGroup implements Lockable {
    private static final int ANIMATION_DURATION = 300;
    private static final float COLLAPSED_PLUS_ROTATION = 180.0f;
    private static final float EXPANDED_PLUS_ROTATION = 0.0f;
    private static Interpolator sAlphaExpandInterpolator = new DecelerateInterpolator();
    private static Interpolator sCollapseInterpolator = new DecelerateInterpolator(3.0f);
    private static Interpolator sExpandInterpolator = new OvershootInterpolator();
    private AnimatorUpdateListener animatorUpdateListener;
    private boolean isPortrait;
    public AddFloatingActionButton mAddButton;
    private int mAddButtonColorNormal;
    private int mAddButtonColorPressed;
    private LinearLayout mAddButtonLayout;
    private int mAddButtonPlusColor;
    private int mButtonSpacing;
    private AnimatorSet mCollapseAnimation;
    private AnimatorSet mExpandAnimation;
    public boolean mExpanded;
    private int mIconId;
    private boolean mIsFirstUseManual;
    private ManualItem mItemF;
    private ManualItem mItemISO;
    private ManualItem mItemS;
    private ManualItem mItemWb;
    private boolean mLayoutDone;
    private ManualMenuExpandChangeListener mMenuExpandChangeListener;
    private boolean mMenuOnTop;
    private Lock mMultiLock;
    private RotatingDrawable mRotatingDrawable;

    private class LayoutParams extends android.view.ViewGroup.LayoutParams {
        private ObjectAnimator mCollapseAlpha = new ObjectAnimator();
        private ObjectAnimator mCollapseX = new ObjectAnimator();
        private ObjectAnimator mCollapseY = new ObjectAnimator();
        private ObjectAnimator mExpandAlpha = new ObjectAnimator();
        private ObjectAnimator mExpandX = new ObjectAnimator();
        private ObjectAnimator mExpandY = new ObjectAnimator();

        public LayoutParams(android.view.ViewGroup.LayoutParams source) {
            super(source);
            this.mExpandY.setInterpolator(FloatingActionsMenu.sExpandInterpolator);
            this.mExpandX.setInterpolator(FloatingActionsMenu.sExpandInterpolator);
            this.mExpandAlpha.setInterpolator(FloatingActionsMenu.sAlphaExpandInterpolator);
            this.mCollapseY.setInterpolator(FloatingActionsMenu.sCollapseInterpolator);
            this.mCollapseX.setInterpolator(FloatingActionsMenu.sCollapseInterpolator);
            this.mCollapseAlpha.setInterpolator(FloatingActionsMenu.sCollapseInterpolator);
            this.mCollapseAlpha.setProperty(View.ALPHA);
            this.mCollapseAlpha.setFloatValues(new float[]{1.0f, 0.0f});
            this.mExpandAlpha.setProperty(View.ALPHA);
            this.mExpandAlpha.setFloatValues(new float[]{0.0f, 1.0f});
            this.mCollapseY.setProperty(View.TRANSLATION_Y);
            this.mCollapseX.setProperty(View.TRANSLATION_X);
            this.mExpandY.setProperty(View.TRANSLATION_Y);
            this.mExpandX.setProperty(View.TRANSLATION_X);
            FloatingActionsMenu.this.mExpandAnimation.play(this.mExpandAlpha);
            if (FloatingActionsMenu.this.isPortrait) {
                FloatingActionsMenu.this.mExpandAnimation.play(this.mExpandY);
            } else {
                FloatingActionsMenu.this.mExpandAnimation.play(this.mExpandX);
            }
            FloatingActionsMenu.this.mCollapseAnimation.play(this.mCollapseAlpha);
            if (FloatingActionsMenu.this.isPortrait) {
                FloatingActionsMenu.this.mCollapseAnimation.play(this.mCollapseY);
            } else {
                FloatingActionsMenu.this.mCollapseAnimation.play(this.mCollapseX);
            }
        }

        public void setAnimationsTarget(View view) {
            this.mCollapseAlpha.setTarget(view);
            this.mCollapseY.setTarget(view);
            this.mCollapseX.setTarget(view);
            this.mExpandAlpha.setTarget(view);
            this.mExpandY.setTarget(view);
            this.mExpandX.setTarget(view);
        }
    }

    public interface ManualMenuExpandChangeListener {
        void onManualMenuClick();

        void onManualMenuExpandChanged(boolean z);
    }

    private static class RotatingDrawable extends LayerDrawable {
        private float mRotation = FloatingActionsMenu.COLLAPSED_PLUS_ROTATION;

        public RotatingDrawable(Drawable drawable) {
            super(new Drawable[]{drawable});
        }

        public float getRotation() {
            return this.mRotation;
        }

        public void setRotation(float rotation) {
            this.mRotation = rotation;
            invalidateSelf();
        }

        public void draw(Canvas canvas) {
            canvas.save();
            canvas.rotate(this.mRotation, (float) getBounds().centerX(), (float) getBounds().centerY());
            super.draw(canvas);
            canvas.restore();
        }
    }

    public static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public boolean mExpanded;

        /* synthetic */ SavedState(Parcel x0, AnonymousClass1 x1) {
            this(x0);
        }

        public SavedState(Parcelable parcel) {
            super(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            boolean z = true;
            if (in.readInt() != 1) {
                z = false;
            }
            this.mExpanded = z;
        }

        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.mExpanded);
        }
    }

    public void toggleForTutorial() {
        if (this.mIsFirstUseManual && this.mMenuExpandChangeListener != null && this.mExpanded) {
            this.mMenuExpandChangeListener.onManualMenuExpandChanged(this.mExpanded);
        }
    }

    public void manualAddButtonClick() {
        if (this.mAddButton != null) {
            this.mAddButton.performClick();
        }
    }

    public FloatingActionsMenu(Context context) {
        this(context, null);
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mExpanded = true;
        this.mLayoutDone = false;
        this.isPortrait = true;
        this.mExpandAnimation = new AnimatorSet().setDuration(300);
        this.mCollapseAnimation = new AnimatorSet().setDuration(300);
        this.mIconId = R.drawable.ic_manual_settings;
        this.mIsFirstUseManual = false;
        this.mMenuOnTop = false;
        this.mMultiLock = LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK);
        init(context, attrs);
    }

    public FloatingActionsMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mExpanded = true;
        this.mLayoutDone = false;
        this.isPortrait = true;
        this.mExpandAnimation = new AnimatorSet().setDuration(300);
        this.mCollapseAnimation = new AnimatorSet().setDuration(300);
        this.mIconId = R.drawable.ic_manual_settings;
        this.mIsFirstUseManual = false;
        this.mMenuOnTop = false;
        this.mMultiLock = LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK);
        init(context, attrs);
    }

    public void setMenuExpandChangeListener(ManualMenuExpandChangeListener listener, boolean isFirstUseManual) {
        this.mMenuExpandChangeListener = listener;
        this.mIsFirstUseManual = isFirstUseManual;
    }

    private void init(Context context, AttributeSet attributeSet) {
        this.mAddButtonPlusColor = getColor(17170443);
        this.mAddButtonColorNormal = getColor(17170451);
        this.mAddButtonColorPressed = getColor(17170450);
        this.mButtonSpacing = (int) ((getResources().getDimension(R.dimen.fab_actions_spacing) - getResources().getDimension(R.dimen.fab_shadow_radius)) - getResources().getDimension(R.dimen.fab_shadow_offset));
        if (attributeSet != null) {
            TypedArray attr = context.obtainStyledAttributes(attributeSet, R.styleable.FloatingActionsMenu, 0, 0);
            if (attr != null) {
                try {
                    this.mAddButtonPlusColor = attr.getColor(4, getColor(17170443));
                    this.mAddButtonColorNormal = attr.getColor(0, getColor(17170451));
                    this.mAddButtonColorPressed = attr.getColor(1, getColor(17170450));
                } finally {
                    attr.recycle();
                }
            }
        }
        createAddButton(context);
    }

    private void createAddButton(Context context) {
        this.mAddButton = new AddFloatingActionButton(context) {
            /* Access modifiers changed, original: 0000 */
            public void updateBackground() {
                this.mPlusColor = FloatingActionsMenu.this.mAddButtonPlusColor;
                this.mColorNormal = FloatingActionsMenu.this.mAddButtonColorNormal;
                this.mColorPressed = FloatingActionsMenu.this.mAddButtonColorPressed;
                super.updateBackground();
            }

            /* Access modifiers changed, original: 0000 */
            /* JADX WARNING: Incorrect type for fill-array insn 0x0035, element type: float, insn element type: null */
            public android.graphics.drawable.Drawable getIconDrawable() {
                /*
                r5 = this;
                r0 = new com.hmdglobal.app.camera.widget.FloatingActionsMenu$RotatingDrawable;
                r1 = r5.getResources();
                r2 = com.hmdglobal.app.camera.widget.FloatingActionsMenu.this;
                r2 = r2.mIconId;
                r1 = r1.getDrawable(r2);
                r0.<init>(r1);
                r1 = com.hmdglobal.app.camera.widget.FloatingActionsMenu.this;
                r1.mRotatingDrawable = r0;
                r1 = new android.view.animation.OvershootInterpolator;
                r1.<init>();
                r2 = com.hmdglobal.app.camera.widget.FloatingActionsMenu.this;
                r2 = r2.mMenuOnTop;
                r3 = 2;
                if (r2 == 0) goto L_0x003d;
            L_0x0026:
                r2 = "rotation";
                r4 = new float[r3];
                r4 = {0, 1127481344};
                r2 = android.animation.ObjectAnimator.ofFloat(r0, r2, r4);
                r4 = "rotation";
                r3 = new float[r3];
                r3 = {1127481344, 0};
                r3 = android.animation.ObjectAnimator.ofFloat(r0, r4, r3);
                goto L_0x0053;
            L_0x003d:
                r2 = "rotation";
                r4 = new float[r3];
                r4 = {1127481344, 0};
                r2 = android.animation.ObjectAnimator.ofFloat(r0, r2, r4);
                r4 = "rotation";
                r3 = new float[r3];
                r3 = {0, 1127481344};
                r3 = android.animation.ObjectAnimator.ofFloat(r0, r4, r3);
            L_0x0053:
                r2.setInterpolator(r1);
                r3.setInterpolator(r1);
                r4 = com.hmdglobal.app.camera.widget.FloatingActionsMenu.this;
                r4 = r4.mExpandAnimation;
                r4.play(r3);
                r4 = com.hmdglobal.app.camera.widget.FloatingActionsMenu.this;
                r4 = r4.mCollapseAnimation;
                r4.play(r2);
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.widget.FloatingActionsMenu$AnonymousClass1.getIconDrawable():android.graphics.drawable.Drawable");
            }
        };
        this.mAddButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FloatingActionsMenu.this.toggle();
            }
        });
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(-2, -2);
        lp.setMarginEnd((getDimension(R.dimen.manual_item_height) / 2) - (getDimension(R.dimen.fab_icon_size) / 2));
        this.mAddButton.setLayoutParams(lp);
        this.mAddButtonLayout = new LinearLayout(context);
        this.mAddButtonLayout.setGravity(GravityCompat.END);
        this.mAddButtonLayout.addView(this.mAddButton);
        addView(this.mAddButtonLayout, super.generateDefaultLayoutParams());
    }

    /* Access modifiers changed, original: 0000 */
    public int getDimension(@DimenRes int id) {
        return (int) getResources().getDimension(id);
    }

    private int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        int i;
        int childrenCount;
        int menuSpacing;
        int childrenTotalHeight = 0;
        int childrenCount2 = getChildCount();
        for (i = childrenCount2 - 1; i >= 0; i--) {
            childrenTotalHeight += getChildAt(i).getMeasuredHeight();
        }
        this.mButtonSpacing = getDimension(R.dimen.fab_icon_margin);
        int i2 = 2;
        i = ((getMeasuredHeight() - childrenTotalHeight) - (this.mButtonSpacing * 3)) / 2;
        if (i <= 0) {
            this.mButtonSpacing = (getMeasuredHeight() - childrenTotalHeight) / 4;
            i = this.mButtonSpacing / 2;
        }
        int addButtonHeight = this.mAddButton.getMeasuredHeight();
        int addButtonY = 0;
        int bottomX = r - l;
        int bottomY = 0 + addButtonHeight;
        int i3 = 0;
        if (this.mMenuOnTop) {
            this.mAddButtonLayout.layout(0, 0, bottomX, addButtonHeight);
        } else {
            bottomY = 0;
            addButtonY = getMeasuredHeight() - addButtonHeight;
            this.mAddButtonLayout.layout(0, addButtonY, bottomX, getMeasuredHeight());
        }
        bottomY += i;
        int i4 = getChildCount() - 1;
        while (i4 >= 0) {
            int childrenTotalHeight2;
            int i5;
            View child = getChildAt(i4);
            if (child == this.mAddButtonLayout) {
                childrenTotalHeight2 = childrenTotalHeight;
                childrenCount = childrenCount2;
                menuSpacing = i;
                childrenCount2 = i2;
                i5 = i3;
            } else {
                int childY = bottomY;
                child.layout(0, childY, bottomX, childY + child.getMeasuredHeight());
                float collapsedTranslation = (float) (addButtonY - childY);
                child.setTranslationY(this.mExpanded ? 0.0f : collapsedTranslation);
                child.setAlpha(this.mExpanded ? 1.0f : 0.0f);
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                childrenTotalHeight2 = childrenTotalHeight;
                childrenTotalHeight = params.mCollapseY;
                childrenCount = childrenCount2;
                menuSpacing = i;
                childrenCount2 = 2;
                r3 = new float[2];
                i5 = 0;
                r3[0] = 0.0f;
                r3[1] = collapsedTranslation;
                childrenTotalHeight.setFloatValues(r3);
                params.mExpandY.setFloatValues(new float[]{collapsedTranslation, 0.0f});
                params.setAnimationsTarget(child);
                bottomY = (this.mButtonSpacing + childY) + child.getMeasuredHeight();
            }
            i4--;
            i2 = childrenCount2;
            i3 = i5;
            childrenTotalHeight = childrenTotalHeight2;
            childrenCount2 = childrenCount;
            i = menuSpacing;
        }
        childrenCount = childrenCount2;
        menuSpacing = i;
        this.mLayoutDone = true;
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public android.view.ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(super.generateDefaultLayoutParams());
    }

    public android.view.ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(super.generateLayoutParams(attrs));
    }

    /* Access modifiers changed, original: protected */
    public android.view.ViewGroup.LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(super.generateLayoutParams(p));
    }

    /* Access modifiers changed, original: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        bringChildToFront(this.mAddButtonLayout);
        this.mItemISO = (ManualItem) findViewById(R.id.item_iso);
        this.mItemS = (ManualItem) findViewById(R.id.item_s);
        this.mItemWb = (ManualItem) findViewById(R.id.item_wb);
        this.mItemF = (ManualItem) findViewById(R.id.item_f);
    }

    public void collapse() {
        if (this.mExpanded && this.mLayoutDone) {
            this.mExpanded = false;
            this.mItemISO.resetView();
            this.mItemS.resetView();
            this.mItemWb.resetView();
            this.mItemF.resetView();
            this.mCollapseAnimation.start();
            this.mExpandAnimation.cancel();
        }
    }

    public void setIcon(int icon) {
        this.mIconId = icon;
        this.mAddButton.updateBackground();
    }

    public void toggle() {
        if (this.mExpanded) {
            collapse();
            if (this.mIsFirstUseManual && this.mMenuExpandChangeListener != null) {
                this.mMenuExpandChangeListener.onManualMenuClick();
                return;
            }
            return;
        }
        post(new Runnable() {
            public void run() {
                FloatingActionsMenu.this.expand();
            }
        });
    }

    public void expand() {
        if (!this.mExpanded) {
            this.mExpanded = true;
            this.mCollapseAnimation.cancel();
            this.mExpandAnimation.start();
        }
    }

    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mExpanded = this.mExpanded;
        return savedState;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            this.mExpanded = savedState.mExpanded;
            if (this.mRotatingDrawable != null) {
                float f;
                RotatingDrawable rotatingDrawable = this.mRotatingDrawable;
                if (this.mExpanded) {
                    f = 0.0f;
                } else {
                    f = COLLAPSED_PLUS_ROTATION;
                }
                rotatingDrawable.setRotation(f);
            }
            super.onRestoreInstanceState(savedState.getSuperState());
            return;
        }
        super.onRestoreInstanceState(state);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (isLocked()) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void lockSelf() {
        this.mMultiLock.aquireLock(hashCode());
    }

    public int lock() {
        return this.mMultiLock.aquireLock().intValue();
    }

    public boolean unlockWithToken(int token) {
        return this.mMultiLock.unlockWithToken(Integer.valueOf(token));
    }

    public void unLockSelf() {
        this.mMultiLock.unlockWithToken(Integer.valueOf(hashCode()));
    }

    public boolean isLocked() {
        return this.mMultiLock.isLocked();
    }
}
