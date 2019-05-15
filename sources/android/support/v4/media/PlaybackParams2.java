package android.support.v4.media;

import android.media.PlaybackParams;
import android.os.Build.VERSION;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PlaybackParams2 {
    public static final int AUDIO_FALLBACK_MODE_DEFAULT = 0;
    public static final int AUDIO_FALLBACK_MODE_FAIL = 2;
    public static final int AUDIO_FALLBACK_MODE_MUTE = 1;
    private Integer mAudioFallbackMode;
    private Float mPitch;
    private PlaybackParams mPlaybackParams;
    private Float mSpeed;

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AudioFallbackMode {
    }

    public static final class Builder {
        private Integer mAudioFallbackMode;
        private Float mPitch;
        private PlaybackParams mPlaybackParams;
        private Float mSpeed;

        public Builder() {
            if (VERSION.SDK_INT >= 23) {
                this.mPlaybackParams = new PlaybackParams();
            }
        }

        @RequiresApi(23)
        @RestrictTo({Scope.LIBRARY_GROUP})
        public Builder(PlaybackParams playbackParams) {
            this.mPlaybackParams = playbackParams;
        }

        public Builder setAudioFallbackMode(int audioFallbackMode) {
            if (VERSION.SDK_INT >= 23) {
                this.mPlaybackParams.setAudioFallbackMode(audioFallbackMode);
            } else {
                this.mAudioFallbackMode = Integer.valueOf(audioFallbackMode);
            }
            return this;
        }

        public Builder setPitch(float pitch) {
            if (pitch >= 0.0f) {
                if (VERSION.SDK_INT >= 23) {
                    this.mPlaybackParams.setPitch(pitch);
                } else {
                    this.mPitch = Float.valueOf(pitch);
                }
                return this;
            }
            throw new IllegalArgumentException("pitch must not be negative");
        }

        public Builder setSpeed(float speed) {
            if (VERSION.SDK_INT >= 23) {
                this.mPlaybackParams.setSpeed(speed);
            } else {
                this.mSpeed = Float.valueOf(speed);
            }
            return this;
        }

        public PlaybackParams2 build() {
            if (VERSION.SDK_INT >= 23) {
                return new PlaybackParams2(this.mPlaybackParams);
            }
            return new PlaybackParams2(this.mAudioFallbackMode, this.mPitch, this.mSpeed);
        }
    }

    private PlaybackParams2(Integer audioFallbackMode, Float pitch, Float speed) {
        this.mAudioFallbackMode = audioFallbackMode;
        this.mPitch = pitch;
        this.mSpeed = speed;
    }

    @RequiresApi(23)
    private PlaybackParams2(PlaybackParams playbackParams) {
        this.mPlaybackParams = playbackParams;
    }

    public Integer getAudioFallbackMode() {
        if (VERSION.SDK_INT < 23) {
            return this.mAudioFallbackMode;
        }
        try {
            return Integer.valueOf(this.mPlaybackParams.getAudioFallbackMode());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public Float getPitch() {
        if (VERSION.SDK_INT < 23) {
            return this.mPitch;
        }
        try {
            return Float.valueOf(this.mPlaybackParams.getPitch());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    public Float getSpeed() {
        if (VERSION.SDK_INT < 23) {
            return this.mPitch;
        }
        try {
            return Float.valueOf(this.mPlaybackParams.getSpeed());
        } catch (IllegalStateException e) {
            return null;
        }
    }

    @RequiresApi(23)
    @RestrictTo({Scope.LIBRARY_GROUP})
    public PlaybackParams getPlaybackParams() {
        if (VERSION.SDK_INT >= 23) {
            return this.mPlaybackParams;
        }
        return null;
    }
}
