package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class RotateLayout extends ViewGroup implements Rotatable {
    private static final Tag TAG = new Tag("RotateLayout");
    protected View mChild;
    private Matrix mMatrix = new Matrix();
    private int mOrientation;

    public RotateLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(17170445);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        this.mChild = getChildAt(0);
        this.mChild.setPivotX(0.0f);
        this.mChild.setPivotY(0.0f);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean change, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int i = this.mOrientation;
        if (i != 0) {
            if (i != 90) {
                if (i != MediaProviderUtils.ROTATION_180) {
                    if (i != MediaProviderUtils.ROTATION_270) {
                        return;
                    }
                }
            }
            this.mChild.layout(0, 0, height, width);
            return;
        }
        this.mChild.layout(0, 0, width, height);
    }

    /* Access modifiers changed, original: protected */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x006b  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x003f  */
    /* JADX WARNING: Missing block: B:4:0x0010, code skipped:
            if (r2 != com.morphoinc.utils.multimedia.MediaProviderUtils.ROTATION_270) goto L_0x0037;
     */
    public void onMeasure(int r8, int r9) {
        /*
        r7 = this;
        r0 = 0;
        r1 = 0;
        r2 = r7.mOrientation;
        r3 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        r4 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        r5 = 90;
        if (r2 == 0) goto L_0x0025;
    L_0x000c:
        if (r2 == r5) goto L_0x0013;
    L_0x000e:
        if (r2 == r4) goto L_0x0025;
    L_0x0010:
        if (r2 == r3) goto L_0x0013;
    L_0x0012:
        goto L_0x0037;
    L_0x0013:
        r2 = r7.mChild;
        r7.measureChild(r2, r9, r8);
        r2 = r7.mChild;
        r0 = r2.getMeasuredHeight();
        r2 = r7.mChild;
        r1 = r2.getMeasuredWidth();
        goto L_0x0037;
    L_0x0025:
        r2 = r7.mChild;
        r7.measureChild(r2, r8, r9);
        r2 = r7.mChild;
        r0 = r2.getMeasuredWidth();
        r2 = r7.mChild;
        r1 = r2.getMeasuredHeight();
    L_0x0037:
        r7.setMeasuredDimension(r0, r1);
        r2 = r7.mOrientation;
        r6 = 0;
        if (r2 == 0) goto L_0x006b;
    L_0x003f:
        if (r2 == r5) goto L_0x005f;
    L_0x0041:
        if (r2 == r4) goto L_0x0052;
    L_0x0043:
        if (r2 == r3) goto L_0x0046;
    L_0x0045:
        goto L_0x0076;
    L_0x0046:
        r2 = r7.mChild;
        r3 = (float) r0;
        r2.setTranslationX(r3);
        r2 = r7.mChild;
        r2.setTranslationY(r6);
        goto L_0x0076;
    L_0x0052:
        r2 = r7.mChild;
        r3 = (float) r0;
        r2.setTranslationX(r3);
        r2 = r7.mChild;
        r3 = (float) r1;
        r2.setTranslationY(r3);
        goto L_0x0076;
    L_0x005f:
        r2 = r7.mChild;
        r2.setTranslationX(r6);
        r2 = r7.mChild;
        r3 = (float) r1;
        r2.setTranslationY(r3);
        goto L_0x0076;
    L_0x006b:
        r2 = r7.mChild;
        r2.setTranslationX(r6);
        r2 = r7.mChild;
        r2.setTranslationY(r6);
    L_0x0076:
        r2 = r7.mChild;
        r3 = r7.mOrientation;
        r3 = -r3;
        r3 = (float) r3;
        r2.setRotation(r3);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.RotateLayout.onMeasure(int, int):void");
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void setOrientation(int orientation, boolean animation) {
        orientation %= 360;
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }
}
