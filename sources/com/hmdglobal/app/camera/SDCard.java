package com.hmdglobal.app.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class SDCard {
    private static final String TAG = "SDCard";
    private static final int VOLUME_SDCARD_INDEX = 1;
    private static SDCard sSDCard;
    private Context mContext;
    private BroadcastReceiver mMediaBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try {
                SDCard.this.initVolume();
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    };
    private String mPath = null;
    private String mRawpath = null;
    private StorageManager mStorageManager = ((StorageManager) null);
    private StorageVolume mVolume = null;

    public StorageVolume[] getVolumeList() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (StorageVolume[]) StorageManager.class.getDeclaredMethod("getVolumeList", new Class[0]).invoke(this.mStorageManager, new Object[0]);
    }

    public String getPath() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (String) StorageVolume.class.getMethod("getPath", new Class[0]).invoke(this.mVolume, new Object[0]);
    }

    public boolean isWriteable() {
        if (this.mVolume == null) {
            return false;
        }
        if ("mounted".equals(getSDCardStorageState())) {
            return true;
        }
        return false;
    }

    public String getDirectory() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (this.mVolume == null) {
            return null;
        }
        if (this.mPath == null) {
            File[] dirs = this.mContext.getExternalFilesDirs(null);
            if (dirs != null) {
                for (int i = 0; i < dirs.length; i++) {
                    if (dirs[i] != null) {
                        String dir = dirs[i].getAbsolutePath();
                        if (dir.startsWith(getPath())) {
                            this.mPath = dir;
                            break;
                        }
                    }
                }
            }
        }
        return this.mPath;
    }

    public static void initialize(Context context) {
        if (sSDCard == null) {
            sSDCard = new SDCard(context);
        }
    }

    public static synchronized SDCard instance() {
        SDCard sDCard;
        synchronized (SDCard.class) {
            sDCard = sSDCard;
        }
        return sDCard;
    }

    public String getSDCardStorageState() {
        return this.mVolume.getState();
    }

    private SDCard(Context context) {
        try {
            this.mContext = context;
            this.mStorageManager = (StorageManager) context.getSystemService("storage");
            initVolume();
            registerMediaBroadcastreceiver(context);
        } catch (Exception e) {
            Log.e(TAG, "couldn't talk to MountService", e);
        }
    }

    private void initVolume() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        StorageVolume[] volumes = getVolumeList();
        this.mVolume = volumes.length > 1 ? volumes[1] : null;
        this.mPath = null;
        this.mRawpath = null;
    }

    private void registerMediaBroadcastreceiver(Context context) {
        IntentFilter filter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
        filter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        filter.addDataScheme("file");
        context.registerReceiver(this.mMediaBroadcastReceiver, filter);
    }
}
