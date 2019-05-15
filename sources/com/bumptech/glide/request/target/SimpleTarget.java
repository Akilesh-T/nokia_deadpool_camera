package com.bumptech.glide.request.target;

public abstract class SimpleTarget<Z> extends BaseTarget<Z> {
    private final int height;
    private final int width;

    public SimpleTarget() {
        this(-1, -1);
    }

    public SimpleTarget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public final void getSize(SizeReadyCallback cb) {
        if (this.width <= 0 || this.height <= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Width and height must both be > 0, but given width: ");
            stringBuilder.append(this.width);
            stringBuilder.append(" and height: ");
            stringBuilder.append(this.height);
            stringBuilder.append(", either provide dimensions in the constructor or call override()");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        cb.onSizeReady(this.width, this.height);
    }
}
