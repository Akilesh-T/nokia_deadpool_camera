package com.hmdglobal.app.camera.instantcapture;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.hmdglobal.app.camera.FyuseAPI;
import com.hmdglobal.app.camera.GrantAccessActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.PermissionsUtil;

public class InstantCaptureService extends Service {
    public static final int ACTIVITY_LAUNCH_PROTECT = 2;
    public static final int ACTIVITY_LAUNCH_PROTECT_TIMEOUT = 400;
    private static final String BOOMKEY_DOUBLE_PRESSS = "boom_double_press";
    private static final String BOOMKEY_DOWN_SCREEN_OFF = "down_screen_off";
    private static final String BOOMKEY_DOWN_SCREEN_OFF_GLOBAL = "global_down_screen_off";
    private static final String BOOMKEY_DOWN_SCREEN_ON = "down";
    private static final String BOOMKEY_DOWN_SCREEN_ON_GLOBAL = "global_down";
    private static final String BOOMKEY_LONG_PRESSS = "boom_long_press";
    private static final String BOOMKEY_SINGLE_PRESSS = "boom_single_press";
    private static final String BOOMKEY_UP = "up";
    private static final String BOOMKEY_UP_GLOBAL = "global_up";
    public static final int CAPTURE_TIMEOUT = 7;
    public static final int CAPTURE_TIMEOUT_DELAY = 3500;
    public static final int JUDGE_TIMEOUT = 3;
    public static final int JUDGE_TIMEOUT_DELAY = 1000;
    public static final int LOW_BATTERY_LEVEL = 5;
    public static final int NO_DATA_RETURN_ERROR = 6;
    public static final int NO_DATA_RETURN_TIMEOUT = 500;
    public static final int PICTURE_CAPTURED_MSG = 1;
    public static final int PREPARE_CAMERA = 1;
    public static final int RELEASE_CAMERA = 3;
    public static final int SET_DISPLAY_ORIENTATION = 4;
    private static final Tag TAG = new Tag("InstantService");
    private static final int TYPE_LOW_BATTERY = 0;
    private static final int TYPE_LOW_BATTERY_AND_STORAGE = 2;
    private static final int TYPE_LOW_STORAGE = 1;
    private static final int TYPE_PERMISSION_NEED = 3;
    private final CaptureState mCaptureState = new CaptureState();
    private InstantCaptureHelper mInstantCaptureHelper;
    private Handler mMainHandler;
    private boolean mPendingOpenApp = false;
    private ServiceHandler mServiceHandler;
    private int mWarningType = -1;

    private final class CameraConflictException extends RuntimeException {
        public CameraConflictException(String msg) {
            super(msg);
        }
    }

    public class CaptureState {
        public static final int CAMERA_ERROR = 4;
        public static final int CAMERA_JPEG_TIMEOUT = 5;
        public static final int CAMERA_PENDING_STOP = 10;
        public static final int CAMERA_PREPARED = 3;
        public static final int CAMERA_PREPAREING = 2;
        public static final int CAMERA_RELEASING = 9;
        public static final int CAMERA_SNAPSHOT_IN_PROGRESS = 6;
        public static final int CAMERA_SNAPSHOT_LONGSHOT = 7;
        public static final int CAMERA_SNAPSHOT_LONGSHOT_PENDING_STOP = 8;
        public static final int CAMERA_UNOPENED = 1;
        public static final int CAPTURE_WITH_SINGLE_CLICK = 7;
        public static final int CAPTURE_WITH_SINGLE_CLICK_DONE = 8;
        public static final int DEFINITIVE_KEY_TIMEOUT = 9;
        public static final int DOUBLE_CLICK = 4;
        public static final int FIRST_KEY_DOWN = 1;
        public static final int LONG_PRESS_BURST = 5;
        public static final int LONG_PRESS_STOP = 6;
        public static final int SINGLE_CLICK = 2;
        public static final int SINGLE_CLICK_WITH_OPEN_APP = 3;
        private int mCameraState = 1;
        private int mKeyState;

