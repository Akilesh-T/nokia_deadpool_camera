package android.support.v7.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.support.v7.widget.LinearLayoutCompat.LayoutParams;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import com.google.common.primitives.Ints;

@RestrictTo({Scope.LIBRARY_GROUP})
public class AlertDialogLayout extends LinearLayoutCompat {
    public AlertDialogLayout(@Nullable Context context) {
        super(context);
    }

    public AlertDialogLayout(@Nullable Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!tryOnMeasure(widthMeasureSpec, heightMeasureSpec)) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private boolean tryOnMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        int id;
        int childHeightSpec;
        int heightToGive;
        View buttonPanel;
        View middlePanel;
        int i2 = widthMeasureSpec;
        int i3 = heightMeasureSpec;
        int count = getChildCount();
        View middlePanel2 = null;
        View buttonPanel2 = null;
        View topPanel = null;
        for (i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                id = child.getId();
                if (id == R.id.topPanel) {
                    topPanel = child;
                } else if (id == R.id.buttonPanel) {
                    buttonPanel2 = child;
                } else if ((id != R.id.contentPanel && id != R.id.customPanel) || middlePanel2 != null) {
                    return false;
                } else {
                    middlePanel2 = child;
                }
            }
        }
        i = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int childState = 0;
        int usedHeight = getPaddingTop() + getPaddingBottom();
        if (topPanel != null) {
            topPanel.measure(i2, 0);
            usedHeight += topPanel.getMeasuredHeight();
            childState = View.combineMeasuredStates(0, topPanel.getMeasuredState());
        }
        int buttonHeight = 0;
        int buttonWantsHeight = 0;
        if (buttonPanel2 != null) {
            buttonPanel2.measure(i2, 0);
            buttonHeight = resolveMinimumHeight(buttonPanel2);
            buttonWantsHeight = buttonPanel2.getMeasuredHeight() - buttonHeight;
            usedHeight += buttonHeight;
            childState = View.combineMeasuredStates(childState, buttonPanel2.getMeasuredState());
        }
        id = 0;
        if (middlePanel2 != null) {
            if (i == 0) {
                childHeightSpec = 0;
                View view = topPanel;
            } else {
                childHeightSpec = MeasureSpec.makeMeasureSpec(Math.max(0, heightSize - usedHeight), i);
            }
            middlePanel2.measure(i2, childHeightSpec);
            id = middlePanel2.getMeasuredHeight();
            usedHeight += id;
            childState = View.combineMeasuredStates(childState, middlePanel2.getMeasuredState());
        }
        int remainingHeight = heightSize - usedHeight;
        if (buttonPanel2 != null) {
            usedHeight -= buttonHeight;
            childHeightSpec = Math.min(remainingHeight, buttonWantsHeight);
            if (childHeightSpec > 0) {
                remainingHeight -= childHeightSpec;
                buttonHeight += childHeightSpec;
            }
            int remainingHeight2 = remainingHeight;
            buttonPanel2.measure(i2, MeasureSpec.makeMeasureSpec(buttonHeight, Ints.MAX_POWER_OF_TWO));
            usedHeight += buttonPanel2.getMeasuredHeight();
            childState = View.combineMeasuredStates(childState, buttonPanel2.getMeasuredState());
            remainingHeight = remainingHeight2;
        }
        if (middlePanel2 == null || remainingHeight <= 0) {
        } else {
            usedHeight -= id;
            heightToGive = remainingHeight;
            int remainingHeight3 = remainingHeight - heightToGive;
            middlePanel2.measure(i2, MeasureSpec.makeMeasureSpec(id + heightToGive, i));
            usedHeight += middlePanel2.getMeasuredHeight();
            childState = View.combineMeasuredStates(childState, middlePanel2.getMeasuredState());
            remainingHeight = remainingHeight3;
        }
        heightToGive = 0;
        i = 0;
        while (i < count) {
            int remainingHeight4 = remainingHeight;
            remainingHeight = getChildAt(i);
            buttonPanel = buttonPanel2;
            middlePanel = middlePanel2;
            if (remainingHeight.getVisibility() != 8) {
                heightToGive = Math.max(heightToGive, remainingHeight.getMeasuredWidth());
            }
            i++;
            remainingHeight = remainingHeight4;
            buttonPanel2 = buttonPanel;
            middlePanel2 = middlePanel;
        }
        buttonPanel = buttonPanel2;
        middlePanel = middlePanel2;
        setMeasuredDimension(View.resolveSizeAndState(heightToGive + (getPaddingLeft() + getPaddingRight()), i2, childState), View.resolveSizeAndState(usedHeight, i3, 0));
        if (widthMode != Ints.MAX_POWER_OF_TWO) {
            forceUniformWidth(count, i3);
        }
        return true;
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), Ints.MAX_POWER_OF_TWO);
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
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

    private static int resolveMinimumHeight(View v) {
        int minHeight = ViewCompat.getMinimumHeight(v);
        if (minHeight > 0) {
            return minHeight;
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            if (vg.getChildCount() == 1) {
                return resolveMinimumHeight(vg.getChildAt(0));
            }
        }
        return 0;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childTop;
        int majorGravity;
        int count;
        AlertDialogLayout alertDialogLayout = this;
        int paddingLeft = getPaddingLeft();
        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = (width - paddingLeft) - getPaddingRight();
        int totalLength = getMeasuredHeight();
        int count2 = getChildCount();
        int gravity = getGravity();
        int majorGravity2 = gravity & 112;
        int minorGravity = gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if (majorGravity2 == 16) {
            childTop = getPaddingTop() + (((bottom - top) - totalLength) / 2);
        } else if (majorGravity2 != 80) {
            childTop = getPaddingTop();
        } else {
            childTop = ((getPaddingTop() + bottom) - top) - totalLength;
        }
        Drawable dividerDrawable = getDividerDrawable();
        int i = 0;
        int dividerHeight = dividerDrawable == null ? 0 : dividerDrawable.getIntrinsicHeight();
        while (i < count2) {
            Drawable dividerDrawable2;
            int i2;
            View child = alertDialogLayout.getChildAt(i);
            if (child != null) {
                dividerDrawable2 = dividerDrawable;
                majorGravity = majorGravity2;
                if (child.getVisibility() != 8) {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    int layoutGravity = lp.gravity;
                    if (layoutGravity < 0) {
                        layoutGravity = minorGravity;
                    }
                    View child2 = child;
                    int absoluteGravity = GravityCompat.getAbsoluteGravity(layoutGravity, ViewCompat.getLayoutDirection(this)) & 7;
                    absoluteGravity = absoluteGravity != 1 ? absoluteGravity != 5 ? lp.leftMargin + paddingLeft : (childRight - childWidth) - lp.rightMargin : ((((childSpace - childWidth) / 2) + paddingLeft) + lp.leftMargin) - lp.rightMargin;
                    if (alertDialogLayout.hasDividerBeforeChildAt(i)) {
                        childTop += dividerHeight;
                    }
                    int childTop2 = childTop + lp.topMargin;
                    i2 = i;
                    LayoutParams lp2 = lp;
                    count = count2;
                    alertDialogLayout.setChildFrame(child2, absoluteGravity, childTop2, childWidth, childHeight);
                    childTop = childTop2 + (childHeight + lp2.bottomMargin);
                } else {
                    i2 = i;
                    count = count2;
                }
            } else {
                i2 = i;
                dividerDrawable2 = dividerDrawable;
                majorGravity = majorGravity2;
                count = count2;
            }
            i = i2 + 1;
            dividerDrawable = dividerDrawable2;
            majorGravity2 = majorGravity;
            count2 = count;
            alertDialogLayout = this;
        }
        majorGravity = majorGravity2;
        count = count2;
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }
}
