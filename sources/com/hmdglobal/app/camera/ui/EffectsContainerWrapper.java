package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.MultiToggleImageButton;
import com.hmdglobal.app.camera.R;

public class EffectsContainerWrapper extends LinearLayout {
    private MultiToggleImageButton mBeautyToggleButton;
    private MultiToggleImageButton mBolkenToggleButton;
    private FrameLayout mEffectOperatorContainer;
    private MultiToggleImageButton mEffectToggleButton;
    private LinearLayout mEffectsToggleButtonLayout;

    public EffectsContainerWrapper(Context context) {
        this(context, null);
    }

    public EffectsContainerWrapper(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EffectsContainerWrapper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void init() {
        this.mEffectOperatorContainer = (FrameLayout) findViewById(R.id.effect_operator_layout);
        this.mEffectsToggleButtonLayout = (LinearLayout) findViewById(R.id.effects_toggle_button_layout);
        this.mEffectToggleButton = (MultiToggleImageButton) this.mEffectsToggleButtonLayout.findViewById(R.id.effect_toggle_button);
        this.mBeautyToggleButton = (MultiToggleImageButton) this.mEffectsToggleButtonLayout.findViewById(R.id.beauty_toggle_button);
        this.mBolkenToggleButton = (MultiToggleImageButton) this.mEffectsToggleButtonLayout.findViewById(R.id.live_bolken_toggle_state_button);
        this.mEffectsToggleButtonLayout.setVisibility(0);
        this.mEffectToggleButton.setState(1);
        this.mBeautyToggleButton.setState(0);
        this.mBolkenToggleButton.setState(0);
    }
}
