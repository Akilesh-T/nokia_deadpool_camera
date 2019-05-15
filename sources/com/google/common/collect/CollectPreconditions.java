package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

@GwtCompatible
final class CollectPreconditions {
    CollectPreconditions() {
    }

    static void checkEntryNotNull(Object key, Object value) {
        StringBuilder stringBuilder;
        if (key == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("null key in entry: null=");
            stringBuilder.append(value);
            throw new NullPointerException(stringBuilder.toString());
        } else if (value == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("null value in entry: ");
            stringBuilder.append(key);
            stringBuilder.append("=null");
            throw new NullPointerException(stringBuilder.toString());
        }
    }

    static int checkNonnegative(int value, String name) {
        if (value >= 0) {
            return value;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" cannot be negative but was: ");
        stringBuilder.append(value);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    static void checkRemove(boolean canRemove) {
        Preconditions.checkState(canRemove, "no calls to next() since the last call to remove()");
    }
}
