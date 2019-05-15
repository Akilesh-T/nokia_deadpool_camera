package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.hmdglobal.app.camera.OnCancelSelectionMenuListener;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.Thumbnail;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.BitmapPackager;

public class PeekImageView extends RotateImageView {
    private static final int ANIMATE_BULB_DURATION = 50;
    public static final float CIRCLE_OFFSET = 0.0f;
    private static final float SCALE_DEFAULT = 1.0f;
    private static final float SCALE_LOWER = 0.1f;
    private static final float SCALE_UPPER = 1.0f;
    public static final float STROKE_WIDTH = 2.0f;
    private Tag TAG = new Tag("PeekImageView");
    private float mAnimatorFraction = 1.0f;
    private BitmapPackager mBitmap;
    private BitmapPackager mBitmapPackager;
    private ValueAnimator mBulbAnimator;
    private OnCancelSelectionMenuListener mCancelSelectionMenuListener;
    private Drawable mDrawable;
    private boolean mNeedSuperDrawable = true;
    private Bitmap mSrcBitmap;
    private Uri mUri;

    public void setOnCancelSelectionMenu(OnCancelSelectionMenuListener onCancelSelectionMenuListener) {
        this.mCancelSelectionMenuListener = onCancelSelectionMenuListener;
    }

    public PeekImageView(Context context) {
        super(context);
    }

