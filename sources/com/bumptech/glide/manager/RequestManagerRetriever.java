package com.bumptech.glide.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.util.Util;
import java.util.HashMap;
import java.util.Map;

public class RequestManagerRetriever implements Callback {
    private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
    private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;
    private static final RequestManagerRetriever INSTANCE = new RequestManagerRetriever();
    static final String TAG = "com.bumptech.glide.manager";
    private volatile RequestManager applicationManager;
    private final Handler handler = new Handler(Looper.getMainLooper(), this);
    final Map<FragmentManager, RequestManagerFragment> pendingRequestManagerFragments = new HashMap();
    final Map<android.support.v4.app.FragmentManager, SupportRequestManagerFragment> pendingSupportRequestManagerFragments = new HashMap();

    public static RequestManagerRetriever get() {
        return INSTANCE;
    }

    RequestManagerRetriever() {
    }

    private RequestManager getApplicationManager(Context context) {
        if (this.applicationManager == null) {
            synchronized (this) {
                if (this.applicationManager == null) {
                    this.applicationManager = new RequestManager(context.getApplicationContext(), new ApplicationLifecycle());
                }
            }
        }
        return this.applicationManager;
    }

    public RequestManager get(Context context) {
        if (context != null) {
            if (Util.isOnMainThread() && !(context instanceof Application)) {
                if (context instanceof FragmentActivity) {
                    return get((FragmentActivity) context);
                }
                if (context instanceof Activity) {
                    return get((Activity) context);
                }
                if (context instanceof ContextWrapper) {
                    return get(((ContextWrapper) context).getBaseContext());
                }
            }
            return getApplicationManager(context);
        }
        throw new IllegalArgumentException("You cannot start a load on a null Context");
    }

    public RequestManager get(FragmentActivity activity) {
        if (Util.isOnBackgroundThread()) {
            return get(activity.getApplicationContext());
        }
        assertNotDestroyed(activity);
        return supportFragmentGet(activity, activity.getSupportFragmentManager());
    }

    public RequestManager get(Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        } else if (Util.isOnBackgroundThread()) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            if (fragment.isDetached()) {
                throw new IllegalArgumentException("You cannot start a load on a detached fragment");
            }
            return supportFragmentGet(fragment.getActivity(), fragment.getChildFragmentManager());
        }
    }

    @TargetApi(11)
    public RequestManager get(Activity activity) {
        if (Util.isOnBackgroundThread() || VERSION.SDK_INT < 11) {
            return get(activity.getApplicationContext());
        }
        assertNotDestroyed(activity);
        return fragmentGet(activity, activity.getFragmentManager());
    }

    @TargetApi(17)
    private static void assertNotDestroyed(Activity activity) {
        if (VERSION.SDK_INT >= 17 && activity.isDestroyed()) {
            throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
        }
    }

    @TargetApi(17)
    public RequestManager get(android.app.Fragment fragment) {
        if (fragment.getActivity() == null) {
            throw new IllegalArgumentException("You cannot start a load on a fragment before it is attached");
        } else if (Util.isOnBackgroundThread() || VERSION.SDK_INT < 17) {
            return get(fragment.getActivity().getApplicationContext());
        } else {
            assertNotDetached(fragment);
            return fragmentGet(fragment.getActivity(), fragment.getChildFragmentManager());
        }
    }

    @TargetApi(13)
    private static void assertNotDetached(android.app.Fragment fragment) {
        if (VERSION.SDK_INT >= 13 && fragment.isDetached()) {
            throw new IllegalArgumentException("You cannot start a load on a detached fragment");
        }
    }

    /* Access modifiers changed, original: 0000 */
    @TargetApi(11)
    public RequestManager fragmentGet(Context context, FragmentManager fm) {
        RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(TAG);
        if (current == null) {
            current = (RequestManagerFragment) this.pendingRequestManagerFragments.get(fm);
            if (current == null) {
                current = new RequestManagerFragment();
                this.pendingRequestManagerFragments.put(fm, current);
                fm.beginTransaction().add(current, TAG).commitAllowingStateLoss();
                this.handler.obtainMessage(1, fm).sendToTarget();
            }
        }
        RequestManager requestManager = current.getRequestManager();
        if (requestManager != null) {
            return requestManager;
        }
        requestManager = new RequestManager(context, current.getGlideLifecycle());
        current.setRequestManager(requestManager);
        return requestManager;
    }

    /* Access modifiers changed, original: 0000 */
    public RequestManager supportFragmentGet(Context context, android.support.v4.app.FragmentManager fm) {
        SupportRequestManagerFragment current = (SupportRequestManagerFragment) fm.findFragmentByTag(TAG);
        if (current == null) {
            current = (SupportRequestManagerFragment) this.pendingSupportRequestManagerFragments.get(fm);
            if (current == null) {
                current = new SupportRequestManagerFragment();
                this.pendingSupportRequestManagerFragments.put(fm, current);
                fm.beginTransaction().add((Fragment) current, TAG).commitAllowingStateLoss();
                this.handler.obtainMessage(2, fm).sendToTarget();
            }
        }
        RequestManager requestManager = current.getRequestManager();
        if (requestManager != null) {
            return requestManager;
        }
        requestManager = new RequestManager(context, current.getGlideLifecycle());
        current.setRequestManager(requestManager);
        return requestManager;
    }

    public boolean handleMessage(Message message) {
        boolean handled = true;
        Object removed = null;
        Object key = null;
        switch (message.what) {
            case 1:
                FragmentManager fm = message.obj;
                key = fm;
                removed = this.pendingRequestManagerFragments.remove(fm);
                break;
            case 2:
                android.support.v4.app.FragmentManager supportFm = message.obj;
                key = supportFm;
                removed = this.pendingSupportRequestManagerFragments.remove(supportFm);
                break;
            default:
                handled = false;
                break;
        }
        if (handled && removed == null && Log.isLoggable(TAG, 5)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to remove expected request manager fragment, manager: ");
            stringBuilder.append(key);
            Log.w(str, stringBuilder.toString());
        }
        return handled;
    }
}
