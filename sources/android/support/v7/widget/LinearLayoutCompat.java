package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.common.primitives.Ints;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LinearLayoutCompat extends ViewGroup {
    public static final int HORIZONTAL = 0;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_FILL = 3;
    private static final int INDEX_TOP = 1;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_END = 4;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int VERTICAL = 1;
    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private boolean mBaselineAligned;
    private int mBaselineAlignedChildIndex;
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;
    private int mGravity;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    private int mOrientation;
    private int mShowDividers;
    private int mTotalLength;
    private boolean mUseLargestChild;
    private float mWeightSum;

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DividerMode {
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity;
        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.LinearLayoutCompat_Layout);
            this.weight = a.getFloat(R.styleable.LinearLayoutCompat_Layout_android_layout_weight, 0.0f);
            this.gravity = a.getInt(R.styleable.LinearLayoutCompat_Layout_android_layout_gravity, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = -1;
            this.weight = 0.0f;
        }

        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.gravity = -1;
            this.weight = weight;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams p) {
            super(p);
            this.gravity = -1;
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            this.gravity = -1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = -1;
            this.weight = source.weight;
            this.gravity = source.gravity;
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public LinearLayoutCompat(Context context) {
        this(context, null);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.LinearLayoutCompat, defStyleAttr, 0);
        int index = a.getInt(R.styleable.LinearLayoutCompat_android_orientation, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        index = a.getInt(R.styleable.LinearLayoutCompat_android_gravity, -1);
        if (index >= 0) {
            setGravity(index);
        }
        boolean baselineAligned = a.getBoolean(R.styleable.LinearLayoutCompat_android_baselineAligned, true);
        if (!baselineAligned) {
            setBaselineAligned(baselineAligned);
        }
        this.mWeightSum = a.getFloat(R.styleable.LinearLayoutCompat_android_weightSum, -1.0f);
        this.mBaselineAlignedChildIndex = a.getInt(R.styleable.LinearLayoutCompat_android_baselineAlignedChildIndex, -1);
        this.mUseLargestChild = a.getBoolean(R.styleable.LinearLayoutCompat_measureWithLargestChild, false);
        setDividerDrawable(a.getDrawable(R.styleable.LinearLayoutCompat_divider));
        this.mShowDividers = a.getInt(R.styleable.LinearLayoutCompat_showDividers, 0);
        this.mDividerPadding = a.getDimensionPixelSize(R.styleable.LinearLayoutCompat_dividerPadding, 0);
        a.recycle();
    }

    public void setShowDividers(int showDividers) {
        if (showDividers != this.mShowDividers) {
            requestLayout();
        }
        this.mShowDividers = showDividers;
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public int getShowDividers() {
        return this.mShowDividers;
    }

    public Drawable getDividerDrawable() {
        return this.mDivider;
    }

    public void setDividerDrawable(Drawable divider) {
        if (divider != this.mDivider) {
            this.mDivider = divider;
            boolean z = false;
            if (divider != null) {
                this.mDividerWidth = divider.getIntrinsicWidth();
                this.mDividerHeight = divider.getIntrinsicHeight();
            } else {
                this.mDividerWidth = 0;
                this.mDividerHeight = 0;
            }
            if (divider == null) {
                z = true;
            }
            setWillNotDraw(z);
            requestLayout();
        }
    }

    public void setDividerPadding(int padding) {
        this.mDividerPadding = padding;
    }

    public int getDividerPadding() {
        return this.mDividerPadding;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int getDividerWidth() {
        return this.mDividerWidth;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (this.mDivider != null) {
            if (this.mOrientation == 1) {
                drawDividersVertical(canvas);
            } else {
                drawDividersHorizontal(canvas);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drawDividersVertical(Canvas canvas) {
        int count = getVirtualChildCount();
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                drawHorizontalDivider(canvas, (child.getTop() - ((LayoutParams) child.getLayoutParams()).topMargin) - this.mDividerHeight);
            }
            i++;
        }
        if (hasDividerBeforeChildAt(count)) {
            int bottom;
            View child2 = getVirtualChildAt(count - 1);
            if (child2 == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                bottom = child2.getBottom() + ((LayoutParams) child2.getLayoutParams()).bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drawDividersHorizontal(Canvas canvas) {
        int count = getVirtualChildCount();
        boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        int i = 0;
        while (i < count) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                int position;
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isLayoutRtl) {
                    position = child.getRight() + lp.rightMargin;
                } else {
                    position = (child.getLeft() - lp.leftMargin) - this.mDividerWidth;
                }
                drawVerticalDivider(canvas, position);
            }
            i++;
        }
        if (hasDividerBeforeChildAt(count)) {
            int position2;
            View child2 = getVirtualChildAt(count - 1);
            if (child2 != null) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (isLayoutRtl) {
                    position2 = (child2.getLeft() - lp2.leftMargin) - this.mDividerWidth;
                } else {
                    position2 = child2.getRight() + lp2.rightMargin;
                }
            } else if (isLayoutRtl) {
                position2 = getPaddingLeft();
            } else {
                position2 = (getWidth() - getPaddingRight()) - this.mDividerWidth;
            }
            drawVerticalDivider(canvas, position2);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void drawHorizontalDivider(Canvas canvas, int top) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, top, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + top);
        this.mDivider.draw(canvas);
    }

    /* Access modifiers changed, original: 0000 */
    public void drawVerticalDivider(Canvas canvas, int left) {
        this.mDivider.setBounds(left, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + left, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    public boolean isBaselineAligned() {
        return this.mBaselineAligned;
    }

    public void setBaselineAligned(boolean baselineAligned) {
        this.mBaselineAligned = baselineAligned;
    }

    public boolean isMeasureWithLargestChildEnabled() {
        return this.mUseLargestChild;
    }

    public void setMeasureWithLargestChildEnabled(boolean enabled) {
        this.mUseLargestChild = enabled;
    }

    public int getBaseline() {
        if (this.mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        if (getChildCount() > this.mBaselineAlignedChildIndex) {
            View child = getChildAt(this.mBaselineAlignedChildIndex);
            int childBaseline = child.getBaseline();
            if (childBaseline != -1) {
                int childTop = this.mBaselineChildTop;
                if (this.mOrientation == 1) {
                    int majorGravity = this.mGravity & 112;
                    if (majorGravity != 48) {
                        if (majorGravity == 16) {
                            childTop += ((((getBottom() - getTop()) - getPaddingTop()) - getPaddingBottom()) - this.mTotalLength) / 2;
                        } else if (majorGravity == 80) {
                            childTop = ((getBottom() - getTop()) - getPaddingBottom()) - this.mTotalLength;
                        }
                    }
                }
                return (((LayoutParams) child.getLayoutParams()).topMargin + childTop) + childBaseline;
            } else if (this.mBaselineAlignedChildIndex == 0) {
                return -1;
            } else {
                throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
            }
        }
        throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
    }

    public int getBaselineAlignedChildIndex() {
        return this.mBaselineAlignedChildIndex;
    }

    public void setBaselineAlignedChildIndex(int i) {
        if (i < 0 || i >= getChildCount()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("base aligned child index out of range (0, ");
            stringBuilder.append(getChildCount());
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.mBaselineAlignedChildIndex = i;
    }

    /* Access modifiers changed, original: 0000 */
    public View getVirtualChildAt(int index) {
        return getChildAt(index);
    }

    /* Access modifiers changed, original: 0000 */
    public int getVirtualChildCount() {
        return getChildCount();
    }

    public float getWeightSum() {
        return this.mWeightSum;
    }

    public void setWeightSum(float weightSum) {
        this.mWeightSum = Math.max(0.0f, weightSum);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOrientation == 1) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /* Access modifiers changed, original: protected */
    @RestrictTo({Scope.LIBRARY})
    public boolean hasDividerBeforeChildAt(int childIndex) {
        boolean hasVisibleViewBefore = false;
        if (childIndex == 0) {
            if ((this.mShowDividers & 1) != 0) {
                hasVisibleViewBefore = true;
            }
            return hasVisibleViewBefore;
        } else if (childIndex == getChildCount()) {
            if ((this.mShowDividers & 4) != 0) {
                hasVisibleViewBefore = true;
            }
            return hasVisibleViewBefore;
        } else if ((this.mShowDividers & 2) == 0) {
            return false;
        } else {
            hasVisibleViewBefore = false;
            for (int i = childIndex - 1; i >= 0; i--) {
                if (getChildAt(i).getVisibility() != 8) {
                    hasVisibleViewBefore = true;
                    break;
                }
            }
            return hasVisibleViewBefore;
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:157:0x03d1  */
    /* JADX WARNING: Removed duplicated region for block: B:156:0x03ce  */
    /* JADX WARNING: Removed duplicated region for block: B:163:0x03e2  */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x03d8  */
    /* JADX WARNING: Removed duplicated region for block: B:192:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x0457  */
    public void measureVertical(int r58, int r59) {
        /*
        r57 = this;
        r7 = r57;
        r8 = r58;
        r9 = r59;
        r10 = 0;
        r7.mTotalLength = r10;
        r0 = 0;
        r1 = 0;
        r2 = 0;
        r3 = 0;
        r4 = 1;
        r5 = 0;
        r11 = r57.getVirtualChildCount();
        r12 = android.view.View.MeasureSpec.getMode(r58);
        r13 = android.view.View.MeasureSpec.getMode(r59);
        r6 = 0;
        r14 = 0;
        r15 = r7.mBaselineAlignedChildIndex;
        r10 = r7.mUseLargestChild;
        r17 = 0;
        r18 = r6;
        r6 = r2;
        r2 = r0;
        r0 = 0;
        r56 = r4;
        r4 = r3;
        r3 = r17;
        r17 = r56;
    L_0x002f:
        r19 = r4;
        r21 = 1;
        r22 = 0;
        if (r0 >= r11) goto L_0x01bd;
    L_0x0037:
        r4 = r7.getVirtualChildAt(r0);
        if (r4 != 0) goto L_0x0054;
    L_0x003d:
        r25 = r1;
        r1 = r7.mTotalLength;
        r20 = r7.measureNullChild(r0);
        r1 = r1 + r20;
        r7.mTotalLength = r1;
        r34 = r11;
        r33 = r13;
        r4 = r19;
        r1 = r25;
        goto L_0x01b1;
    L_0x0054:
        r25 = r1;
        r1 = r4.getVisibility();
        r26 = r2;
        r2 = 8;
        if (r1 != r2) goto L_0x0072;
    L_0x0060:
        r1 = r7.getChildrenSkipCount(r4, r0);
        r0 = r0 + r1;
        r34 = r11;
        r33 = r13;
        r4 = r19;
        r1 = r25;
        r2 = r26;
        goto L_0x01b1;
    L_0x0072:
        r1 = r7.hasDividerBeforeChildAt(r0);
        if (r1 == 0) goto L_0x007f;
    L_0x0078:
        r1 = r7.mTotalLength;
        r2 = r7.mDividerHeight;
        r1 = r1 + r2;
        r7.mTotalLength = r1;
    L_0x007f:
        r1 = r4.getLayoutParams();
        r2 = r1;
        r2 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r2;
        r1 = r2.weight;
        r23 = r5 + r1;
        r5 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r13 != r5) goto L_0x00bd;
    L_0x008e:
        r1 = r2.height;
        if (r1 != 0) goto L_0x00bd;
    L_0x0092:
        r1 = r2.weight;
        r1 = (r1 > r22 ? 1 : (r1 == r22 ? 0 : -1));
        if (r1 <= 0) goto L_0x00bd;
    L_0x0098:
        r1 = r7.mTotalLength;
        r5 = r2.topMargin;
        r5 = r5 + r1;
        r27 = r0;
        r0 = r2.bottomMargin;
        r5 = r5 + r0;
        r0 = java.lang.Math.max(r1, r5);
        r7.mTotalLength = r0;
        r14 = 1;
        r0 = r2;
        r8 = r4;
        r9 = r6;
        r34 = r11;
        r33 = r13;
        r30 = r14;
        r35 = r19;
        r14 = r25;
        r32 = r26;
        r29 = r27;
        goto L_0x012e;
    L_0x00bd:
        r27 = r0;
        r0 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        r1 = r2.height;
        if (r1 != 0) goto L_0x00cf;
    L_0x00c5:
        r1 = r2.weight;
        r1 = (r1 > r22 ? 1 : (r1 == r22 ? 0 : -1));
        if (r1 <= 0) goto L_0x00cf;
    L_0x00cb:
        r0 = 0;
        r1 = -2;
        r2.height = r1;
    L_0x00cf:
        r5 = r0;
        r24 = 0;
        r0 = (r23 > r22 ? 1 : (r23 == r22 ? 0 : -1));
        if (r0 != 0) goto L_0x00db;
    L_0x00d6:
        r0 = r7.mTotalLength;
        r28 = r0;
        goto L_0x00dd;
    L_0x00db:
        r28 = 0;
    L_0x00dd:
        r1 = r27;
        r0 = r7;
        r29 = r1;
        r30 = r14;
        r14 = r25;
        r1 = r4;
        r31 = r2;
        r32 = r26;
        r2 = r29;
        r33 = r13;
        r13 = r3;
        r3 = r8;
        r8 = r4;
        r34 = r11;
        r35 = r19;
        r11 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        r4 = r24;
        r11 = r5;
        r5 = r9;
        r9 = r6;
        r6 = r28;
        r0.measureChildBeforeLayout(r1, r2, r3, r4, r5, r6);
        r0 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        if (r11 == r0) goto L_0x010b;
    L_0x0106:
        r0 = r31;
        r0.height = r11;
        goto L_0x010d;
    L_0x010b:
        r0 = r31;
    L_0x010d:
        r1 = r8.getMeasuredHeight();
        r2 = r7.mTotalLength;
        r3 = r2 + r1;
        r4 = r0.topMargin;
        r3 = r3 + r4;
        r4 = r0.bottomMargin;
        r3 = r3 + r4;
        r4 = r7.getNextLocationOffset(r8);
        r3 = r3 + r4;
        r3 = java.lang.Math.max(r2, r3);
        r7.mTotalLength = r3;
        if (r10 == 0) goto L_0x012d;
    L_0x0128:
        r3 = java.lang.Math.max(r1, r13);
        goto L_0x012e;
    L_0x012d:
        r3 = r13;
    L_0x012e:
        if (r15 < 0) goto L_0x013b;
    L_0x0130:
        r1 = r29;
        r2 = r1 + 1;
        if (r15 != r2) goto L_0x013d;
    L_0x0136:
        r2 = r7.mTotalLength;
        r7.mBaselineChildTop = r2;
        goto L_0x013d;
    L_0x013b:
        r1 = r29;
    L_0x013d:
        if (r1 >= r15) goto L_0x014e;
    L_0x013f:
        r2 = r0.weight;
        r2 = (r2 > r22 ? 1 : (r2 == r22 ? 0 : -1));
        if (r2 > 0) goto L_0x0146;
    L_0x0145:
        goto L_0x014e;
    L_0x0146:
        r2 = new java.lang.RuntimeException;
        r4 = "A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.";
        r2.<init>(r4);
        throw r2;
    L_0x014e:
        r2 = 0;
        r4 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 == r4) goto L_0x015c;
    L_0x0153:
        r4 = r0.width;
        r6 = -1;
        if (r4 != r6) goto L_0x015d;
    L_0x0158:
        r18 = 1;
        r2 = 1;
        goto L_0x015d;
    L_0x015c:
        r6 = -1;
    L_0x015d:
        r4 = r0.leftMargin;
        r5 = r0.rightMargin;
        r4 = r4 + r5;
        r5 = r8.getMeasuredWidth();
        r5 = r5 + r4;
        r11 = r32;
        r11 = java.lang.Math.max(r11, r5);
        r13 = r8.getMeasuredState();
        r13 = android.view.View.combineMeasuredStates(r14, r13);
        if (r17 == 0) goto L_0x017f;
    L_0x0178:
        r14 = r0.width;
        if (r14 != r6) goto L_0x017f;
    L_0x017c:
        r6 = r21;
        goto L_0x0180;
    L_0x017f:
        r6 = 0;
    L_0x0180:
        r14 = r0.weight;
        r14 = (r14 > r22 ? 1 : (r14 == r22 ? 0 : -1));
        if (r14 <= 0) goto L_0x0194;
    L_0x0186:
        if (r2 == 0) goto L_0x018a;
    L_0x0188:
        r14 = r4;
        goto L_0x018b;
    L_0x018a:
        r14 = r5;
    L_0x018b:
        r36 = r6;
        r6 = r35;
        r6 = java.lang.Math.max(r6, r14);
        goto L_0x01a1;
    L_0x0194:
        r36 = r6;
        r6 = r35;
        if (r2 == 0) goto L_0x019c;
    L_0x019a:
        r14 = r4;
        goto L_0x019d;
    L_0x019c:
        r14 = r5;
    L_0x019d:
        r9 = java.lang.Math.max(r9, r14);
    L_0x01a1:
        r14 = r7.getChildrenSkipCount(r8, r1);
        r0 = r1 + r14;
        r4 = r6;
        r6 = r9;
        r2 = r11;
        r1 = r13;
        r5 = r23;
        r14 = r30;
        r17 = r36;
    L_0x01b1:
        r0 = r0 + 1;
        r13 = r33;
        r11 = r34;
        r8 = r58;
        r9 = r59;
        goto L_0x002f;
    L_0x01bd:
        r9 = r6;
        r34 = r11;
        r33 = r13;
        r30 = r14;
        r6 = r19;
        r0 = -1;
        r14 = r1;
        r11 = r2;
        r13 = r3;
        r1 = r7.mTotalLength;
        if (r1 <= 0) goto L_0x01de;
    L_0x01ce:
        r1 = r34;
        r2 = r7.hasDividerBeforeChildAt(r1);
        if (r2 == 0) goto L_0x01e0;
    L_0x01d6:
        r2 = r7.mTotalLength;
        r3 = r7.mDividerHeight;
        r2 = r2 + r3;
        r7.mTotalLength = r2;
        goto L_0x01e0;
    L_0x01de:
        r1 = r34;
    L_0x01e0:
        if (r10 == 0) goto L_0x0237;
    L_0x01e2:
        r2 = r33;
        r3 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        if (r2 == r3) goto L_0x01ea;
    L_0x01e8:
        if (r2 != 0) goto L_0x0239;
    L_0x01ea:
        r3 = 0;
        r7.mTotalLength = r3;
        r3 = 0;
    L_0x01ee:
        if (r3 >= r1) goto L_0x0239;
    L_0x01f0:
        r4 = r7.getVirtualChildAt(r3);
        if (r4 != 0) goto L_0x0201;
    L_0x01f6:
        r8 = r7.mTotalLength;
        r19 = r7.measureNullChild(r3);
        r8 = r8 + r19;
        r7.mTotalLength = r8;
        goto L_0x020f;
    L_0x0201:
        r8 = r4.getVisibility();
        r0 = 8;
        if (r8 != r0) goto L_0x0212;
    L_0x0209:
        r0 = r7.getChildrenSkipCount(r4, r3);
        r3 = r3 + r0;
    L_0x020f:
        r37 = r3;
        goto L_0x0233;
        r0 = r4.getLayoutParams();
        r0 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r0;
        r8 = r7.mTotalLength;
        r19 = r8 + r13;
        r37 = r3;
        r3 = r0.topMargin;
        r19 = r19 + r3;
        r3 = r0.bottomMargin;
        r19 = r19 + r3;
        r3 = r7.getNextLocationOffset(r4);
        r3 = r19 + r3;
        r3 = java.lang.Math.max(r8, r3);
        r7.mTotalLength = r3;
    L_0x0233:
        r3 = r37 + 1;
        r0 = -1;
        goto L_0x01ee;
    L_0x0237:
        r2 = r33;
    L_0x0239:
        r0 = r7.mTotalLength;
        r3 = r57.getPaddingTop();
        r4 = r57.getPaddingBottom();
        r3 = r3 + r4;
        r0 = r0 + r3;
        r7.mTotalLength = r0;
        r0 = r7.mTotalLength;
        r3 = r57.getSuggestedMinimumHeight();
        r0 = java.lang.Math.max(r0, r3);
        r3 = r59;
        r4 = 0;
        r8 = android.view.View.resolveSizeAndState(r0, r3, r4);
        r4 = 16777215; // 0xffffff float:2.3509886E-38 double:8.2890456E-317;
        r0 = r8 & r4;
        r4 = r7.mTotalLength;
        r4 = r0 - r4;
        if (r30 != 0) goto L_0x02f6;
    L_0x0263:
        if (r4 == 0) goto L_0x0273;
    L_0x0265:
        r19 = (r5 > r22 ? 1 : (r5 == r22 ? 0 : -1));
        if (r19 <= 0) goto L_0x0273;
    L_0x0269:
        r38 = r0;
        r39 = r4;
        r40 = r5;
        r41 = r6;
        goto L_0x02fe;
    L_0x0273:
        r9 = java.lang.Math.max(r9, r6);
        if (r10 == 0) goto L_0x02e0;
    L_0x0279:
        r38 = r0;
        r0 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r2 == r0) goto L_0x02d7;
    L_0x027f:
        r16 = 0;
    L_0x0281:
        r0 = r16;
        if (r0 >= r1) goto L_0x02d7;
    L_0x0285:
        r39 = r4;
        r4 = r7.getVirtualChildAt(r0);
        if (r4 == 0) goto L_0x02c6;
    L_0x028d:
        r40 = r5;
        r5 = r4.getVisibility();
        r41 = r6;
        r6 = 8;
        if (r5 != r6) goto L_0x029d;
        r44 = r9;
        goto L_0x02cc;
        r5 = r4.getLayoutParams();
        r5 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r5;
        r6 = r5.weight;
        r16 = (r6 > r22 ? 1 : (r6 == r22 ? 0 : -1));
        if (r16 <= 0) goto L_0x02c3;
        r42 = r5;
        r5 = r4.getMeasuredWidth();
        r43 = r6;
        r6 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r5 = android.view.View.MeasureSpec.makeMeasureSpec(r5, r6);
        r44 = r9;
        r9 = android.view.View.MeasureSpec.makeMeasureSpec(r13, r6);
        r4.measure(r5, r9);
        goto L_0x02cc;
    L_0x02c3:
        r44 = r9;
        goto L_0x02cc;
    L_0x02c6:
        r40 = r5;
        r41 = r6;
        r44 = r9;
    L_0x02cc:
        r16 = r0 + 1;
        r4 = r39;
        r5 = r40;
        r6 = r41;
        r9 = r44;
        goto L_0x0281;
    L_0x02d7:
        r39 = r4;
        r40 = r5;
        r41 = r6;
        r44 = r9;
        goto L_0x02ea;
    L_0x02e0:
        r38 = r0;
        r39 = r4;
        r40 = r5;
        r41 = r6;
        r44 = r9;
    L_0x02ea:
        r51 = r2;
        r45 = r10;
        r46 = r13;
        r47 = r15;
        r13 = r58;
        goto L_0x0434;
    L_0x02f6:
        r38 = r0;
        r39 = r4;
        r40 = r5;
        r41 = r6;
    L_0x02fe:
        r0 = r7.mWeightSum;
        r0 = (r0 > r22 ? 1 : (r0 == r22 ? 0 : -1));
        if (r0 <= 0) goto L_0x0307;
    L_0x0304:
        r5 = r7.mWeightSum;
        goto L_0x0309;
    L_0x0307:
        r5 = r40;
    L_0x0309:
        r0 = r5;
        r4 = 0;
        r7.mTotalLength = r4;
        r0 = r4;
        r6 = r39;
    L_0x0310:
        if (r0 >= r1) goto L_0x0417;
    L_0x0312:
        r4 = r7.getVirtualChildAt(r0);
        r45 = r10;
        r10 = r4.getVisibility();
        r46 = r13;
        r13 = 8;
        if (r10 != r13) goto L_0x032b;
        r51 = r2;
        r47 = r15;
        r13 = r58;
        goto L_0x040a;
    L_0x032b:
        r10 = r4.getLayoutParams();
        r10 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r10;
        r13 = r10.weight;
        r16 = (r13 > r22 ? 1 : (r13 == r22 ? 0 : -1));
        if (r16 <= 0) goto L_0x03a3;
    L_0x0337:
        r47 = r15;
        r15 = (float) r6;
        r15 = r15 * r13;
        r15 = r15 / r5;
        r15 = (int) r15;
        r5 = r5 - r13;
        r6 = r6 - r15;
        r16 = r57.getPaddingLeft();
        r19 = r57.getPaddingRight();
        r16 = r16 + r19;
        r48 = r5;
        r5 = r10.leftMargin;
        r16 = r16 + r5;
        r5 = r10.rightMargin;
        r5 = r16 + r5;
        r49 = r6;
        r6 = r10.width;
        r50 = r13;
        r13 = r58;
        r5 = getChildMeasureSpec(r13, r5, r6);
        r6 = r10.height;
        if (r6 != 0) goto L_0x037e;
    L_0x0364:
        r6 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r2 == r6) goto L_0x036b;
    L_0x0368:
        r51 = r2;
        goto L_0x0380;
    L_0x036b:
        if (r15 <= 0) goto L_0x036f;
    L_0x036d:
        r6 = r15;
        goto L_0x0370;
    L_0x036f:
        r6 = 0;
    L_0x0370:
        r51 = r2;
        r2 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r6 = android.view.View.MeasureSpec.makeMeasureSpec(r6, r2);
        r4.measure(r5, r6);
        r52 = r15;
        goto L_0x0395;
    L_0x037e:
        r51 = r2;
    L_0x0380:
        r2 = r4.getMeasuredHeight();
        r2 = r2 + r15;
        if (r2 >= 0) goto L_0x0388;
    L_0x0387:
        r2 = 0;
        r52 = r15;
        r6 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r15 = android.view.View.MeasureSpec.makeMeasureSpec(r2, r6);
        r4.measure(r5, r15);
        r2 = r4.getMeasuredState();
        r2 = r2 & -256;
        r14 = android.view.View.combineMeasuredStates(r14, r2);
        r5 = r48;
        goto L_0x03ad;
    L_0x03a3:
        r51 = r2;
        r50 = r13;
        r47 = r15;
        r13 = r58;
        r49 = r6;
    L_0x03ad:
        r2 = r10.leftMargin;
        r6 = r10.rightMargin;
        r2 = r2 + r6;
        r6 = r4.getMeasuredWidth();
        r6 = r6 + r2;
        r11 = java.lang.Math.max(r11, r6);
        r15 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 == r15) goto L_0x03c9;
    L_0x03bf:
        r15 = r10.width;
        r53 = r2;
        r2 = -1;
        if (r15 != r2) goto L_0x03cb;
    L_0x03c6:
        r2 = r21;
        goto L_0x03cc;
    L_0x03c9:
        r53 = r2;
    L_0x03cb:
        r2 = 0;
    L_0x03cc:
        if (r2 == 0) goto L_0x03d1;
    L_0x03ce:
        r15 = r53;
        goto L_0x03d2;
    L_0x03d1:
        r15 = r6;
    L_0x03d2:
        r9 = java.lang.Math.max(r9, r15);
        if (r17 == 0) goto L_0x03e2;
    L_0x03d8:
        r15 = r10.width;
        r54 = r2;
        r2 = -1;
        if (r15 != r2) goto L_0x03e5;
    L_0x03df:
        r15 = r21;
        goto L_0x03e6;
    L_0x03e2:
        r54 = r2;
        r2 = -1;
    L_0x03e5:
        r15 = 0;
    L_0x03e6:
        r2 = r7.mTotalLength;
        r16 = r4.getMeasuredHeight();
        r16 = r2 + r16;
        r55 = r5;
        r5 = r10.topMargin;
        r16 = r16 + r5;
        r5 = r10.bottomMargin;
        r16 = r16 + r5;
        r5 = r7.getNextLocationOffset(r4);
        r5 = r16 + r5;
        r5 = java.lang.Math.max(r2, r5);
        r7.mTotalLength = r5;
        r17 = r15;
        r6 = r49;
        r5 = r55;
    L_0x040a:
        r0 = r0 + 1;
        r10 = r45;
        r13 = r46;
        r15 = r47;
        r2 = r51;
        r4 = 0;
        goto L_0x0310;
    L_0x0417:
        r51 = r2;
        r45 = r10;
        r46 = r13;
        r47 = r15;
        r13 = r58;
        r0 = r7.mTotalLength;
        r2 = r57.getPaddingTop();
        r4 = r57.getPaddingBottom();
        r2 = r2 + r4;
        r0 = r0 + r2;
        r7.mTotalLength = r0;
        r39 = r6;
        r44 = r9;
    L_0x0434:
        if (r17 != 0) goto L_0x043c;
    L_0x0436:
        r0 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 == r0) goto L_0x043c;
    L_0x043a:
        r11 = r44;
    L_0x043c:
        r0 = r57.getPaddingLeft();
        r2 = r57.getPaddingRight();
        r0 = r0 + r2;
        r11 = r11 + r0;
        r0 = r57.getSuggestedMinimumWidth();
        r0 = java.lang.Math.max(r11, r0);
        r2 = android.view.View.resolveSizeAndState(r0, r13, r14);
        r7.setMeasuredDimension(r2, r8);
        if (r18 == 0) goto L_0x045a;
    L_0x0457:
        r7.forceUniformWidth(r1, r3);
    L_0x045a:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.LinearLayoutCompat.measureVertical(int, int):void");
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), Ints.MAX_POWER_OF_TWO);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == -1) {
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0215  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01be  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0215  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x01be  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0215  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0205  */
    /* JADX WARNING: Removed duplicated region for block: B:203:0x051c  */
    /* JADX WARNING: Removed duplicated region for block: B:195:0x04e4  */
    /* JADX WARNING: Removed duplicated region for block: B:224:0x05d1  */
    /* JADX WARNING: Removed duplicated region for block: B:223:0x05c9  */
    public void measureHorizontal(int r64, int r65) {
        /*
        r63 = this;
        r7 = r63;
        r8 = r64;
        r9 = r65;
        r10 = 0;
        r7.mTotalLength = r10;
        r0 = 0;
        r1 = 0;
        r2 = 0;
        r3 = 0;
        r4 = 1;
        r5 = 0;
        r11 = r63.getVirtualChildCount();
        r12 = android.view.View.MeasureSpec.getMode(r64);
        r13 = android.view.View.MeasureSpec.getMode(r65);
        r6 = 0;
        r14 = 0;
        r15 = r7.mMaxAscent;
        if (r15 == 0) goto L_0x0025;
    L_0x0021:
        r15 = r7.mMaxDescent;
        if (r15 != 0) goto L_0x002e;
    L_0x0025:
        r15 = 4;
        r10 = new int[r15];
        r7.mMaxAscent = r10;
        r10 = new int[r15];
        r7.mMaxDescent = r10;
    L_0x002e:
        r10 = r7.mMaxAscent;
        r15 = r7.mMaxDescent;
        r17 = 3;
        r18 = r6;
        r6 = -1;
        r10[r17] = r6;
        r19 = 2;
        r10[r19] = r6;
        r20 = 1;
        r10[r20] = r6;
        r16 = 0;
        r10[r16] = r6;
        r15[r17] = r6;
        r15[r19] = r6;
        r15[r20] = r6;
        r15[r16] = r6;
        r6 = r7.mBaselineAligned;
        r22 = r14;
        r14 = r7.mUseLargestChild;
        r23 = r15;
        r15 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 != r15) goto L_0x005c;
    L_0x0059:
        r24 = r20;
        goto L_0x005e;
    L_0x005c:
        r24 = 0;
    L_0x005e:
        r25 = 0;
        r26 = r1;
        r1 = r25;
        r25 = r18;
        r18 = r4;
        r4 = r0;
        r0 = 0;
        r62 = r3;
        r3 = r2;
        r2 = r62;
    L_0x006f:
        r28 = 0;
        if (r0 >= r11) goto L_0x0242;
    L_0x0073:
        r15 = r7.getVirtualChildAt(r0);
        if (r15 != 0) goto L_0x008c;
    L_0x0079:
        r30 = r1;
        r1 = r7.mTotalLength;
        r27 = r7.measureNullChild(r0);
        r1 = r1 + r27;
        r7.mTotalLength = r1;
        r21 = r6;
        r1 = r30;
        goto L_0x0236;
    L_0x008c:
        r30 = r1;
        r1 = r15.getVisibility();
        r31 = r2;
        r2 = 8;
        if (r1 != r2) goto L_0x00a6;
    L_0x0098:
        r1 = r7.getChildrenSkipCount(r15, r0);
        r0 = r0 + r1;
        r21 = r6;
        r1 = r30;
        r2 = r31;
        goto L_0x0236;
    L_0x00a6:
        r1 = r7.hasDividerBeforeChildAt(r0);
        if (r1 == 0) goto L_0x00b3;
    L_0x00ac:
        r1 = r7.mTotalLength;
        r2 = r7.mDividerWidth;
        r1 = r1 + r2;
        r7.mTotalLength = r1;
        r1 = r15.getLayoutParams();
        r2 = r1;
        r2 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r2;
        r1 = r2.weight;
        r29 = r5 + r1;
        r1 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 != r1) goto L_0x0117;
    L_0x00c3:
        r1 = r2.width;
        if (r1 != 0) goto L_0x0117;
    L_0x00c7:
        r1 = r2.weight;
        r1 = (r1 > r28 ? 1 : (r1 == r28 ? 0 : -1));
        if (r1 <= 0) goto L_0x0117;
    L_0x00cd:
        if (r24 == 0) goto L_0x00dc;
    L_0x00cf:
        r1 = r7.mTotalLength;
        r5 = r2.leftMargin;
        r32 = r0;
        r0 = r2.rightMargin;
        r5 = r5 + r0;
        r1 = r1 + r5;
        r7.mTotalLength = r1;
        goto L_0x00ec;
    L_0x00dc:
        r32 = r0;
        r0 = r7.mTotalLength;
        r1 = r2.leftMargin;
        r1 = r1 + r0;
        r5 = r2.rightMargin;
        r1 = r1 + r5;
        r1 = java.lang.Math.max(r0, r1);
        r7.mTotalLength = r1;
    L_0x00ec:
        if (r6 == 0) goto L_0x0107;
    L_0x00ee:
        r0 = 0;
        r1 = android.view.View.MeasureSpec.makeMeasureSpec(r0, r0);
        r15.measure(r1, r1);
        r0 = r2;
        r39 = r3;
        r40 = r4;
        r21 = r6;
        r2 = r30;
        r38 = r31;
        r35 = r32;
        r9 = -1;
        goto L_0x019a;
    L_0x0107:
        r22 = 1;
        r0 = r2;
        r39 = r3;
        r40 = r4;
        r21 = r6;
        r38 = r31;
        r35 = r32;
        r9 = -1;
        goto L_0x019c;
    L_0x0117:
        r32 = r0;
        r0 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        r1 = r2.width;
        if (r1 != 0) goto L_0x0129;
    L_0x011f:
        r1 = r2.weight;
        r1 = (r1 > r28 ? 1 : (r1 == r28 ? 0 : -1));
        if (r1 <= 0) goto L_0x0129;
    L_0x0125:
        r0 = 0;
        r1 = -2;
        r2.width = r1;
    L_0x0129:
        r5 = r0;
        r0 = (r29 > r28 ? 1 : (r29 == r28 ? 0 : -1));
        if (r0 != 0) goto L_0x0133;
    L_0x012e:
        r0 = r7.mTotalLength;
        r33 = r0;
        goto L_0x0135;
    L_0x0133:
        r33 = 0;
    L_0x0135:
        r34 = 0;
        r1 = r32;
        r0 = r7;
        r35 = r1;
        r36 = r30;
        r1 = r15;
        r37 = r2;
        r38 = r31;
        r2 = r35;
        r39 = r3;
        r3 = r8;
        r40 = r4;
        r4 = r33;
        r8 = r5;
        r5 = r9;
        r21 = r6;
        r9 = -1;
        r6 = r34;
        r0.measureChildBeforeLayout(r1, r2, r3, r4, r5, r6);
        r0 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        if (r8 == r0) goto L_0x015f;
    L_0x015a:
        r0 = r37;
        r0.width = r8;
        goto L_0x0161;
    L_0x015f:
        r0 = r37;
    L_0x0161:
        r1 = r15.getMeasuredWidth();
        if (r24 == 0) goto L_0x0178;
    L_0x0167:
        r2 = r7.mTotalLength;
        r3 = r0.leftMargin;
        r3 = r3 + r1;
        r4 = r0.rightMargin;
        r3 = r3 + r4;
        r4 = r7.getNextLocationOffset(r15);
        r3 = r3 + r4;
        r2 = r2 + r3;
        r7.mTotalLength = r2;
        goto L_0x018d;
    L_0x0178:
        r2 = r7.mTotalLength;
        r3 = r2 + r1;
        r4 = r0.leftMargin;
        r3 = r3 + r4;
        r4 = r0.rightMargin;
        r3 = r3 + r4;
        r4 = r7.getNextLocationOffset(r15);
        r3 = r3 + r4;
        r3 = java.lang.Math.max(r2, r3);
        r7.mTotalLength = r3;
    L_0x018d:
        if (r14 == 0) goto L_0x0198;
    L_0x018f:
        r2 = r36;
        r1 = java.lang.Math.max(r1, r2);
        r30 = r1;
        goto L_0x019c;
    L_0x0198:
        r2 = r36;
    L_0x019a:
        r30 = r2;
    L_0x019c:
        r1 = 0;
        r2 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r13 == r2) goto L_0x01a8;
    L_0x01a1:
        r2 = r0.height;
        if (r2 != r9) goto L_0x01a8;
    L_0x01a5:
        r25 = 1;
        r1 = 1;
    L_0x01a8:
        r2 = r0.topMargin;
        r3 = r0.bottomMargin;
        r2 = r2 + r3;
        r3 = r15.getMeasuredHeight();
        r3 = r3 + r2;
        r4 = r15.getMeasuredState();
        r6 = r26;
        r4 = android.view.View.combineMeasuredStates(r6, r4);
        if (r21 == 0) goto L_0x01ec;
    L_0x01be:
        r5 = r15.getBaseline();
        if (r5 == r9) goto L_0x01ec;
    L_0x01c4:
        r6 = r0.gravity;
        if (r6 >= 0) goto L_0x01cb;
    L_0x01c8:
        r6 = r7.mGravity;
        goto L_0x01cd;
    L_0x01cb:
        r6 = r0.gravity;
    L_0x01cd:
        r6 = r6 & 112;
        r8 = r6 >> 4;
        r26 = -2;
        r8 = r8 & -2;
        r8 = r8 >> 1;
        r9 = r10[r8];
        r9 = java.lang.Math.max(r9, r5);
        r10[r8] = r9;
        r9 = r23[r8];
        r41 = r2;
        r2 = r3 - r5;
        r2 = java.lang.Math.max(r9, r2);
        r23[r8] = r2;
        goto L_0x01ee;
    L_0x01ec:
        r41 = r2;
    L_0x01ee:
        r8 = r40;
        r2 = java.lang.Math.max(r8, r3);
        if (r18 == 0) goto L_0x01fe;
    L_0x01f6:
        r5 = r0.height;
        r6 = -1;
        if (r5 != r6) goto L_0x01fe;
    L_0x01fb:
        r5 = r20;
        goto L_0x01ff;
    L_0x01fe:
        r5 = 0;
    L_0x01ff:
        r6 = r0.weight;
        r6 = (r6 > r28 ? 1 : (r6 == r28 ? 0 : -1));
        if (r6 <= 0) goto L_0x0215;
    L_0x0205:
        if (r1 == 0) goto L_0x020a;
    L_0x0207:
        r6 = r41;
        goto L_0x020b;
    L_0x020a:
        r6 = r3;
    L_0x020b:
        r9 = r38;
        r6 = java.lang.Math.max(r9, r6);
        r9 = r6;
        r6 = r39;
        goto L_0x0223;
    L_0x0215:
        r9 = r38;
        if (r1 == 0) goto L_0x021c;
    L_0x0219:
        r6 = r41;
        goto L_0x021d;
    L_0x021c:
        r6 = r3;
    L_0x021d:
        r8 = r39;
        r6 = java.lang.Math.max(r8, r6);
    L_0x0223:
        r8 = r35;
        r18 = r7.getChildrenSkipCount(r15, r8);
        r0 = r8 + r18;
        r26 = r4;
        r18 = r5;
        r3 = r6;
        r5 = r29;
        r1 = r30;
        r4 = r2;
        r2 = r9;
    L_0x0236:
        r0 = r0 + 1;
        r6 = r21;
        r8 = r64;
        r9 = r65;
        r15 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        goto L_0x006f;
    L_0x0242:
        r9 = r2;
        r8 = r4;
        r21 = r6;
        r6 = r26;
        r2 = r1;
        r0 = r7.mTotalLength;
        if (r0 <= 0) goto L_0x025a;
    L_0x024d:
        r0 = r7.hasDividerBeforeChildAt(r11);
        if (r0 == 0) goto L_0x025a;
    L_0x0253:
        r0 = r7.mTotalLength;
        r1 = r7.mDividerWidth;
        r0 = r0 + r1;
        r7.mTotalLength = r0;
    L_0x025a:
        r0 = r10[r20];
        r1 = -1;
        if (r0 != r1) goto L_0x0271;
    L_0x025f:
        r0 = 0;
        r4 = r10[r0];
        if (r4 != r1) goto L_0x0271;
    L_0x0264:
        r0 = r10[r19];
        if (r0 != r1) goto L_0x0271;
    L_0x0268:
        r0 = r10[r17];
        if (r0 == r1) goto L_0x026d;
    L_0x026c:
        goto L_0x0271;
    L_0x026d:
        r42 = r6;
        r4 = r8;
        goto L_0x02a3;
    L_0x0271:
        r0 = r10[r17];
        r1 = 0;
        r4 = r10[r1];
        r15 = r10[r20];
        r1 = r10[r19];
        r1 = java.lang.Math.max(r15, r1);
        r1 = java.lang.Math.max(r4, r1);
        r0 = java.lang.Math.max(r0, r1);
        r1 = r23[r17];
        r4 = 0;
        r15 = r23[r4];
        r4 = r23[r20];
        r42 = r6;
        r6 = r23[r19];
        r4 = java.lang.Math.max(r4, r6);
        r4 = java.lang.Math.max(r15, r4);
        r1 = java.lang.Math.max(r1, r4);
        r4 = r0 + r1;
        r4 = java.lang.Math.max(r8, r4);
    L_0x02a3:
        if (r14 == 0) goto L_0x030a;
    L_0x02a5:
        r0 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        if (r12 == r0) goto L_0x02ab;
    L_0x02a9:
        if (r12 != 0) goto L_0x030a;
    L_0x02ab:
        r0 = 0;
        r7.mTotalLength = r0;
        r0 = 0;
    L_0x02af:
        if (r0 >= r11) goto L_0x030a;
    L_0x02b1:
        r1 = r7.getVirtualChildAt(r0);
        if (r1 != 0) goto L_0x02c4;
    L_0x02b7:
        r6 = r7.mTotalLength;
        r8 = r7.measureNullChild(r0);
        r6 = r6 + r8;
        r7.mTotalLength = r6;
        r43 = r0;
        goto L_0x0305;
    L_0x02c4:
        r6 = r1.getVisibility();
        r8 = 8;
        if (r6 != r8) goto L_0x02d2;
    L_0x02cc:
        r6 = r7.getChildrenSkipCount(r1, r0);
        r0 = r0 + r6;
        goto L_0x0307;
        r6 = r1.getLayoutParams();
        r6 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r6;
        if (r24 == 0) goto L_0x02ee;
    L_0x02db:
        r8 = r7.mTotalLength;
        r15 = r6.leftMargin;
        r15 = r15 + r2;
        r43 = r0;
        r0 = r6.rightMargin;
        r15 = r15 + r0;
        r0 = r7.getNextLocationOffset(r1);
        r15 = r15 + r0;
        r8 = r8 + r15;
        r7.mTotalLength = r8;
        goto L_0x0305;
    L_0x02ee:
        r43 = r0;
        r0 = r7.mTotalLength;
        r8 = r0 + r2;
        r15 = r6.leftMargin;
        r8 = r8 + r15;
        r15 = r6.rightMargin;
        r8 = r8 + r15;
        r15 = r7.getNextLocationOffset(r1);
        r8 = r8 + r15;
        r8 = java.lang.Math.max(r0, r8);
        r7.mTotalLength = r8;
    L_0x0305:
        r0 = r43;
    L_0x0307:
        r0 = r0 + 1;
        goto L_0x02af;
    L_0x030a:
        r0 = r7.mTotalLength;
        r1 = r63.getPaddingLeft();
        r6 = r63.getPaddingRight();
        r1 = r1 + r6;
        r0 = r0 + r1;
        r7.mTotalLength = r0;
        r0 = r7.mTotalLength;
        r1 = r63.getSuggestedMinimumWidth();
        r0 = java.lang.Math.max(r0, r1);
        r1 = r64;
        r6 = 0;
        r8 = android.view.View.resolveSizeAndState(r0, r1, r6);
        r6 = 16777215; // 0xffffff float:2.3509886E-38 double:8.2890456E-317;
        r0 = r8 & r6;
        r6 = r7.mTotalLength;
        r6 = r0 - r6;
        if (r22 != 0) goto L_0x03bc;
    L_0x0334:
        if (r6 == 0) goto L_0x0342;
    L_0x0336:
        r26 = (r5 > r28 ? 1 : (r5 == r28 ? 0 : -1));
        if (r26 <= 0) goto L_0x0342;
    L_0x033a:
        r44 = r0;
        r49 = r2;
        r46 = r4;
        goto L_0x03c2;
    L_0x0342:
        r3 = java.lang.Math.max(r3, r9);
        if (r14 == 0) goto L_0x03a4;
    L_0x0348:
        r15 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 == r15) goto L_0x03a4;
    L_0x034c:
        r16 = 0;
    L_0x034e:
        r15 = r16;
        if (r15 >= r11) goto L_0x03a4;
    L_0x0352:
        r44 = r0;
        r0 = r7.getVirtualChildAt(r15);
        if (r0 == 0) goto L_0x0393;
    L_0x035a:
        r45 = r3;
        r3 = r0.getVisibility();
        r46 = r4;
        r4 = 8;
        if (r3 != r4) goto L_0x036a;
        r49 = r2;
        goto L_0x0399;
        r3 = r0.getLayoutParams();
        r3 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r3;
        r4 = r3.weight;
        r16 = (r4 > r28 ? 1 : (r4 == r28 ? 0 : -1));
        if (r16 <= 0) goto L_0x0390;
        r47 = r3;
        r48 = r4;
        r3 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r4 = android.view.View.MeasureSpec.makeMeasureSpec(r2, r3);
        r49 = r2;
        r2 = r0.getMeasuredHeight();
        r2 = android.view.View.MeasureSpec.makeMeasureSpec(r2, r3);
        r0.measure(r4, r2);
        goto L_0x0399;
    L_0x0390:
        r49 = r2;
        goto L_0x0399;
    L_0x0393:
        r49 = r2;
        r45 = r3;
        r46 = r4;
    L_0x0399:
        r16 = r15 + 1;
        r0 = r44;
        r3 = r45;
        r4 = r46;
        r2 = r49;
        goto L_0x034e;
    L_0x03a4:
        r44 = r0;
        r49 = r2;
        r45 = r3;
        r46 = r4;
        r50 = r5;
        r51 = r9;
        r56 = r11;
        r52 = r14;
        r2 = r45;
        r15 = r46;
        r14 = r65;
        goto L_0x05a0;
    L_0x03bc:
        r44 = r0;
        r49 = r2;
        r46 = r4;
    L_0x03c2:
        r0 = r7.mWeightSum;
        r0 = (r0 > r28 ? 1 : (r0 == r28 ? 0 : -1));
        if (r0 <= 0) goto L_0x03cb;
    L_0x03c8:
        r0 = r7.mWeightSum;
        goto L_0x03cc;
    L_0x03cb:
        r0 = r5;
    L_0x03cc:
        r2 = -1;
        r10[r17] = r2;
        r10[r19] = r2;
        r10[r20] = r2;
        r4 = 0;
        r10[r4] = r2;
        r23[r17] = r2;
        r23[r19] = r2;
        r23[r20] = r2;
        r23[r4] = r2;
        r15 = -1;
        r7.mTotalLength = r4;
        r2 = r3;
        r4 = r42;
        r3 = r0;
        r0 = 0;
    L_0x03e6:
        if (r0 >= r11) goto L_0x0544;
    L_0x03e8:
        r50 = r5;
        r5 = r7.getVirtualChildAt(r0);
        if (r5 == 0) goto L_0x052c;
    L_0x03f0:
        r51 = r9;
        r9 = r5.getVisibility();
        r52 = r14;
        r14 = 8;
        if (r9 != r14) goto L_0x0405;
        r56 = r11;
        r14 = r65;
        r26 = -2;
        goto L_0x0536;
        r9 = r5.getLayoutParams();
        r9 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r9;
        r14 = r9.weight;
        r26 = (r14 > r28 ? 1 : (r14 == r28 ? 0 : -1));
        if (r26 <= 0) goto L_0x0476;
    L_0x0412:
        r1 = (float) r6;
        r1 = r1 * r14;
        r1 = r1 / r3;
        r1 = (int) r1;
        r3 = r3 - r14;
        r6 = r6 - r1;
        r26 = r63.getPaddingTop();
        r27 = r63.getPaddingBottom();
        r26 = r26 + r27;
        r53 = r3;
        r3 = r9.topMargin;
        r26 = r26 + r3;
        r3 = r9.bottomMargin;
        r3 = r26 + r3;
        r54 = r6;
        r6 = r9.height;
        r56 = r11;
        r55 = r14;
        r11 = -1;
        r14 = r65;
        r3 = getChildMeasureSpec(r14, r3, r6);
        r6 = r9.width;
        if (r6 != 0) goto L_0x0454;
    L_0x0440:
        r6 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r12 == r6) goto L_0x0445;
    L_0x0444:
        goto L_0x0454;
    L_0x0445:
        if (r1 <= 0) goto L_0x0449;
    L_0x0447:
        r11 = r1;
        goto L_0x044a;
    L_0x0449:
        r11 = 0;
    L_0x044a:
        r11 = android.view.View.MeasureSpec.makeMeasureSpec(r11, r6);
        r5.measure(r11, r3);
        r57 = r1;
        goto L_0x0469;
    L_0x0454:
        r6 = r5.getMeasuredWidth();
        r6 = r6 + r1;
        if (r6 >= 0) goto L_0x045c;
    L_0x045b:
        r6 = 0;
        r57 = r1;
        r11 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r1 = android.view.View.MeasureSpec.makeMeasureSpec(r6, r11);
        r5.measure(r1, r3);
        r1 = r5.getMeasuredState();
        r6 = -16777216; // 0xffffffffff000000 float:-1.7014118E38 double:NaN;
        r1 = r1 & r6;
        r4 = android.view.View.combineMeasuredStates(r4, r1);
        goto L_0x0480;
    L_0x0476:
        r56 = r11;
        r55 = r14;
        r14 = r65;
        r53 = r3;
        r54 = r6;
    L_0x0480:
        if (r24 == 0) goto L_0x0497;
    L_0x0482:
        r1 = r7.mTotalLength;
        r3 = r5.getMeasuredWidth();
        r6 = r9.leftMargin;
        r3 = r3 + r6;
        r6 = r9.rightMargin;
        r3 = r3 + r6;
        r6 = r7.getNextLocationOffset(r5);
        r3 = r3 + r6;
        r1 = r1 + r3;
        r7.mTotalLength = r1;
        goto L_0x04af;
    L_0x0497:
        r1 = r7.mTotalLength;
        r3 = r5.getMeasuredWidth();
        r3 = r3 + r1;
        r6 = r9.leftMargin;
        r3 = r3 + r6;
        r6 = r9.rightMargin;
        r3 = r3 + r6;
        r6 = r7.getNextLocationOffset(r5);
        r3 = r3 + r6;
        r3 = java.lang.Math.max(r1, r3);
        r7.mTotalLength = r3;
    L_0x04af:
        r1 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r13 == r1) goto L_0x04bb;
    L_0x04b3:
        r1 = r9.height;
        r3 = -1;
        if (r1 != r3) goto L_0x04bb;
    L_0x04b8:
        r1 = r20;
        goto L_0x04bc;
    L_0x04bb:
        r1 = 0;
    L_0x04bc:
        r3 = r9.topMargin;
        r6 = r9.bottomMargin;
        r3 = r3 + r6;
        r6 = r5.getMeasuredHeight();
        r6 = r6 + r3;
        r11 = java.lang.Math.max(r15, r6);
        if (r1 == 0) goto L_0x04ce;
    L_0x04cc:
        r15 = r3;
        goto L_0x04cf;
    L_0x04ce:
        r15 = r6;
    L_0x04cf:
        r2 = java.lang.Math.max(r2, r15);
        if (r18 == 0) goto L_0x04df;
    L_0x04d5:
        r15 = r9.height;
        r58 = r1;
        r1 = -1;
        if (r15 != r1) goto L_0x04e1;
    L_0x04dc:
        r1 = r20;
        goto L_0x04e2;
    L_0x04df:
        r58 = r1;
    L_0x04e1:
        r1 = 0;
    L_0x04e2:
        if (r21 == 0) goto L_0x051c;
    L_0x04e4:
        r15 = r5.getBaseline();
        r59 = r1;
        r1 = -1;
        if (r15 == r1) goto L_0x0517;
    L_0x04ed:
        r1 = r9.gravity;
        if (r1 >= 0) goto L_0x04f4;
    L_0x04f1:
        r1 = r7.mGravity;
        goto L_0x04f6;
    L_0x04f4:
        r1 = r9.gravity;
    L_0x04f6:
        r1 = r1 & 112;
        r18 = r1 >> 4;
        r26 = -2;
        r18 = r18 & -2;
        r18 = r18 >> 1;
        r60 = r1;
        r1 = r10[r18];
        r1 = java.lang.Math.max(r1, r15);
        r10[r18] = r1;
        r1 = r23[r18];
        r61 = r2;
        r2 = r6 - r15;
        r1 = java.lang.Math.max(r1, r2);
        r23[r18] = r1;
        goto L_0x0522;
    L_0x0517:
        r61 = r2;
        r26 = -2;
        goto L_0x0522;
    L_0x051c:
        r59 = r1;
        r61 = r2;
        r26 = -2;
    L_0x0522:
        r15 = r11;
        r3 = r53;
        r6 = r54;
        r18 = r59;
        r2 = r61;
        goto L_0x0536;
    L_0x052c:
        r51 = r9;
        r56 = r11;
        r52 = r14;
        r14 = r65;
        r26 = -2;
    L_0x0536:
        r0 = r0 + 1;
        r5 = r50;
        r9 = r51;
        r14 = r52;
        r11 = r56;
        r1 = r64;
        goto L_0x03e6;
    L_0x0544:
        r50 = r5;
        r51 = r9;
        r56 = r11;
        r52 = r14;
        r14 = r65;
        r0 = r7.mTotalLength;
        r1 = r63.getPaddingLeft();
        r5 = r63.getPaddingRight();
        r1 = r1 + r5;
        r0 = r0 + r1;
        r7.mTotalLength = r0;
        r0 = r10[r20];
        r1 = -1;
        if (r0 != r1) goto L_0x056e;
    L_0x0561:
        r0 = 0;
        r5 = r10[r0];
        if (r5 != r1) goto L_0x056e;
    L_0x0566:
        r0 = r10[r19];
        if (r0 != r1) goto L_0x056e;
    L_0x056a:
        r0 = r10[r17];
        if (r0 == r1) goto L_0x059e;
    L_0x056e:
        r0 = r10[r17];
        r1 = 0;
        r5 = r10[r1];
        r9 = r10[r20];
        r11 = r10[r19];
        r9 = java.lang.Math.max(r9, r11);
        r5 = java.lang.Math.max(r5, r9);
        r0 = java.lang.Math.max(r0, r5);
        r5 = r23[r17];
        r1 = r23[r1];
        r9 = r23[r20];
        r11 = r23[r19];
        r9 = java.lang.Math.max(r9, r11);
        r1 = java.lang.Math.max(r1, r9);
        r1 = java.lang.Math.max(r5, r1);
        r5 = r0 + r1;
        r0 = java.lang.Math.max(r15, r5);
        r15 = r0;
    L_0x059e:
        r42 = r4;
    L_0x05a0:
        if (r18 != 0) goto L_0x05a7;
    L_0x05a2:
        r0 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        if (r13 == r0) goto L_0x05a7;
    L_0x05a6:
        r15 = r2;
    L_0x05a7:
        r0 = r63.getPaddingTop();
        r1 = r63.getPaddingBottom();
        r0 = r0 + r1;
        r15 = r15 + r0;
        r0 = r63.getSuggestedMinimumHeight();
        r0 = java.lang.Math.max(r15, r0);
        r1 = -16777216; // 0xffffffffff000000 float:-1.7014118E38 double:NaN;
        r1 = r42 & r1;
        r1 = r1 | r8;
        r3 = r42 << 16;
        r3 = android.view.View.resolveSizeAndState(r0, r14, r3);
        r7.setMeasuredDimension(r1, r3);
        if (r25 == 0) goto L_0x05d1;
    L_0x05c9:
        r3 = r56;
        r1 = r64;
        r7.forceUniformHeight(r3, r1);
        goto L_0x05d5;
    L_0x05d1:
        r3 = r56;
        r1 = r64;
    L_0x05d5:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.LinearLayoutCompat.measureHorizontal(int, int):void");
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), Ints.MAX_POWER_OF_TWO);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == -1) {
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public int getChildrenSkipCount(View child, int index) {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public int measureNullChild(int childIndex) {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec, int totalHeight) {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    /* Access modifiers changed, original: 0000 */
    public int getLocationOffset(View child) {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public int getNextLocationOffset(View child) {
        return 0;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mOrientation == 1) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void layoutVertical(int left, int top, int right, int bottom) {
        int childTop;
        int paddingLeft = getPaddingLeft();
        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = (width - paddingLeft) - getPaddingRight();
        int count = getVirtualChildCount();
        int majorGravity = this.mGravity & 112;
        int minorGravity = this.mGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if (majorGravity == 16) {
            childTop = getPaddingTop() + (((bottom - top) - this.mTotalLength) / 2);
        } else if (majorGravity != 80) {
            childTop = getPaddingTop();
        } else {
            childTop = ((getPaddingTop() + bottom) - top) - this.mTotalLength;
        }
        int i = 0;
        while (true) {
            int i2 = i;
            int paddingLeft2;
            if (i2 < count) {
                int majorGravity2;
                View child = getVirtualChildAt(i2);
                if (child == null) {
                    childTop += measureNullChild(i2);
                    majorGravity2 = majorGravity;
                    paddingLeft2 = paddingLeft;
                } else if (child.getVisibility() != 8) {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int gravity = lp.gravity;
                    if (gravity < 0) {
                        gravity = minorGravity;
                    }
                    int layoutDirection = ViewCompat.getLayoutDirection(this);
                    int gravity2 = gravity;
                    gravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection) & 7;
                    majorGravity2 = majorGravity;
                    gravity = gravity != 1 ? gravity != 5 ? lp.leftMargin + paddingLeft : (childRight - childWidth) - lp.rightMargin : ((((childSpace - childWidth) / 2) + paddingLeft) + lp.leftMargin) - lp.rightMargin;
                    if (hasDividerBeforeChildAt(i2) != 0) {
                        childTop += this.mDividerHeight;
                    }
                    gravity2 = childTop + lp.topMargin;
                    LayoutParams lp2 = lp;
                    View child2 = child;
                    paddingLeft2 = paddingLeft;
                    paddingLeft = i2;
                    setChildFrame(child, gravity, gravity2 + getLocationOffset(child), childWidth, childHeight);
                    i2 = paddingLeft + getChildrenSkipCount(child2, paddingLeft);
                    childTop = gravity2 + ((childHeight + lp2.bottomMargin) + getNextLocationOffset(child2));
                } else {
                    majorGravity2 = majorGravity;
                    paddingLeft2 = paddingLeft;
                    paddingLeft = i2;
                }
                i = i2 + 1;
                majorGravity = majorGravity2;
                paddingLeft = paddingLeft2;
            } else {
                paddingLeft2 = paddingLeft;
                return;
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00c3  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00fb  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00cf  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x010e  */
    public void layoutHorizontal(int r32, int r33, int r34, int r35) {
        /*
        r31 = this;
        r6 = r31;
        r9 = android.support.v7.widget.ViewUtils.isLayoutRtl(r31);
        r10 = r31.getPaddingTop();
        r13 = r35 - r33;
        r0 = r31.getPaddingBottom();
        r14 = r13 - r0;
        r0 = r13 - r10;
        r1 = r31.getPaddingBottom();
        r15 = r0 - r1;
        r5 = r31.getVirtualChildCount();
        r0 = r6.mGravity;
        r1 = 8388615; // 0x800007 float:1.1754953E-38 double:4.1445265E-317;
        r4 = r0 & r1;
        r0 = r6.mGravity;
        r16 = r0 & 112;
        r2 = r6.mBaselineAligned;
        r1 = r6.mMaxAscent;
        r0 = r6.mMaxDescent;
        r3 = android.support.v4.view.ViewCompat.getLayoutDirection(r31);
        r11 = android.support.v4.view.GravityCompat.getAbsoluteGravity(r4, r3);
        r17 = 2;
        r12 = 1;
        if (r11 == r12) goto L_0x0052;
    L_0x003c:
        r12 = 5;
        if (r11 == r12) goto L_0x0046;
    L_0x003f:
        r11 = r31.getPaddingLeft();
    L_0x0043:
        r18 = r3;
        goto L_0x0061;
    L_0x0046:
        r11 = r31.getPaddingLeft();
        r11 = r11 + r34;
        r11 = r11 - r32;
        r12 = r6.mTotalLength;
        r11 = r11 - r12;
        goto L_0x0043;
    L_0x0052:
        r11 = r31.getPaddingLeft();
        r12 = r34 - r32;
        r18 = r3;
        r3 = r6.mTotalLength;
        r12 = r12 - r3;
        r12 = r12 / 2;
        r11 = r11 + r12;
    L_0x0061:
        r3 = r11;
        r11 = 0;
        r12 = 1;
        if (r9 == 0) goto L_0x0069;
    L_0x0066:
        r11 = r5 + -1;
        r12 = -1;
    L_0x0069:
        r19 = 0;
        r20 = r3;
    L_0x006d:
        r3 = r19;
        if (r3 >= r5) goto L_0x015f;
    L_0x0071:
        r19 = r12 * r3;
        r7 = r11 + r19;
        r8 = r6.getVirtualChildAt(r7);
        if (r8 != 0) goto L_0x008f;
    L_0x007b:
        r19 = r6.measureNullChild(r7);
        r20 = r20 + r19;
        r26 = r0;
        r28 = r1;
        r25 = r2;
        r22 = r4;
        r27 = r5;
        r30 = r9;
        goto L_0x014e;
    L_0x008f:
        r21 = r3;
        r3 = r8.getVisibility();
        r22 = r4;
        r4 = 8;
        if (r3 == r4) goto L_0x0142;
    L_0x009b:
        r19 = r8.getMeasuredWidth();
        r23 = r8.getMeasuredHeight();
        r3 = -1;
        r4 = r8.getLayoutParams();
        r4 = (android.support.v7.widget.LinearLayoutCompat.LayoutParams) r4;
        r24 = r3;
        r3 = -1;
        if (r2 == 0) goto L_0x00bb;
    L_0x00b0:
        r25 = r2;
        r2 = r4.height;
        if (r2 == r3) goto L_0x00bd;
    L_0x00b6:
        r2 = r8.getBaseline();
        goto L_0x00bf;
    L_0x00bb:
        r25 = r2;
    L_0x00bd:
        r2 = r24;
    L_0x00bf:
        r3 = r4.gravity;
        if (r3 >= 0) goto L_0x00c5;
    L_0x00c3:
        r3 = r16;
    L_0x00c5:
        r24 = r3;
        r3 = r24 & 112;
        r27 = r5;
        r5 = 16;
        if (r3 == r5) goto L_0x00fb;
    L_0x00cf:
        r5 = 48;
        if (r3 == r5) goto L_0x00ed;
    L_0x00d3:
        r5 = 80;
        if (r3 == r5) goto L_0x00d9;
    L_0x00d7:
        r3 = r10;
        goto L_0x0107;
    L_0x00d9:
        r3 = r14 - r23;
        r5 = r4.bottomMargin;
        r3 = r3 - r5;
        r5 = -1;
        if (r2 == r5) goto L_0x0107;
    L_0x00e1:
        r5 = r8.getMeasuredHeight();
        r5 = r5 - r2;
        r26 = r0[r17];
        r26 = r26 - r5;
        r3 = r3 - r26;
        goto L_0x0107;
    L_0x00ed:
        r3 = r4.topMargin;
        r3 = r3 + r10;
        r5 = -1;
        if (r2 == r5) goto L_0x0107;
    L_0x00f3:
        r5 = 1;
        r26 = r1[r5];
        r26 = r26 - r2;
        r3 = r3 + r26;
        goto L_0x0107;
    L_0x00fb:
        r3 = r15 - r23;
        r3 = r3 / 2;
        r3 = r3 + r10;
        r5 = r4.topMargin;
        r3 = r3 + r5;
        r5 = r4.bottomMargin;
        r3 = r3 - r5;
        r5 = r6.hasDividerBeforeChildAt(r7);
        if (r5 == 0) goto L_0x0112;
    L_0x010e:
        r5 = r6.mDividerWidth;
        r20 = r20 + r5;
    L_0x0112:
        r5 = r4.leftMargin;
        r20 = r20 + r5;
        r5 = r6.getLocationOffset(r8);
        r5 = r20 + r5;
        r26 = r0;
        r0 = r6;
        r28 = r1;
        r1 = r8;
        r29 = r2;
        r2 = r5;
        r5 = r4;
        r4 = r19;
        r30 = r9;
        r9 = r5;
        r5 = r23;
        r0.setChildFrame(r1, r2, r3, r4, r5);
        r0 = r9.rightMargin;
        r0 = r19 + r0;
        r1 = r6.getNextLocationOffset(r8);
        r0 = r0 + r1;
        r20 = r20 + r0;
        r0 = r6.getChildrenSkipCount(r8, r7);
        r3 = r21 + r0;
        goto L_0x014e;
    L_0x0142:
        r26 = r0;
        r28 = r1;
        r25 = r2;
        r27 = r5;
        r30 = r9;
        r3 = r21;
    L_0x014e:
        r0 = 1;
        r19 = r3 + 1;
        r4 = r22;
        r2 = r25;
        r0 = r26;
        r5 = r27;
        r1 = r28;
        r9 = r30;
        goto L_0x006d;
    L_0x015f:
        r26 = r0;
        r28 = r1;
        r25 = r2;
        r22 = r4;
        r27 = r5;
        r30 = r9;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.LinearLayoutCompat.layoutHorizontal(int, int, int, int):void");
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            if ((GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK & gravity) == 0) {
                gravity |= GravityCompat.START;
            }
            if ((gravity & 112) == 0) {
                gravity |= 48;
            }
            this.mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public void setHorizontalGravity(int horizontalGravity) {
        int gravity = horizontalGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if ((GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK & this.mGravity) != gravity) {
            this.mGravity = (this.mGravity & -8388616) | gravity;
            requestLayout();
        }
    }

    public void setVerticalGravity(int verticalGravity) {
        int gravity = verticalGravity & 112;
        if ((this.mGravity & 112) != gravity) {
            this.mGravity = (this.mGravity & -113) | gravity;
            requestLayout();
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateDefaultLayoutParams() {
        if (this.mOrientation == 0) {
            return new LayoutParams(-2, -2);
        }
        if (this.mOrientation == 1) {
            return new LayoutParams(-1, -2);
        }
        return null;
    }

    /* Access modifiers changed, original: protected */
    public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /* Access modifiers changed, original: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(LinearLayoutCompat.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(LinearLayoutCompat.class.getName());
    }
}
