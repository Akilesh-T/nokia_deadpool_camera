package com.android.grafika;

import android.view.Surface;

interface IVideoEncoderCore {
    void drainEncoder(boolean z);

    Surface getInputSurface();

    void release();
}
