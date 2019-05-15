package com.morphoinc.app.panoramagp3;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Size;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class LeftDirectionFunction extends DirectionFunction {
    public LeftDirectionFunction(int inputWidth, int inputHeight, int maxWidth, int maxHeight, int scale, int angle) {
        super(inputWidth, inputHeight, maxWidth, maxHeight, scale, angle);
        this.direction = 0;
    }

    public Size getPreviewSize() {
        return getHorizontalPreviewSize();
    }

    public boolean calcPreviewRect(Rect previewRect) {
        if (MediaProviderUtils.ROTATION_270 == this.angle) {
            UpDirectionFunction.calcPreviewRect(previewRect, this.inputWidth, this.inputHeight);
            return true;
        } else if (90 == this.angle) {
            DownDirectionFunction.calcPreviewRect(previewRect, this.inputWidth, this.inputHeight);
            return true;
        } else {
            calcPreviewRect(previewRect, this.inputWidth, this.inputHeight);
            return true;
        }
    }

    public static void calcPreviewRect(Rect previewRect, int width, int height) {
        previewRect.left -= previewRect.right - width;
        previewRect.right = width;
        previewRect.bottom = previewRect.height();
        previewRect.top = 0;
    }

    /* Access modifiers changed, original: protected */
    public boolean checkImageComplete(Rect boundingRect) {
        boolean z = false;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            if (this.maxWidth <= boundingRect.height()) {
                z = true;
            }
            return z;
        }
        if (this.maxWidth <= boundingRect.width()) {
            z = true;
        }
        return z;
    }

    public int checkError(Rect clippingRect) {
        int w;
        int h;
        int input_h;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            w = clippingRect.height();
            h = clippingRect.width();
            input_h = this.inputWidth;
        } else {
            w = clippingRect.width();
            h = clippingRect.height();
            input_h = this.inputHeight;
        }
        if (w <= 0 || h <= input_h / 3) {
            return -1;
        }
        if (h > input_h) {
            return -2;
        }
        return 0;
    }

    public boolean getHorizontalNaviVisibility() {
        return true;
    }

    public Rect getCenterLinePadding(Rect previewRect, Point centerPos, int viewWidth, int viewHeight) {
        return getHorizontalCenterLinePadding(previewRect, centerPos, viewHeight);
    }

    public boolean enabled() {
        return true;
    }
}
