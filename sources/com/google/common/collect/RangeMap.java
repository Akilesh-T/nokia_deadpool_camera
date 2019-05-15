package com.google.common.collect;

import com.google.common.annotations.Beta;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@Beta
public interface RangeMap<K extends Comparable, V> {
    Map<Range<K>, V> asMapOfRanges();

    void clear();

    boolean equals(@Nullable Object obj);

    @Nullable
    V get(K k);

    @Nullable
    Entry<Range<K>, V> getEntry(K k);

    int hashCode();

    void put(Range<K> range, V v);

    void putAll(RangeMap<K, V> rangeMap);

    void remove(Range<K> range);

    Range<K> span();

    RangeMap<K, V> subRangeMap(Range<K> range);

    String toString();
}
