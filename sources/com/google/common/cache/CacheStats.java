package com.google.common.cache;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import javax.annotation.Nullable;

@GwtCompatible
@Beta
public final class CacheStats {
    private final long evictionCount;
    private final long hitCount;
    private final long loadExceptionCount;
    private final long loadSuccessCount;
    private final long missCount;
    private final long totalLoadTime;

    public CacheStats(long hitCount, long missCount, long loadSuccessCount, long loadExceptionCount, long totalLoadTime, long evictionCount) {
        long j = hitCount;
        long j2 = missCount;
        long j3 = loadSuccessCount;
        long j4 = loadExceptionCount;
        long j5 = totalLoadTime;
        long j6 = evictionCount;
        Preconditions.checkArgument(j >= 0);
        Preconditions.checkArgument(j2 >= 0);
        Preconditions.checkArgument(j3 >= 0);
        Preconditions.checkArgument(j4 >= 0);
        Preconditions.checkArgument(j5 >= 0);
        Preconditions.checkArgument(j6 >= 0);
        this.hitCount = j;
        this.missCount = j2;
        this.loadSuccessCount = j3;
        this.loadExceptionCount = j4;
        this.totalLoadTime = j5;
        this.evictionCount = j6;
    }

    public long requestCount() {
        return this.hitCount + this.missCount;
    }

    public long hitCount() {
        return this.hitCount;
    }

    public double hitRate() {
        long requestCount = requestCount();
        return requestCount == 0 ? 1.0d : ((double) this.hitCount) / ((double) requestCount);
    }

    public long missCount() {
        return this.missCount;
    }

    public double missRate() {
        long requestCount = requestCount();
        return requestCount == 0 ? Camera2ParamsFragment.TARGET_EV : ((double) this.missCount) / ((double) requestCount);
    }

    public long loadCount() {
        return this.loadSuccessCount + this.loadExceptionCount;
    }

    public long loadSuccessCount() {
        return this.loadSuccessCount;
    }

    public long loadExceptionCount() {
        return this.loadExceptionCount;
    }

    public double loadExceptionRate() {
        long totalLoadCount = this.loadSuccessCount + this.loadExceptionCount;
        if (totalLoadCount == 0) {
            return Camera2ParamsFragment.TARGET_EV;
        }
        return ((double) this.loadExceptionCount) / ((double) totalLoadCount);
    }

    public long totalLoadTime() {
        return this.totalLoadTime;
    }

    public double averageLoadPenalty() {
        long totalLoadCount = this.loadSuccessCount + this.loadExceptionCount;
        if (totalLoadCount == 0) {
            return Camera2ParamsFragment.TARGET_EV;
        }
        return ((double) this.totalLoadTime) / ((double) totalLoadCount);
    }

    public long evictionCount() {
        return this.evictionCount;
    }

    public CacheStats minus(CacheStats other) {
        CacheStats cacheStats = other;
        return new CacheStats(Math.max(0, this.hitCount - cacheStats.hitCount), Math.max(0, this.missCount - cacheStats.missCount), Math.max(0, this.loadSuccessCount - cacheStats.loadSuccessCount), Math.max(0, this.loadExceptionCount - cacheStats.loadExceptionCount), Math.max(0, this.totalLoadTime - cacheStats.totalLoadTime), Math.max(0, this.evictionCount - cacheStats.evictionCount));
    }

    public CacheStats plus(CacheStats other) {
        CacheStats cacheStats = other;
        return new CacheStats(cacheStats.hitCount + this.hitCount, cacheStats.missCount + this.missCount, cacheStats.loadSuccessCount + this.loadSuccessCount, cacheStats.loadExceptionCount + this.loadExceptionCount, cacheStats.totalLoadTime + this.totalLoadTime, this.evictionCount + cacheStats.evictionCount);
    }

    public int hashCode() {
        return Objects.hashCode(Long.valueOf(this.hitCount), Long.valueOf(this.missCount), Long.valueOf(this.loadSuccessCount), Long.valueOf(this.loadExceptionCount), Long.valueOf(this.totalLoadTime), Long.valueOf(this.evictionCount));
    }

    public boolean equals(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof CacheStats)) {
            return false;
        }
        CacheStats other = (CacheStats) object;
        if (this.hitCount == other.hitCount && this.missCount == other.missCount && this.loadSuccessCount == other.loadSuccessCount && this.loadExceptionCount == other.loadExceptionCount && this.totalLoadTime == other.totalLoadTime && this.evictionCount == other.evictionCount) {
            z = true;
        }
        return z;
    }

    public String toString() {
        return MoreObjects.toStringHelper((Object) this).add("hitCount", this.hitCount).add("missCount", this.missCount).add("loadSuccessCount", this.loadSuccessCount).add("loadExceptionCount", this.loadExceptionCount).add("totalLoadTime", this.totalLoadTime).add("evictionCount", this.evictionCount).toString();
    }
}
