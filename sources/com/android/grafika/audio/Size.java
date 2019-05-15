package com.android.grafika.audio;

public final class Size {
    private final int mHeight;
    private final int mWidth;

    public Size(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Size)) {
            return false;
        }
        Size other = (Size) obj;
        if (this.mWidth == other.mWidth && this.mHeight == other.mHeight) {
            z = true;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mWidth);
        stringBuilder.append("x");
        stringBuilder.append(this.mHeight);
        return stringBuilder.toString();
    }

    private static NumberFormatException invalidSize(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid Size: \"");
        stringBuilder.append(s);
        stringBuilder.append("\"");
        throw new NumberFormatException(stringBuilder.toString());
    }

    public int hashCode() {
        return this.mHeight ^ ((this.mWidth << 16) | (this.mWidth >>> 16));
    }
}
