package android.support.v4.media.subtitle;

import android.content.Context;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.media.subtitle.SubtitleTrack.RenderingWidget;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptioningChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

@RequiresApi(28)
@RestrictTo({Scope.LIBRARY_GROUP})
public class SubtitleController {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int WHAT_HIDE = 2;
    private static final int WHAT_SELECT_DEFAULT_TRACK = 4;
    private static final int WHAT_SELECT_TRACK = 3;
    private static final int WHAT_SHOW = 1;
    private Anchor mAnchor;
    private final Callback mCallback;
    private CaptioningChangeListener mCaptioningChangeListener;
    private CaptioningManager mCaptioningManager;
    private Handler mHandler;
    private Listener mListener;
    private ArrayList<Renderer> mRenderers;
    private final Object mRenderersLock;
    private SubtitleTrack mSelectedTrack;
    private boolean mShowing;
    private MediaTimeProvider mTimeProvider;
    private boolean mTrackIsExplicit;
    private ArrayList<SubtitleTrack> mTracks;
    private final Object mTracksLock;
    private boolean mVisibilityIsExplicit;

    public interface Anchor {
        Looper getSubtitleLooper();

        void setSubtitleWidget(RenderingWidget renderingWidget);
    }

    interface Listener {
        void onSubtitleTrackSelected(SubtitleTrack subtitleTrack);
    }

    static class MediaFormatUtil {
        MediaFormatUtil() {
        }

        static int getInteger(MediaFormat format, String name, int defaultValue) {
            try {
                return format.getInteger(name);
            } catch (ClassCastException | NullPointerException e) {
                return defaultValue;
            }
        }
    }

    public static abstract class Renderer {
        public abstract SubtitleTrack createTrack(MediaFormat mediaFormat);

        public abstract boolean supports(MediaFormat mediaFormat);
    }

    public SubtitleController(Context context) {
        this(context, null, null);
    }

