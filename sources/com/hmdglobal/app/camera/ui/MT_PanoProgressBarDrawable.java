package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import com.hmdglobal.app.camera.R;

class MT_PanoProgressBarDrawable extends Drawable {
    private static final String TAG = "MT_ProgressBarDrawable";
    private View mAttachedView;
    private int[] mBlockSizes = null;
    private Drawable mCleanBlock;
    private Drawable mDirtyBlock;
    private int mPadding;
    private final Paint mPaint = new Paint();

    public MT_PanoProgressBarDrawable(Context context, View view, int[] blockSizes, int padding) {
        Resources res = context.getResources();
        this.mBlockSizes = blockSizes;
        this.mPadding = padding;
        this.mCleanBlock = res.getDrawable(R.drawable.ic_panorama_block);
        this.mDirtyBlock = res.getDrawable(R.drawable.ic_panorama_block_highlight);
        this.mAttachedView = view;
    }

    /* Access modifiers changed, original: protected */
    public boolean onLevelChange(int level) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[onLevelChange:]level = ");
        stringBuilder.append(level);
        Log.d(str, stringBuilder.toString());
        invalidateSelf();
        return true;
    }

    public int getIntrinsicWidth() {
        int width = 0;
        for (int i = 0; i < this.mBlockSizes.length - 1; i++) {
            width += this.mBlockSizes[i] + this.mPadding;
        }
        width += this.mBlockSizes[this.mBlockSizes.length - 1];
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[getIntrinsicWidth]");
        stringBuilder.append(width);
        Log.d(str, stringBuilder.toString());
        return width;
    }

    public void setColorFilter(ColorFilter cf) {
        this.mPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return -3;
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    public void draw(Canvas canvas) {
        int i;
        int yoffset;
        int xoffset = 0;
        int level = getLevel();
        for (i = 0; i < level; i++) {
            yoffset = (this.mAttachedView.getHeight() - this.mBlockSizes[i]) / 2;
            this.mDirtyBlock.setBounds(xoffset, yoffset, this.mBlockSizes[i] + xoffset, this.mBlockSizes[i] + yoffset);
            this.mDirtyBlock.draw(canvas);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[draw]dirty block,i=");
            stringBuilder.append(i);
            stringBuilder.append(" xoffset = ");
            stringBuilder.append(xoffset);
            stringBuilder.append(" yoffset = ");
            stringBuilder.append(yoffset);
            Log.v(str, stringBuilder.toString());
            xoffset += this.mBlockSizes[i] + this.mPadding;
        }
        yoffset = this.mBlockSizes.length;
        for (i = level; i < yoffset; i++) {
            int yoffset2 = (this.mAttachedView.getHeight() - this.mBlockSizes[i]) / 2;
            this.mCleanBlock.setBounds(xoffset, yoffset2, this.mBlockSizes[i] + xoffset, this.mBlockSizes[i] + yoffset2);
            this.mCleanBlock.draw(canvas);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[draw]rest,i=");
            stringBuilder2.append(i);
            stringBuilder2.append(" xoffset = ");
            stringBuilder2.append(xoffset);
            stringBuilder2.append(" yoffset = ");
            stringBuilder2.append(yoffset2);
            Log.d(str2, stringBuilder2.toString());
            xoffset += this.mBlockSizes[i] + this.mPadding;
        }
    }
}
