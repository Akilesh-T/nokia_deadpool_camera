package com.hmdglobal.app.camera.util;

import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class AccessibilityUtils {
    public static void makeAnnouncement(View view, CharSequence announcement) {
        if (view != null) {
            if (ApiHelper.HAS_ANNOUNCE_FOR_ACCESSIBILITY) {
                view.announceForAccessibility(announcement);
            } else {
                AccessibilityManager am = (AccessibilityManager) view.getContext().getSystemService("accessibility");
                if (am.isEnabled()) {
                    AccessibilityEvent event = AccessibilityEvent.obtain(64);
                    new AccessibilityRecordCompat(event).setSource(view);
                    event.setClassName(view.getClass().getName());
                    event.setPackageName(view.getContext().getPackageName());
                    event.setEnabled(view.isEnabled());
                    event.getText().add(announcement);
                    am.sendAccessibilityEvent(event);
                }
            }
        }
    }
}
