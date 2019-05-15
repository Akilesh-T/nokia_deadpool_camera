package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DimenRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.instantcapture.InstantCaptureService;

public class ToastUtil {
    private static final Tag TAG = new Tag("ToastUtil");
    private static Runnable mCancelRunnable = new Runnable() {
        public void run() {
            ToastUtil.mToast.cancel();
        }
    };
    private static int mDuration;
    private static Handler mHandler = new Handler(Looper.getMainLooper());
    private static Toast mToast;
    private static TextView mToastBackground;
    private static View view;

    private static Toast makeText(Context context, String text, int duration, boolean higher) {
        if (mToastBackground == null && view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.toast_util, null);
            mToastBackground = (TextView) view.findViewById(R.id.toast);
        }
        if (mToast == null) {
            mToast = Toast.makeText(CameraApp.getContext(), text, duration);
            mToast.setGravity(1, 0, getDimension(context, R.dimen.toast_margin_Bottom));
            if (ApiHelper.isMOrHigher()) {
                mToast.setView(view);
            }
        }
        if (higher) {
            mToast.setGravity(1, 0, getDimension(context, R.dimen.toast_margin_Bottom_higher));
        }
        mToastBackground.setText(text);
        mDuration = duration;
        if (duration == 0) {
            mDuration = 2000;
        } else if (duration == 1) {
            mDuration = InstantCaptureService.CAPTURE_TIMEOUT_DELAY;
        }
        return mToast;
    }

    private static void show() {
        mHandler.removeCallbacks(mCancelRunnable);
        mHandler.postDelayed(mCancelRunnable, (long) mDuration);
        mToast.show();
    }

    public static void showHigherToast(Context context, String text, int duration) {
        makeText(context, text, duration, true);
        show();
    }

    public static void showHigherToast(Context context, int id, int duration) {
        showHigherToast(context, context.getResources().getString(id), duration);
    }

    public static void showToast(Context context, String text, int duration) {
        makeText(context, text, duration, false);
        show();
    }

    public static void showToast(Context context, int id, int duration) {
        showToast(context, context.getResources().getString(id), duration);
    }

    public static void cancelToast() {
        mHandler.removeCallbacks(mCancelRunnable);
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }

    private static int getDimension(Context context, @DimenRes int id) {
        return (int) context.getResources().getDimension(id);
    }
}
