package com.android.ex.camera2.portability;

import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AndroidCamera2AgentImpl$Camera2Handler$RwBTxUHV_ZNMMA8uo7LM8OEDehM implements OnImageAvailableListener {
    private final /* synthetic */ Camera2Handler f$0;

    public /* synthetic */ -$$Lambda$AndroidCamera2AgentImpl$Camera2Handler$RwBTxUHV_ZNMMA8uo7LM8OEDehM(Camera2Handler camera2Handler) {
        this.f$0 = camera2Handler;
    }

    public final void onImageAvailable(ImageReader imageReader) {
        Camera2Handler.lambda$handleMessage$0(this.f$0, imageReader);
    }
}
