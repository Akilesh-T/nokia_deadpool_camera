package android.support.v4.media;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.v4.util.ObjectsCompat;
import android.text.TextUtils;
import android.util.Log;

class MediaSessionManagerImplBase implements MediaSessionManagerImpl {
    private static final boolean DEBUG = MediaSessionManager.DEBUG;
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String PERMISSION_MEDIA_CONTENT_CONTROL = "android.permission.MEDIA_CONTENT_CONTROL";
    private static final String PERMISSION_STATUS_BAR_SERVICE = "android.permission.STATUS_BAR_SERVICE";
    private static final String TAG = "MediaSessionManager";
    ContentResolver mContentResolver = this.mContext.getContentResolver();
    Context mContext;

    static class RemoteUserInfo implements RemoteUserInfoImpl {
        private String mPackageName;
        private int mPid;
        private int mUid;

        RemoteUserInfo(String packageName, int pid, int uid) {
            this.mPackageName = packageName;
            this.mPid = pid;
            this.mUid = uid;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getPid() {
            return this.mPid;
        }

        public int getUid() {
            return this.mUid;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof RemoteUserInfo)) {
                return false;
            }
            RemoteUserInfo otherUserInfo = (RemoteUserInfo) obj;
            if (TextUtils.equals(this.mPackageName, otherUserInfo.mPackageName) && this.mPid == otherUserInfo.mPid && this.mUid == otherUserInfo.mUid) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return ObjectsCompat.hash(this.mPackageName, Integer.valueOf(this.mPid), Integer.valueOf(this.mUid));
        }
    }

    MediaSessionManagerImplBase(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return this.mContext;
    }

    public boolean isTrustedForMediaControl(@NonNull RemoteUserInfoImpl userInfo) {
        boolean z = false;
        String str;
        StringBuilder stringBuilder;
        try {
            if (this.mContext.getPackageManager().getApplicationInfo(userInfo.getPackageName(), 0).uid != userInfo.getUid()) {
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Package name ");
                    stringBuilder.append(userInfo.getPackageName());
                    stringBuilder.append(" doesn't match with the uid ");
                    stringBuilder.append(userInfo.getUid());
                    Log.d(str, stringBuilder.toString());
                }
                return false;
            }
            if (isPermissionGranted(userInfo, PERMISSION_STATUS_BAR_SERVICE) || isPermissionGranted(userInfo, PERMISSION_MEDIA_CONTENT_CONTROL) || userInfo.getUid() == 1000 || isEnabledNotificationListener(userInfo)) {
                z = true;
            }
            return z;
        } catch (NameNotFoundException e) {
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Package ");
                stringBuilder.append(userInfo.getPackageName());
                stringBuilder.append(" doesn't exist");
                Log.d(str, stringBuilder.toString());
            }
            return false;
        }
    }

    private boolean isPermissionGranted(RemoteUserInfoImpl userInfo, String permission) {
        boolean z = false;
        if (userInfo.getPid() < 0) {
            if (this.mContext.getPackageManager().checkPermission(permission, userInfo.getPackageName()) == 0) {
                z = true;
            }
            return z;
        }
        if (this.mContext.checkPermission(permission, userInfo.getPid(), userInfo.getUid()) == 0) {
            z = true;
        }
        return z;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isEnabledNotificationListener(@NonNull RemoteUserInfoImpl userInfo) {
        String enabledNotifListeners = Secure.getString(this.mContentResolver, ENABLED_NOTIFICATION_LISTENERS);
        if (enabledNotifListeners != null) {
            String[] components = enabledNotifListeners.split(":");
            for (ComponentName component : components) {
                ComponentName component2 = ComponentName.unflattenFromString(component2);
                if (component2 != null && component2.getPackageName().equals(userInfo.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