    public SubtitleController(Context context, MediaTimeProvider timeProvider, Listener listener) {
        this.mRenderersLock = new Object();
        this.mTracksLock = new Object();
        this.mCallback = new Callback() {
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        SubtitleController.this.doShow();
                        return true;
                    case 2:
                        SubtitleController.this.doHide();
                        return true;
                    case 3:
                        SubtitleController.this.doSelectTrack((SubtitleTrack) msg.obj);
                        return true;
                    case 4:
                        SubtitleController.this.doSelectDefaultTrack();
                        return true;
                    default:
                        return false;
                }
            }
        };
        this.mCaptioningChangeListener = new CaptioningChangeListener() {
            public void onEnabledChanged(boolean enabled) {
                SubtitleController.this.selectDefaultTrack();
            }

            public void onLocaleChanged(Locale locale) {
                SubtitleController.this.selectDefaultTrack();
            }
        };
        this.mTrackIsExplicit = false;
        this.mVisibilityIsExplicit = false;
        this.mTimeProvider = timeProvider;
        this.mListener = listener;
        this.mRenderers = new ArrayList();
        this.mShowing = false;
        this.mTracks = new ArrayList();
        this.mCaptioningManager = (CaptioningManager) context.getSystemService("captioning");
    }

    /* Access modifiers changed, original: protected */
    public void finalize() throws Throwable {
        this.mCaptioningManager.removeCaptioningChangeListener(this.mCaptioningChangeListener);
        super.finalize();
    }

    public SubtitleTrack[] getTracks() {
        SubtitleTrack[] tracks;
        synchronized (this.mTracksLock) {
            tracks = new SubtitleTrack[this.mTracks.size()];
            this.mTracks.toArray(tracks);
        }
        return tracks;
    }

    public SubtitleTrack getSelectedTrack() {
        return this.mSelectedTrack;
    }

    private RenderingWidget getRenderingWidget() {
        if (this.mSelectedTrack == null) {
            return null;
        }
        return this.mSelectedTrack.getRenderingWidget();
    }

    public boolean selectTrack(SubtitleTrack track) {
        if (track != null && !this.mTracks.contains(track)) {
            return false;
        }
        processOnAnchor(this.mHandler.obtainMessage(3, track));
        return true;
    }

    private void doSelectTrack(SubtitleTrack track) {
        this.mTrackIsExplicit = true;
        if (this.mSelectedTrack != track) {
            if (this.mSelectedTrack != null) {
                this.mSelectedTrack.hide();
                this.mSelectedTrack.setTimeProvider(null);
            }
            this.mSelectedTrack = track;
            if (this.mAnchor != null) {
                this.mAnchor.setSubtitleWidget(getRenderingWidget());
            }
            if (this.mSelectedTrack != null) {
                this.mSelectedTrack.setTimeProvider(this.mTimeProvider);
                this.mSelectedTrack.show();
            }
            if (this.mListener != null) {
                this.mListener.onSubtitleTrackSelected(track);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:33:0x0086  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x0098  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0095  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00a1  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x009e  */
    public android.support.v4.media.subtitle.SubtitleTrack getDefaultTrack() {
        /*
        r18 = this;
        r1 = r18;
        r2 = 0;
        r3 = -1;
        r0 = r1.mCaptioningManager;
        r4 = r0.getLocale();
        r0 = r4;
        if (r0 != 0) goto L_0x0011;
    L_0x000d:
        r0 = java.util.Locale.getDefault();
    L_0x0011:
        r5 = r0;
        r0 = r1.mCaptioningManager;
        r0 = r0.isEnabled();
        r6 = 1;
        r0 = r0 ^ r6;
        r7 = r0;
        r8 = r1.mTracksLock;
        monitor-enter(r8);
        r0 = r1.mTracks;	 Catch:{ all -> 0x00c0 }
        r0 = r0.iterator();	 Catch:{ all -> 0x00c0 }
    L_0x0024:
        r9 = r0.hasNext();	 Catch:{ all -> 0x00c0 }
        if (r9 == 0) goto L_0x00be;
    L_0x002a:
        r9 = r0.next();	 Catch:{ all -> 0x00c0 }
        r9 = (android.support.v4.media.subtitle.SubtitleTrack) r9;	 Catch:{ all -> 0x00c0 }
        r10 = r9.getFormat();	 Catch:{ all -> 0x00c0 }
        r11 = "language";
        r11 = r10.getString(r11);	 Catch:{ all -> 0x00c0 }
        r12 = "is-forced-subtitle";
        r13 = 0;
        r12 = android.support.v4.media.subtitle.SubtitleController.MediaFormatUtil.getInteger(r10, r12, r13);	 Catch:{ all -> 0x00c0 }
        if (r12 == 0) goto L_0x0045;
    L_0x0043:
        r12 = r6;
        goto L_0x0046;
    L_0x0045:
        r12 = r13;
    L_0x0046:
        r14 = "is-autoselect";
        r14 = android.support.v4.media.subtitle.SubtitleController.MediaFormatUtil.getInteger(r10, r14, r6);	 Catch:{ all -> 0x00c0 }
        if (r14 == 0) goto L_0x0050;
    L_0x004e:
        r14 = r6;
        goto L_0x0051;
    L_0x0050:
        r14 = r13;
    L_0x0051:
        r15 = "is-default";
        r15 = android.support.v4.media.subtitle.SubtitleController.MediaFormatUtil.getInteger(r10, r15, r13);	 Catch:{ all -> 0x00c0 }
        if (r15 == 0) goto L_0x005b;
    L_0x0059:
        r15 = r6;
        goto L_0x005c;
    L_0x005b:
        r15 = r13;
    L_0x005c:
        if (r5 == 0) goto L_0x0081;
    L_0x005e:
        r6 = r5.getLanguage();	 Catch:{ all -> 0x00c0 }
        r13 = "";
        r6 = r6.equals(r13);	 Catch:{ all -> 0x00c0 }
        if (r6 != 0) goto L_0x0081;
    L_0x006a:
        r6 = r5.getISO3Language();	 Catch:{ all -> 0x00c0 }
        r6 = r6.equals(r11);	 Catch:{ all -> 0x00c0 }
        if (r6 != 0) goto L_0x0081;
    L_0x0074:
        r6 = r5.getLanguage();	 Catch:{ all -> 0x00c0 }
        r6 = r6.equals(r11);	 Catch:{ all -> 0x00c0 }
        if (r6 == 0) goto L_0x007f;
    L_0x007e:
        goto L_0x0081;
    L_0x007f:
        r6 = 0;
        goto L_0x0082;
    L_0x0081:
        r6 = 1;
    L_0x0082:
        if (r12 == 0) goto L_0x0086;
    L_0x0084:
        r13 = 0;
        goto L_0x0088;
    L_0x0086:
        r13 = 8;
    L_0x0088:
        if (r4 != 0) goto L_0x008f;
    L_0x008a:
        if (r15 == 0) goto L_0x008f;
    L_0x008c:
        r17 = 4;
        goto L_0x0091;
    L_0x008f:
        r17 = 0;
    L_0x0091:
        r13 = r13 + r17;
        if (r14 == 0) goto L_0x0098;
    L_0x0095:
        r17 = 0;
        goto L_0x009a;
    L_0x0098:
        r17 = 2;
    L_0x009a:
        r13 = r13 + r17;
        if (r6 == 0) goto L_0x00a1;
    L_0x009e:
        r16 = 1;
        goto L_0x00a3;
    L_0x00a1:
        r16 = 0;
    L_0x00a3:
        r13 = r13 + r16;
        if (r7 == 0) goto L_0x00ad;
    L_0x00a7:
        if (r12 != 0) goto L_0x00ad;
    L_0x00aa:
        r6 = 1;
        goto L_0x0024;
    L_0x00ad:
        if (r4 != 0) goto L_0x00b1;
    L_0x00af:
        if (r15 != 0) goto L_0x00b9;
    L_0x00b1:
        if (r6 == 0) goto L_0x00bd;
    L_0x00b3:
        if (r14 != 0) goto L_0x00b9;
    L_0x00b5:
        if (r12 != 0) goto L_0x00b9;
    L_0x00b7:
        if (r4 == 0) goto L_0x00bd;
    L_0x00b9:
        if (r13 <= r3) goto L_0x00bd;
    L_0x00bb:
        r3 = r13;
        r2 = r9;
    L_0x00bd:
        goto L_0x00aa;
    L_0x00be:
        monitor-exit(r8);	 Catch:{ all -> 0x00c0 }
        return r2;
    L_0x00c0:
        r0 = move-exception;
        monitor-exit(r8);	 Catch:{ all -> 0x00c0 }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.subtitle.SubtitleController.getDefaultTrack():android.support.v4.media.subtitle.SubtitleTrack");
    }

    public void selectDefaultTrack() {
        processOnAnchor(this.mHandler.obtainMessage(4));
    }

    private void doSelectDefaultTrack() {
        if (this.mTrackIsExplicit) {
            if (!this.mVisibilityIsExplicit) {
                if (this.mCaptioningManager.isEnabled() || !(this.mSelectedTrack == null || MediaFormatUtil.getInteger(this.mSelectedTrack.getFormat(), "is-forced-subtitle", 0) == 0)) {
                    show();
                } else if (this.mSelectedTrack != null && this.mSelectedTrack.getTrackType() == 4) {
                    hide();
                }
                this.mVisibilityIsExplicit = false;
            } else {
                return;
            }
        }
        SubtitleTrack track = getDefaultTrack();
        if (track != null) {
            selectTrack(track);
            this.mTrackIsExplicit = false;
            if (!this.mVisibilityIsExplicit) {
                show();
                this.mVisibilityIsExplicit = false;
            }
        }
    }

    public void reset() {
        checkAnchorLooper();
        hide();
        selectTrack(null);
        this.mTracks.clear();
        this.mTrackIsExplicit = false;
        this.mVisibilityIsExplicit = false;
        this.mCaptioningManager.removeCaptioningChangeListener(this.mCaptioningChangeListener);
    }

    public SubtitleTrack addTrack(MediaFormat format) {
        synchronized (this.mRenderersLock) {
            Iterator it = this.mRenderers.iterator();
            while (it.hasNext()) {
                Renderer renderer = (Renderer) it.next();
                if (renderer.supports(format)) {
                    SubtitleTrack track = renderer.createTrack(format);
                    if (track != null) {
                        synchronized (this.mTracksLock) {
                            if (this.mTracks.size() == 0) {
                                this.mCaptioningManager.addCaptioningChangeListener(this.mCaptioningChangeListener);
                            }
                            this.mTracks.add(track);
                        }
                        return track;
                    }
                }
            }
            return null;
        }
    }

    public void show() {
        processOnAnchor(this.mHandler.obtainMessage(1));
    }

    private void doShow() {
        this.mShowing = true;
        this.mVisibilityIsExplicit = true;
        if (this.mSelectedTrack != null) {
            this.mSelectedTrack.show();
        }
    }

    public void hide() {
        processOnAnchor(this.mHandler.obtainMessage(2));
    }

    private void doHide() {
        this.mVisibilityIsExplicit = true;
        if (this.mSelectedTrack != null) {
            this.mSelectedTrack.hide();
        }
        this.mShowing = false;
    }

    public void registerRenderer(Renderer renderer) {
        synchronized (this.mRenderersLock) {
            if (!this.mRenderers.contains(renderer)) {
                this.mRenderers.add(renderer);
            }
        }
    }

    public boolean hasRendererFor(MediaFormat format) {
        synchronized (this.mRenderersLock) {
            Iterator it = this.mRenderers.iterator();
            while (it.hasNext()) {
                if (((Renderer) it.next()).supports(format)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setAnchor(Anchor anchor) {
        if (this.mAnchor != anchor) {
            if (this.mAnchor != null) {
                checkAnchorLooper();
                this.mAnchor.setSubtitleWidget(null);
            }
            this.mAnchor = anchor;
            this.mHandler = null;
            if (this.mAnchor != null) {
                this.mHandler = new Handler(this.mAnchor.getSubtitleLooper(), this.mCallback);
                checkAnchorLooper();
                this.mAnchor.setSubtitleWidget(getRenderingWidget());
            }
        }
    }

    private void checkAnchorLooper() {
    }

    private void processOnAnchor(Message m) {
        if (Looper.myLooper() == this.mHandler.getLooper()) {
            this.mHandler.dispatchMessage(m);
        } else {
            this.mHandler.sendMessage(m);
        }
    }
}
