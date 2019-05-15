package com.android.ex.camera2.portability;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$T9Z_aZahj6zP0kcfRPhjBB1PwIM implements Runnable {
    private final /* synthetic */ AndroidCamera2ProxyImpl f$0;
    private final /* synthetic */ CaptureAvailableListener f$1;

    public /* synthetic */ -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$T9Z_aZahj6zP0kcfRPhjBB1PwIM(AndroidCamera2ProxyImpl androidCamera2ProxyImpl, CaptureAvailableListener captureAvailableListener) {
        this.f$0 = androidCamera2ProxyImpl;
        this.f$1 = captureAvailableListener;
    }

    public final void run() {
        AndroidCamera2ProxyImpl.lambda$takePicture$0(this.f$0, this.f$1);
    }
}
