package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.util.LruCache;
import com.bumptech.glide.util.Util;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class SafeKeyGenerator {
    private final LruCache<Key, String> loadIdToSafeHash = new LruCache(1000);

    SafeKeyGenerator() {
    }

    public String getSafeKey(Key key) {
        String safeKey;
        synchronized (this.loadIdToSafeHash) {
            safeKey = (String) this.loadIdToSafeHash.get(key);
        }
        if (safeKey != null) {
            return safeKey;
        }
        String safeKey2;
        try {
            safeKey2 = MessageDigest.getInstance("SHA-256");
            key.updateDiskCacheKey(safeKey2);
            safeKey = Util.sha256BytesToHex(safeKey2.digest());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
        }
        safeKey2 = safeKey;
        synchronized (this.loadIdToSafeHash) {
            this.loadIdToSafeHash.put(key, safeKey2);
        }
        return safeKey2;
    }
}
