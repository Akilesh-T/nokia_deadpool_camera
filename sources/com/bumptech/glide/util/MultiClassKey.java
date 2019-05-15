package com.bumptech.glide.util;

public class MultiClassKey {
    private Class<?> first;
    private Class<?> second;

    public MultiClassKey(Class<?> first, Class<?> second) {
        set(first, second);
    }

    public void set(Class<?> first, Class<?> second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MultiClassKey{first=");
        stringBuilder.append(this.first);
        stringBuilder.append(", second=");
        stringBuilder.append(this.second);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MultiClassKey that = (MultiClassKey) o;
        if (this.first.equals(that.first) && this.second.equals(that.second)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * this.first.hashCode()) + this.second.hashCode();
    }
}
