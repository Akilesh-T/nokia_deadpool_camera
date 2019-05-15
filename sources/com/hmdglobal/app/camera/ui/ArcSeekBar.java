package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Scroller;
import com.hmdglobal.app.camera.ManualUI;
import com.hmdglobal.app.camera.R;
import java.math.BigDecimal;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

public class ArcSeekBar extends View implements Rotatable {
    private static final int HIDE_TITLE = 1;
    private static final String TAG = "ArcSeekBar_Tag";
    private Paint arcPaint;
    private int ballSrcId;
    private int ballSrcIdPress;
    private int currentPercentLevel;
    private float downY;
    private String[] focusStrList;
    Handler handler;
    private boolean isDisplayTitle;
    private boolean isECEnable;
    private boolean isEnable;
    private boolean isPress;
    private int lastPercentLevel;
    private int mArcMode;
    private ArcSeekBarEventListener mArcSeekBarEventListener;
    private Bitmap mBallBitmap;
    private Paint mBallPaint;
    private int mBallRadius;
    private float mBallX;
    private float mBallY;
    private int mBandWidth;
    private Context mContext;
    private ArrayList<String> mExposureCompensationTitles;
    private ArrayList<String> mExposureTimeTitles;
    private float mHalfRadius;
    private int mHeight;
    private ArrayList<Integer> mISOValues;
    private boolean mIsDrag;
    private int mLevel;
    private int mOrientation;
    private int mPrecentLevel;
    private float mRadius;
    private float mStartAngle;
    private float mSweepAngle;
    private float mTransY;
    private ArrayList<String> mWBTitles;
    private int mWidth;
    private float maxBallX;
    private float minBallX;
    private String progressStr;
    private Scroller scroller;
    private Paint textPaint;
    private String titleStr;
    private TypedArray wabBallSrcIdArray;
    private TypedArray wabBallSrcIdPressArray;

    public ArcSeekBar(Context context) {
        this(context, null);
    }

