package android.support.v4.content.res;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.FontRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.util.Preconditions;
import android.util.TypedValue;

public final class ResourcesCompat {
    private static final String TAG = "ResourcesCompat";

    public static abstract class FontCallback {
        public abstract void onFontRetrievalFailed(int i);

        public abstract void onFontRetrieved(@NonNull Typeface typeface);

        @RestrictTo({Scope.LIBRARY_GROUP})
        public final void callbackSuccessAsync(final Typeface typeface, @Nullable Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrieved(typeface);
                }
            });
        }

        @RestrictTo({Scope.LIBRARY_GROUP})
        public final void callbackFailAsync(final int reason, @Nullable Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrievalFailed(reason);
                }
            });
        }
    }

    @Nullable
    public static Drawable getDrawable(@NonNull Resources res, @DrawableRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawable(id, theme);
        }
        return res.getDrawable(id);
    }

    @Nullable
    public static Drawable getDrawableForDensity(@NonNull Resources res, @DrawableRes int id, int density, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 21) {
            return res.getDrawableForDensity(id, density, theme);
        }
        if (VERSION.SDK_INT >= 15) {
            return res.getDrawableForDensity(id, density);
        }
        return res.getDrawable(id);
    }

    @ColorInt
    public static int getColor(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColor(id, theme);
        }
        return res.getColor(id);
    }

    @Nullable
    public static ColorStateList getColorStateList(@NonNull Resources res, @ColorRes int id, @Nullable Theme theme) throws NotFoundException {
        if (VERSION.SDK_INT >= 23) {
            return res.getColorStateList(id, theme);
        }
        return res.getColorStateList(id);
    }

    @Nullable
    public static Typeface getFont(@NonNull Context context, @FontRes int id) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, new TypedValue(), 0, null, null, false);
    }

    public static void getFont(@NonNull Context context, @FontRes int id, @NonNull FontCallback fontCallback, @Nullable Handler handler) throws NotFoundException {
        Preconditions.checkNotNull(fontCallback);
        if (context.isRestricted()) {
            fontCallback.callbackFailAsync(-4, handler);
            return;
        }
        loadFont(context, id, new TypedValue(), 0, fontCallback, handler, false);
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public static Typeface getFont(@NonNull Context context, @FontRes int id, TypedValue value, int style, @Nullable FontCallback fontCallback) throws NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null, true);
    }

    private static Typeface loadFont(@NonNull Context context, int id, TypedValue value, int style, @Nullable FontCallback fontCallback, @Nullable Handler handler, boolean isRequestFromLayoutInflator) {
        Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler, isRequestFromLayoutInflator);
        if (typeface != null || fontCallback != null) {
            return typeface;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Font resource ID #0x");
        stringBuilder.append(Integer.toHexString(id));
        stringBuilder.append(" could not be retrieved.");
        throw new NotFoundException(stringBuilder.toString());
    }

    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    /* JADX WARNING: Removed duplicated region for block: B:63:0x00f1  */
    private static android.graphics.Typeface loadFont(@android.support.annotation.NonNull android.content.Context r20, android.content.res.Resources r21, android.util.TypedValue r22, int r23, int r24, @android.support.annotation.Nullable android.support.v4.content.res.ResourcesCompat.FontCallback r25, @android.support.annotation.Nullable android.os.Handler r26, boolean r27) {
        /*
        r9 = r21;
        r10 = r22;
        r11 = r23;
        r12 = r24;
        r13 = r25;
        r14 = r26;
        r0 = r10.string;
        if (r0 == 0) goto L_0x00f5;
    L_0x0010:
        r0 = r10.string;
        r15 = r0.toString();
        r0 = "res/";
        r0 = r15.startsWith(r0);
        r16 = 0;
        r8 = -3;
        if (r0 != 0) goto L_0x0027;
    L_0x0021:
        if (r13 == 0) goto L_0x0026;
    L_0x0023:
        r13.callbackFailAsync(r8, r14);
    L_0x0026:
        return r16;
    L_0x0027:
        r7 = android.support.v4.graphics.TypefaceCompat.findFromCache(r9, r11, r12);
        if (r7 == 0) goto L_0x0033;
    L_0x002d:
        if (r13 == 0) goto L_0x0032;
    L_0x002f:
        r13.callbackSuccessAsync(r7, r14);
    L_0x0032:
        return r7;
    L_0x0033:
        r0 = r15.toLowerCase();	 Catch:{ XmlPullParserException -> 0x00d2, IOException -> 0x00b5 }
        r1 = ".xml";
        r0 = r0.endsWith(r1);	 Catch:{ XmlPullParserException -> 0x00d2, IOException -> 0x00b5 }
        if (r0 == 0) goto L_0x0093;
    L_0x003f:
        r0 = r9.getXml(r11);	 Catch:{ XmlPullParserException -> 0x008c, IOException -> 0x0085 }
        r1 = android.support.v4.content.res.FontResourcesParserCompat.parse(r0, r9);	 Catch:{ XmlPullParserException -> 0x008c, IOException -> 0x0085 }
        r17 = r1;
        if (r17 != 0) goto L_0x0069;
    L_0x004c:
        r1 = "ResourcesCompat";
        r2 = "Failed to find font-family tag";
        android.util.Log.e(r1, r2);	 Catch:{ XmlPullParserException -> 0x0061, IOException -> 0x0059 }
        if (r13 == 0) goto L_0x0058;
    L_0x0055:
        r13.callbackFailAsync(r8, r14);	 Catch:{ XmlPullParserException -> 0x0061, IOException -> 0x0059 }
    L_0x0058:
        return r16;
    L_0x0059:
        r0 = move-exception;
        r1 = r20;
        r18 = r7;
        r10 = r8;
        goto L_0x00bb;
    L_0x0061:
        r0 = move-exception;
        r1 = r20;
        r18 = r7;
        r10 = r8;
        goto L_0x00d8;
    L_0x0069:
        r1 = r20;
        r2 = r17;
        r3 = r9;
        r4 = r11;
        r5 = r12;
        r6 = r13;
        r18 = r7;
        r7 = r14;
        r10 = r8;
        r8 = r27;
        r1 = android.support.v4.graphics.TypefaceCompat.createFromResourcesFamilyXml(r1, r2, r3, r4, r5, r6, r7, r8);	 Catch:{ XmlPullParserException -> 0x0080, IOException -> 0x007c }
        return r1;
    L_0x007c:
        r0 = move-exception;
        r1 = r20;
        goto L_0x00bb;
    L_0x0080:
        r0 = move-exception;
        r1 = r20;
        goto L_0x00d8;
    L_0x0085:
        r0 = move-exception;
        r18 = r7;
        r10 = r8;
        r1 = r20;
        goto L_0x00bb;
    L_0x008c:
        r0 = move-exception;
        r18 = r7;
        r10 = r8;
        r1 = r20;
        goto L_0x00d8;
    L_0x0093:
        r18 = r7;
        r10 = r8;
        r1 = r20;
        r0 = android.support.v4.graphics.TypefaceCompat.createFromResourcesFontFile(r1, r9, r11, r15, r12);	 Catch:{ XmlPullParserException -> 0x00b3, IOException -> 0x00b1 }
        r7 = r0;
        if (r13 == 0) goto L_0x00b0;
    L_0x009f:
        if (r7 == 0) goto L_0x00ad;
    L_0x00a1:
        r13.callbackSuccessAsync(r7, r14);	 Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x00a5 }
        goto L_0x00b0;
    L_0x00a5:
        r0 = move-exception;
        r18 = r7;
        goto L_0x00bb;
    L_0x00a9:
        r0 = move-exception;
        r18 = r7;
        goto L_0x00d8;
    L_0x00ad:
        r13.callbackFailAsync(r10, r14);	 Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x00a5 }
    L_0x00b0:
        return r7;
    L_0x00b1:
        r0 = move-exception;
        goto L_0x00bb;
    L_0x00b3:
        r0 = move-exception;
        goto L_0x00d8;
    L_0x00b5:
        r0 = move-exception;
        r1 = r20;
        r18 = r7;
        r10 = r8;
    L_0x00bb:
        r2 = "ResourcesCompat";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Failed to read xml resource ";
        r3.append(r4);
        r3.append(r15);
        r3 = r3.toString();
        android.util.Log.e(r2, r3, r0);
        goto L_0x00ef;
    L_0x00d2:
        r0 = move-exception;
        r1 = r20;
        r18 = r7;
        r10 = r8;
    L_0x00d8:
        r2 = "ResourcesCompat";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Failed to parse xml resource ";
        r3.append(r4);
        r3.append(r15);
        r3 = r3.toString();
        android.util.Log.e(r2, r3, r0);
    L_0x00ef:
        if (r13 == 0) goto L_0x00f4;
    L_0x00f1:
        r13.callbackFailAsync(r10, r14);
    L_0x00f4:
        return r16;
    L_0x00f5:
        r1 = r20;
        r0 = new android.content.res.Resources$NotFoundException;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Resource \"";
        r2.append(r3);
        r3 = r9.getResourceName(r11);
        r2.append(r3);
        r3 = "\" (";
        r2.append(r3);
        r3 = java.lang.Integer.toHexString(r23);
        r2.append(r3);
        r3 = ") is not a Font: ";
        r2.append(r3);
        r3 = r22;
        r2.append(r3);
        r2 = r2.toString();
        r0.<init>(r2);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.content.res.ResourcesCompat.loadFont(android.content.Context, android.content.res.Resources, android.util.TypedValue, int, int, android.support.v4.content.res.ResourcesCompat$FontCallback, android.os.Handler, boolean):android.graphics.Typeface");
    }

    private ResourcesCompat() {
    }
}
