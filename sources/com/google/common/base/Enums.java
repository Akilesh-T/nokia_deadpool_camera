package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
@Beta
public final class Enums {
    @GwtIncompatible("java.lang.ref.WeakReference")
    private static final Map<Class<? extends Enum<?>>, Map<String, WeakReference<? extends Enum<?>>>> enumConstantCache = new WeakHashMap();

    private static final class StringConverter<T extends Enum<T>> extends Converter<String, T> implements Serializable {
        private static final long serialVersionUID = 0;
        private final Class<T> enumClass;

        StringConverter(Class<T> enumClass) {
            this.enumClass = (Class) Preconditions.checkNotNull(enumClass);
        }

        /* Access modifiers changed, original: protected */
        public T doForward(String value) {
            return Enum.valueOf(this.enumClass, value);
        }

        /* Access modifiers changed, original: protected */
        public String doBackward(T enumValue) {
            return enumValue.name();
        }

        public boolean equals(@Nullable Object object) {
            if (!(object instanceof StringConverter)) {
                return false;
            }
            return this.enumClass.equals(((StringConverter) object).enumClass);
        }

        public int hashCode() {
            return this.enumClass.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Enums.stringConverter(");
            stringBuilder.append(this.enumClass.getName());
            stringBuilder.append(".class)");
            return stringBuilder.toString();
        }
    }

    private Enums() {
    }

    @GwtIncompatible("reflection")
    public static Field getField(Enum<?> enumValue) {
        try {
            return enumValue.getDeclaringClass().getDeclaredField(enumValue.name());
        } catch (NoSuchFieldException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public static <T extends Enum<T>> Optional<T> getIfPresent(Class<T> enumClass, String value) {
        Preconditions.checkNotNull(enumClass);
        Preconditions.checkNotNull(value);
        return Platform.getEnumIfPresent(enumClass, value);
    }

    @GwtIncompatible("java.lang.ref.WeakReference")
    private static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> populateCache(Class<T> enumClass) {
        Map<String, WeakReference<? extends Enum<?>>> result = new HashMap();
        Iterator it = EnumSet.allOf(enumClass).iterator();
        while (it.hasNext()) {
            Enum enumInstance = (Enum) it.next();
            result.put(enumInstance.name(), new WeakReference(enumInstance));
        }
        enumConstantCache.put(enumClass, result);
        return result;
    }

    @GwtIncompatible("java.lang.ref.WeakReference")
    static <T extends Enum<T>> Map<String, WeakReference<? extends Enum<?>>> getEnumConstants(Class<T> enumClass) {
        Map<String, WeakReference<? extends Enum<?>>> constants;
        synchronized (enumConstantCache) {
            constants = (Map) enumConstantCache.get(enumClass);
            if (constants == null) {
                constants = populateCache(enumClass);
            }
        }
        return constants;
    }

    public static <T extends Enum<T>> Converter<String, T> stringConverter(Class<T> enumClass) {
        return new StringConverter(enumClass);
    }
}
