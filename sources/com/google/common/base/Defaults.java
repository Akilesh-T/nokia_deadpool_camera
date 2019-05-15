package com.google.common.base;

import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class Defaults {
    private static final Map<Class<?>, Object> DEFAULTS;

    private Defaults() {
    }

    static {
        Map<Class<?>, Object> map = new HashMap();
        put(map, Boolean.TYPE, Boolean.valueOf(false));
        put(map, Character.TYPE, Character.valueOf(0));
        put(map, Byte.TYPE, Byte.valueOf((byte) 0));
        put(map, Short.TYPE, Short.valueOf((short) 0));
        put(map, Integer.TYPE, Integer.valueOf(0));
        put(map, Long.TYPE, Long.valueOf(0));
        put(map, Float.TYPE, Float.valueOf(0.0f));
        put(map, Double.TYPE, Double.valueOf(Camera2ParamsFragment.TARGET_EV));
        DEFAULTS = Collections.unmodifiableMap(map);
    }

    private static <T> void put(Map<Class<?>, Object> map, Class<T> type, T value) {
        map.put(type, value);
    }

    @Nullable
    public static <T> T defaultValue(Class<T> type) {
        return DEFAULTS.get(Preconditions.checkNotNull(type));
    }
}
