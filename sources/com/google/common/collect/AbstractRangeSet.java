package com.google.common.collect;

import javax.annotation.Nullable;

abstract class AbstractRangeSet<C extends Comparable> implements RangeSet<C> {
    public abstract boolean encloses(Range<C> range);

    public abstract Range<C> rangeContaining(C c);

    AbstractRangeSet() {
    }

    public boolean contains(C value) {
        return rangeContaining(value) != null;
    }

    public boolean isEmpty() {
        return asRanges().isEmpty();
    }

    public void add(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    public void remove(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        remove(Range.all());
    }

    public boolean enclosesAll(RangeSet<C> other) {
        for (Range<C> range : other.asRanges()) {
            if (!encloses(range)) {
                return false;
            }
        }
        return true;
    }

    public void addAll(RangeSet<C> other) {
        for (Range<C> range : other.asRanges()) {
            add(range);
        }
    }

    public void removeAll(RangeSet<C> other) {
        for (Range<C> range : other.asRanges()) {
            remove(range);
        }
    }

    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof RangeSet)) {
            return false;
        }
        return asRanges().equals(((RangeSet) obj).asRanges());
    }

    public final int hashCode() {
        return asRanges().hashCode();
    }

    public final String toString() {
        return asRanges().toString();
    }
}
