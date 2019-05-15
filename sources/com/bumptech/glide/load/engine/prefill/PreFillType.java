package com.bumptech.glide.load.engine.prefill;

import android.graphics.Bitmap.Config;

public final class PreFillType {
    static final Config DEFAULT_CONFIG = Config.RGB_565;
    private final Config config;
    private final int height;
    private final int weight;
    private final int width;

    public static class Builder {
        private Config config;
        private final int height;
        private int weight;
        private final int width;

        public Builder(int size) {
            this(size, size);
        }

        public Builder(int width, int height) {
            this.weight = 1;
            if (width <= 0) {
                throw new IllegalArgumentException("Width must be > 0");
            } else if (height > 0) {
                this.width = width;
                this.height = height;
            } else {
                throw new IllegalArgumentException("Height must be > 0");
            }
        }

        public Builder setConfig(Config config) {
            this.config = config;
            return this;
        }

        /* Access modifiers changed, original: 0000 */
        public Config getConfig() {
            return this.config;
        }

        public Builder setWeight(int weight) {
            if (weight > 0) {
                this.weight = weight;
                return this;
            }
            throw new IllegalArgumentException("Weight must be > 0");
        }

        /* Access modifiers changed, original: 0000 */
        public PreFillType build() {
            return new PreFillType(this.width, this.height, this.config, this.weight);
        }
    }

    PreFillType(int width, int height, Config config, int weight) {
        if (config != null) {
            this.width = width;
            this.height = height;
            this.config = config;
            this.weight = weight;
            return;
        }
        throw new NullPointerException("Config must not be null");
    }

    /* Access modifiers changed, original: 0000 */
    public int getWidth() {
        return this.width;
    }

    /* Access modifiers changed, original: 0000 */
    public int getHeight() {
        return this.height;
    }

    /* Access modifiers changed, original: 0000 */
    public Config getConfig() {
        return this.config;
    }

    /* Access modifiers changed, original: 0000 */
    public int getWeight() {
        return this.weight;
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof PreFillType)) {
            return false;
        }
        PreFillType other = (PreFillType) o;
        if (this.height == other.height && this.width == other.width && this.weight == other.weight && this.config == other.config) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * this.width) + this.height)) + this.config.hashCode())) + this.weight;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PreFillSize{width=");
        stringBuilder.append(this.width);
        stringBuilder.append(", height=");
        stringBuilder.append(this.height);
        stringBuilder.append(", config=");
        stringBuilder.append(this.config);
        stringBuilder.append(", weight=");
        stringBuilder.append(this.weight);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
