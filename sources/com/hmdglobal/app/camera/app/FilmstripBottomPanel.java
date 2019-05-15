package com.hmdglobal.app.camera.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomPanel;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomPanel.Listener;
import com.hmdglobal.app.camera.util.Gusterpolator;
import com.hmdglobal.app.camera.widget.Cling;
import com.hmdglobal.app.camera.widget.ExternalViewerButton;

class FilmstripBottomPanel implements BottomPanel {
    private static final int ANIM_DURATION = 150;
    private final View mControlLayout = this.mLayout.findViewById(R.id.bottom_control_panel);
    private final AppController mController;
    private ImageButton mDeleteButton;
    private ImageButton mEditButton;
    private final ViewGroup mLayout;
    private Listener mListener;
    private final View mMiddleFiller = this.mLayout.findViewById(R.id.filmstrip_bottom_control_middle_filler);
    private ProgressBar mProgressBar;
    private View mProgressErrorLayout;
    private TextView mProgressErrorText;
    private View mProgressLayout;
    private TextView mProgressText;
    private ImageButton mShareButton;
    private boolean mTinyPlanetEnabled;
    private ExternalViewerButton mViewButton;

    public FilmstripBottomPanel(AppController controller, ViewGroup bottomControlsLayout) {
        this.mController = controller;
        this.mLayout = bottomControlsLayout;
        setupEditButton();
        setupViewButton();
        setupDeleteButton();
        setupShareButton();
        setupProgressUi();
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setClingForViewer(int viewerType, Cling cling) {
        this.mViewButton.setClingForViewer(viewerType, cling);
    }

    public void clearClingForViewer(int viewerType) {
        this.mViewButton.clearClingForViewer(viewerType);
    }

    public Cling getClingForViewer(int viewerType) {
        return this.mViewButton.getClingForViewer(viewerType);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            this.mLayout.setVisibility(0);
        } else {
            this.mLayout.setVisibility(4);
        }
    }

    public void setEditButtonVisibility(boolean visible) {
        this.mEditButton.setVisibility(visible ? 0 : 8);
        updateMiddleFillerLayoutVisibility();
    }

    public void setEditEnabled(boolean enabled) {
        this.mEditButton.setEnabled(enabled);
    }

    public void setViewerButtonVisibility(int state) {
        this.mViewButton.setState(state);
        updateMiddleFillerLayoutVisibility();
    }

    public void setViewEnabled(boolean enabled) {
        this.mViewButton.setEnabled(enabled);
    }

    public void setTinyPlanetEnabled(boolean enabled) {
        this.mTinyPlanetEnabled = enabled;
    }

    public void setDeleteButtonVisibility(boolean visible) {
        this.mDeleteButton.setVisibility(visible ? 0 : 4);
    }

    public void setDeleteEnabled(boolean enabled) {
        this.mDeleteButton.setEnabled(enabled);
    }

    public void setShareButtonVisibility(boolean visible) {
        this.mShareButton.setVisibility(visible ? 0 : 4);
    }

    public void setShareEnabled(boolean enabled) {
        this.mShareButton.setEnabled(enabled);
    }

    public void setProgressText(CharSequence text) {
        this.mProgressText.setText(text);
    }

    public void setProgress(int progress) {
        this.mProgressBar.setProgress(progress);
    }

    public void showProgressError(CharSequence message) {
        hideControls();
        hideProgress();
        this.mProgressErrorLayout.setVisibility(0);
        this.mProgressErrorText.setText(message);
    }

    public void hideProgressError() {
        this.mProgressErrorLayout.setVisibility(4);
    }

    public void showProgress() {
        this.mProgressLayout.setVisibility(0);
        hideProgressError();
    }

    public void hideProgress() {
        this.mProgressLayout.setVisibility(4);
    }

    public void showControls() {
        this.mControlLayout.setVisibility(0);
    }

    public void hideControls() {
        this.mControlLayout.setVisibility(4);
    }

    private void setupEditButton() {
        this.mEditButton = (ImageButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_edit);
        this.mEditButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mTinyPlanetEnabled) {
                    FilmstripBottomPanel.this.mController.openContextMenu(FilmstripBottomPanel.this.mEditButton);
                } else if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onEdit();
                }
            }
        });
        this.mController.registerForContextMenu(this.mEditButton);
        this.mEditButton.setLongClickable(false);
    }

    private void setupViewButton() {
        this.mViewButton = (ExternalViewerButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_view);
        this.mViewButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onExternalViewer();
                }
            }
        });
    }

    private void setupDeleteButton() {
        this.mDeleteButton = (ImageButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_delete);
        this.mDeleteButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onDelete();
                }
            }
        });
    }

    private void setupShareButton() {
        this.mShareButton = (ImageButton) this.mLayout.findViewById(R.id.filmstrip_bottom_control_share);
        this.mShareButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onShare();
                }
            }
        });
    }

    private void setupProgressUi() {
        this.mProgressLayout = this.mLayout.findViewById(R.id.bottom_progress_panel);
        this.mProgressText = (TextView) this.mLayout.findViewById(R.id.bottom_session_progress_text);
        this.mProgressBar = (ProgressBar) this.mLayout.findViewById(R.id.bottom_session_progress_bar);
        this.mProgressBar.setMax(100);
        this.mProgressLayout.setVisibility(4);
        this.mProgressErrorText = (TextView) this.mLayout.findViewById(R.id.bottom_progress_error_text);
        this.mProgressErrorLayout = this.mLayout.findViewById(R.id.bottom_progress_error_panel);
        this.mProgressErrorLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (FilmstripBottomPanel.this.mListener != null) {
                    FilmstripBottomPanel.this.mListener.onProgressErrorClicked();
                }
            }
        });
    }

    private void updateMiddleFillerLayoutVisibility() {
        if (this.mEditButton.getVisibility() == 0 && this.mViewButton.getVisibility() == 0) {
            this.mMiddleFiller.setVisibility(4);
        } else {
            this.mMiddleFiller.setVisibility(8);
        }
    }

    public void show() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this.mLayout, "translationY", new float[]{(float) this.mLayout.getHeight(), 0.0f});
        animator.setDuration(150);
        animator.setInterpolator(Gusterpolator.INSTANCE);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                FilmstripBottomPanel.this.mViewButton.updateClingVisibility();
            }
        });
        this.mViewButton.hideClings();
        animator.start();
    }

    public void hide() {
        if (this.mLayout.getTranslationY() < ((float) this.mLayout.getHeight())) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this.mLayout, "translationY", new float[]{this.mLayout.getTranslationY(), (float) this.mLayout.getHeight()});
            animator.setDuration(150);
            animator.setInterpolator(Gusterpolator.INSTANCE);
            this.mViewButton.hideClings();
            animator.start();
        }
    }
}
