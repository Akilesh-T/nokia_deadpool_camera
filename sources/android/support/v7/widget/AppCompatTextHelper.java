package android.support.v7.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.content.res.ResourcesCompat.FontCallback;
import android.support.v4.widget.AutoSizeableTextView;
import android.support.v7.appcompat.R;
import android.widget.TextView;
import java.lang.ref.WeakReference;

class AppCompatTextHelper {
    private static final int MONOSPACE = 3;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private boolean mAsyncFontPending;
    @NonNull
    private final AppCompatTextViewAutoSizeHelper mAutoSizeTextHelper;
    private TintInfo mDrawableBottomTint;
    private TintInfo mDrawableEndTint;
    private TintInfo mDrawableLeftTint;
    private TintInfo mDrawableRightTint;
    private TintInfo mDrawableStartTint;
    private TintInfo mDrawableTopTint;
    private Typeface mFontTypeface;
    private int mStyle = 0;
    private final TextView mView;

    AppCompatTextHelper(TextView view) {
        this.mView = view;
        this.mAutoSizeTextHelper = new AppCompatTextViewAutoSizeHelper(this.mView);
    }

    /* Access modifiers changed, original: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x01e4  */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x01eb  */
    /* JADX WARNING: Removed duplicated region for block: B:87:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x01f2  */
    @android.annotation.SuppressLint({"NewApi"})
    public void loadFromAttributes(android.util.AttributeSet r19, int r20) {
        /*
        r18 = this;
        r0 = r18;
        r1 = r19;
        r2 = r20;
        r3 = r0.mView;
        r3 = r3.getContext();
        r4 = android.support.v7.widget.AppCompatDrawableManager.get();
        r5 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper;
        r6 = 0;
        r5 = android.support.v7.widget.TintTypedArray.obtainStyledAttributes(r3, r1, r5, r2, r6);
        r7 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_textAppearance;
        r8 = -1;
        r7 = r5.getResourceId(r7, r8);
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableLeft;
        r9 = r5.hasValue(r9);
        if (r9 == 0) goto L_0x0032;
    L_0x0026:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableLeft;
        r9 = r5.getResourceId(r9, r6);
        r9 = createTintInfo(r3, r4, r9);
        r0.mDrawableLeftTint = r9;
    L_0x0032:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableTop;
        r9 = r5.hasValue(r9);
        if (r9 == 0) goto L_0x0046;
    L_0x003a:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableTop;
        r9 = r5.getResourceId(r9, r6);
        r9 = createTintInfo(r3, r4, r9);
        r0.mDrawableTopTint = r9;
    L_0x0046:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableRight;
        r9 = r5.hasValue(r9);
        if (r9 == 0) goto L_0x005a;
    L_0x004e:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableRight;
        r9 = r5.getResourceId(r9, r6);
        r9 = createTintInfo(r3, r4, r9);
        r0.mDrawableRightTint = r9;
    L_0x005a:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableBottom;
        r9 = r5.hasValue(r9);
        if (r9 == 0) goto L_0x006e;
    L_0x0062:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableBottom;
        r9 = r5.getResourceId(r9, r6);
        r9 = createTintInfo(r3, r4, r9);
        r0.mDrawableBottomTint = r9;
    L_0x006e:
        r9 = android.os.Build.VERSION.SDK_INT;
        r10 = 17;
        if (r9 < r10) goto L_0x009c;
    L_0x0074:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableStart;
        r9 = r5.hasValue(r9);
        if (r9 == 0) goto L_0x0088;
    L_0x007c:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableStart;
        r9 = r5.getResourceId(r9, r6);
        r9 = createTintInfo(r3, r4, r9);
        r0.mDrawableStartTint = r9;
    L_0x0088:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableEnd;
        r9 = r5.hasValue(r9);
        if (r9 == 0) goto L_0x009c;
    L_0x0090:
        r9 = android.support.v7.appcompat.R.styleable.AppCompatTextHelper_android_drawableEnd;
        r9 = r5.getResourceId(r9, r6);
        r9 = createTintInfo(r3, r4, r9);
        r0.mDrawableEndTint = r9;
    L_0x009c:
        r5.recycle();
        r9 = r0.mView;
        r9 = r9.getTransformationMethod();
        r9 = r9 instanceof android.text.method.PasswordTransformationMethod;
        r10 = 0;
        r11 = 0;
        r12 = 0;
        r13 = 0;
        r14 = 0;
        r15 = 23;
        if (r7 == r8) goto L_0x00fd;
    L_0x00b0:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance;
        r5 = android.support.v7.widget.TintTypedArray.obtainStyledAttributes(r3, r7, r8);
        if (r9 != 0) goto L_0x00c8;
    L_0x00b8:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_textAllCaps;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x00c8;
    L_0x00c0:
        r8 = 1;
        r11 = android.support.v7.appcompat.R.styleable.TextAppearance_textAllCaps;
        r10 = r5.getBoolean(r11, r6);
        r11 = r8;
    L_0x00c8:
        r0.updateTypefaceAndStyle(r3, r5);
        r8 = android.os.Build.VERSION.SDK_INT;
        if (r8 >= r15) goto L_0x00fa;
    L_0x00cf:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x00dd;
    L_0x00d7:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor;
        r12 = r5.getColorStateList(r8);
    L_0x00dd:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorHint;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x00eb;
    L_0x00e5:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorHint;
        r13 = r5.getColorStateList(r8);
    L_0x00eb:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorLink;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x00fa;
    L_0x00f3:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorLink;
        r8 = r5.getColorStateList(r8);
        r14 = r8;
    L_0x00fa:
        r5.recycle();
    L_0x00fd:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance;
        r5 = android.support.v7.widget.TintTypedArray.obtainStyledAttributes(r3, r1, r8, r2, r6);
        if (r9 != 0) goto L_0x0114;
    L_0x0105:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_textAllCaps;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x0114;
    L_0x010d:
        r11 = 1;
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_textAllCaps;
        r10 = r5.getBoolean(r8, r6);
    L_0x0114:
        r8 = android.os.Build.VERSION.SDK_INT;
        if (r8 >= r15) goto L_0x0144;
    L_0x0118:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x0127;
    L_0x0120:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor;
        r8 = r5.getColorStateList(r8);
        r12 = r8;
    L_0x0127:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorHint;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x0136;
    L_0x012f:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorHint;
        r8 = r5.getColorStateList(r8);
        r13 = r8;
    L_0x0136:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorLink;
        r8 = r5.hasValue(r8);
        if (r8 == 0) goto L_0x0144;
    L_0x013e:
        r8 = android.support.v7.appcompat.R.styleable.TextAppearance_android_textColorLink;
        r14 = r5.getColorStateList(r8);
    L_0x0144:
        r0.updateTypefaceAndStyle(r3, r5);
        r5.recycle();
        if (r12 == 0) goto L_0x0151;
    L_0x014c:
        r8 = r0.mView;
        r8.setTextColor(r12);
    L_0x0151:
        if (r13 == 0) goto L_0x0158;
    L_0x0153:
        r8 = r0.mView;
        r8.setHintTextColor(r13);
    L_0x0158:
        if (r14 == 0) goto L_0x015f;
    L_0x015a:
        r8 = r0.mView;
        r8.setLinkTextColor(r14);
    L_0x015f:
        if (r9 != 0) goto L_0x0166;
    L_0x0161:
        if (r11 == 0) goto L_0x0166;
    L_0x0163:
        r0.setAllCaps(r10);
    L_0x0166:
        r8 = r0.mFontTypeface;
        if (r8 == 0) goto L_0x0173;
    L_0x016a:
        r8 = r0.mView;
        r15 = r0.mFontTypeface;
        r6 = r0.mStyle;
        r8.setTypeface(r15, r6);
    L_0x0173:
        r6 = r0.mAutoSizeTextHelper;
        r6.loadFromAttributes(r1, r2);
        r6 = android.support.v4.widget.AutoSizeableTextView.PLATFORM_SUPPORTS_AUTOSIZE;
        if (r6 == 0) goto L_0x01c2;
    L_0x017c:
        r6 = r0.mAutoSizeTextHelper;
        r6 = r6.getAutoSizeTextType();
        if (r6 == 0) goto L_0x01c2;
    L_0x0184:
        r6 = r0.mAutoSizeTextHelper;
        r6 = r6.getAutoSizeTextAvailableSizes();
        r8 = r6.length;
        if (r8 <= 0) goto L_0x01c2;
    L_0x018d:
        r8 = r0.mView;
        r8 = r8.getAutoSizeStepGranularity();
        r8 = (float) r8;
        r15 = -1082130432; // 0xffffffffbf800000 float:-1.0 double:NaN;
        r8 = (r8 > r15 ? 1 : (r8 == r15 ? 0 : -1));
        if (r8 == 0) goto L_0x01b7;
    L_0x019a:
        r8 = r0.mView;
        r15 = r0.mAutoSizeTextHelper;
        r15 = r15.getAutoSizeMinTextSize();
        r2 = r0.mAutoSizeTextHelper;
        r2 = r2.getAutoSizeMaxTextSize();
        r16 = r4;
        r4 = r0.mAutoSizeTextHelper;
        r4 = r4.getAutoSizeStepGranularity();
        r17 = r5;
        r5 = 0;
        r8.setAutoSizeTextTypeUniformWithConfiguration(r15, r2, r4, r5);
        goto L_0x01c6;
    L_0x01b7:
        r16 = r4;
        r17 = r5;
        r5 = 0;
        r2 = r0.mView;
        r2.setAutoSizeTextTypeUniformWithPresetSizes(r6, r5);
        goto L_0x01c6;
    L_0x01c2:
        r16 = r4;
        r17 = r5;
    L_0x01c6:
        r2 = android.support.v7.appcompat.R.styleable.AppCompatTextView;
        r2 = android.support.v7.widget.TintTypedArray.obtainStyledAttributes(r3, r1, r2);
        r4 = android.support.v7.appcompat.R.styleable.AppCompatTextView_firstBaselineToTopHeight;
        r5 = -1;
        r4 = r2.getDimensionPixelSize(r4, r5);
        r6 = android.support.v7.appcompat.R.styleable.AppCompatTextView_lastBaselineToBottomHeight;
        r6 = r2.getDimensionPixelSize(r6, r5);
        r8 = android.support.v7.appcompat.R.styleable.AppCompatTextView_lineHeight;
        r8 = r2.getDimensionPixelSize(r8, r5);
        r2.recycle();
        if (r4 == r5) goto L_0x01e9;
    L_0x01e4:
        r15 = r0.mView;
        android.support.v4.widget.TextViewCompat.setFirstBaselineToTopHeight(r15, r4);
    L_0x01e9:
        if (r6 == r5) goto L_0x01f0;
    L_0x01eb:
        r15 = r0.mView;
        android.support.v4.widget.TextViewCompat.setLastBaselineToBottomHeight(r15, r6);
    L_0x01f0:
        if (r8 == r5) goto L_0x01f7;
    L_0x01f2:
        r5 = r0.mView;
        android.support.v4.widget.TextViewCompat.setLineHeight(r5, r8);
    L_0x01f7:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.AppCompatTextHelper.loadFromAttributes(android.util.AttributeSet, int):void");
    }

    private void updateTypefaceAndStyle(Context context, TintTypedArray a) {
        this.mStyle = a.getInt(R.styleable.TextAppearance_android_textStyle, this.mStyle);
        boolean z = true;
        if (a.hasValue(R.styleable.TextAppearance_android_fontFamily) || a.hasValue(R.styleable.TextAppearance_fontFamily)) {
            this.mFontTypeface = null;
            int fontFamilyId = a.hasValue(R.styleable.TextAppearance_fontFamily) ? R.styleable.TextAppearance_fontFamily : R.styleable.TextAppearance_android_fontFamily;
            if (!context.isRestricted()) {
                final WeakReference<TextView> textViewWeak = new WeakReference(this.mView);
                try {
                    this.mFontTypeface = a.getFont(fontFamilyId, this.mStyle, new FontCallback() {
                        public void onFontRetrieved(@NonNull Typeface typeface) {
                            AppCompatTextHelper.this.onAsyncTypefaceReceived(textViewWeak, typeface);
                        }

                        public void onFontRetrievalFailed(int reason) {
                        }
                    });
                    if (this.mFontTypeface != null) {
                        z = false;
                    }
                    this.mAsyncFontPending = z;
                } catch (NotFoundException | UnsupportedOperationException e) {
                }
            }
            if (this.mFontTypeface == null) {
                String fontFamilyName = a.getString(fontFamilyId);
                if (fontFamilyName != null) {
                    this.mFontTypeface = Typeface.create(fontFamilyName, this.mStyle);
                }
            }
            return;
        }
        if (a.hasValue(R.styleable.TextAppearance_android_typeface)) {
            this.mAsyncFontPending = false;
            switch (a.getInt(R.styleable.TextAppearance_android_typeface, 1)) {
                case 1:
                    this.mFontTypeface = Typeface.SANS_SERIF;
                    break;
                case 2:
                    this.mFontTypeface = Typeface.SERIF;
                    break;
                case 3:
                    this.mFontTypeface = Typeface.MONOSPACE;
                    break;
            }
        }
    }

    private void onAsyncTypefaceReceived(WeakReference<TextView> textViewWeak, Typeface typeface) {
        if (this.mAsyncFontPending) {
            this.mFontTypeface = typeface;
            TextView textView = (TextView) textViewWeak.get();
            if (textView != null) {
                textView.setTypeface(typeface, this.mStyle);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void onSetTextAppearance(Context context, int resId) {
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, resId, R.styleable.TextAppearance);
        if (a.hasValue(R.styleable.TextAppearance_textAllCaps)) {
            setAllCaps(a.getBoolean(R.styleable.TextAppearance_textAllCaps, false));
        }
        if (VERSION.SDK_INT < 23 && a.hasValue(R.styleable.TextAppearance_android_textColor)) {
            ColorStateList textColor = a.getColorStateList(R.styleable.TextAppearance_android_textColor);
            if (textColor != null) {
                this.mView.setTextColor(textColor);
            }
        }
        updateTypefaceAndStyle(context, a);
        a.recycle();
        if (this.mFontTypeface != null) {
            this.mView.setTypeface(this.mFontTypeface, this.mStyle);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void setAllCaps(boolean allCaps) {
        this.mView.setAllCaps(allCaps);
    }

    /* Access modifiers changed, original: 0000 */
    public void applyCompoundDrawablesTints() {
        Drawable[] compoundDrawables;
        if (!(this.mDrawableLeftTint == null && this.mDrawableTopTint == null && this.mDrawableRightTint == null && this.mDrawableBottomTint == null)) {
            compoundDrawables = this.mView.getCompoundDrawables();
            applyCompoundDrawableTint(compoundDrawables[0], this.mDrawableLeftTint);
            applyCompoundDrawableTint(compoundDrawables[1], this.mDrawableTopTint);
            applyCompoundDrawableTint(compoundDrawables[2], this.mDrawableRightTint);
            applyCompoundDrawableTint(compoundDrawables[3], this.mDrawableBottomTint);
        }
        if (VERSION.SDK_INT < 17) {
            return;
        }
        if (this.mDrawableStartTint != null || this.mDrawableEndTint != null) {
            compoundDrawables = this.mView.getCompoundDrawablesRelative();
            applyCompoundDrawableTint(compoundDrawables[0], this.mDrawableStartTint);
            applyCompoundDrawableTint(compoundDrawables[2], this.mDrawableEndTint);
        }
    }

    private void applyCompoundDrawableTint(Drawable drawable, TintInfo info) {
        if (drawable != null && info != null) {
            AppCompatDrawableManager.tintDrawable(drawable, info, this.mView.getDrawableState());
        }
    }

    private static TintInfo createTintInfo(Context context, AppCompatDrawableManager drawableManager, int drawableId) {
        ColorStateList tintList = drawableManager.getTintList(context, drawableId);
        if (tintList == null) {
            return null;
        }
        TintInfo tintInfo = new TintInfo();
        tintInfo.mHasTintList = true;
        tintInfo.mTintList = tintList;
        return tintInfo;
    }

    /* Access modifiers changed, original: 0000 */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!AutoSizeableTextView.PLATFORM_SUPPORTS_AUTOSIZE) {
            autoSizeText();
        }
    }

    /* Access modifiers changed, original: 0000 */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void setTextSize(int unit, float size) {
        if (!AutoSizeableTextView.PLATFORM_SUPPORTS_AUTOSIZE && !isAutoSizeEnabled()) {
            setTextSizeInternal(unit, size);
        }
    }

    /* Access modifiers changed, original: 0000 */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public void autoSizeText() {
        this.mAutoSizeTextHelper.autoSizeText();
    }

    /* Access modifiers changed, original: 0000 */
    @RestrictTo({Scope.LIBRARY_GROUP})
    public boolean isAutoSizeEnabled() {
        return this.mAutoSizeTextHelper.isAutoSizeEnabled();
    }

    private void setTextSizeInternal(int unit, float size) {
        this.mAutoSizeTextHelper.setTextSizeInternal(unit, size);
    }

    /* Access modifiers changed, original: 0000 */
    public void setAutoSizeTextTypeWithDefaults(int autoSizeTextType) {
        this.mAutoSizeTextHelper.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
    }

    /* Access modifiers changed, original: 0000 */
    public void setAutoSizeTextTypeUniformWithConfiguration(int autoSizeMinTextSize, int autoSizeMaxTextSize, int autoSizeStepGranularity, int unit) throws IllegalArgumentException {
        this.mAutoSizeTextHelper.setAutoSizeTextTypeUniformWithConfiguration(autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
    }

    /* Access modifiers changed, original: 0000 */
    public void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull int[] presetSizes, int unit) throws IllegalArgumentException {
        this.mAutoSizeTextHelper.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
    }

    /* Access modifiers changed, original: 0000 */
    public int getAutoSizeTextType() {
        return this.mAutoSizeTextHelper.getAutoSizeTextType();
    }

    /* Access modifiers changed, original: 0000 */
    public int getAutoSizeStepGranularity() {
        return this.mAutoSizeTextHelper.getAutoSizeStepGranularity();
    }

    /* Access modifiers changed, original: 0000 */
    public int getAutoSizeMinTextSize() {
        return this.mAutoSizeTextHelper.getAutoSizeMinTextSize();
    }

    /* Access modifiers changed, original: 0000 */
    public int getAutoSizeMaxTextSize() {
        return this.mAutoSizeTextHelper.getAutoSizeMaxTextSize();
    }

    /* Access modifiers changed, original: 0000 */
    public int[] getAutoSizeTextAvailableSizes() {
        return this.mAutoSizeTextHelper.getAutoSizeTextAvailableSizes();
    }
}
