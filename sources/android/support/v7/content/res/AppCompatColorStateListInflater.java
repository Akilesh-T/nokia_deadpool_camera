package android.support.v7.content.res;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.util.StateSet;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class AppCompatColorStateListInflater {
    private static final int DEFAULT_COLOR = -65536;

    private AppCompatColorStateListInflater() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:9:0x0017  */
    /* JADX WARNING: Removed duplicated region for block: B:7:0x0012  */
    @android.support.annotation.NonNull
    public static android.content.res.ColorStateList createFromXml(@android.support.annotation.NonNull android.content.res.Resources r4, @android.support.annotation.NonNull org.xmlpull.v1.XmlPullParser r5, @android.support.annotation.Nullable android.content.res.Resources.Theme r6) throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        /*
        r0 = android.util.Xml.asAttributeSet(r5);
    L_0x0004:
        r1 = r5.next();
        r2 = r1;
        r3 = 2;
        if (r1 == r3) goto L_0x0010;
    L_0x000c:
        r1 = 1;
        if (r2 == r1) goto L_0x0010;
    L_0x000f:
        goto L_0x0004;
    L_0x0010:
        if (r2 != r3) goto L_0x0017;
    L_0x0012:
        r1 = createFromXmlInner(r4, r5, r0, r6);
        return r1;
    L_0x0017:
        r1 = new org.xmlpull.v1.XmlPullParserException;
        r3 = "No start tag found";
        r1.<init>(r3);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.content.res.AppCompatColorStateListInflater.createFromXml(android.content.res.Resources, org.xmlpull.v1.XmlPullParser, android.content.res.Resources$Theme):android.content.res.ColorStateList");
    }

    @NonNull
    private static ColorStateList createFromXmlInner(@NonNull Resources r, @NonNull XmlPullParser parser, @NonNull AttributeSet attrs, @Nullable Theme theme) throws XmlPullParserException, IOException {
        String name = parser.getName();
        if (name.equals("selector")) {
            return inflate(r, parser, attrs, theme);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(parser.getPositionDescription());
        stringBuilder.append(": invalid color state list tag ");
        stringBuilder.append(name);
        throw new XmlPullParserException(stringBuilder.toString());
    }

    private static ColorStateList inflate(@NonNull Resources r, @NonNull XmlPullParser parser, @NonNull AttributeSet attrs, @Nullable Theme theme) throws XmlPullParserException, IOException {
        AttributeSet attributeSet = attrs;
        int i = 1;
        int innerDepth = parser.getDepth() + 1;
        int[][] stateSpecList = new int[20][];
        int[] colorList = new int[stateSpecList.length];
        int defaultColor = -65536;
        int listSize = 0;
        while (true) {
            int next = parser.next();
            int type = next;
            Resources resources;
            Theme theme2;
            int i2;
            int i3;
            if (next == i) {
                resources = r;
                theme2 = theme;
                i2 = innerDepth;
                i3 = defaultColor;
                break;
            }
            next = parser.getDepth();
            int depth = next;
            if (next < innerDepth && type == 3) {
                resources = r;
                theme2 = theme;
                i2 = innerDepth;
                i3 = defaultColor;
                break;
            }
            if (type != 2 || depth > innerDepth) {
                resources = r;
                theme2 = theme;
                i2 = innerDepth;
                i3 = defaultColor;
            } else if (parser.getName().equals("item")) {
                TypedArray a = obtainAttributes(r, theme, attributeSet, R.styleable.ColorStateListItem);
                int baseColor = a.getColor(R.styleable.ColorStateListItem_android_color, -65281);
                float alphaMod = 1.0f;
                if (a.hasValue(R.styleable.ColorStateListItem_android_alpha)) {
                    alphaMod = a.getFloat(R.styleable.ColorStateListItem_android_alpha, 1.0f);
                } else if (a.hasValue(R.styleable.ColorStateListItem_alpha)) {
                    alphaMod = a.getFloat(R.styleable.ColorStateListItem_alpha, 1.0f);
                }
                a.recycle();
                i = attrs.getAttributeCount();
                int[] stateSpec = new int[i];
                i2 = innerDepth;
                innerDepth = 0;
                int i4 = 0;
                while (i4 < i) {
                    int numAttrs = i;
                    i = attributeSet.getAttributeNameResource(i4);
                    i3 = defaultColor;
                    if (!(i == 16843173 || i == 16843551 || i == R.attr.alpha)) {
                        int j = innerDepth + 1;
                        stateSpec[innerDepth] = attributeSet.getAttributeBooleanValue(i4, 0) ? i : -i;
                        innerDepth = j;
                    }
                    i4++;
                    i = numAttrs;
                    defaultColor = i3;
                }
                i3 = defaultColor;
                Object stateSpec2 = StateSet.trimStateSet(stateSpec, innerDepth);
                int color = modulateColorAlpha(baseColor, alphaMod);
                if (listSize == 0 || stateSpec2.length == 0) {
                    defaultColor = color;
                } else {
                    defaultColor = i3;
                }
                colorList = GrowingArrayUtils.append(colorList, listSize, color);
                stateSpecList = (int[][]) GrowingArrayUtils.append((Object[]) stateSpecList, listSize, stateSpec2);
                listSize++;
                innerDepth = i2;
                i = 1;
            } else {
                resources = r;
                theme2 = theme;
                i2 = innerDepth;
                i3 = defaultColor;
            }
            innerDepth = i2;
            defaultColor = i3;
            i = 1;
        }
        int[] colors = new int[listSize];
        int[][] stateSpecs = new int[listSize][];
        System.arraycopy(colorList, 0, colors, 0, listSize);
        System.arraycopy(stateSpecList, 0, stateSpecs, 0, listSize);
        return new ColorStateList(stateSpecs, colors);
    }

    private static TypedArray obtainAttributes(Resources res, Theme theme, AttributeSet set, int[] attrs) {
        if (theme == null) {
            return res.obtainAttributes(set, attrs);
        }
        return theme.obtainStyledAttributes(set, attrs, 0, 0);
    }

    private static int modulateColorAlpha(int color, float alphaMod) {
        return ColorUtils.setAlphaComponent(color, Math.round(((float) Color.alpha(color)) * alphaMod));
    }
}
