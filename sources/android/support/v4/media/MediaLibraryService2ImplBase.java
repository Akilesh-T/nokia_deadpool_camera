package android.support.v4.media;

import android.support.v4.media.MediaLibraryService2.MediaLibrarySession;

class MediaLibraryService2ImplBase extends MediaSessionService2ImplBase {
    MediaLibraryService2ImplBase() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0030  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x002b  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0030  */
    public android.os.IBinder onBind(android.content.Intent r4) {
        /*
        r3 = this;
        r0 = r4.getAction();
        r1 = r0.hashCode();
        r2 = 901933117; // 0x35c2683d float:1.4484464E-6 double:4.45614168E-315;
        if (r1 == r2) goto L_0x001d;
    L_0x000d:
        r2 = 1665850838; // 0x634addd6 float:3.7422273E21 double:8.2303967E-315;
        if (r1 == r2) goto L_0x0013;
    L_0x0012:
        goto L_0x0027;
    L_0x0013:
        r1 = "android.media.browse.MediaBrowserService";
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x0027;
    L_0x001b:
        r0 = 1;
        goto L_0x0028;
    L_0x001d:
        r1 = "android.media.MediaLibraryService2";
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x0027;
    L_0x0025:
        r0 = 0;
        goto L_0x0028;
    L_0x0027:
        r0 = -1;
    L_0x0028:
        switch(r0) {
            case 0: goto L_0x003d;
            case 1: goto L_0x0030;
            default: goto L_0x002b;
        };
    L_0x002b:
        r0 = super.onBind(r4);
        return r0;
    L_0x0030:
        r0 = r3.getSession();
        r0 = r0.getImpl();
        r0 = r0.getLegacySessionBinder();
        return r0;
    L_0x003d:
        r0 = r3.getSession();
        r0 = r0.getSessionBinder();
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.MediaLibraryService2ImplBase.onBind(android.content.Intent):android.os.IBinder");
    }

    public MediaLibrarySession getSession() {
        return (MediaLibrarySession) super.getSession();
    }

    public int getSessionType() {
        return 2;
    }
}
