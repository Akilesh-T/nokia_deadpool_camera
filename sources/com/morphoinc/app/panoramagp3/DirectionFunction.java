package com.morphoinc.app.panoramagp3;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Size;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class DirectionFunction {
    static final int DIRECTION_DOWN = 3;
    static final int DIRECTION_LEFT = 0;
    private static final int DIRECTION_NONE = -1;
    static final int DIRECTION_RIGHT = 1;
    static final int DIRECTION_UP = 2;
    public static final int ERROR_NO_EFFECTIVE_PIXEL = -1;
    public static final int ERROR_OVER_SWING = -2;
    public static final int SUCCEEDED = 0;
    final int angle;
    int direction = -1;
    final int inputHeight;
    final int inputWidth;
    final int maxHeight;
    final int maxWidth;
    private boolean requestQuitFlag;
    private final int scale;

    public int getScale() {
        return this.scale;
    }

    public int getAngle() {
        return this.angle;
    }

    public DirectionFunction(int inputWidth, int inputHeight, int maxWidth, int maxHeight, int scale, int angle) {
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        this.scale = scale * 2;
        this.angle = angle;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.requestQuitFlag = false;
    }

    public Size getPreviewSize() {
        return new Size(this.inputWidth, this.inputHeight);
    }

    /* Access modifiers changed, original: 0000 */
    public Size getVerticalPreviewSize() {
        int width;
        int height = ((this.maxHeight + this.scale) - 1) / this.scale;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            width = ((this.inputHeight + this.scale) - 1) / this.scale;
        } else {
            width = ((this.inputWidth + this.scale) - 1) / this.scale;
        }
        return new Size(width & -2, height & -2);
    }

    /* Access modifiers changed, original: 0000 */
    public Size getHorizontalPreviewSize() {
        int height;
        int width = ((this.maxWidth + this.scale) - 1) / this.scale;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            height = ((this.inputWidth + this.scale) - 1) / this.scale;
        } else {
            height = ((this.inputHeight + this.scale) - 1) / this.scale;
        }
        return new Size(width & -2, height & -2);
    }

    public boolean calcPreviewRect(Rect previewRect) {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean checkImageComplete(Rect boundingRect) {
        return true;
    }

    public boolean isImageComplete(Rect boundingRect) {
        return this.requestQuitFlag || checkImageComplete(boundingRect);
    }

    public boolean isImageComplete() {
        return this.requestQuitFlag;
    }

    public int checkError(Rect clippingRect) {
        return 0;
    }

    public boolean getVerticalNaviVisibility() {
        return false;
    }

    public boolean getHorizontalNaviVisibility() {
        return false;
    }

    public Rect getCenterLinePadding(Rect previewRect, Point centerPos, int viewWidth, int viewHeight) {
        return new Rect(0, 0, 0, 0);
    }

    /* Access modifiers changed, original: 0000 */
    public Rect getHorizontalCenterLinePadding(Rect previewRect, Point centerPos, int viewHeight) {
        int top;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            top = getPaddingTop(previewRect.left, previewRect.right, centerPos.x, viewHeight);
        } else {
            top = getPaddingTop(previewRect.top, previewRect.bottom, centerPos.y, viewHeight);
        }
        return new Rect(0, top, 0, 0);
    }

    private int getPaddingTop(int previewTop, int previewBottom, int centerY, int viewHeight) {
        return (viewHeight >> 1) + ((((previewBottom - previewTop) >> 1) - centerY) / this.scale);
    }

    /* Access modifiers changed, original: 0000 */
    public Rect getVerticalCenterLinePadding(Rect previewRect, Point centerPos, int viewWidth) {
        int left;
        if (90 == this.angle || MediaProviderUtils.ROTATION_270 == this.angle) {
            left = getPaddingLeft(previewRect.top, previewRect.bottom, centerPos.y, viewWidth);
        } else {
            left = getPaddingLeft(previewRect.left, previewRect.right, centerPos.x, viewWidth);
        }
        return new Rect(left, 0, 0, 0);
    }

    private int getPaddingLeft(int previewLeft, int previewRight, int centerX, int viewWidth) {
        return (viewWidth >> 1) + ((((previewRight - previewLeft) >> 1) - centerX) / this.scale);
    }

    public boolean enabled() {
        return false;
    }

    public void requestQuit() {
        this.requestQuitFlag = true;
    }

    public int getDirection() {
        return this.direction;
    }

    public static int reverseDirection(int direction) {
        switch (direction) {
            case 0:
                return 1;
            case 1:
                return 0;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return direction;
        }
    }
}
