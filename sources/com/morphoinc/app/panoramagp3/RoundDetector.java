package com.morphoinc.app.panoramagp3;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Range;

public class RoundDetector implements SensorEventListener {
    protected static final float DETECT_DIRECTION_MARGIN_DEGREE = 20.0f;
    protected static final String LOG_TAG = "Camera2App";
    private static final int MATRIX_SIZE = 16;
    public static final Object SynchronizedObject = new Object();
    protected boolean isLandscape;
    private float[] mAccelerometerValues = new float[0];
    protected String mClassName = "RoundDetector";
    protected int mCurrentDegreeLandscape;
    protected int mCurrentDegreePortrait;
    protected int mDirection = 1;
    private LoopRange mEndDegreeRange = emptyRange();
    private float[] mI = new float[16];
    private float[] mInR = new float[16];
    protected boolean mIsEndOk;
    private float[] mMagneticValues = new float[0];
    private float[] mOutR = new float[16];
    private int mStartDegree;

    private class LoopRange {
        protected Range<Integer> mRange;
        protected boolean[] mRangePassed = new boolean[2];
        protected Range<Integer> mRangeSecond;

        public LoopRange(int lower, int upper, int max) {
            if (lower <= upper) {
                this.mRange = new Range(Integer.valueOf(lower), Integer.valueOf(upper));
                this.mRangeSecond = new Range(Integer.valueOf(-1), Integer.valueOf(-1));
                this.mRangePassed[0] = false;
                this.mRangePassed[1] = true;
                return;
            }
            this.mRange = new Range(Integer.valueOf(0), Integer.valueOf(Math.max(5, upper)));
            this.mRangeSecond = new Range(Integer.valueOf(Math.min(lower, max - 5)), Integer.valueOf(max));
            this.mRangePassed[0] = false;
            this.mRangePassed[1] = false;
        }

        public boolean contains(int value) {
            if (!this.mRangePassed[0]) {
                this.mRangePassed[0] = this.mRange.contains(Integer.valueOf(value));
            }
            if (!this.mRangePassed[1]) {
                this.mRangePassed[1] = this.mRangeSecond.contains(Integer.valueOf(value));
            }
            if (this.mRangeSecond.contains(Integer.valueOf(value)) && this.mRangePassed[0]) {
                this.mRangePassed[0] = false;
            }
            if (this.mRangePassed[0] && this.mRangePassed[1]) {
                return true;
            }
            return false;
        }

        public String toString() {
            if (((Integer) this.mRangeSecond.getUpper()).intValue() < 0) {
                return this.mRange.toString();
            }
            return String.format("%s, %s", new Object[]{this.mRangeSecond.toString(), this.mRange.toString()});
        }
    }

    private class LoopRangeLeft extends LoopRange {
        public LoopRangeLeft(int lower, int upper, int max) {
            super(lower, upper, max);
            if (lower > upper) {
                this.mRange = new Range(Integer.valueOf(Math.min(lower, max - 5)), Integer.valueOf(max));
                this.mRangeSecond = new Range(Integer.valueOf(0), Integer.valueOf(Math.max(5, upper)));
            }
        }

        public String toString() {
            if (((Integer) this.mRangeSecond.getUpper()).intValue() < 0) {
                return this.mRange.toString();
            }
            return String.format("%s, %s", new Object[]{this.mRange.toString(), this.mRangeSecond.toString()});
        }
    }

    private LoopRange emptyRange() {
        return new LoopRange(-1, -1, 360);
    }

