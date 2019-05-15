package com.hmdglobal.app.camera.ui;

import android.view.View;

public interface ArcSeekBarEventListener {
    void OnProgressChanged(int i, int i2);

    void onMovementChanged(View view, boolean z);
}
