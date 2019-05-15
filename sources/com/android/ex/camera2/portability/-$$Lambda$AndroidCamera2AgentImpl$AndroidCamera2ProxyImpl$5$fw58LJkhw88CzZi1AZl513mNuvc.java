package com.android.ex.camera2.portability;

import com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.AnonymousClass5;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$5$fw58LJkhw88CzZi1AZl513mNuvc implements Runnable {
    private final /* synthetic */ AnonymousClass5 f$0;
    private final /* synthetic */ CameraShutterCallback f$1;

    public /* synthetic */ -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$5$fw58LJkhw88CzZi1AZl513mNuvc(AnonymousClass5 anonymousClass5, CameraShutterCallback cameraShutterCallback) {
        this.f$0 = anonymousClass5;
        this.f$1 = cameraShutterCallback;
    }

    public final void run() {
        this.f$1.onShutter(AndroidCamera2ProxyImpl.this);
    }
}