        private void onCurrentState() {
            int i = this.mCameraState;
            if (i != 1) {
                if (i != 7) {
                    if (i != 10) {
                        switch (i) {
                            case 3:
                                if (this.mKeyState == 4 || this.mKeyState == 5 || this.mKeyState == 6 || this.mKeyState == 8) {
                                    if (this.mKeyState == 4 || this.mKeyState == 8) {
                                        this.mCameraState = 6;
                                    } else if (this.mKeyState == 5) {
                                        this.mCameraState = 7;
                                    } else {
                                        this.mCameraState = 8;
                                    }
                                    try {
                                        InstantCaptureService.this.mInstantCaptureHelper.capture();
                                        return;
                                    } catch (Exception e) {
                                        Log.i(InstantCaptureService.TAG, "capture exception  ", e);
                                        changeCameraState(4);
                                        return;
                                    }
                                } else if (this.mKeyState == 1) {
                                    Log.i(InstantCaptureService.TAG, "camera prepared, waiting more user input");
                                    return;
                                } else {
                                    this.mCameraState = 9;
                                    InstantCaptureService.this.mServiceHandler.sendEmptyMessage(3);
                                    return;
                                }
                            case 4:
                                if (this.mKeyState != 1) {
                                    InstantCaptureService.this.mInstantCaptureHelper.setForbidStartViewImageActivity(true);
                                    InstantCaptureService.this.mInstantCaptureHelper.dismissViewImageActivity();
                                    this.mCameraState = 9;
                                    InstantCaptureService.this.mServiceHandler.sendEmptyMessage(3);
                                    return;
                                }
                                return;
                            case 5:
                                this.mCameraState = 9;
                                InstantCaptureService.this.mServiceHandler.sendEmptyMessage(3);
                                return;
                            default:
                                return;
                        }
                    }
                    this.mCameraState = 9;
                    InstantCaptureService.this.mServiceHandler.sendEmptyMessage(3);
                } else if (this.mKeyState == 6) {
                    this.mCameraState = 8;
                }
            } else if (this.mKeyState == 1) {
                this.mCameraState = 2;
                InstantCaptureService.this.mServiceHandler.sendEmptyMessage(1);
            } else if (this.mKeyState == 3) {
                InstantCaptureService.this.mInstantCaptureHelper.startCameraActivity(InstantCaptureService.this, null);
            } else if (this.mKeyState == 7) {
                this.mKeyState = 8;
                this.mCameraState = 2;
                InstantCaptureService.this.mServiceHandler.sendEmptyMessage(1);
            }
        }

        public boolean isInvalidCameraState() {
            boolean z;
            synchronized (InstantCaptureService.this) {
                z = true;
                if (this.mCameraState != 1) {
                    z = false;
                }
            }
            return z;
        }

        public void changeCameraState(int state) {
            synchronized (InstantCaptureService.this) {
                Tag access$200 = InstantCaptureService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("changeCameraState from ");
                stringBuilder.append(this.mCameraState);
                stringBuilder.append(" to ");
                stringBuilder.append(state);
                stringBuilder.append(",  key state ");
                stringBuilder.append(this.mKeyState);
                Log.i(access$200, stringBuilder.toString());
                this.mCameraState = state;
                onCurrentState();
            }
        }

        public void changeKeyState(int keyState) {
            synchronized (InstantCaptureService.this) {
                Tag access$200 = InstantCaptureService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("changeKeyState from ");
                stringBuilder.append(this.mKeyState);
                stringBuilder.append(" to ");
                stringBuilder.append(keyState);
                stringBuilder.append(", camera state ");
                stringBuilder.append(this.mCameraState);
                Log.i(access$200, stringBuilder.toString());
                this.mKeyState = keyState;
                onCurrentState();
            }
        }

        public boolean isKeyState(int state) {
            boolean z;
            Tag access$200 = InstantCaptureService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isKeyState ");
            stringBuilder.append(this.mKeyState);
            stringBuilder.append(" ,");
            stringBuilder.append(state);
            Log.i(access$200, stringBuilder.toString());
            synchronized (InstantCaptureService.this) {
                z = this.mKeyState == state;
            }
            return z;
        }

