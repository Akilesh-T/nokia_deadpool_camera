package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.hmdglobal.app.camera.HelpTip;
import com.hmdglobal.app.camera.R;
import java.util.ArrayList;
import java.util.List;

public class HelpTipCling extends FrameLayout {
    private static final float CIRCLE_BOLD = 7.0f;
    private static final int ERASE_COLOR = 16777215;
    private static final int RING_WIDTH = 2;
    private int mBackColor;
    private Bitmap mBitmap;
    private int mBottom;
    private Canvas mCanvas;
    private Paint mCirclePaint;
    private float mDensity;
    private View mDrawCircleView;
    private int mDrawRectangleEraseRadiuX;
    private int mDrawRectangleEraseRadiuY;
    private int mDrawRectangleRadiuX;
    private int mDrawRectangleRadiuY;
    private int mDrawType;
    private Paint mErasePaint;
    private View mFocusView;
    private List<Rect> mHitClickRect = new ArrayList();
    private int mLeft;
    private HelpTip mListener;
    private RelativeLayout mManualMenuTip;
    private View mPreViewOverLay;
    private RectF mRectF = new RectF();
    private int mRight;
    private float mRingWidth;
    private int mTop;

    public static class LayoutParams extends android.widget.FrameLayout.LayoutParams {
        private boolean isCircleView = false;
        private boolean isFocusView = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.HelpTipCling);
            this.isFocusView = a.getBoolean(1, false);
            this.isCircleView = a.getBoolean(0, false);
            a.recycle();
        }

        public Boolean isFocusView() {
            return Boolean.valueOf(this.isFocusView);
        }

        public Boolean isDrawCircleView() {
            return Boolean.valueOf(this.isCircleView);
        }
    }

    public HelpTipCling(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        initPainter();
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics);
        this.mDensity = metrics.density;
        this.mRingWidth = 2.0f * metrics.density;
        this.mDrawRectangleRadiuX = getResources().getInteger(R.integer.help_tip_draw_rectangle_radius_x);
        this.mDrawRectangleRadiuY = getResources().getInteger(R.integer.help_tip_draw_rectangle_radius_y);
        this.mDrawRectangleEraseRadiuX = getResources().getInteger(R.integer.help_tip_draw_rectangle_erase_radius_x);
        this.mDrawRectangleEraseRadiuY = getResources().getInteger(R.integer.help_tip_draw_rectangle_erase_radius_y);
    }

    public void setListener(HelpTip listener, int drawType) {
        this.mListener = listener;
        this.mDrawType = drawType;
    }

    public void setPreViewOverLay(View preViewOverLay) {
        this.mPreViewOverLay = preViewOverLay;
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
        this.mCanvas = new Canvas(this.mBitmap);
    }

    private void initPainter() {
        this.mCirclePaint = new Paint(1);
        this.mCirclePaint.setColor(getResources().getColor(R.color.help_tip_animation_circle_color));
        this.mCirclePaint.setStrokeWidth(CIRCLE_BOLD);
        this.mCirclePaint.setStyle(Style.STROKE);
        this.mBackColor = getResources().getColor(R.color.tourial_semitransparent);
        this.mErasePaint = new Paint();
        this.mErasePaint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));
        this.mErasePaint.setColor(16777215);
        this.mErasePaint.setAntiAlias(true);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int i = 0;
        if (this.mListener == null) {
            return false;
        }
        int index = -1;
        if (this.mHitClickRect != null) {
            while (i <= this.mHitClickRect.size() - 1) {
                if (((Rect) this.mHitClickRect.get(i)).contains((int) ev.getX(), (int) ev.getY())) {
                    index = i;
                    break;
                }
                i++;
            }
        }
        if (index != -1) {
            this.mListener.clickHitRectResponse(index);
        }
        if (this.mPreViewOverLay != null) {
            return onTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mPreViewOverLay != null) {
            return this.mPreViewOverLay.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mListener != null) {
            if (this.mDrawType == -1) {
                drawHelpTipDefault(canvas);
            } else if (this.mDrawType == 0) {
                drawHelpTipCircle(canvas);
            } else if (this.mDrawType == 1) {
                drawHelpTipRectangle(canvas);
            } else if (this.mDrawType == 2) {
                drawHelpTipLine(canvas);
            }
        }
    }

    private void drawHelpTipDefault(Canvas canvas) {
        this.mBitmap.eraseColor(0);
        this.mCanvas.drawColor(this.mBackColor);
        canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, null);
    }

    private void drawHelpTipCircle(Canvas canvas) {
        this.mBitmap.eraseColor(0);
        this.mCanvas.drawColor(this.mBackColor);
        if (this.mFocusView != null) {
            this.mLeft = this.mFocusView.getLeft();
            this.mTop = this.mFocusView.getTop();
            this.mRight = this.mFocusView.getRight();
            this.mBottom = this.mFocusView.getBottom();
            int width = this.mRight - this.mLeft;
            int height = this.mBottom - this.mTop;
            float cx = (float) ((this.mLeft + this.mRight) / 2);
            float cy = (float) ((this.mTop + this.mBottom) / 2);
            float radius = (float) (width > height ? width / 2 : height / 2);
            this.mCanvas.drawCircle(cx, cy, radius, this.mCirclePaint);
            this.mCanvas.drawCircle(cx, cy, radius - this.mRingWidth, this.mErasePaint);
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, null);
        }
    }

    private void drawHelpTipRectangle(Canvas canvas) {
        this.mBitmap.eraseColor(0);
        this.mCanvas.drawColor(this.mBackColor);
        if (this.mFocusView != null) {
            Rect rect = new Rect();
            this.mFocusView.getHitRect(rect);
            this.mRectF.set(rect);
            if (this.mListener.getCurTipGroupId() == 2) {
                this.mDrawRectangleRadiuX += getResources().getInteger(R.integer.manual_tip_menu_item_radius_increment_x);
                this.mDrawRectangleRadiuY += getResources().getInteger(R.integer.manual_tip_menu_item_radius_increment_y);
                this.mDrawRectangleEraseRadiuX += getResources().getInteger(R.integer.manual_tip_menu_item_erase_radius_increment_x);
                this.mDrawRectangleEraseRadiuY += getResources().getInteger(R.integer.manual_tip_menu_item_erase_radius_increment_y);
            }
            this.mCanvas.drawRoundRect(this.mRectF, (float) this.mDrawRectangleRadiuX, (float) this.mDrawRectangleRadiuY, this.mCirclePaint);
            this.mRectF.inset(this.mDensity, this.mDensity);
            this.mCanvas.drawRoundRect(this.mRectF, (float) this.mDrawRectangleEraseRadiuX, (float) this.mDrawRectangleEraseRadiuY, this.mErasePaint);
        }
        if (this.mDrawCircleView != null) {
            this.mLeft = this.mDrawCircleView.getLeft();
            this.mTop = this.mDrawCircleView.getTop();
            this.mRight = this.mDrawCircleView.getRight();
            this.mBottom = this.mDrawCircleView.getBottom();
            int width = this.mRight - this.mLeft;
            int height = this.mBottom - this.mTop;
            float cx = (float) ((this.mLeft + this.mRight) / 2.8E-45f);
            float cy = (float) ((this.mTop + this.mBottom) / 2);
            float radius = (float) (width > height ? width / 2 : height / 2);
            this.mCanvas.drawCircle(cx, cy, radius, this.mCirclePaint);
            this.mCanvas.drawCircle(cx, cy, radius - this.mRingWidth, this.mErasePaint);
        }
        canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, null);
    }

    private void drawHelpTipLine(Canvas canvas) {
        if (this.mFocusView != null) {
            this.mBitmap.eraseColor(0);
            this.mCanvas.drawColor(this.mBackColor);
            this.mLeft = this.mFocusView.getLeft();
            this.mTop = this.mFocusView.getTop();
            this.mRight = this.mFocusView.getRight();
            this.mBottom = this.mFocusView.getBottom();
            this.mCanvas.drawRect(0.0f, (float) this.mTop, (float) this.mRight, (float) this.mBottom, this.mCirclePaint);
            this.mCanvas.drawRect(0.0f, (float) this.mTop, (float) this.mRight, (float) this.mBottom, this.mErasePaint);
            this.mCanvas.drawLine(0.0f, (float) this.mTop, (float) this.mRight, (float) this.mTop, this.mCirclePaint);
            this.mCanvas.drawLine(0.0f, (float) this.mBottom, (float) this.mRight, (float) this.mBottom, this.mCirclePaint);
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, null);
        }
    }

    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (params instanceof LayoutParams) {
            LayoutParams clingLayoutParams = (LayoutParams) params;
            if (clingLayoutParams.isFocusView().booleanValue()) {
                this.mFocusView = child;
            }
            if (clingLayoutParams.isDrawCircleView().booleanValue()) {
                this.mDrawCircleView = child;
            }
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public void cleanDestroy() {
        this.mCirclePaint = null;
        this.mErasePaint = null;
        this.mFocusView = null;
        this.mDrawCircleView = null;
    }

    public void setHitRect(List<Rect> listRect) {
        this.mHitClickRect = listRect;
    }
}
