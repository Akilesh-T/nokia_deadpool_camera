package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageButton;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class ExternalViewerButton extends ImageButton {
    private static final Tag TAG = new Tag("ExtViewerButton");
    private final SparseArray<Cling> mClingMap = new SparseArray();
    private int mState = 0;

    public ExternalViewerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        updateClingVisibility();
    }

    /* Access modifiers changed, original: protected */
    public void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        if (this.mClingMap != null) {
            updateClingVisibility();
        }
    }

    public void setClingForViewer(int viewerType, Cling cling) {
        if (cling == null) {
            Log.w(TAG, "Cannot set a null cling for viewer");
            return;
        }
        this.mClingMap.put(viewerType, cling);
        cling.setReferenceView(this);
    }

    public void clearClingForViewer(int viewerType) {
        Cling cling = (Cling) this.mClingMap.get(viewerType);
        if (cling == null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cling does not exist for the given viewer type: ");
            stringBuilder.append(viewerType);
            Log.w(tag, stringBuilder.toString());
        }
        cling.setReferenceView(null);
        this.mClingMap.remove(viewerType);
    }

    public Cling getClingForViewer(int viewerType) {
        return (Cling) this.mClingMap.get(viewerType);
    }

    public void setState(int state) {
        int newVisibility;
        this.mState = state;
        if (state == 0) {
            newVisibility = 8;
        } else {
            setImageResource(getViewButtonResource(state));
            newVisibility = 0;
        }
        if (newVisibility != getVisibility()) {
            setVisibility(newVisibility);
        } else if (newVisibility == 0) {
            updateClingVisibility();
        }
    }

    public void hideClings() {
        for (int i = 0; i < this.mClingMap.size(); i++) {
            ((Cling) this.mClingMap.valueAt(i)).setVisibility(4);
        }
    }

    private int getViewButtonResource(int state) {
        switch (state) {
            case 1:
                return R.drawable.ic_view_photosphere;
            case 2:
                return R.drawable.ic_refocus_normal;
            default:
                return R.drawable.ic_control_play;
        }
    }

    public void updateClingVisibility() {
        hideClings();
        if (isShown()) {
            Cling cling = (Cling) this.mClingMap.get(this.mState);
            if (cling != null) {
                cling.adjustPosition();
                cling.setVisibility(0);
            }
        }
    }
}
