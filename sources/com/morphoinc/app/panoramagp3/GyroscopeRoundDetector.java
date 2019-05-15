package com.morphoinc.app.panoramagp3;

import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class GyroscopeRoundDetector extends RoundDetector {
    private static final float NS2S = 1.0E-9f;
    private float mLastTimestamp;
    private float mRadianLandscape;
    private float mRadianPortrait;
    private float mTargetDegree;
    private boolean useSensor;

    public GyroscopeRoundDetector() {
        this.mTargetDegree = 360.0f;
        this.mClassName = "GyroscopeRoundDetector";
    }

    public void setStartPosition(int rotation, int direction, float wideH, float wideV, boolean make_360) {
        boolean z = rotation == 0 || rotation == MediaProviderUtils.ROTATION_180;
        this.isLandscape = z;
        synchronized (SynchronizedObject) {
            this.mLastTimestamp = 0.0f;
            this.mRadianLandscape = 0.0f;
            this.mRadianPortrait = 0.0f;
            this.mCurrentDegreeLandscape = 0;
            this.mCurrentDegreePortrait = 0;
            this.mDirection = direction;
            this.mIsEndOk = false;
            this.useSensor = true;
            if (!make_360) {
                if (this.isLandscape) {
                    this.mTargetDegree = (360.0f - wideH) + 20.0f;
                } else {
                    this.mTargetDegree = (360.0f - wideV) + 20.0f;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:18:0x0033, code skipped:
            return r2;
     */
    public boolean detect() {
        /*
        r10 = this;
        r0 = SynchronizedObject;
        monitor-enter(r0);
        r1 = r10.mIsEndOk;	 Catch:{ all -> 0x0034 }
        r2 = 0;
        if (r1 != 0) goto L_0x000a;
    L_0x0008:
        monitor-exit(r0);	 Catch:{ all -> 0x0034 }
        return r2;
    L_0x000a:
        r1 = r10.mDirection;	 Catch:{ all -> 0x0034 }
        r3 = 1;
        if (r1 != 0) goto L_0x001e;
    L_0x000f:
        r1 = r10.mTargetDegree;	 Catch:{ all -> 0x0034 }
        r4 = r10.currentDegree();	 Catch:{ all -> 0x0034 }
        r4 = (float) r4;	 Catch:{ all -> 0x0034 }
        r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1));
        if (r1 > 0) goto L_0x001c;
    L_0x001a:
        r2 = r3;
    L_0x001c:
        monitor-exit(r0);	 Catch:{ all -> 0x0034 }
        return r2;
    L_0x001e:
        r1 = r10.currentDegree();	 Catch:{ all -> 0x0034 }
        r4 = (double) r1;	 Catch:{ all -> 0x0034 }
        r6 = 4645040803167600640; // 0x4076800000000000 float:0.0 double:360.0;
        r1 = r10.mTargetDegree;	 Catch:{ all -> 0x0034 }
        r8 = (double) r1;	 Catch:{ all -> 0x0034 }
        r6 = r6 - r8;
        r1 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
        if (r1 > 0) goto L_0x0032;
    L_0x0030:
        r2 = r3;
    L_0x0032:
        monitor-exit(r0);	 Catch:{ all -> 0x0034 }
        return r2;
    L_0x0034:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0034 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.GyroscopeRoundDetector.detect():boolean");
    }

    public void stop() {
        synchronized (SynchronizedObject) {
            this.useSensor = false;
        }
    }

    /* JADX WARNING: Missing block: B:61:0x00bd, code skipped:
            return;
     */
    public void onSensorChanged(android.hardware.SensorEvent r11) {
        /*
        r10 = this;
        r0 = r11.sensor;
        r0 = r0.getType();
        r1 = 4;
        if (r0 == r1) goto L_0x0014;
    L_0x0009:
        r0 = r11.sensor;
        r0 = r0.getType();
        r1 = 16;
        if (r0 == r1) goto L_0x0014;
    L_0x0013:
        return;
    L_0x0014:
        r0 = SynchronizedObject;
        monitor-enter(r0);
        r1 = r10.useSensor;	 Catch:{ all -> 0x00be }
        if (r1 != 0) goto L_0x001d;
    L_0x001b:
        monitor-exit(r0);	 Catch:{ all -> 0x00be }
        return;
    L_0x001d:
        r1 = r10.mLastTimestamp;	 Catch:{ all -> 0x00be }
        r2 = 0;
        r1 = (r1 > r2 ? 1 : (r1 == r2 ? 0 : -1));
        r2 = 0;
        r3 = 1;
        if (r1 == 0) goto L_0x0046;
    L_0x0026:
        r4 = r11.timestamp;	 Catch:{ all -> 0x00be }
        r1 = (float) r4;	 Catch:{ all -> 0x00be }
        r4 = r10.mLastTimestamp;	 Catch:{ all -> 0x00be }
        r1 = r1 - r4;
        r4 = 814313567; // 0x3089705f float:1.0E-9 double:4.023243584E-315;
        r1 = r1 * r4;
        r4 = r11.values;	 Catch:{ all -> 0x00be }
        r4 = r4[r2];	 Catch:{ all -> 0x00be }
        r5 = r11.values;	 Catch:{ all -> 0x00be }
        r5 = r5[r3];	 Catch:{ all -> 0x00be }
        r6 = r10.mRadianLandscape;	 Catch:{ all -> 0x00be }
        r7 = r4 * r1;
        r6 = r6 + r7;
        r10.mRadianLandscape = r6;	 Catch:{ all -> 0x00be }
        r6 = r10.mRadianPortrait;	 Catch:{ all -> 0x00be }
        r7 = r5 * r1;
        r6 = r6 + r7;
        r10.mRadianPortrait = r6;	 Catch:{ all -> 0x00be }
    L_0x0046:
        r4 = r11.timestamp;	 Catch:{ all -> 0x00be }
        r1 = (float) r4;	 Catch:{ all -> 0x00be }
        r10.mLastTimestamp = r1;	 Catch:{ all -> 0x00be }
        r1 = r10.mRadianLandscape;	 Catch:{ all -> 0x00be }
        r1 = com.morphoinc.app.panoramagp3.RoundDetector.radianToDegree(r1);	 Catch:{ all -> 0x00be }
        r4 = r10.mRadianPortrait;	 Catch:{ all -> 0x00be }
        r4 = com.morphoinc.app.panoramagp3.RoundDetector.radianToDegree(r4);	 Catch:{ all -> 0x00be }
        if (r1 > 0) goto L_0x005f;
    L_0x0059:
        r5 = r10.mDirection;	 Catch:{ all -> 0x00be }
        if (r5 != r3) goto L_0x005f;
    L_0x005d:
        r1 = r1 + 360;
    L_0x005f:
        if (r4 > 0) goto L_0x0067;
    L_0x0061:
        r5 = r10.mDirection;	 Catch:{ all -> 0x00be }
        if (r5 != r3) goto L_0x0067;
    L_0x0065:
        r4 = r4 + 360;
    L_0x0067:
        r5 = r10.isLandscape;	 Catch:{ all -> 0x00be }
        if (r5 == 0) goto L_0x006d;
    L_0x006b:
        r5 = r1;
        goto L_0x006e;
    L_0x006d:
        r5 = r4;
    L_0x006e:
        r6 = r10.isLandscape;	 Catch:{ all -> 0x00be }
        if (r6 == 0) goto L_0x0075;
    L_0x0072:
        r6 = r10.mCurrentDegreeLandscape;	 Catch:{ all -> 0x00be }
        goto L_0x0077;
    L_0x0075:
        r6 = r10.mCurrentDegreePortrait;	 Catch:{ all -> 0x00be }
    L_0x0077:
        r7 = r10.mDirection;	 Catch:{ all -> 0x00be }
        r8 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r7 == 0) goto L_0x008e;
    L_0x007d:
        if (r6 == 0) goto L_0x008c;
    L_0x007f:
        if (r6 < r5) goto L_0x008a;
    L_0x0081:
        r7 = r5 - r6;
        r7 = java.lang.Math.abs(r7);	 Catch:{ all -> 0x00be }
        if (r7 >= r8) goto L_0x008a;
    L_0x0089:
        goto L_0x008c;
    L_0x008a:
        r7 = r2;
        goto L_0x009f;
    L_0x008c:
        r7 = r3;
        goto L_0x009f;
    L_0x008e:
        if (r6 == 0) goto L_0x009d;
    L_0x0090:
        if (r6 > r5) goto L_0x009b;
    L_0x0092:
        r7 = r5 - r6;
        r7 = java.lang.Math.abs(r7);	 Catch:{ all -> 0x00be }
        if (r7 >= r8) goto L_0x009b;
    L_0x009a:
        goto L_0x009d;
    L_0x009b:
        r7 = r2;
        goto L_0x009e;
    L_0x009d:
        r7 = r3;
        if (r7 == 0) goto L_0x00a6;
    L_0x00a2:
        r10.mCurrentDegreeLandscape = r1;	 Catch:{ all -> 0x00be }
        r10.mCurrentDegreePortrait = r4;	 Catch:{ all -> 0x00be }
    L_0x00a6:
        r9 = r10.mIsEndOk;	 Catch:{ all -> 0x00be }
        if (r9 != 0) goto L_0x00bc;
    L_0x00aa:
        r9 = r10.currentDegree();	 Catch:{ all -> 0x00be }
        if (r8 > r9) goto L_0x00ba;
    L_0x00b0:
        r8 = r10.currentDegree();	 Catch:{ all -> 0x00be }
        r9 = 190; // 0xbe float:2.66E-43 double:9.4E-322;
        if (r8 > r9) goto L_0x00ba;
    L_0x00b8:
        r2 = r3;
    L_0x00ba:
        r10.mIsEndOk = r2;	 Catch:{ all -> 0x00be }
    L_0x00bc:
        monitor-exit(r0);	 Catch:{ all -> 0x00be }
        return;
    L_0x00be:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x00be }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.GyroscopeRoundDetector.onSensorChanged(android.hardware.SensorEvent):void");
    }
}
