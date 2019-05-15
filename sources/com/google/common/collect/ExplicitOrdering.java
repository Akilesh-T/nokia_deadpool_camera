package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap.Builder;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;

@GwtCompatible(serializable = true)
final class ExplicitOrdering<T> extends Ordering<T> implements Serializable {
    private static final long serialVersionUID = 0;
    final ImmutableMap<T, Integer> rankMap;

    ExplicitOrdering(List<T> valuesInOrder) {
        this(buildRankMap(valuesInOrder));
    }

    ExplicitOrdering(ImmutableMap<T, Integer> rankMap) {
        this.rankMap = rankMap;
    }

    public int compare(T left, T right) {
        return rank(left) - rank(right);
    }

    private int rank(T value) {
        Integer rank = (Integer) this.rankMap.get(value);
        if (rank != null) {
            return rank.intValue();
        }
        throw new IncomparableValueException(value);
    }

    private static <T> ImmutableMap<T, Integer> buildRankMap(List<T> valuesInOrder) {
        Builder<T, Integer> builder = ImmutableMap.builder();
        int rank = 0;
        for (T value : valuesInOrder) {
            int rank2 = rank + 1;
            builder.put(value, Integer.valueOf(rank));
            rank = rank2;
        }
        return builder.build();
    }

    public boolean equals(@Nullable Object object) {
        if (!(object instanceof ExplicitOrdering)) {
            return false;
        }
        return this.rankMap.equals(((ExplicitOrdering) object).rankMap);
    }

    public int hashCode() {
        return this.rankMap.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ordering.explicit(");
        stringBuilder.append(this.rankMap.keySet());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