    public String getName() {
        return this.mClassName;
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x004d A:{Catch:{ all -> 0x002d }} */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0057 A:{Catch:{ all -> 0x002d }} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0051 A:{Catch:{ all -> 0x002d }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0078 A:{Catch:{ all -> 0x002d }} */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006c A:{Catch:{ all -> 0x002d }} */
    public void setStartPosition(int r19, int r20, float r21, float r22, boolean r23) {
        /*
        r18 = this;
        r1 = r18;
        r2 = r19;
        r3 = r20;
        r4 = 0;
        if (r2 == 0) goto L_0x0010;
    L_0x0009:
        r5 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r2 != r5) goto L_0x000e;
    L_0x000d:
        goto L_0x0010;
    L_0x000e:
        r5 = r4;
        goto L_0x0011;
    L_0x0010:
        r5 = 1;
    L_0x0011:
        r1.isLandscape = r5;
        r5 = r1.isLandscape;
        if (r5 == 0) goto L_0x001e;
    L_0x0017:
        r5 = r21;
        r6 = (int) r5;
        r7 = r6;
        r6 = r22;
        goto L_0x0023;
    L_0x001e:
        r5 = r21;
        r6 = r22;
        r7 = (int) r6;
    L_0x0023:
        r8 = SynchronizedObject;
        monitor-enter(r8);
        if (r23 == 0) goto L_0x0030;
    L_0x0028:
        r9 = r18.currentDegree();	 Catch:{ all -> 0x002d }
    L_0x002c:
        goto L_0x0049;
    L_0x002d:
        r0 = move-exception;
        goto L_0x00c8;
    L_0x0030:
        if (r3 != 0) goto L_0x003e;
    L_0x0032:
        r9 = r18.currentDegree();	 Catch:{ all -> 0x002d }
        r10 = r7 + -20;
        r9 = r9 + r10;
        r9 = correctionCircleDegree(r9);	 Catch:{ all -> 0x002d }
        goto L_0x002c;
    L_0x003e:
        r9 = r18.currentDegree();	 Catch:{ all -> 0x002d }
        r10 = r7 + -20;
        r9 = r9 - r10;
        r9 = correctionCircleDegree(r9);	 Catch:{ all -> 0x002d }
    L_0x0049:
        r10 = 2;
        switch(r3) {
            case 0: goto L_0x0057;
            case 1: goto L_0x0051;
            default: goto L_0x004d;
        };	 Catch:{ all -> 0x002d }
    L_0x004d:
        r0 = 0;
        r1.mIsEndOk = r0;	 Catch:{ all -> 0x002d }
        goto L_0x00b9;
    L_0x0051:
        r11 = r7 * 3;
        r11 = r11 / r10;
        r11 = r9 - r11;
        goto L_0x005a;
    L_0x0057:
        r11 = r9 + r7;
        r12 = r7 / 2;
        r12 = r12 + r11;
        r13 = correctionCircleDegree(r11);	 Catch:{ all -> 0x002d }
        r11 = r13;
        r13 = correctionCircleDegree(r12);	 Catch:{ all -> 0x002d }
        r12 = r13;
        r13 = 360; // 0x168 float:5.04E-43 double:1.78E-321;
        if (r3 != 0) goto L_0x0078;
    L_0x006c:
        if (r11 >= r9) goto L_0x0070;
    L_0x006e:
        r11 = 360; // 0x168 float:5.04E-43 double:1.78E-321;
    L_0x0070:
        r14 = new com.morphoinc.app.panoramagp3.RoundDetector$LoopRangeLeft;	 Catch:{ all -> 0x002d }
        r14.<init>(r11, r12, r13);	 Catch:{ all -> 0x002d }
        r1.mEndDegreeRange = r14;	 Catch:{ all -> 0x002d }
        goto L_0x0082;
    L_0x0078:
        if (r9 >= r12) goto L_0x007b;
    L_0x007a:
        r12 = 0;
    L_0x007b:
        r14 = new com.morphoinc.app.panoramagp3.RoundDetector$LoopRange;	 Catch:{ all -> 0x002d }
        r14.<init>(r11, r12, r13);	 Catch:{ all -> 0x002d }
        r1.mEndDegreeRange = r14;	 Catch:{ all -> 0x002d }
    L_0x0082:
        r1.mDirection = r3;	 Catch:{ all -> 0x002d }
        r1.mStartDegree = r9;	 Catch:{ all -> 0x002d }
        r1.mIsEndOk = r4;	 Catch:{ all -> 0x002d }
        r13 = "Camera2App";
        r14 = java.util.Locale.US;	 Catch:{ all -> 0x002d }
        r10 = "%s, start:%d(>>%d)";
        r0 = 3;
        r0 = new java.lang.Object[r0];	 Catch:{ all -> 0x002d }
        r4 = r1.mEndDegreeRange;	 Catch:{ all -> 0x002d }
        r4 = r4.toString();	 Catch:{ all -> 0x002d }
        r17 = 0;
        r0[r17] = r4;	 Catch:{ all -> 0x002d }
        r4 = r18.currentDegree();	 Catch:{ all -> 0x002d }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ all -> 0x002d }
        r16 = 1;
        r0[r16] = r4;	 Catch:{ all -> 0x002d }
        r4 = r1.mStartDegree;	 Catch:{ all -> 0x002d }
        r4 = java.lang.Integer.valueOf(r4);	 Catch:{ all -> 0x002d }
        r15 = 2;
        r0[r15] = r4;	 Catch:{ all -> 0x002d }
        r0 = java.lang.String.format(r14, r10, r0);	 Catch:{ all -> 0x002d }
        com.morphoinc.app.LogFilter.d(r13, r0);	 Catch:{ all -> 0x002d }
        monitor-exit(r8);	 Catch:{ all -> 0x002d }
        return;
    L_0x00b9:
        r0 = r18.emptyRange();	 Catch:{ all -> 0x002d }
        r1.mEndDegreeRange = r0;	 Catch:{ all -> 0x002d }
        r0 = "Camera2App";
        r4 = "Unsupported Direction.";
        com.morphoinc.app.LogFilter.e(r0, r4);	 Catch:{ all -> 0x002d }
        monitor-exit(r8);	 Catch:{ all -> 0x002d }
        return;
    L_0x00c8:
        monitor-exit(r8);	 Catch:{ all -> 0x002d }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.RoundDetector.setStartPosition(int, int, float, float, boolean):void");
    }

    /* Access modifiers changed, original: protected */
    public int currentDegree() {
        return this.isLandscape ? this.mCurrentDegreeLandscape : this.mCurrentDegreePortrait;
    }

    /* Access modifiers changed, original: protected */
    public int currentDegree0Base() {
        int degree = currentDegree();
        if (this.mDirection != 0) {
            return 360 - degree;
        }
        return degree;
    }

    /* JADX WARNING: Missing block: B:13:0x001a, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:18:0x0026, code skipped:
            return r2;
     */
    public boolean detect() {
        /*
        r5 = this;
        r0 = SynchronizedObject;
        monitor-enter(r0);
        r1 = r5.mIsEndOk;	 Catch:{ all -> 0x0027 }
        r2 = 0;
        if (r1 != 0) goto L_0x000a;
    L_0x0008:
        monitor-exit(r0);	 Catch:{ all -> 0x0027 }
        return r2;
    L_0x000a:
        r1 = r5.mDirection;	 Catch:{ all -> 0x0027 }
        r3 = 1;
        if (r1 != 0) goto L_0x001b;
    L_0x000f:
        r1 = r5.mStartDegree;	 Catch:{ all -> 0x0027 }
        r4 = r5.currentDegree();	 Catch:{ all -> 0x0027 }
        if (r1 < r4) goto L_0x0019;
    L_0x0017:
        r2 = r3;
    L_0x0019:
        monitor-exit(r0);	 Catch:{ all -> 0x0027 }
        return r2;
    L_0x001b:
        r1 = r5.mStartDegree;	 Catch:{ all -> 0x0027 }
        r4 = r5.currentDegree();	 Catch:{ all -> 0x0027 }
        if (r1 > r4) goto L_0x0025;
    L_0x0023:
        r2 = r3;
    L_0x0025:
        monitor-exit(r0);	 Catch:{ all -> 0x0027 }
        return r2;
    L_0x0027:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0027 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.RoundDetector.detect():boolean");
    }

    public void stop() {
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case 1:
                this.mAccelerometerValues = (float[]) event.values.clone();
                break;
            case 2:
                this.mMagneticValues = (float[]) event.values.clone();
                break;
            default:
                return;
        }
        if (this.mMagneticValues.length > 0 && this.mAccelerometerValues.length > 0) {
            SensorManager.getRotationMatrix(this.mInR, this.mI, this.mAccelerometerValues, this.mMagneticValues);
            float[] orientationValues = new float[3];
            SensorManager.remapCoordinateSystem(this.mInR, 3, Const.CODE_C1_CW1, this.mOutR);
            SensorManager.getOrientation(this.mOutR, orientationValues);
            int degreeLandscape = radianToDegree(orientationValues[0]);
            if (degreeLandscape < 0) {
                degreeLandscape += 360;
            }
            SensorManager.remapCoordinateSystem(this.mInR, 1, 3, this.mOutR);
            SensorManager.getOrientation(this.mOutR, orientationValues);
            int degreePortrait = radianToDegree(orientationValues[0]);
            if (degreePortrait < 0) {
                degreePortrait += 360;
            }
            synchronized (SynchronizedObject) {
                this.mCurrentDegreeLandscape = degreeLandscape;
                this.mCurrentDegreePortrait = degreePortrait;
                this.mIsEndOk = this.mEndDegreeRange.contains(currentDegree());
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected static int radianToDegree(float radian) {
        return (int) Math.floor(Math.toDegrees((double) radian));
    }

    private static int correctionCircleDegree(int degree) {
        if (degree < 0) {
            return degree + 360;
        }
        if (360 < degree) {
            return degree - 360;
        }
        return degree;
    }
}
