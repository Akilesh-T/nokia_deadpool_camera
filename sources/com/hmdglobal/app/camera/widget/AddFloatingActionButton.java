package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.util.AttributeSet;
import com.hmdglobal.app.camera.R;

public class AddFloatingActionButton extends FloatingActionButton {
    int mPlusColor;

    public AddFloatingActionButton(Context context) {
        this(context, null);
    }

    public AddFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AddFloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /* Access modifiers changed, original: 0000 */
    public void init(Context context, AttributeSet attributeSet) {
        super.init(context, attributeSet);
    }

    /* Access modifiers changed, original: 0000 */
    public Drawable getIconDrawable() {
        float iconSize = getDimension(1.7812047E38f);
        float iconHalfSize = iconSize / 2.0f;
        final float dimension = (iconSize - getDimension(R.dimen.fab_plus_icon_size)) / 2.0f;
        final float f = iconHalfSize;
        final float dimension2 = getDimension(R.dimen.fab_plus_icon_stroke) / 2.0f;
        final float f2 = iconSize;
        ShapeDrawable drawable = new ShapeDrawable(new Shape() {
            public void draw(Canvas canvas, Paint paint) {
                canvas.drawRect(dimension, f - dimension2, f2 - dimension, dimension2 + f, paint);
                canvas.drawRect(f - dimension2, dimension, f + dimension2, f2 - dimension, paint);
            }
        });
        Paint paint = drawable.getPaint();
        paint.setColor(this.mPlusColor);
        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);
        return drawable;
    }
}
