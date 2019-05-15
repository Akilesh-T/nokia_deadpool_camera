package com.android.ex.camera2.portability.debug;

public class Log {
    public static final String CAMERA_LOGTAG_PREFIX = "CAM2PORT_";
    private static final Tag TAG = new Tag("Log");

    public static final class Tag {
        private static final int MAX_TAG_LEN = (23 - Log.CAMERA_LOGTAG_PREFIX.length());
        final String mValue;

        public Tag(String tag) {
            int lenDiff = tag.length() - MAX_TAG_LEN;
            if (lenDiff > 0) {
                Tag access$000 = Log.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Tag ");
                stringBuilder.append(tag);
                stringBuilder.append(" is ");
                stringBuilder.append(lenDiff);
                stringBuilder.append(" chars longer than limit.");
                Log.w(access$000, stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Log.CAMERA_LOGTAG_PREFIX);
            stringBuilder2.append(lenDiff > 0 ? tag.substring(0, MAX_TAG_LEN) : tag);
            this.mValue = stringBuilder2.toString();
        }

        public String toString() {
            return this.mValue;
        }
    }

    public static void d(Tag tag, String msg) {
        if (isLoggable(tag, 3)) {
            android.util.Log.d(tag.toString(), msg);
        }
    }

    public static void d(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, 3)) {
            android.util.Log.d(tag.toString(), msg, tr);
        }
    }

    public static void e(Tag tag, String msg) {
        if (isLoggable(tag, 6)) {
            android.util.Log.e(tag.toString(), msg);
        }
    }

    public static void e(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, 6)) {
            android.util.Log.e(tag.toString(), msg, tr);
        }
    }

    public static void i(Tag tag, String msg) {
        if (isLoggable(tag, 4)) {
            android.util.Log.i(tag.toString(), msg);
        }
    }

    public static void i(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, 4)) {
            android.util.Log.i(tag.toString(), msg, tr);
        }
    }

    public static void v(Tag tag, String msg) {
        if (isLoggable(tag, 2)) {
            android.util.Log.v(tag.toString(), msg);
        }
    }

    public static void v(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, 2)) {
            android.util.Log.v(tag.toString(), msg, tr);
        }
    }

    public static void w(Tag tag, String msg) {
        if (isLoggable(tag, 5)) {
            android.util.Log.w(tag.toString(), msg);
        }
    }

    public static void w(Tag tag, String msg, Throwable tr) {
        if (isLoggable(tag, 5)) {
            android.util.Log.w(tag.toString(), msg, tr);
        }
    }

    private static boolean isLoggable(Tag tag, int level) {
        boolean z = false;
        try {
            if (LogHelper.getOverrideLevel() != 0) {
                if (LogHelper.getOverrideLevel() <= level) {
                    z = true;
                }
                return z;
            }
            if (android.util.Log.isLoggable(CAMERA_LOGTAG_PREFIX, level) || android.util.Log.isLoggable(tag.toString(), level)) {
                z = true;
            }
            return z;
        } catch (IllegalArgumentException e) {
            Tag tag2 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Tag too long:");
            stringBuilder.append(tag);
            e(tag2, stringBuilder.toString());
            return false;
        }
    }
}