        public boolean isCameraState(int state) {
            boolean z;
            Tag access$200 = InstantCaptureService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isCameraState ");
            stringBuilder.append(this.mCameraState);
            stringBuilder.append(" ,");
            stringBuilder.append(state);
            Log.i(access$200, stringBuilder.toString());
            synchronized (InstantCaptureService.this) {
                z = this.mCameraState == state;
            }
            return z;
        }

        public boolean isCaptureDone() {
            boolean z;
            synchronized (InstantCaptureService.this) {
                z = true;
                if (!(this.mCameraState == 1 || this.mCameraState == 4 || this.mCameraState == 5 || this.mCameraState == 9)) {
                    if (this.mCameraState != 10) {
                        z = false;
                    }
                }
            }
            return z;
        }

        public boolean isInCaptureProgress() {
            boolean z;
            synchronized (InstantCaptureService.this) {
                if (!(this.mCameraState == 6 || this.mCameraState == 7)) {
                    if (this.mCameraState != 8) {
                        z = false;
                    }
                }
                z = true;
            }
            return z;
        }

        public boolean isSingleShot() {
            boolean z;
            synchronized (InstantCaptureService.this) {
                if (this.mKeyState != 4) {
                    if (this.mKeyState != 8) {
                        z = false;
                    }
                }
                z = true;
            }
            return z;
        }

        public int getCameraState() {
            int i;
            synchronized (InstantCaptureService.this) {
                i = this.mCameraState;
            }
            return i;
        }

