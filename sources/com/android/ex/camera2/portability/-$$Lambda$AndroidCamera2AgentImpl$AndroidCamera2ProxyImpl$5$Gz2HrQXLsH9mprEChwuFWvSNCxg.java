package com.android.ex.camera2.portability;

import com.android.ex.camera2.portability.AndroidCamera2AgentImpl.AndroidCamera2ProxyImpl.AnonymousClass5;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$5$Gz2HrQXLsH9mprEChwuFWvSNCxg implements Runnable {
    private final /* synthetic */ AnonymousClass5 f$0;
    private final /* synthetic */ CameraPictureCallback f$1;
    private final /* synthetic */ byte[] f$2;

    public /* synthetic */ -$$Lambda$AndroidCamera2AgentImpl$AndroidCamera2ProxyImpl$5$Gz2HrQXLsH9mprEChwuFWvSNCxg(AnonymousClass5 anonymousClass5, CameraPictureCallback cameraPictureCallback, byte[] bArr) {
        this.f$0 = anonymousClass5;
        this.f$1 = cameraPictureCallback;
        this.f$2 = bArr;
    }

    public final void run() {
        this.f$1.onPictureTaken(this.f$2, AndroidCamera2ProxyImpl.this);
    }
}
