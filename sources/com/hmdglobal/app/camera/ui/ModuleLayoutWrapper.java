package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.app.AppController;

public class ModuleLayoutWrapper extends FrameLayout {
    private AppController mAppController;
    private OnAllViewRemovedListener mListener;

    public interface OnAllViewRemovedListener {
        void onAllViewRemoved(AppController appController);
    }

    public ModuleLayoutWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ModuleLayoutWrapper(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ModuleLayoutWrapper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnAllViewRemovedListener(OnAllViewRemovedListener listener, AppController controllerToBeReturned) {
        this.mListener = listener;
        this.mAppController = controllerToBeReturned;
    }

    public void removeAllViews() {
        super.removeAllViews();
        if (this.mListener != null) {
            this.mListener.onAllViewRemoved(this.mAppController);
        }
    }
}
