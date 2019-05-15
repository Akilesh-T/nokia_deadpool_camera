package com.morphoinc.app.panoramagp3;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Size;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class UpDirectionFunction extends DirectionFunction {
    public UpDirectionFunction(int inputWidth, int inputHeight, int maxWidth, int maxHeight, int scale, int angle) {
        super(inputWidth, inputHeight, maxWidth, maxHeight, scale, angle);
        this.direction = 2;
    }

    public Size getPreviewSize() {
        return getVerticalPreviewSize();
    }

    public boolean calcPreviewRect(Rect previewRect) {
        if (MediaProviderUtils.ROTATION_270 == this.angle) {
            RightDirectionFunction.calcPreviewRect(previewRect, this.inputWidth, this.inputHeight);
            return true;
        } else if (90 == this.angle) {
            LeftDirectionFunction.calcPreviewRect(previewRect, this.inputWidth, this.inputHeight);
            return true;
        } else {
            calcPreviewRect(previewRect, this.inputWidth, this.inputHeight);
            return true;
        }
    }

    public static void calcPreviewRect(Rect previewRect, int width, int height) {
        previewRect.right = previewRect.width();
        previewRect.left = 0;
        previewRect.top -= previewRect.bottom - height;
        previewRect.bottom = height;
    }

    /* Access modifiers changed, original: protected */
    public boolean checkImageComplete(Rect boundingRect) {
        boolean z = false;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            if (this.maxHeight <= boundingRect.width()) {
                z = true;
            }
            return z;
        }
        if (this.maxHeight <= boundingRect.height()) {
            z = true;
        }
        return z;
    }

    public int checkError(Rect clippingRect) {
        int w;
        int h;
        int input_h;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            w = clippingRect.width();
            h = clippingRect.height();
            input_h = this.inputHeight;
        } else {
            w = clippingRect.height();
            h = clippingRect.width();
            input_h = this.inputWidth;
        }
        if (w <= 0 || h <= input_h / 3) {
            return -1;
        }
        if (h > input_h) {
            return -2;
        }
        return 0;
    }

    public boolean getVerticalNaviVisibility() {
        return true;
    }

    public Rect getCenterLinePadding(Rect previewRect, Point centerPos, int viewWidth, int viewHeight) {
        return getVerticalCenterLinePadding(previewRect, centerPos, viewWidth);
    }

    public boolean enabled() {
        return true;
    }
}
