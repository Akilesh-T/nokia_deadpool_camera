package com.hmdglobal.app.camera.util;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action.Builder;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.remote.RemoteCameraModule;
import com.hmdglobal.app.camera.remote.RemoteShutterListener;

public class RemoteShutterHelper {
    private static final int NOTIFICATION_ID = 1;
    private static final String REMOTE_SHUTTER_INTENT = "RemoteShutter";
    private static final String TAG = "RemoteShutterHelper";
    private static final int TARGET_WIDTH = 300;
    private static boolean mAndroidWareInstalled = false;
    private static BroadcastReceiver mBroadcastReceiver = null;
    private static Context mContext;
    private static int mLastResouceId = -1;
    private static int mPressCount = 0;
    private static RemoteCameraModule mRemoteCameraModule = null;

    static /* synthetic */ int access$108() {
        int i = mPressCount;
        mPressCount = i + 1;
        return i;
    }

    public static RemoteShutterListener create(Context context) {
        mContext = context;
        mPressCount = 0;
        mRemoteCameraModule = null;
        mBroadcastReceiver = null;
        mLastResouceId = -1;
        if (checkIfAndroidWearInstalled(mContext)) {
            mAndroidWareInstalled = true;
        }
        ListenAndroidWare(mContext);
        return new RemoteShutterListener() {
            public void onPictureTaken(byte[] photoData) {
                Log.v(RemoteShutterHelper.TAG, "onPictureTaken start");
                if (RemoteShutterHelper.mAndroidWareInstalled && RemoteShutterHelper.mPressCount > 0) {
                    RemoteShutterHelper.mPressCount = 0;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length, null);
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    if (width > 300) {
                        float scale = ((float) 300) / ((float) width);
                        int w = Math.round(((float) width) * scale);
                        int h = Math.round(((float) height) * scale);
                        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
                        String str = RemoteShutterHelper.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onPictureTaken width ");
                        stringBuilder.append(width);
                        stringBuilder.append("  height  ");
                        stringBuilder.append(height);
                        stringBuilder.append(" w ");
                        stringBuilder.append(w);
                        stringBuilder.append(" h ");
                        stringBuilder.append(h);
                        Log.v(str, stringBuilder.toString());
                    }
                    if (bitmap != null) {
                        RemoteShutterHelper.sendRemoteNotify(RemoteShutterHelper.mContext, RemoteShutterHelper.mLastResouceId, bitmap);
                    }
                }
                Log.v(RemoteShutterHelper.TAG, "onPictureTaken end");
            }

            public void onModuleReady(RemoteCameraModule module, int resouceId) {
                Log.v(RemoteShutterHelper.TAG, "onModuleReady start");
                if (RemoteShutterHelper.mAndroidWareInstalled) {
                    RemoteShutterHelper.mRemoteCameraModule = module;
                    RemoteShutterHelper.sendRemoteNotify(RemoteShutterHelper.mContext, resouceId, null);
                    RemoteShutterHelper.regestShutter(RemoteShutterHelper.mContext);
                }
                Log.v(RemoteShutterHelper.TAG, "onModuleReady end");
            }

            public void onModuleExit() {
                Log.v(RemoteShutterHelper.TAG, "onModuleExit start");
                if (RemoteShutterHelper.mAndroidWareInstalled) {
                    RemoteShutterHelper.unRegestShutter(RemoteShutterHelper.mContext);
                    RemoteShutterHelper.cancelRemoteNotify(RemoteShutterHelper.mContext);
                    RemoteShutterHelper.mRemoteCameraModule = null;
                }
                Log.v(RemoteShutterHelper.TAG, "onModuleExit end");
            }
        };
    }

    private static void sendRemoteNotify(Context context, int resouceId, Bitmap bitmap) {
        if (resouceId >= 0) {
            mLastResouceId = resouceId;
            WearableExtender wearableExtender = new WearableExtender().addAction(new Builder(resouceId, "action", PendingIntent.getBroadcast(context, 0, new Intent(REMOTE_SHUTTER_INTENT), 0)).build());
            if (bitmap != null) {
                wearableExtender.setBackground(bitmap);
            } else {
                wearableExtender.setBackground(BitmapFactory.decodeResource(context.getResources(), R.drawable.wallpaper_wearable_default, null));
            }
            NotificationManagerCompat.from(context).notify(1, new NotificationCompat.Builder(context).setSmallIcon(resouceId).setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(R.string.remote_content)).extend(wearableExtender).setVibrate(new long[]{100}).build());
        }
    }

    private static void cancelRemoteNotify(Context context) {
        NotificationManagerCompat.from(context).cancel(1);
    }

    private static void regestShutter(Context context) {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    if (RemoteShutterHelper.mRemoteCameraModule != null) {
                        RemoteShutterHelper.access$108();
                        RemoteShutterHelper.mRemoteCameraModule.onRemoteShutterPress();
                    }
                }
            };
            context.registerReceiver(mBroadcastReceiver, new IntentFilter(REMOTE_SHUTTER_INTENT));
        }
    }

    private static void unRegestShutter(Context context) {
        if (mBroadcastReceiver != null) {
            context.unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    private static boolean checkIfAndroidWearInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.google.android.wearable.app", 128);
            return true;
        } catch (NameNotFoundException e) {
            Log.v(TAG, "checkIfAndroidWearInstalled failed! cn checking");
            try {
                context.getPackageManager().getPackageInfo("com.google.android.wearable.app.cn", 128);
                return true;
            } catch (NameNotFoundException e2) {
                Log.v(TAG, "checkIfAndroidWearInstalled failed ecn!");
                return false;
            }
        }
    }

    private static void ListenAndroidWare(Context context) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String packageName;
                String str;
                StringBuilder stringBuilder;
                if (intent.getAction().equalsIgnoreCase("android.intent.action.PACKAGE_ADDED")) {
                    packageName = intent.getData().getSchemeSpecificPart();
                    str = RemoteShutterHelper.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ListenAndroidWare add ");
                    stringBuilder.append(packageName);
                    Log.v(str, stringBuilder.toString());
                    if (packageName.equalsIgnoreCase("com.google.android.wearable.app") || packageName.equalsIgnoreCase("com.google.android.wearable.app.cn")) {
                        RemoteShutterHelper.mAndroidWareInstalled = true;
                    }
                } else if (intent.getAction().equalsIgnoreCase("android.intent.action.PACKAGE_REMOVED")) {
                    packageName = intent.getData().getSchemeSpecificPart();
                    str = RemoteShutterHelper.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ListenAndroidWare remove ");
                    stringBuilder.append(packageName);
                    Log.v(str, stringBuilder.toString());
                    if (packageName.equalsIgnoreCase("com.google.android.wearable.app") || packageName.equalsIgnoreCase("com.google.android.wearable.app.cn")) {
                        RemoteShutterHelper.mAndroidWareInstalled = RemoteShutterHelper.checkIfAndroidWearInstalled(context);
                    }
                }
            }
        };
        IntentFilter intFilter = new IntentFilter();
        intFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intFilter.addDataScheme("package");
        context.registerReceiver(broadcastReceiver, intFilter);
    }
}
