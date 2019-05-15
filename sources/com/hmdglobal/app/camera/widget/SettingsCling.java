package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.R;

public class SettingsCling extends FrameLayout {
    private final Paint mClingPaint = new Paint();
    private final int mClingTriangleHeight;
    private final int mClingTriangleWidth;
    private final Path mTrianglePath = new Path();

    public SettingsCling(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        this.mClingTriangleHeight = getResources().getDimensionPixelSize(R.dimen.settings_cling_triangle_height);
        this.mClingTriangleWidth = getResources().getDimensionPixelSize(R.dimen.settings_cling_triangle_width);
        this.mClingPaint.setColor(getResources().getColor(R.color.settings_cling_color));
        this.mClingPaint.setStyle(Style.FILL);
    }

    public void updatePosition(View referenceView) {
        if (referenceView != null) {
            setTranslationX((referenceView.getX() + ((float) referenceView.getMeasuredWidth())) - ((float) getMeasuredWidth()));
            float referenceTop = referenceView.getY();
            float triangleStartX;
            if (referenceTop < ((float) getMeasuredHeight())) {
                setTranslationY(((float) referenceView.getMeasuredHeight()) + referenceTop);
                triangleStartX = (float) (getMeasuredWidth() - (referenceView.getMeasuredWidth() / 2));
                this.mTrianglePath.reset();
                this.mTrianglePath.moveTo(triangleStartX, 0.0f);
                this.mTrianglePath.lineTo(triangleStartX - ((float) (this.mClingTriangleWidth / 2)), ((float) this.mClingTriangleHeight) + 0.0f);
                this.mTrianglePath.lineTo(((float) (this.mClingTriangleWidth / 2)) + triangleStartX, ((float) this.mClingTriangleHeight) + 0.0f);
                this.mTrianglePath.lineTo(triangleStartX, 0.0f);
            } else {
                setTranslationY(referenceTop - ((float) getMeasuredHeight()));
                triangleStartX = (float) (getMeasuredWidth() - (referenceView.getMeasuredWidth() / 2));
                float triangleStartY = (float) getMeasuredHeight();
                this.mTrianglePath.reset();
                this.mTrianglePath.moveTo(triangleStartX, triangleStartY);
                this.mTrianglePath.lineTo(triangleStartX - ((float) (this.mClingTriangleWidth / 2)), triangleStartY - ((float) this.mClingTriangleHeight));
                this.mTrianglePath.lineTo(((float) (this.mClingTriangleWidth / 2)) + triangleStartX, triangleStartY - ((float) this.mClingTriangleHeight));
                this.mTrianglePath.lineTo(triangleStartX, triangleStartY);
            }
            invalidate();
        }
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(this.mTrianglePath, this.mClingPaint);
    }
}
