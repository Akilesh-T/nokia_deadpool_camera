package com.bumptech.glide.request.animation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class DrawableCrossFadeFactory<T extends Drawable> implements GlideAnimationFactory<T> {
    private static final int DEFAULT_DURATION_MS = 300;
    private DrawableCrossFadeViewAnimation<T> animation;
    private final ViewAnimationFactory<T> animationFactory;
    private final int duration;

    private static class DefaultAnimationFactory implements AnimationFactory {
        private DefaultAnimationFactory() {
        }

        public Animation build() {
            AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(150);
            return animation;
        }
    }

    public DrawableCrossFadeFactory() {
        this(300);
    }

    public DrawableCrossFadeFactory(int duration) {
        this(new ViewAnimationFactory(new DefaultAnimationFactory()), duration);
    }

    public DrawableCrossFadeFactory(Context context, int defaultAnimationId, int duration) {
        this(new ViewAnimationFactory(context, defaultAnimationId), duration);
    }

    public DrawableCrossFadeFactory(Animation defaultAnimation, int duration) {
        this(new ViewAnimationFactory(defaultAnimation), duration);
    }

    DrawableCrossFadeFactory(ViewAnimationFactory<T> animationFactory, int duration) {
        this.animationFactory = animationFactory;
        this.duration = duration;
    }

    public GlideAnimation<T> build(boolean isFromMemoryCache, boolean isFirstResource) {
        if (isFromMemoryCache) {
            return NoAnimation.get();
        }
        if (this.animation == null) {
            this.animation = new DrawableCrossFadeViewAnimation(this.animationFactory.build(false, isFirstResource), this.duration);
        }
        return this.animation;
    }
}