    public PeekImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(context, attrs);
    }

    private void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PeekImageView, 0, 0);
        int src = a.getResourceId(0, 0);
        a.recycle();
        setViewImage(src);
    }

    public void setViewImage(int resId) {
        this.mDrawable = getResources().getDrawable(resId, null);
        this.mBitmap = new BitmapPackager(BitmapFactory.decodeResource(getResources(), resId));
        invalidate();
    }

    public void setViewImage(Bitmap bmp) {
        this.mDrawable = new BitmapDrawable(getContext().getResources(), bmp);
        this.mBitmap = new BitmapPackager(bmp);
        invalidate();
    }

    public void setViewThumb(Thumbnail thumb) {
        if (thumb != null) {
            this.mUri = thumb.getUri();
            setViewImage(thumb.getBitmap());
            return;
        }
        this.mUri = null;
        setViewImage((int) R.drawable.camera_photo_library);
    }

    public void setViewThumbBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            setViewImage(bitmap);
        } else {
            setViewImage((int) R.drawable.camera_photo_library);
        }
    }

    public void animateThumbBitmap(Bitmap bitmap) {
        if (this.mBulbAnimator == null) {
            this.mBulbAnimator = buildBulbAnimator(bitmap);
        }
        setViewImage(bitmap);
        if (this.mBulbAnimator.isRunning()) {
            this.mBulbAnimator.cancel();
        }
        this.mNeedSuperDrawable = false;
        this.mBulbAnimator.start();
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0003, element type: float, insn element type: null */
    private android.animation.ValueAnimator buildBulbAnimator(android.graphics.Bitmap r4) {
        /*
        r3 = this;
        r0 = 2;
        r0 = new float[r0];
        r0 = {1036831949, 1065353216};
        r0 = android.animation.ValueAnimator.ofFloat(r0);
        r1 = 50;
        r0.setDuration(r1);
        r1 = new com.hmdglobal.app.camera.ui.PeekImageView$1;
        r1.<init>();
        r0.addUpdateListener(r1);
        r1 = new com.hmdglobal.app.camera.ui.PeekImageView$2;
        r1.<init>();
        r0.addListener(r1);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.PeekImageView.buildBulbAnimator(android.graphics.Bitmap):android.animation.ValueAnimator");
    }

    /* Access modifiers changed, original: protected */
    public boolean needSuperDrawable() {
        return this.mNeedSuperDrawable;
    }

    public void setViewThumbUri(Uri peekthumbUri) {
        this.mUri = peekthumbUri;
    }

    public Uri getUri() {
        return this.mUri;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (canvas.getWidth() <= 0) {
            if (this.mNeedSuperDrawable) {
                super.onDraw(canvas);
            }
            return;
        }
        float radius = ((float) (getWidth() / 2)) - 0.0f;
        float x = radius + 0.0f;
        float y = x;
        Matrix mat = new Matrix();
        mat.postTranslate(0.0f, 0.0f);
        mat.postScale(this.mAnimatorFraction, this.mAnimatorFraction, x, y);
        Paint paint = new Paint();
        paint.setColor(-1);
        float mStrokeWidth = (float) getContext().getResources().getDimensionPixelSize(R.dimen.bottom_bar_peek_thumb_stroke_width);
        setAlpha(this.mAnimatorFraction);
        canvas.drawBitmap(generatePeekBitmap(radius, this.mBitmap.get()).get(), mat, paint);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(mStrokeWidth);
        paint.setStyle(Style.STROKE);
        if (this.mNeedSuperDrawable) {
            super.onDraw(canvas);
        }
        canvas.drawCircle(x, y, radius - (mStrokeWidth / 2.0f), paint);
    }

    public float getCenterPoint() {
        return getRadius() + 0.0f;
    }

    public float getRadius() {
        return ((float) (getWidth() / 2)) - 0.0f;
    }

    private BitmapPackager generatePeekBitmap(float radius, Bitmap src) {
        float f = radius;
        Bitmap bitmap = src;
        if (bitmap == this.mSrcBitmap) {
            return this.mBitmapPackager;
        }
        int transX;
        int transY;
        int subHeight;
        int subWidth;
        int bmpWidth = src.getWidth();
        int bmpHeight = src.getHeight();
        int scaledWidth;
        int scaledHeight;
        if (bmpWidth > bmpHeight) {
            scaledWidth = ((((int) f) * 2) * bmpWidth) / bmpHeight;
            scaledHeight = ((int) f) * 2;
            transX = (((int) ((((float) scaledWidth) - (2.0f * f)) / 2.0f)) * bmpWidth) / scaledWidth;
            transY = 0;
            subHeight = bmpHeight;
            subWidth = bmpHeight;
        } else {
            scaledWidth = ((int) f) * 2;
            scaledHeight = ((((int) f) * 2) * bmpHeight) / bmpWidth;
            transX = 0;
            transY = (((int) ((((float) scaledHeight) - (2.0f * f)) / 2.0f)) * bmpHeight) / scaledHeight;
            subHeight = bmpWidth;
            subWidth = bmpWidth;
        }
        Bitmap bmp = Bitmap.createBitmap(((int) f) * 2, ((int) f) * 2, Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        Log.w(this.TAG, String.format("width is %d height is %d transX is %d transY is %d subWidth is %d subHeight is %d", new Object[]{Integer.valueOf(bmpWidth), Integer.valueOf(bmpHeight), Integer.valueOf(transX), Integer.valueOf(transY), Integer.valueOf(subWidth), Integer.valueOf(subHeight)}));
        BitmapPackager scaledbitmap = new BitmapPackager(Bitmap.createBitmap(bitmap, transX, transY, subWidth, subHeight));
        Matrix mat = new Matrix();
        mat.setScale((f * 2.0f) / ((float) subWidth), (2.0f * f) / ((float) subHeight));
        Path path = new Path();
        path.addCircle(f, f, f, Direction.CW);
        canvas.clipPath(path);
        canvas.drawBitmap(scaledbitmap.get(), mat, paint);
        this.mBitmapPackager = new BitmapPackager(bmp);
        this.mSrcBitmap = bitmap;
        setBitmap(bmp);
        return this.mBitmapPackager;
    }

    /* Access modifiers changed, original: protected */
    public boolean needTranslation() {
        return false;
    }

    public void setEnabled(boolean enabled) {
        enableFilter(false);
        super.setEnabled(enabled);
    }

    public boolean onTouchEvent(MotionEvent event) {
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PeekImageView] onTouchEvent isLocked = ");
        stringBuilder.append(isLocked());
        stringBuilder.append(" action = ");
        stringBuilder.append(event.getAction());
        stringBuilder.append(" isIgnoreLock = ");
        stringBuilder.append(isIgnoreLock());
        Log.d(tag, stringBuilder.toString());
        if (isLocked() && isIgnoreLock() && event.getAction() == 1 && this.mCancelSelectionMenuListener != null) {
            this.mCancelSelectionMenuListener.onCancelSelectionMenu();
        }
        return super.onTouchEvent(event);
    }
}
