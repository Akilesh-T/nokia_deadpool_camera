package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@GwtCompatible(emulated = true)
@Deprecated
@Beta
abstract class GenericMapMaker<K0, V0> {
    @GwtIncompatible("To be supported")
    RemovalListener<K0, V0> removalListener;

    @GwtIncompatible("To be supported")
    enum NullListener implements RemovalListener<Object, Object> {
        INSTANCE;

        public void onRemoval(RemovalNotification<Object, Object> removalNotification) {
        }
    }

    public abstract GenericMapMaker<K0, V0> concurrencyLevel(int i);

    @GwtIncompatible("To be supported")
    public abstract GenericMapMaker<K0, V0> expireAfterAccess(long j, TimeUnit timeUnit);

    public abstract GenericMapMaker<K0, V0> expireAfterWrite(long j, TimeUnit timeUnit);

    public abstract GenericMapMaker<K0, V0> initialCapacity(int i);

    @GwtIncompatible("To be supported")
    public abstract GenericMapMaker<K0, V0> keyEquivalence(Equivalence<Object> equivalence);

    @Deprecated
    public abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeComputingMap(Function<? super K, ? extends V> function);

    @GwtIncompatible("MapMakerInternalMap")
    public abstract <K, V> MapMakerInternalMap<K, V> makeCustomMap();

    public abstract <K extends K0, V extends V0> ConcurrentMap<K, V> makeMap();

    public abstract GenericMapMaker<K0, V0> maximumSize(int i);

    @GwtIncompatible("java.lang.ref.SoftReference")
    @Deprecated
    public abstract GenericMapMaker<K0, V0> softValues();

    @GwtIncompatible("java.lang.ref.WeakReference")
    public abstract GenericMapMaker<K0, V0> weakKeys();

    @GwtIncompatible("java.lang.ref.WeakReference")
    public abstract GenericMapMaker<K0, V0> weakValues();

    GenericMapMaker() {
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("To be supported")
    public <K extends K0, V extends V0> RemovalListener<K, V> getRemovalListener() {
        return (RemovalListener) MoreObjects.firstNonNull(this.removalListener, NullListener.INSTANCE);
    }
}
