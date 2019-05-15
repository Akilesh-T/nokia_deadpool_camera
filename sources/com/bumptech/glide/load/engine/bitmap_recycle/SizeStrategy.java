package com.bumptech.glide.load.engine.bitmap_recycle;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import com.bumptech.glide.util.Util;
import java.util.Map.Entry;
import java.util.TreeMap;

@TargetApi(19)
class SizeStrategy implements LruPoolStrategy {
    private static final int MAX_SIZE_MULTIPLE = 8;
    private final GroupedLinkedMap<Key, Bitmap> groupedMap = new GroupedLinkedMap();
    private final KeyPool keyPool = new KeyPool();
    private final TreeMap<Integer, Integer> sortedSizes = new PrettyPrintTreeMap();

    private static class PrettyPrintTreeMap<K, V> extends TreeMap<K, V> {
        private PrettyPrintTreeMap() {
        }

        public String toString() {
            String result;
            StringBuilder sb = new StringBuilder();
            sb.append("( ");
            for (Entry<K, V> entry : entrySet()) {
                sb.append('{');
                sb.append(entry.getKey());
                sb.append(':');
                sb.append(entry.getValue());
                sb.append("}, ");
            }
            if (isEmpty()) {
                result = sb.toString();
            } else {
                result = sb.substring(null, sb.length() - 2);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append(" )");
            return stringBuilder.toString();
        }
    }

    static final class Key implements Poolable {
        private final KeyPool pool;
        private int size;

        Key(KeyPool pool) {
            this.pool = pool;
        }

        public void init(int size) {
            this.size = size;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof Key)) {
                return false;
            }
            if (this.size == ((Key) o).size) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.size;
        }

        public String toString() {
            return SizeStrategy.getBitmapString(this.size);
        }

        public void offer() {
            this.pool.offer(this);
        }
    }

    static class KeyPool extends BaseKeyPool<Key> {
        KeyPool() {
        }

        public Key get(int size) {
            Key result = (Key) get();
            result.init(size);
            return result;
        }

        /* Access modifiers changed, original: protected */
        public Key create() {
            return new Key(this);
        }
    }

    SizeStrategy() {
    }

    public void put(Bitmap bitmap) {
        Key key = this.keyPool.get(Util.getBitmapByteSize(bitmap));
        this.groupedMap.put(key, bitmap);
        Integer current = (Integer) this.sortedSizes.get(Integer.valueOf(key.size));
        TreeMap treeMap = this.sortedSizes;
        Integer valueOf = Integer.valueOf(key.size);
        int i = 1;
        if (current != null) {
            i = 1 + current.intValue();
        }
        treeMap.put(valueOf, Integer.valueOf(i));
    }

    public Bitmap get(int width, int height, Config config) {
        int size = Util.getBitmapByteSize(width, height, config);
        Key key = this.keyPool.get(size);
        Integer possibleSize = (Integer) this.sortedSizes.ceilingKey(Integer.valueOf(size));
        if (!(possibleSize == null || possibleSize.intValue() == size || possibleSize.intValue() > size * 8)) {
            this.keyPool.offer(key);
            key = this.keyPool.get(possibleSize.intValue());
        }
        Bitmap result = (Bitmap) this.groupedMap.get(key);
        if (result != null) {
            result.reconfigure(width, height, config);
            decrementBitmapOfSize(possibleSize);
        }
        return result;
    }

    public Bitmap removeLast() {
        Bitmap removed = (Bitmap) this.groupedMap.removeLast();
        if (removed != null) {
            decrementBitmapOfSize(Integer.valueOf(Util.getBitmapByteSize(removed)));
        }
        return removed;
    }

    private void decrementBitmapOfSize(Integer size) {
        Integer current = (Integer) this.sortedSizes.get(size);
        if (current.intValue() == 1) {
            this.sortedSizes.remove(size);
        } else {
            this.sortedSizes.put(size, Integer.valueOf(current.intValue() - 1));
        }
    }

    public String logBitmap(Bitmap bitmap) {
        return getBitmapString(bitmap);
    }

    public String logBitmap(int width, int height, Config config) {
        return getBitmapString(Util.getBitmapByteSize(width, height, config));
    }

    public int getSize(Bitmap bitmap) {
        return Util.getBitmapByteSize(bitmap);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SizeStrategy:\n  ");
        stringBuilder.append(this.groupedMap);
        stringBuilder.append("\n  SortedSizes");
        stringBuilder.append(this.sortedSizes);
        return stringBuilder.toString();
    }

    private static String getBitmapString(Bitmap bitmap) {
        return getBitmapString(Util.getBitmapByteSize(bitmap));
    }

    private static String getBitmapString(int size) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(size);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
