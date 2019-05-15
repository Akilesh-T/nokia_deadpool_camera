package android.support.v7.widget;

import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.graphics.drawable.WrappedDrawable;
import android.support.v7.graphics.drawable.DrawableWrapper;

@RestrictTo({Scope.LIBRARY_GROUP})
public class DrawableUtils {
    public static final Rect INSETS_NONE = new Rect();
    private static final String TAG = "DrawableUtils";
    private static final String VECTOR_DRAWABLE_CLAZZ_NAME = "android.graphics.drawable.VectorDrawable";
    private static Class<?> sInsetsClazz;

    static {
        if (VERSION.SDK_INT >= 18) {
            try {
                sInsetsClazz = Class.forName("android.graphics.Insets");
            } catch (ClassNotFoundException e) {
            }
        }
    }

    private DrawableUtils() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0079 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x008f A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0088 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0081 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0079 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x008f A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0088 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0081 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0079 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x008f A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0088 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0081 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0079 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x008f A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0088 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0081 A:{Catch:{ Exception -> 0x009b }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x007a A:{Catch:{ Exception -> 0x009b }} */
    public static android.graphics.Rect getOpticalBounds(android.graphics.drawable.Drawable r12) {
        /*
        r0 = sInsetsClazz;
        if (r0 == 0) goto L_0x00a3;
    L_0x0004:
        r0 = android.support.v4.graphics.drawable.DrawableCompat.unwrap(r12);	 Catch:{ Exception -> 0x009b }
        r12 = r0;
        r0 = r12.getClass();	 Catch:{ Exception -> 0x009b }
        r1 = "getOpticalInsets";
        r2 = 0;
        r3 = new java.lang.Class[r2];	 Catch:{ Exception -> 0x009b }
        r0 = r0.getMethod(r1, r3);	 Catch:{ Exception -> 0x009b }
        r1 = new java.lang.Object[r2];	 Catch:{ Exception -> 0x009b }
        r1 = r0.invoke(r12, r1);	 Catch:{ Exception -> 0x009b }
        if (r1 == 0) goto L_0x009a;
    L_0x001e:
        r3 = new android.graphics.Rect;	 Catch:{ Exception -> 0x009b }
        r3.<init>();	 Catch:{ Exception -> 0x009b }
        r4 = sInsetsClazz;	 Catch:{ Exception -> 0x009b }
        r4 = r4.getFields();	 Catch:{ Exception -> 0x009b }
        r5 = r4.length;	 Catch:{ Exception -> 0x009b }
        r6 = r2;
    L_0x002b:
        if (r6 >= r5) goto L_0x0099;
    L_0x002d:
        r7 = r4[r6];	 Catch:{ Exception -> 0x009b }
        r8 = r7.getName();	 Catch:{ Exception -> 0x009b }
        r9 = -1;
        r10 = r8.hashCode();	 Catch:{ Exception -> 0x009b }
        r11 = -1383228885; // 0xffffffffad8d9a2b float:-1.6098308E-11 double:NaN;
        if (r10 == r11) goto L_0x006b;
    L_0x003d:
        r11 = 115029; // 0x1c155 float:1.6119E-40 double:5.6832E-319;
        if (r10 == r11) goto L_0x0061;
    L_0x0042:
        r11 = 3317767; // 0x32a007 float:4.649182E-39 double:1.6391947E-317;
        if (r10 == r11) goto L_0x0057;
    L_0x0047:
        r11 = 108511772; // 0x677c21c float:4.6598146E-35 double:5.36119387E-316;
        if (r10 == r11) goto L_0x004d;
    L_0x004c:
        goto L_0x0075;
    L_0x004d:
        r10 = "right";
        r8 = r8.equals(r10);	 Catch:{ Exception -> 0x009b }
        if (r8 == 0) goto L_0x0075;
    L_0x0055:
        r8 = 2;
        goto L_0x0076;
    L_0x0057:
        r10 = "left";
        r8 = r8.equals(r10);	 Catch:{ Exception -> 0x009b }
        if (r8 == 0) goto L_0x0075;
    L_0x005f:
        r8 = r2;
        goto L_0x0076;
    L_0x0061:
        r10 = "top";
        r8 = r8.equals(r10);	 Catch:{ Exception -> 0x009b }
        if (r8 == 0) goto L_0x0075;
    L_0x0069:
        r8 = 1;
        goto L_0x0076;
    L_0x006b:
        r10 = "bottom";
        r8 = r8.equals(r10);	 Catch:{ Exception -> 0x009b }
        if (r8 == 0) goto L_0x0075;
    L_0x0073:
        r8 = 3;
        goto L_0x0076;
    L_0x0075:
        r8 = r9;
    L_0x0076:
        switch(r8) {
            case 0: goto L_0x008f;
            case 1: goto L_0x0088;
            case 2: goto L_0x0081;
            case 3: goto L_0x007a;
            default: goto L_0x0079;
        };	 Catch:{ Exception -> 0x009b }
    L_0x0079:
        goto L_0x0096;
    L_0x007a:
        r8 = r7.getInt(r1);	 Catch:{ Exception -> 0x009b }
        r3.bottom = r8;	 Catch:{ Exception -> 0x009b }
        goto L_0x0096;
    L_0x0081:
        r8 = r7.getInt(r1);	 Catch:{ Exception -> 0x009b }
        r3.right = r8;	 Catch:{ Exception -> 0x009b }
        goto L_0x0096;
    L_0x0088:
        r8 = r7.getInt(r1);	 Catch:{ Exception -> 0x009b }
        r3.top = r8;	 Catch:{ Exception -> 0x009b }
        goto L_0x0096;
    L_0x008f:
        r8 = r7.getInt(r1);	 Catch:{ Exception -> 0x009b }
        r3.left = r8;	 Catch:{ Exception -> 0x009b }
    L_0x0096:
        r6 = r6 + 1;
        goto L_0x002b;
    L_0x0099:
        return r3;
    L_0x009a:
        goto L_0x00a3;
    L_0x009b:
        r0 = move-exception;
        r1 = "DrawableUtils";
        r2 = "Couldn't obtain the optical insets. Ignoring.";
        android.util.Log.e(r1, r2);
    L_0x00a3:
        r0 = INSETS_NONE;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.DrawableUtils.getOpticalBounds(android.graphics.drawable.Drawable):android.graphics.Rect");
    }

    static void fixDrawable(@NonNull Drawable drawable) {
        if (VERSION.SDK_INT == 21 && VECTOR_DRAWABLE_CLAZZ_NAME.equals(drawable.getClass().getName())) {
            fixVectorDrawableTinting(drawable);
        }
    }

    public static boolean canSafelyMutateDrawable(@NonNull Drawable drawable) {
        if (VERSION.SDK_INT < 15 && (drawable instanceof InsetDrawable)) {
            return false;
        }
        if (VERSION.SDK_INT < 15 && (drawable instanceof GradientDrawable)) {
            return false;
        }
        if (VERSION.SDK_INT < 17 && (drawable instanceof LayerDrawable)) {
            return false;
        }
        if (drawable instanceof DrawableContainer) {
            ConstantState state = drawable.getConstantState();
            if (state instanceof DrawableContainerState) {
                for (Drawable child : ((DrawableContainerState) state).getChildren()) {
                    if (!canSafelyMutateDrawable(child)) {
                        return false;
                    }
                }
            }
        } else if (drawable instanceof WrappedDrawable) {
            return canSafelyMutateDrawable(((WrappedDrawable) drawable).getWrappedDrawable());
        } else {
            if (drawable instanceof DrawableWrapper) {
                return canSafelyMutateDrawable(((DrawableWrapper) drawable).getWrappedDrawable());
            }
            if (drawable instanceof ScaleDrawable) {
                return canSafelyMutateDrawable(((ScaleDrawable) drawable).getDrawable());
            }
        }
        return true;
    }

    private static void fixVectorDrawableTinting(Drawable drawable) {
        int[] originalState = drawable.getState();
        if (originalState == null || originalState.length == 0) {
            drawable.setState(ThemeUtils.CHECKED_STATE_SET);
        } else {
            drawable.setState(ThemeUtils.EMPTY_STATE_SET);
        }
        drawable.setState(originalState);
    }

    public static Mode parseTintMode(int value, Mode defaultMode) {
        if (value == 3) {
            return Mode.SRC_OVER;
        }
        if (value == 5) {
            return Mode.SRC_IN;
        }
        if (value == 9) {
            return Mode.SRC_ATOP;
        }
        switch (value) {
            case 14:
                return Mode.MULTIPLY;
            case 15:
                return Mode.SCREEN;
            case 16:
                return Mode.ADD;
            default:
                return defaultMode;
        }
    }
}
