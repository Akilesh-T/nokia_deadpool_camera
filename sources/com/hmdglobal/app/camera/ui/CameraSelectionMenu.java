package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import com.hmdglobal.app.camera.ButtonManager;
import com.hmdglobal.app.camera.MultiToggleImageButton;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.beauty.util.Util;
import com.hmdglobal.app.camera.util.LockUtils;
import com.hmdglobal.app.camera.util.LockUtils.LockType;

public class CameraSelectionMenu extends LinearLayout implements OnClickListener {
    private ButtonManager mBM;
    private MultiToggleImageButton mButton;
    private AppController mController;
    private LinearLayout mMenuContainer;
    private Resources mResources;
    private int mState;
    private View mTopViewContainer;

    public CameraSelectionMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mResources = context.getResources();
        initSubViews();
    }

    private void initSubViews() {
        LayoutParams paramsTop = new LayoutParams(-1, Util.dp2px(getContext(), 30.0f));
        this.mTopViewContainer = new View(getContext());
        this.mTopViewContainer.setBackgroundResource(R.color.selectiomenu_black);
        LayoutParams paramsBottom = new LayoutParams(-1, Util.dp2px(getContext(), 63.0f));
        this.mMenuContainer = new LinearLayout(getContext());
        this.mMenuContainer.setBackgroundResource(R.color.selectiomenu_black);
        this.mMenuContainer.getBackground().setAlpha(200);
        this.mMenuContainer.setOrientation(0);
        setOrientation(1);
        addView(this.mTopViewContainer, paramsTop);
        addView(this.mMenuContainer, 1, paramsBottom);
        requestLayout();
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x016d  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x016d  */
    public void bindButtonAndIcons(com.hmdglobal.app.camera.app.AppController r21, com.hmdglobal.app.camera.MultiToggleImageButton r22, int r23, int r24) {
        /*
        r20 = this;
        r1 = r20;
        r2 = r21;
        r0 = r1.mController;
        if (r0 != 0) goto L_0x000a;
    L_0x0008:
        r1.mController = r2;
    L_0x000a:
        r3 = r22;
        r1.mButton = r3;
        r0 = 0;
        r4 = r0;
        r5 = r20.getResources();	 Catch:{ all -> 0x0166 }
        r6 = r23;
        r5 = r5.obtainTypedArray(r6);	 Catch:{ all -> 0x0164 }
        r4 = r5;
        r5 = r4.length();	 Catch:{ all -> 0x015e }
        r5 = new int[r5];	 Catch:{ all -> 0x015e }
        r7 = 0;
        r8 = r7;
    L_0x0023:
        r9 = r4.length();	 Catch:{ all -> 0x015e }
        if (r8 >= r9) goto L_0x0032;
    L_0x0029:
        r9 = r4.getResourceId(r8, r7);	 Catch:{ all -> 0x0164 }
        r5[r8] = r9;	 Catch:{ all -> 0x0164 }
        r8 = r8 + 1;
        goto L_0x0023;
    L_0x0032:
        if (r4 == 0) goto L_0x0037;
    L_0x0034:
        r4.recycle();
    L_0x0037:
        r8 = r20.getResources();
        r9 = r24;
        r8 = r8.getStringArray(r9);
        r10 = r1.mMenuContainer;
        r10 = r10.getChildCount();
        if (r10 <= 0) goto L_0x004e;
    L_0x0049:
        r10 = r1.mMenuContainer;
        r10.removeAllViews();
    L_0x004e:
        r10 = r1.mMenuContainer;
        r11 = r5.length;
        r11 = (float) r11;
        r10.setWeightSum(r11);
        r10 = r7;
    L_0x0056:
        r11 = r5.length;
        r12 = 2131231224; // 0x7f0801f8 float:1.8078523E38 double:1.052968131E-314;
        if (r10 >= r11) goto L_0x011e;
    L_0x005c:
        r11 = new com.hmdglobal.app.camera.ui.RotateImageView;
        r14 = r20.getContext();
        r11.<init>(r14);
        r14 = new android.widget.LinearLayout$LayoutParams;
        r15 = r20.getContext();
        r13 = 1110048768; // 0x422a0000 float:42.5 double:5.484369615E-315;
        r15 = com.hmdglobal.app.camera.beauty.util.Util.dp2px(r15, r13);
        r0 = r20.getContext();
        r0 = com.hmdglobal.app.camera.beauty.util.Util.dp2px(r0, r13);
        r14.<init>(r15, r0);
        r0 = r14;
        r13 = r5[r10];
        r11.setImageResource(r13);
        r11.setSelected(r7);
        r13 = 17;
        r0.gravity = r13;
        r14 = new android.widget.TextView;
        r15 = r20.getContext();
        r14.<init>(r15);
        r15 = new android.widget.LinearLayout$LayoutParams;
        r13 = -2;
        r15.<init>(r13, r13);
        r14.setId(r12);
        r12 = r8[r10];
        r14.setText(r12);
        r12 = 1098907648; // 0x41800000 float:16.0 double:5.42932517E-315;
        r14.setTextSize(r12);
        r14.setSelected(r7);
        r12 = r20.getResources();
        r7 = 2131034314; // 0x7f0500ca float:1.7679142E38 double:1.0528708447E-314;
        r7 = r12.getColor(r7);
        r14.setTextColor(r7);
        r7 = 17;
        r15.gravity = r7;
        r7 = new android.widget.LinearLayout;
        r12 = r20.getContext();
        r13 = 0;
        r7.<init>(r12, r13);
        r12 = new android.widget.LinearLayout$LayoutParams;
        r13 = -1;
        r12.<init>(r13, r13);
        r13 = 2131230998; // 0x7f080116 float:1.8078065E38 double:1.0529680195E-314;
        r7.setId(r13);
        r13 = 1;
        r7.setOrientation(r13);
        r7.addView(r11, r0);
        r7.addView(r14, r13, r15);
        r13 = new com.hmdglobal.app.camera.ui.RotateLayout;
        r16 = r0;
        r0 = r20.getContext();
        r17 = r4;
        r4 = 0;
        r13.<init>(r0, r4);
        r0 = r13;
        r13 = new android.widget.LinearLayout$LayoutParams;
        r18 = r5;
        r4 = 0;
        r5 = -2;
        r13.<init>(r4, r5);
        r5 = r13;
        r13 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r5.weight = r13;
        r13 = 2131231156; // 0x7f0801b4 float:1.8078385E38 double:1.0529680975E-314;
        r0.setId(r13);
        r0.setOnClickListener(r1);
        r0.addView(r7, r12);
        r0.onFinishInflate();
        r13 = r1.mMenuContainer;
        r13.addView(r0, r5);
        r13 = new com.hmdglobal.app.camera.ui.Rotatable$RotateEntity;
        r4 = 1;
        r13.<init>(r0, r4);
        r2.addRotatableToListenerPool(r13);
        r10 = r10 + 1;
        r4 = r17;
        r5 = r18;
        r0 = 0;
        r7 = 0;
        goto L_0x0056;
    L_0x011e:
        r17 = r4;
        r18 = r5;
        r20.requestLayout();
        r0 = r22.getState();
        r1.mState = r0;
        r0 = r1.mMenuContainer;
        r4 = r1.mState;
        r0 = r0.getChildAt(r4);
        if (r0 == 0) goto L_0x0141;
    L_0x0135:
        r0 = r1.mMenuContainer;
        r4 = r1.mState;
        r0 = r0.getChildAt(r4);
        r4 = 1;
        r0.setSelected(r4);
    L_0x0141:
        r0 = r1.mMenuContainer;
        r4 = r1.mState;
        r0 = r0.getChildAt(r4);
        r0 = r0.findViewById(r12);
        r0 = (android.widget.TextView) r0;
        r4 = r20.getResources();
        r5 = 2131034315; // 0x7f0500cb float:1.7679144E38 double:1.052870845E-314;
        r4 = r4.getColor(r5);
        r0.setTextColor(r4);
        return;
    L_0x015e:
        r0 = move-exception;
        r9 = r24;
        r17 = r4;
        goto L_0x016b;
    L_0x0164:
        r0 = move-exception;
        goto L_0x0169;
    L_0x0166:
        r0 = move-exception;
        r6 = r23;
    L_0x0169:
        r9 = r24;
    L_0x016b:
        if (r4 == 0) goto L_0x0170;
    L_0x016d:
        r4.recycle();
    L_0x0170:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.CameraSelectionMenu.bindButtonAndIcons(com.hmdglobal.app.camera.app.AppController, com.hmdglobal.app.camera.MultiToggleImageButton, int, int):void");
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        this.mController.removeRotatableFromListenerPool(child.hashCode());
    }

    public void setButtonManager(ButtonManager bm) {
        this.mBM = bm;
    }

    public void onClick(View v) {
        setState(this.mMenuContainer.indexOfChild(v));
    }

    public void onMenuCanceled() {
        if (this.mButton != null) {
            this.mButton.setState(this.mState);
        }
    }

    public void onMenuClosed() {
        this.mMenuContainer.removeAllViews();
        LockUtils.getInstance().generateMultiLock(LockType.MULTILOCK).ignoreLock(false);
    }

    private void setState(int state) {
        this.mMenuContainer.getChildAt(this.mState).setSelected(false);
        this.mMenuContainer.getChildAt(state).setSelected(true);
        if (this.mButton != null) {
            this.mButton.setState(state);
        }
        setVisibility(0);
        this.mBM.toggleSelectionMenu();
        this.mState = state;
    }

    public MultiToggleImageButton getBindButton() {
        return this.mButton;
    }
}
