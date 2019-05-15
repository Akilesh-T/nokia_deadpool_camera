package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import javax.annotation.Nullable;

@GwtIncompatible("unnecessary")
abstract class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {

    static final class TerminalEntry<K, V> extends ImmutableMapEntry<K, V> {
        TerminalEntry(ImmutableMapEntry<K, V> contents) {
            super(contents);
        }

        TerminalEntry(K key, V value) {
            super(key, value);
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public ImmutableMapEntry<K, V> getNextInValueBucket() {
            return null;
        }
    }

    @Nullable
    public abstract ImmutableMapEntry<K, V> getNextInKeyBucket();

    @Nullable
    public abstract ImmutableMapEntry<K, V> getNextInValueBucket();

    ImmutableMapEntry(K key, V value) {
        super(key, value);
        CollectPreconditions.checkEntryNotNull(key, value);
    }

    ImmutableMapEntry(ImmutableMapEntry<K, V> contents) {
        super(contents.getKey(), contents.getValue());
    }
}
