package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.hmdglobal.app.camera.R;
import java.util.ArrayList;
import java.util.List;

public class TopRightWeightedLayout extends LinearLayout {
    public TopRightWeightedLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onFinishInflate() {
        checkOrientation(getContext().getResources().getConfiguration().orientation);
        homogeneousLayoutDistribute();
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        checkOrientation(configuration.orientation);
    }

    private void checkOrientation(int orientation) {
        boolean isHorizontal = getOrientation() == 0;
        boolean isPortrait = 1 == orientation;
        if (isPortrait && !isHorizontal) {
            fixGravityAndPadding(0);
            setOrientation(0);
            reverseChildren();
            requestLayout();
        } else if (!isPortrait && isHorizontal) {
            fixGravityAndPadding(1);
            setOrientation(1);
            reverseChildren();
            requestLayout();
        }
    }

    private void reverseChildren() {
        List<View> children = new ArrayList();
        for (int i = getChildCount() - 1; i >= 0; i--) {
            children.add(getChildAt(i));
        }
        for (View v : children) {
            bringChildToFront(v);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        homogeneousLayoutDistribute();
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    public void homogeneousLayoutDistribute() {
        if (1 != getOrientation()) {
            int i;
            int childrenCount = getChildCount();
            List<View> children = new ArrayList();
            int i2 = 0;
            int childrenTotalWidth = 0;
            for (i = 0; i < childrenCount; i++) {
                if (getChildAt(i).getVisibility() != 8) {
                    View button = getChildAt(i);
                    children.add(getChildAt(i));
                    childrenTotalWidth += getChildAt(i).getWidth();
                }
            }
            if (children.size() != 0 && children.size() != 1) {
                i = (int) getResources().getDimension(R.dimen.icon_margin_left);
                int marginWidth = ((getMeasuredWidth() - (((View) children.get(0)).getMeasuredWidth() * children.size())) - (2 * i)) / (children.size() - 1);
                while (i2 < children.size()) {
                    View child = (View) children.get(i2);
                    LayoutParams pChild = (LayoutParams) child.getLayoutParams();
                    if (i2 == 0) {
                        pChild.setMarginStart(i);
                    } else if (i2 == children.size() - 1) {
                        pChild.setMarginStart(marginWidth);
                        pChild.setMarginEnd(i);
                    } else {
                        pChild.setMarginStart(marginWidth);
                    }
                    child.setLayoutParams(pChild);
                    i2++;
                }
                requestLayout();
            }
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    private void fixGravityAndPadding(int direction) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) v.getLayoutParams();
            int gravity = layoutParams.gravity;
            if (direction == 1) {
                if ((gravity & 3) != 0) {
                    gravity = (gravity & -4) | 80;
                }
            } else if ((gravity & 80) != 0) {
                gravity = (gravity & -81) | 3;
            }
            if (direction == 1) {
                if ((gravity & 5) != 0) {
                    gravity = (gravity & -6) | 48;
                }
            } else if ((gravity & 48) != 0) {
                gravity = (gravity & -49) | 5;
            }
            if ((gravity & 17) != 17) {
                if (direction == 1) {
                    if ((gravity & 16) != 0) {
                        gravity = (gravity & -17) | 1;
                    }
                } else if ((gravity & 1) != 0) {
                    gravity = (gravity & -2) | 16;
                }
            }
            layoutParams.gravity = gravity;
            int paddingLeft = v.getPaddingLeft();
            int paddingTop = v.getPaddingTop();
            v.setPadding(v.getPaddingBottom(), v.getPaddingRight(), paddingTop, paddingLeft);
        }
    }
}
