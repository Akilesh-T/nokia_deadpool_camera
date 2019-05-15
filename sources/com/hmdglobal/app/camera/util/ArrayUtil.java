package com.hmdglobal.app.camera.util;

import java.util.Collection;

public final class ArrayUtil {
    private ArrayUtil() {
    }

    public static boolean isEmpty(Collection<?> array) {
        return array == null || array.isEmpty();
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static int sizeOf(Collection<?> array) {
        return array == null ? 0 : array.size();
    }

    public static <T> int sizeOf(T[] array) {
        return array == null ? 0 : array.length;
    }
}
