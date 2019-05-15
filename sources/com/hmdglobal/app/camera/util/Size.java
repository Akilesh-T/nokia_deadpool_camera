package com.hmdglobal.app.camera.util;

import android.graphics.Point;
import java.util.ArrayList;
import java.util.List;

public class Size {
    private final int height;
    private final int width;

    public static Size[] convert(android.util.Size[] sizes) {
        Size[] converted = new Size[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            converted[i] = new Size(sizes[i].getWidth(), sizes[i].getHeight());
        }
        return converted;
    }

    public static List<Size> convert(List<com.android.ex.camera2.portability.Size> sizes) {
        ArrayList<Size> converted = new ArrayList(sizes.size());
        for (com.android.ex.camera2.portability.Size size : sizes) {
            converted.add(new Size(size.width(), size.height()));
        }
        return converted;
    }

    public Size(Point point) {
        this.width = point.x;
        this.height = point.y;
    }

    public Size(android.util.Size size) {
        this.width = size.getWidth();
        this.height = size.getHeight();
    }

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.width);
        stringBuilder.append(" x ");
        stringBuilder.append(this.height);
        return stringBuilder.toString();
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof Size)) {
            return false;
        }
        Size otherSize = (Size) other;
        if (otherSize.width == this.width && otherSize.height == this.height) {
            z = true;
        }
        return z;
    }
}
