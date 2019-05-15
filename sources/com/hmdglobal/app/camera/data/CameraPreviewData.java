package com.hmdglobal.app.camera.data;

import android.view.View;
import com.hmdglobal.app.camera.R;

public class CameraPreviewData extends SimpleViewData {
    private boolean mPreviewLocked = true;

    public CameraPreviewData(View v, int width, int height) {
        super(v, LocalDataViewType.CAMERA_PREVIEW, width, height, -1, -1);
        v.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(LocalDataViewType.CAMERA_PREVIEW.ordinal()));
    }

    public int getViewType() {
        return 1;
    }

    public int getLocalDataType() {
        return 1;
    }

    public boolean canSwipeInFullScreen() {
        return this.mPreviewLocked ^ 1;
    }

    public void lockPreview(boolean lock) {
        this.mPreviewLocked = lock;
    }
}
