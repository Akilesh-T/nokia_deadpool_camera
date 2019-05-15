package com.morphoinc.utils.graphics;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

class ResizedBitmap {
    ResizedBitmap() {
    }

    public static Bitmap createResizedBitmap(ContentResolver cr, Uri uri, int width, int height) {
        Bitmap scaledBmp = null;
        if (cr == null || uri == null || width < 0 || height < 0) {
            return null;
        }
        try {
            InputStream is = cr.openInputStream(uri);
            if (is != null) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                scaledBmp = Bitmap.createScaledBitmap(bmp, width, height, true);
                bmp.recycle();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scaledBmp;
    }

    /* JADX WARNING: Missing block: B:17:0x0032, code skipped:
            return false;
     */
    public static boolean getPictureSize(android.content.ContentResolver r6, android.net.Uri r7, int[] r8) {
        /*
        r0 = 0;
        if (r6 == 0) goto L_0x0032;
    L_0x0003:
        if (r7 == 0) goto L_0x0032;
    L_0x0005:
        if (r8 != 0) goto L_0x0008;
    L_0x0007:
        goto L_0x0032;
    L_0x0008:
        r1 = r8.length;
        r2 = 2;
        if (r1 >= r2) goto L_0x000d;
    L_0x000c:
        return r0;
    L_0x000d:
        r1 = 0;
        r2 = new android.graphics.BitmapFactory$Options;
        r2.<init>();
        r3 = 1;
        r2.inJustDecodeBounds = r3;
        r4 = r6.openInputStream(r7);	 Catch:{ IOException -> 0x0025 }
        if (r4 == 0) goto L_0x0024;
    L_0x001c:
        r5 = 0;
        android.graphics.BitmapFactory.decodeStream(r4, r5, r2);	 Catch:{ IOException -> 0x0025 }
        r4.close();	 Catch:{ IOException -> 0x0025 }
        r1 = 1;
    L_0x0024:
        goto L_0x0029;
    L_0x0025:
        r4 = move-exception;
        r4.printStackTrace();
    L_0x0029:
        r4 = r2.outWidth;
        r8[r0] = r4;
        r0 = r2.outHeight;
        r8[r3] = r0;
        return r1;
    L_0x0032:
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.utils.graphics.ResizedBitmap.getPictureSize(android.content.ContentResolver, android.net.Uri, int[]):boolean");
    }
}
