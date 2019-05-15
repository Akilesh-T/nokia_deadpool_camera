package com.android.ex.camera2.utils;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureRequest.Key;
import android.view.Surface;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Camera2RequestSettingsSet {
    private final Map<Key<?>, Object> mDictionary;
    private long mRevision;

    public Camera2RequestSettingsSet() {
        this.mDictionary = new HashMap();
        this.mRevision = 0;
    }

    public Camera2RequestSettingsSet(Camera2RequestSettingsSet other) {
        if (other != null) {
            this.mDictionary = new HashMap(other.mDictionary);
            this.mRevision = other.mRevision;
            return;
        }
        throw new NullPointerException("Tried to copy null Camera2RequestSettingsSet");
    }

    public <T> boolean set(Key<T> key, T value) {
        if (key != null) {
            Object currentValue = get(key);
            if (this.mDictionary.containsKey(key) && Objects.equals(value, currentValue)) {
                return false;
            }
            this.mDictionary.put(key, value);
            this.mRevision++;
            return true;
        }
        throw new NullPointerException("Received a null key");
    }

    public boolean unset(Key<?> key) {
        if (key == null) {
            throw new NullPointerException("Received a null key");
        } else if (!this.mDictionary.containsKey(key)) {
            return false;
        } else {
            this.mDictionary.remove(key);
            this.mRevision++;
            return true;
        }
    }

    public <T> T get(Key<T> key) {
        if (key != null) {
            return this.mDictionary.get(key);
        }
        throw new NullPointerException("Received a null key");
    }

    public boolean contains(Key<?> key) {
        if (key != null) {
            return this.mDictionary.containsKey(key);
        }
        throw new NullPointerException("Received a null key");
    }

    public <T> boolean matches(Key<T> key, T value) {
        return Objects.equals(get(key), value);
    }

    public long getRevision() {
        return this.mRevision;
    }

    public boolean union(Camera2RequestSettingsSet moreSettings) {
        if (moreSettings == null || moreSettings == this) {
            return false;
        }
        this.mDictionary.putAll(moreSettings.mDictionary);
        this.mRevision++;
        return true;
    }

    public CaptureRequest createRequest(CameraDevice camera, int template, Surface... targets) throws CameraAccessException {
        if (camera != null) {
            Builder reqBuilder = camera.createCaptureRequest(template);
            for (Key<?> key : this.mDictionary.keySet()) {
                setRequestFieldIfNonNull(reqBuilder, key);
            }
            int length = targets.length;
            int i = 0;
            while (i < length) {
                Surface target = targets[i];
                if (target != null) {
                    reqBuilder.addTarget(target);
                    i++;
                } else {
                    throw new NullPointerException("Tried to add null Surface as request target");
                }
            }
            return reqBuilder.build();
        }
        throw new NullPointerException("Tried to create request using null CameraDevice");
    }

    private <T> void setRequestFieldIfNonNull(Builder requestBuilder, Key<T> key) {
        T value = get(key);
        if (value != null) {
            requestBuilder.set(key, value);
        }
    }
}