        public int getKeyState() {
            int i;
            synchronized (InstantCaptureService.this) {
                i = this.mKeyState;
            }
            return i;
        }
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 1 && i == 3) {
                Log.i(InstantCaptureService.TAG, "instant capture kpi,timeout for next cmd");
                InstantCaptureService.this.mCaptureState.changeKeyState(9);
            }
        }
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    try {
                        if (CameraLock.getInstance().block(1000)) {
                            CameraLock.getInstance().close();
                            InstantCaptureService.this.mInstantCaptureHelper.start();
                            InstantCaptureService.this.changeCameraState(3);
                            break;
                        }
                        throw new CameraConflictException();
                    } catch (CameraConflictException e) {
                        Log.i(InstantCaptureService.TAG, "camera already owned by other ");
                        CameraLock.getInstance().open();
                        InstantCaptureService.this.changeCameraState(4);
                        break;
                    } catch (Exception e2) {
                        Log.i(InstantCaptureService.TAG, "prepare exception  ", e2);
                        CameraLock.getInstance().open();
                        InstantCaptureService.this.changeCameraState(4);
                        break;
                    }
                case 3:
                    InstantCaptureService.this.mInstantCaptureHelper.stop();
                    InstantCaptureService.this.changeCameraState(1);
                    break;
                case 4:
                    try {
                        InstantCaptureService.this.mInstantCaptureHelper.setDisplayOrientation();
                        break;
                    } catch (Exception e3) {
                        Log.i(InstantCaptureService.TAG, "SET_DISPLAY_ORIENTATION exception  ", e3);
                        break;
                    }
                case 6:
                    InstantCaptureService.this.mCaptureState.changeCameraState(5);
                    break;
                case 7:
                    if (!InstantCaptureService.this.mInstantCaptureHelper.isCaptureDone()) {
                        InstantCaptureService.this.mInstantCaptureHelper.setForbidStartViewImageActivity(true);
                        InstantCaptureService.this.mInstantCaptureHelper.dismissViewImageActivity();
                        InstantCaptureService.this.changeCameraState(10);
                        break;
                    }
                    return;
            }
        }
    }

    public void changeCameraState(int state) {
        this.mCaptureState.changeCameraState(state);
    }

    public Handler getMainHandler() {
        return this.mMainHandler;
    }

    public Handler getServiceHandler() {
        return this.mServiceHandler;
    }

    public boolean checkInCaptureProgress() {
        return this.mCaptureState.isInCaptureProgress();
    }

    public boolean checkCameraState(int state) {
        return this.mCaptureState.isCameraState(state);
    }

    public boolean isCaptureDone() {
        return this.mCaptureState.isCaptureDone();
    }

    public boolean isSingleShot() {
        return this.mCaptureState.isSingleShot();
    }

    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("InstantCaptureService", 10);
        serviceThread.start();
        this.mServiceHandler = new ServiceHandler(serviceThread.getLooper());
        this.mMainHandler = new MainHandler();
        this.mInstantCaptureHelper = InstantCaptureHelper.getInstance();
        this.mInstantCaptureHelper.init(this);
        Log.i(TAG, "onCreate");
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        this.mInstantCaptureHelper.destroy();
        this.mInstantCaptureHelper = null;
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStartCommand ");
        stringBuilder.append(startId);
        Log.i(tag, stringBuilder.toString());
        parseCommand(intent);
        return 2;
    }

    private String validateIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            Log.i(TAG, "validateIntent invalid intent");
            return null;
        }
        String cmd = intent.getExtras().getString(FyuseAPI.COMMAND);
        if (cmd != null && (cmd.equals(BOOMKEY_DOWN_SCREEN_OFF_GLOBAL) || cmd.equals(BOOMKEY_DOWN_SCREEN_ON_GLOBAL) || cmd.equals(BOOMKEY_UP_GLOBAL) || cmd.equals(BOOMKEY_DOWN_SCREEN_OFF) || cmd.equals(BOOMKEY_DOWN_SCREEN_ON) || cmd.equals(BOOMKEY_UP) || cmd.equals(BOOMKEY_SINGLE_PRESSS) || cmd.equals(BOOMKEY_DOUBLE_PRESSS) || cmd.equals(BOOMKEY_LONG_PRESSS))) {
            return cmd;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parseCommand invalid cmd ");
        stringBuilder.append(cmd);
        Log.i(tag, stringBuilder.toString());
        return null;
    }

    public void parseCommand(android.content.Intent r12) {
        /*
        r11 = this;
        r0 = r11.validateIntent(r12);
        if (r0 != 0) goto L_0x0007;
    L_0x0006:
        return;
    L_0x0007:
        r1 = TAG;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "parseCommand cmd: ";
        r2.append(r3);
        r2.append(r0);
        r3 = ", ";
        r2.append(r3);
        r3 = r11.mWarningType;
        r2.append(r3);
        r3 = ", ";
        r2.append(r3);
        r3 = r11.mCaptureState;
        r3 = r3.getCameraState();
        r2.append(r3);
        r3 = ", ";
        r2.append(r3);
        r3 = r11.mCaptureState;
        r3 = r3.getKeyState();
        r2.append(r3);
        r2 = r2.toString();
        com.android.ex.camera2.portability.debug.Log.i(r1, r2);
        r1 = r0.hashCode();
        r2 = 4;
        r3 = 6;
        r4 = 7;
        r5 = 2;
        r6 = 5;
        r7 = 0;
        r8 = 3;
        r9 = 1;
        r10 = -1;
        switch(r1) {
            case -1508301015: goto L_0x00a6;
            case -841963586: goto L_0x009c;
            case -746997152: goto L_0x0092;
            case 3739: goto L_0x0087;
            case 3089570: goto L_0x007d;
            case 387950775: goto L_0x0073;
            case 820810301: goto L_0x0069;
            case 872524436: goto L_0x005f;
            case 1579146969: goto L_0x0055;
            default: goto L_0x0053;
        };
    L_0x0053:
        goto L_0x00b0;
    L_0x0055:
        r1 = "down_screen_off";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x005d:
        r1 = r4;
        goto L_0x00b1;
    L_0x005f:
        r1 = "boom_long_press";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x0067:
        r1 = r6;
        goto L_0x00b1;
    L_0x0069:
        r1 = "global_down_screen_off";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x0071:
        r1 = r9;
        goto L_0x00b1;
    L_0x0073:
        r1 = "global_up";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x007b:
        r1 = r5;
        goto L_0x00b1;
    L_0x007d:
        r1 = "down";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x0085:
        r1 = r3;
        goto L_0x00b1;
    L_0x0087:
        r1 = "up";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x008f:
        r1 = 8;
        goto L_0x00b1;
    L_0x0092:
        r1 = "boom_single_press";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x009a:
        r1 = r8;
        goto L_0x00b1;
    L_0x009c:
        r1 = "global_down";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x00a4:
        r1 = r7;
        goto L_0x00b1;
    L_0x00a6:
        r1 = "boom_double_press";
        r1 = r0.equals(r1);
        if (r1 == 0) goto L_0x00b0;
    L_0x00ae:
        r1 = r2;
        goto L_0x00b1;
    L_0x00b0:
        r1 = r10;
    L_0x00b1:
        switch(r1) {
            case 0: goto L_0x01b0;
            case 1: goto L_0x01b0;
            case 2: goto L_0x019b;
            case 3: goto L_0x015e;
            case 4: goto L_0x011d;
            case 5: goto L_0x00dc;
            case 6: goto L_0x00b6;
            case 7: goto L_0x00b6;
            default: goto L_0x00b4;
        };
    L_0x00b4:
        goto L_0x01f5;
    L_0x00b6:
        r1 = r11.mCaptureState;
        r1 = r1.isInvalidCameraState();
        if (r1 == 0) goto L_0x01f5;
    L_0x00be:
        r1 = r11.isInvalidUiState();
        if (r1 == 0) goto L_0x00c5;
    L_0x00c4:
        return;
    L_0x00c5:
        r1 = r11.checkInstantCapturePreconditions();
        if (r1 == 0) goto L_0x00d7;
    L_0x00cb:
        r1 = r11.mCaptureState;
        r1.changeKeyState(r4);
        r1 = r11.mInstantCaptureHelper;
        r1.startViewImageActivity(r11);
        goto L_0x01f5;
    L_0x00d7:
        r11.showWarningNotification(r11);
        goto L_0x01f5;
    L_0x00dc:
        r11.mPendingOpenApp = r7;
        r1 = r11.mMainHandler;
        r1.removeMessages(r8);
        r1 = r11.mCaptureState;
        r1 = r1.isKeyState(r9);
        if (r1 != 0) goto L_0x00ef;
    L_0x00eb:
        r1 = r11.mWarningType;
        if (r1 == r10) goto L_0x01f5;
    L_0x00ef:
        r1 = TAG;
        r2 = "instant capture kpi,enter long press and launch view ui";
        com.android.ex.camera2.portability.debug.Log.i(r1, r2);
        r1 = r11.mWarningType;
        if (r1 == r10) goto L_0x0101;
    L_0x00fa:
        r11.showWarningNotification(r11);
        r11.mWarningType = r10;
        goto L_0x01f5;
    L_0x0101:
        r1 = r11.mCaptureState;
        r1.changeKeyState(r6);
        r1 = r11.mInstantCaptureHelper;
        r1 = r1.getForbidStartViewImageActivity();
        if (r1 != 0) goto L_0x01f5;
    L_0x010e:
        r1 = r11.mCaptureState;
        r1 = r1.isCaptureDone();
        if (r1 != 0) goto L_0x01f5;
    L_0x0116:
        r1 = r11.mInstantCaptureHelper;
        r1.startViewImageActivity(r11);
        goto L_0x01f5;
    L_0x011d:
        r11.mPendingOpenApp = r7;
        r1 = r11.mMainHandler;
        r1.removeMessages(r8);
        r1 = r11.mCaptureState;
        r1 = r1.isKeyState(r9);
        if (r1 != 0) goto L_0x0130;
    L_0x012c:
        r1 = r11.mWarningType;
        if (r1 == r10) goto L_0x01f5;
    L_0x0130:
        r1 = TAG;
        r3 = "instant capture kpi,second boom key down";
        com.android.ex.camera2.portability.debug.Log.i(r1, r3);
        r1 = r11.mWarningType;
        if (r1 == r10) goto L_0x0142;
    L_0x013b:
        r11.showWarningNotification(r11);
        r11.mWarningType = r10;
        goto L_0x01f5;
    L_0x0142:
        r1 = r11.mCaptureState;
        r1.changeKeyState(r2);
        r1 = r11.mInstantCaptureHelper;
        r1 = r1.getForbidStartViewImageActivity();
        if (r1 != 0) goto L_0x01f5;
    L_0x014f:
        r1 = r11.mCaptureState;
        r1 = r1.isCaptureDone();
        if (r1 != 0) goto L_0x01f5;
    L_0x0157:
        r1 = r11.mInstantCaptureHelper;
        r1.startViewImageActivity(r11);
        goto L_0x01f5;
    L_0x015e:
        r1 = r11.mMainHandler;
        r1.removeMessages(r8);
        r1 = r11.mCaptureState;
        r1 = r1.isKeyState(r9);
        if (r1 != 0) goto L_0x016f;
    L_0x016b:
        r1 = r11.mWarningType;
        if (r1 == r10) goto L_0x01f5;
    L_0x016f:
        r1 = TAG;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "instant capture kpi, single click ";
        r2.append(r3);
        r3 = r11.mPendingOpenApp;
        r2.append(r3);
        r2 = r2.toString();
        com.android.ex.camera2.portability.debug.Log.i(r1, r2);
        r1 = r11.mPendingOpenApp;
        if (r1 == 0) goto L_0x0191;
    L_0x018b:
        r1 = r11.mCaptureState;
        r1.changeKeyState(r8);
        goto L_0x0196;
    L_0x0191:
        r1 = r11.mCaptureState;
        r1.changeKeyState(r5);
    L_0x0196:
        r11.mWarningType = r10;
        r11.mPendingOpenApp = r7;
        goto L_0x01f5;
    L_0x019b:
        r1 = r11.mCaptureState;
        r1 = r1.isKeyState(r6);
        if (r1 == 0) goto L_0x01f5;
    L_0x01a3:
        r1 = TAG;
        r2 = "instant capture kpi, boom key long press up";
        com.android.ex.camera2.portability.debug.Log.i(r1, r2);
        r1 = r11.mCaptureState;
        r1.changeKeyState(r3);
        goto L_0x01f5;
    L_0x01b0:
        r1 = r11.mCaptureState;
        r1 = r1.isInvalidCameraState();
        if (r1 == 0) goto L_0x01f5;
    L_0x01b8:
        r1 = r11.mWarningType;
        if (r1 != r10) goto L_0x01f5;
    L_0x01bc:
        r1 = r11.isInvalidUiState();
        if (r1 != 0) goto L_0x01f4;
    L_0x01c2:
        r1 = r11.mMainHandler;
        r1 = r1.hasMessages(r5);
        if (r1 == 0) goto L_0x01cb;
    L_0x01ca:
        goto L_0x01f4;
    L_0x01cb:
        r1 = "global_down";
        r1 = android.text.TextUtils.equals(r0, r1);
        if (r1 == 0) goto L_0x01d5;
    L_0x01d3:
        r11.mPendingOpenApp = r9;
    L_0x01d5:
        r1 = TAG;
        r2 = "instant capture kpi, first boom key down";
        com.android.ex.camera2.portability.debug.Log.i(r1, r2);
        r1 = r11.checkInstantCapturePreconditions();
        if (r1 == 0) goto L_0x01ec;
    L_0x01e2:
        r1 = r11.mCaptureState;
        r1.changeKeyState(r9);
        r1 = r11.mInstantCaptureHelper;
        r1.dismissViewImageActivity();
    L_0x01ec:
        r1 = r11.mMainHandler;
        r2 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r1.sendEmptyMessageDelayed(r8, r2);
        goto L_0x01f5;
    L_0x01f4:
        return;
    L_0x01f5:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.instantcapture.InstantCaptureService.parseCommand(android.content.Intent):void");
    }

    public boolean checkInstantCapturePreconditions() {
        Tag tag;
        StringBuilder stringBuilder;
        this.mWarningType = -1;
        boolean lowBattery = false;
        boolean lowStorage = false;
        boolean permissionNeed = false;
        if (!PermissionsUtil.isCriticalPermissionGranted(getApplicationContext())) {
            permissionNeed = true;
        }
        if (ApiHelper.isLOrHigher() && this.mInstantCaptureHelper.needLowBatteryCheck(this)) {
            int batteryLevel = ((BatteryManager) getSystemService("batterymanager")).getIntProperty(4);
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("batteryLevel:");
            stringBuilder.append(batteryLevel);
            Log.i(tag, stringBuilder.toString());
            if (batteryLevel > 0 && batteryLevel <= 5) {
                lowBattery = true;
            }
        }
        Storage.setSavePath(((CameraServices) getApplicationContext()).getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_SAVEPATH, getResources().getString(R.string.pref_camera_savepath_default)));
        if (Storage.getAvailableSpace() < Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            lowStorage = true;
        }
        if (!permissionNeed && !lowBattery && !lowStorage) {
            return true;
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("checkInstantCapturePreconditions ");
        stringBuilder.append(permissionNeed);
        stringBuilder.append(", ");
        stringBuilder.append(lowBattery);
        stringBuilder.append(", ");
        stringBuilder.append(lowStorage);
        Log.i(tag, stringBuilder.toString());
        if (permissionNeed) {
            this.mWarningType = 3;
        } else if (lowBattery || lowStorage) {
            if (lowBattery && lowStorage) {
                this.mWarningType = 2;
            } else if (lowBattery) {
                this.mWarningType = 0;
            } else {
                this.mWarningType = 1;
            }
        }
        return false;
    }

    private boolean isInvalidUiState() {
        if (!this.mInstantCaptureHelper.isScreenOn()) {
            return false;
        }
        CameraApp app = (CameraApp) getApplication();
        boolean isMainActivityActive = app.isMainActivityActive();
        boolean isInstantViewActivityActive = app.isInstantViewActivityActive();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isInvalidState ");
        stringBuilder.append(isMainActivityActive);
        stringBuilder.append(", ");
        stringBuilder.append(isInstantViewActivityActive);
        Log.i(tag, stringBuilder.toString());
        if (isMainActivityActive || isInstantViewActivityActive) {
            return true;
        }
        return false;
    }

    private void showWarningNotification(Context context) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("showWarningNotification ");
        stringBuilder.append(this.mWarningType);
        Log.i(tag, stringBuilder.toString());
        this.mInstantCaptureHelper.wakeUpScreen();
        Resources res = context.getResources();
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        switch (this.mWarningType) {
            case 0:
                nm.notify(0, new Builder(context).setSmallIcon(17301543).setWhen(System.currentTimeMillis()).setContentTitle(res.getString(R.string.battery_warning_title)).setContentText(res.getString(R.string.battery_warning_content)).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).setAutoCancel(true).build());
                return;
            case 1:
                nm.notify(0, new Builder(context).setSmallIcon(17301543).setWhen(System.currentTimeMillis()).setContentTitle(res.getString(R.string.battery_warning_title)).setContentText(res.getString(R.string.storage_warning_content)).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).setAutoCancel(true).build());
                return;
            case 2:
                nm.notify(0, new Builder(context).setSmallIcon(17301543).setWhen(System.currentTimeMillis()).setContentTitle(res.getString(R.string.battery_warning_title)).setContentText(res.getString(R.string.storage_and_battery_warning_content)).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0)).setAutoCancel(true).build());
                return;
            case 3:
                nm.notify(0, new Builder(context).setSmallIcon(17301543).setWhen(System.currentTimeMillis()).setContentTitle(res.getString(R.string.permission_title)).setContentText(res.getString(R.string.permission_content)).setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, GrantAccessActivity.class), 1342177280)).setAutoCancel(true).build());
                return;
            default:
                return;
        }
    }
}