    public ArcSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.isEnable = true;
        this.isECEnable = true;
        this.mTransY = 0.0f;
        this.mOrientation = 0;
        this.isDisplayTitle = false;
        this.mIsDrag = true;
        this.lastPercentLevel = -1;
        this.isPress = false;
        this.handler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    ArcSeekBar.this.isDisplayTitle = false;
                    ArcSeekBar.this.invalidate();
                }
            }
        };
        initAttrs(context, attrs, defStyleAttr);
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyle) {
        this.mContext = context;
        TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.ProMode, defStyle, 0);
        this.mLevel = attrArray.getInteger(5, 2);
        this.mBandWidth = attrArray.getDimensionPixelSize(3, 0);
        this.mBallRadius = attrArray.getDimensionPixelSize(0, 0);
        this.ballSrcId = attrArray.getResourceId(1, R.drawable.main_iso);
        this.ballSrcIdPress = attrArray.getResourceId(2, R.drawable.iso_press);
        this.titleStr = attrArray.getString(6);
        this.isDisplayTitle = attrArray.getBoolean(4, false);
        this.scroller = new Scroller(context);
        if (TextUtils.isEmpty(this.titleStr)) {
            this.titleStr = "...";
        }
        this.focusStrList = new String[]{this.mContext.getString(R.string.pro_mode_arc_auto), this.mContext.getString(R.string.pro_mode_arc_focus_mode_precent1), this.mContext.getString(R.string.pro_mode_arc_focus_mode_precent2)};
    }

    private void initParameter() {
        if (this.mWidth != 0 && this.mHeight != 0) {
            this.mRadius = ((float) this.mHeight) - (((float) this.mBandWidth) / 2.0f);
            this.mHalfRadius = ((float) this.mHeight) - ((float) this.mBandWidth);
            if (this.mLevel != 1) {
                this.minBallX = ((float) this.mWidth) * 0.1f;
                this.maxBallX = ((float) this.mWidth) * 0.9f;
            } else {
                this.minBallX = ((float) this.mWidth) * 0.3f;
                this.maxBallX = ((float) this.mWidth) * 0.7f;
            }
            if (this.currentPercentLevel == 1) {
                this.mBallX = this.minBallX;
            } else if (this.currentPercentLevel == this.mPrecentLevel - 1) {
                this.mBallX = this.maxBallX;
            } else {
                this.mBallX = (((this.maxBallX - this.minBallX) / ((float) this.mPrecentLevel)) * ((float) this.currentPercentLevel)) + this.minBallX;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mWidth = ");
            stringBuilder.append(this.mWidth);
            stringBuilder.append(" mHeight = ");
            stringBuilder.append(this.mHeight);
            stringBuilder.append(" mRadius = ");
            stringBuilder.append(this.mRadius);
            stringBuilder.append(" mHalfRadius = ");
            stringBuilder.append(this.mHalfRadius);
            Log.d(str, stringBuilder.toString());
            calculateAngle();
        }
    }

    public void initValue(String curState) {
        if (curState != null) {
            try {
                this.currentPercentLevel = ((Integer) new JSONObject(curState).get(ManualUI.SETTING_INDEX)).intValue();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            this.currentPercentLevel = this.mArcMode != 4 ? 0 : 2;
        }
        this.currentPercentLevel++;
        this.lastPercentLevel = 0;
        if (this.mArcSeekBarEventListener != null && this.isECEnable) {
            this.mArcSeekBarEventListener.OnProgressChanged(this.mArcMode, this.currentPercentLevel);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    private Bitmap createCircleBitmap(Bitmap resource) {
        int width = resource.getWidth();
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap circleBitmap = Bitmap.createBitmap(width, width, Config.ARGB_8888);
        Canvas canvas = new Canvas(circleBitmap);
        canvas.drawCircle((float) (width / 2), (float) (width / 2), (float) (width / 2), paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(resource, 0.0f, 0.0f, paint);
        return scaleBitmap(circleBitmap, (float) (this.mBallRadius * 2), (float) (this.mBallRadius * 2));
    }

    public static Bitmap readBitmapById(Context context, int resId) {
        Options opt = new Options();
        opt.inPreferredConfig = Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        return BitmapFactory.decodeStream(context.getResources().openRawResource(resId), null, opt);
    }

    private Bitmap scaleBitmap(Bitmap bitmap, float tragetW, float tragetH) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = tragetW / ((float) width);
        float scaleHeight = tragetH / ((float) height);
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private void calculateAngle() {
        float tempAngle = (float) (Math.asin((double) (((float) (this.mWidth / 2)) / this.mHalfRadius)) * 57.29577951308232d);
        this.mStartAngle = 90.0f - tempAngle;
        this.mSweepAngle = 2.0f * tempAngle;
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.mHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (this.mWidth != 0 && this.mHeight != 0) {
            this.mHeight = (this.mWidth / 2) + (this.mBandWidth * this.mLevel);
            setMeasuredDimension(this.mWidth, this.mHeight);
            initParameter();
        }
    }

    private float getControlcBallY(float x) {
        return (float) (((double) this.mHeight) - Math.sqrt(Math.pow((double) this.mHalfRadius, 2.0d) - Math.pow((double) Math.abs(x - ((float) (this.mWidth / 2))), 2.0d)));
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, (this.mHeight > this.mWidth ? this.mWidth : this.mHeight) / 2);
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        float titleTop;
        float titleBottom;
        int titleBaseLineY;
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        canvas.save();
        canvas2.translate(0.0f, 46.0f);
        if (this.mArcMode == 0) {
            int resourceId;
            Context context = this.mContext;
            if (this.isPress) {
                resourceId = this.wabBallSrcIdPressArray.getResourceId(this.currentPercentLevel - 1, -1);
            } else {
                resourceId = this.wabBallSrcIdArray.getResourceId(this.currentPercentLevel - 1, -1);
            }
            this.mBallBitmap = createCircleBitmap(readBitmapById(context, resourceId));
        } else {
            this.mBallBitmap = createCircleBitmap(readBitmapById(this.mContext, this.isPress ? this.ballSrcIdPress : this.ballSrcId));
        }
        this.textPaint = new Paint(1);
        if (this.isDisplayTitle) {
            canvas.save();
            canvas2.translate(0.0f, (float) (-this.mBandWidth));
            this.textPaint.setTextSize(50.0f);
            this.textPaint.setColor(-1);
            this.textPaint.setStyle(Style.FILL);
            this.textPaint.setTextAlign(Align.CENTER);
            FontMetrics fontMetrics = this.textPaint.getFontMetrics();
            titleTop = fontMetrics.top;
            titleBottom = fontMetrics.bottom;
            titleBaseLineY = (int) ((((float) (this.mBandWidth / 2)) - (titleTop / 2.0f)) - (titleBottom / 2.0f));
            canvas.save();
            canvas2.rotate((float) (-this.mOrientation), (float) (this.mWidth / 2), ((float) titleBaseLineY) - ((titleBottom - titleTop) / 4.0f));
            canvas2.drawText(this.titleStr, (float) (this.mWidth / 2), (float) titleBaseLineY, this.textPaint);
            canvas.restore();
            canvas.restore();
            this.handler.removeMessages(1);
            this.handler.sendEmptyMessageDelayed(1, 1500);
        }
        canvas.save();
        canvas2.translate(0.0f, this.mTransY);
        RectF oval = new RectF(-(this.mRadius - ((float) (this.mWidth / 2))), ((float) this.mHeight) - this.mRadius, this.mRadius + ((float) (this.mWidth / 2)), ((float) this.mHeight) + this.mRadius);
        this.arcPaint = new Paint(1);
        this.arcPaint.setShader(new LinearGradient(0.0f, (float) (this.mHeight - ((this.mWidth * 2) / 3)), 0.0f, (float) (this.mHeight - (this.mWidth / 3)), this.mContext.getColor(R.color.pro_arcseekbar_bg), this.mContext.getColor(R.color.pro_bg_bottom), TileMode.CLAMP));
        this.arcPaint.setStyle(Style.STROKE);
        this.arcPaint.setStrokeWidth((float) this.mBandWidth);
        Canvas canvas3 = canvas2;
        int i = R.color.pro_bg_bottom;
        canvas3.drawArc(oval, 180.0f + this.mStartAngle, this.mSweepAngle, false, this.arcPaint);
        oval = new RectF(-((this.mRadius - ((float) (this.mWidth / 2))) - ((float) (this.mBandWidth / 2))), (((float) this.mHeight) - this.mRadius) + ((float) (this.mBandWidth / 2)), (this.mRadius + ((float) (this.mWidth / 2))) - ((float) (this.mBandWidth / 2)), (((float) this.mHeight) + this.mRadius) - ((float) (this.mBandWidth / 2)));
        this.arcPaint.setShader(new LinearGradient(0.0f, (float) (this.mHeight - ((this.mWidth * 2) / 3)), 0.0f, (float) (this.mHeight - (this.mWidth / 3)), this.mContext.getColor(R.color.pro_arcseekbar_progress_line), this.mContext.getColor(i), TileMode.CLAMP));
        this.arcPaint.setStrokeWidth(1.0f);
        canvas2.drawArc(oval, 180.0f + (this.mLevel != 1 ? this.mStartAngle : this.mStartAngle + (this.mSweepAngle / 4.0f)), this.mLevel != 1 ? this.mSweepAngle : this.mSweepAngle / 2.0f, false, this.arcPaint);
        if (this.mArcMode != 4 || this.isECEnable) {
            this.textPaint.reset();
            this.textPaint.setFlags(1);
            this.textPaint.setColor(-1);
            this.textPaint.setTextSize(20.0f);
            this.textPaint.setStyle(Style.FILL);
            this.textPaint.setTextAlign(Align.CENTER);
            FontMetrics fontMetrics2 = this.textPaint.getFontMetrics();
            titleTop = fontMetrics2.top;
            titleBottom = fontMetrics2.bottom;
            titleBaseLineY = (int) ((((float) (this.mBandWidth / 2)) - (titleTop / 2.0f)) - (titleBottom / 2.0f));
            canvas.save();
            canvas2.rotate((float) (-this.mOrientation), (float) (this.mWidth / 2), ((float) titleBaseLineY) - ((titleBottom - titleTop) / 4.0f));
            this.progressStr = getProgressStr(this.currentPercentLevel);
            canvas2.drawText(this.progressStr, (float) (this.mWidth / 2), (float) titleBaseLineY, this.textPaint);
            canvas.restore();
        }
        this.mBallY = getControlcBallY(this.mBallX);
        this.mBallPaint = new Paint(1);
        this.mBallPaint.setAntiAlias(true);
        this.mBallPaint.setFilterBitmap(true);
        canvas.save();
        canvas2.rotate((float) (-this.mOrientation), this.mBallX, this.mBallY);
        canvas2.drawBitmap(this.mBallBitmap, this.mBallX - ((float) this.mBallRadius), this.mBallY - ((float) this.mBallRadius), this.mBallPaint);
        canvas.restore();
        canvas.restore();
        canvas.restore();
    }

    public void computeScroll() {
        if (this.scroller.computeScrollOffset()) {
            this.mBallX = (float) this.scroller.getCurrX();
            invalidate();
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!this.isECEnable) {
            return false;
        }
        if (!this.isEnable) {
            return calculateTouchRange(event.getX(), event.getY());
        }
        float downX;
        switch (event.getAction()) {
            case 0:
                Log.d(TAG, "ACTION_DOWN");
                downX = event.getX();
                this.downY = event.getY();
                boolean result = calculateTouchRange(downX, this.downY - true);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("result = ");
                stringBuilder.append(result);
                Log.d(str, stringBuilder.toString());
                return result;
            case 1:
                Log.d(TAG, "ACTION_UP");
                break;
            case 2:
                Log.d(TAG, "ACTION_MOVE");
                this.isPress = true;
                if (this.mArcSeekBarEventListener != null) {
                    this.mArcSeekBarEventListener.onMovementChanged(this, this.isPress);
                }
                downX = event.getX();
                float moveY = event.getY();
                float f = downX > this.minBallX ? downX > this.maxBallX ? this.maxBallX : downX : this.minBallX;
                this.mBallX = f;
                this.mBallY = getControlcBallY(this.mBallX);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mBallX = ");
                stringBuilder2.append(this.mBallX);
                stringBuilder2.append(" mBallY = ");
                stringBuilder2.append(this.mBallY);
                Log.d(str2, stringBuilder2.toString());
                this.currentPercentLevel = getPercentLevel(this.mBallX);
                progressChanged();
                if (this.mIsDrag && moveY > this.downY && moveY < this.downY + ((float) (this.mBandWidth * (this.mLevel - 1)))) {
                    this.mTransY = moveY - this.downY;
                }
                invalidate();
                break;
            case 3:
                break;
        }
        Log.d(TAG, "ACTION_CANCEL");
        this.isPress = false;
        this.mTransY = 0.0f;
        if (this.mArcMode != 1) {
            if (this.currentPercentLevel == 1) {
                this.mBallX = this.minBallX;
            } else if (this.currentPercentLevel == this.mPrecentLevel - 1) {
                this.mBallX = this.maxBallX;
            } else {
                this.scroller.startScroll((int) this.mBallX, 0, (int) (((((this.maxBallX - this.minBallX) / ((float) this.mPrecentLevel)) * ((float) this.currentPercentLevel)) - this.mBallX) + this.minBallX), 0, 200);
            }
        }
        progressChanged();
        invalidate();
        if (this.mArcSeekBarEventListener != null) {
            this.mArcSeekBarEventListener.onMovementChanged(this, this.isPress);
        }
        return false;
    }

    private void progressChanged() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("lastPercentLevel = ");
        stringBuilder.append(this.lastPercentLevel);
        stringBuilder.append(" currentPercentLevel = ");
        stringBuilder.append(this.currentPercentLevel);
        Log.d(str, stringBuilder.toString());
        if (this.currentPercentLevel != this.lastPercentLevel && this.mArcSeekBarEventListener != null) {
            this.mArcSeekBarEventListener.OnProgressChanged(this.mArcMode, this.currentPercentLevel);
            this.lastPercentLevel = this.currentPercentLevel;
        }
    }

    /* Access modifiers changed, original: protected */
    public void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        this.isPress = false;
        invalidate();
    }

    private boolean calculateTouchRange(float downX, float downY) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ArcSeekBar] calculateTouchRange downX = ");
        stringBuilder.append(downX);
        stringBuilder.append(" minBallX = ");
        stringBuilder.append(this.minBallX);
        stringBuilder.append(" maxBallX = ");
        stringBuilder.append(this.maxBallX);
        Log.d(str, stringBuilder.toString());
        boolean z = false;
        if (this.mArcMode == 4 && (downX < this.minBallX || downX > this.maxBallX)) {
            return false;
        }
        float distance = (float) (Math.pow((double) (downX - ((float) (this.mWidth / 2))), 2.0d) + Math.pow((double) (((float) this.mHeight) - downY), 2.0d));
        if (((double) distance) > Math.pow((double) (this.mRadius - ((float) this.mBandWidth)), 2.0d) && ((double) distance) < Math.pow((double) this.mRadius, 2.0d)) {
            z = true;
        }
        return z;
    }

    private int getPercentLevel(float x) {
        int result = new BigDecimal((double) (((x - this.minBallX) / (this.maxBallX - this.minBallX)) * ((float) this.mPrecentLevel))).setScale(0, 4).intValue();
        if (result < 1) {
            return 1;
        }
        if (result > this.mPrecentLevel - 1) {
            return this.mPrecentLevel - 1;
        }
        return result;
    }

    private String getProgressStr(int level) {
        String percentStr = this.mContext.getString(R.string.pro_mode_arc_auto);
        if (level <= 0) {
            return percentStr;
        }
        switch (this.mArcMode) {
            case 0:
                return this.mWBTitles.size() != 0 ? (String) this.mWBTitles.get(level - 1) : percentStr;
            case 1:
                if (this.mBallX == this.minBallX) {
                    return this.focusStrList[0];
                }
                if (this.mBallX <= this.minBallX || this.mBallX >= this.maxBallX) {
                    return this.focusStrList[2];
                }
                return this.focusStrList[1];
            case 2:
                if (this.mISOValues.size() == 0) {
                    return percentStr;
                }
                String string;
                if (level == 1) {
                    string = this.mContext.getString(R.string.pro_mode_arc_auto);
                } else {
                    string = String.valueOf(this.mISOValues.get(level - 1));
                }
                return string;
            case 3:
                if (this.mExposureTimeTitles.size() != 0) {
                    return (String) this.mExposureTimeTitles.get(level - 1);
                }
                return percentStr;
            case 4:
                if (this.mExposureCompensationTitles.size() != 0) {
                    return (String) this.mExposureCompensationTitles.get(level - 1);
                }
                return "-2";
            default:
                return percentStr;
        }
    }

    public void setLevel(int mLevel) {
        this.mLevel = mLevel;
    }

    public void setBallSrcId(int ballSrcId) {
        this.ballSrcId = ballSrcId;
    }

    public void setBallSrcIdPress(int ballSrcIdPress) {
        this.ballSrcIdPress = ballSrcIdPress;
    }

    public void setWabBallSrcIdArray(TypedArray wabBallSrcIdArray) {
        this.wabBallSrcIdArray = wabBallSrcIdArray;
    }

    public void setWabBallSrcIdPressArray(TypedArray wabBallSrcIdPressArray) {
        this.wabBallSrcIdPressArray = wabBallSrcIdPressArray;
    }

    public void setTitleStr(String titleStr) {
        this.titleStr = titleStr;
    }

    public void setDisplayTitle(boolean displayTitle) {
        this.isDisplayTitle = displayTitle;
    }

    public void setArcSeekBarEventListener(ArcSeekBarEventListener mArcSeekBarEventListener) {
        this.mArcSeekBarEventListener = mArcSeekBarEventListener;
    }

    public void setIsDrag(boolean isDrag) {
        this.mIsDrag = isDrag;
    }

    public void setArcMode(int mArcMode) {
        this.handler.removeMessages(1);
        this.mArcMode = mArcMode;
        if (mArcMode == 1) {
            this.mPrecentLevel = 100;
        }
    }

    public int getArcMode() {
        return this.mArcMode;
    }

    public void setWBTitles(ArrayList<String> mWBTitles) {
        this.mWBTitles = mWBTitles;
        this.mPrecentLevel = mWBTitles.size() + 1;
    }

    public void setISOValues(ArrayList<Integer> mISOValues) {
        this.mISOValues = mISOValues;
        this.mPrecentLevel = mISOValues.size() + 1;
    }

    public void setExposureTimeTitles(ArrayList<String> mExposureTimeTitles) {
        this.mExposureTimeTitles = mExposureTimeTitles;
        this.mPrecentLevel = mExposureTimeTitles.size() + 1;
    }

    public void setExposureCompensationTitles(ArrayList<String> exposureCompensationTitles) {
        this.mExposureCompensationTitles = exposureCompensationTitles;
        this.mPrecentLevel = this.mExposureCompensationTitles.size() + 1;
    }

    /* Access modifiers changed, original: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void setOrientation(int orientation, boolean animation) {
        orientation %= 360;
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            invalidate();
        }
    }

    public void setEnabled(boolean enabled) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pro mArcMode = ");
        stringBuilder.append(this.mArcMode);
        stringBuilder.append(" setEnabled -> ");
        stringBuilder.append(enabled);
        Log.d(str, stringBuilder.toString());
        this.isEnable = enabled;
    }

    public void setECEnabled(boolean enabled) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pro mArcMode = ");
        stringBuilder.append(this.mArcMode);
        stringBuilder.append(" setEnabled -> ");
        stringBuilder.append(enabled);
        Log.d(str, stringBuilder.toString());
        this.isECEnable = enabled;
    }
}
