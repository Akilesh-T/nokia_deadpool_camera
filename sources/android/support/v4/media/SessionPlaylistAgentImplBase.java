package android.support.v4.media;

import android.annotation.TargetApi;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.media.BaseMediaPlayer.PlayerEventCallback;
import android.support.v4.media.MediaSession2.OnDataSourceMissingHelper;
import android.support.v4.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TargetApi(19)
class SessionPlaylistAgentImplBase extends MediaPlaylistAgent {
    @VisibleForTesting
    static final int END_OF_PLAYLIST = -1;
    @VisibleForTesting
    static final int NO_VALID_ITEMS = -2;
    @GuardedBy("mLock")
    private PlayItem mCurrent;
    @GuardedBy("mLock")
    private OnDataSourceMissingHelper mDsmHelper;
    private final PlayItem mEopPlayItem = new PlayItem(-1, null);
    @GuardedBy("mLock")
    private Map<MediaItem2, DataSourceDesc> mItemDsdMap = new ArrayMap();
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaMetadata2 mMetadata;
    @GuardedBy("mLock")
    private BaseMediaPlayer mPlayer;
    private final MyPlayerEventCallback mPlayerCallback;
    @GuardedBy("mLock")
    private ArrayList<MediaItem2> mPlaylist = new ArrayList();
    @GuardedBy("mLock")
    private int mRepeatMode;
    private final MediaSession2ImplBase mSession;
    @GuardedBy("mLock")
    private int mShuffleMode;
    @GuardedBy("mLock")
    private ArrayList<MediaItem2> mShuffledList = new ArrayList();

    private class PlayItem {
        public DataSourceDesc dsd;
        public MediaItem2 mediaItem;
        public int shuffledIdx;

        PlayItem(SessionPlaylistAgentImplBase sessionPlaylistAgentImplBase, int shuffledIdx) {
            this(shuffledIdx, null);
        }

        PlayItem(int shuffledIdx, DataSourceDesc dsd) {
            this.shuffledIdx = shuffledIdx;
            if (shuffledIdx >= 0) {
                this.mediaItem = (MediaItem2) SessionPlaylistAgentImplBase.this.mShuffledList.get(shuffledIdx);
                if (dsd == null) {
                    synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                        this.dsd = SessionPlaylistAgentImplBase.this.retrieveDataSourceDescLocked(this.mediaItem);
                    }
                    return;
                }
                this.dsd = dsd;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isValid() {
            if (this == SessionPlaylistAgentImplBase.this.mEopPlayItem) {
                return true;
            }
            if (this.mediaItem == null || this.dsd == null) {
                return false;
            }
            if (this.mediaItem.getDataSourceDesc() != null && !this.mediaItem.getDataSourceDesc().equals(this.dsd)) {
                return false;
            }
            synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                if (this.shuffledIdx >= SessionPlaylistAgentImplBase.this.mShuffledList.size()) {
                    return false;
                } else if (this.mediaItem != SessionPlaylistAgentImplBase.this.mShuffledList.get(this.shuffledIdx)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    private class MyPlayerEventCallback extends PlayerEventCallback {
        private MyPlayerEventCallback() {
        }

        /* JADX WARNING: Missing block: B:12:0x0035, code skipped:
            return;
     */
        public void onCurrentDataSourceChanged(@android.support.annotation.NonNull android.support.v4.media.BaseMediaPlayer r6, @android.support.annotation.Nullable android.support.v4.media.DataSourceDesc r7) {
            /*
            r5 = this;
            r0 = android.support.v4.media.SessionPlaylistAgentImplBase.this;
            r0 = r0.mLock;
            monitor-enter(r0);
            r1 = android.support.v4.media.SessionPlaylistAgentImplBase.this;	 Catch:{ all -> 0x0036 }
            r1 = r1.mPlayer;	 Catch:{ all -> 0x0036 }
            if (r1 == r6) goto L_0x0011;
        L_0x000f:
            monitor-exit(r0);	 Catch:{ all -> 0x0036 }
            return;
        L_0x0011:
            if (r7 != 0) goto L_0x0034;
        L_0x0013:
            r1 = android.support.v4.media.SessionPlaylistAgentImplBase.this;	 Catch:{ all -> 0x0036 }
            r1 = r1.mCurrent;	 Catch:{ all -> 0x0036 }
            if (r1 == 0) goto L_0x0034;
        L_0x001b:
            r1 = android.support.v4.media.SessionPlaylistAgentImplBase.this;	 Catch:{ all -> 0x0036 }
            r2 = android.support.v4.media.SessionPlaylistAgentImplBase.this;	 Catch:{ all -> 0x0036 }
            r3 = android.support.v4.media.SessionPlaylistAgentImplBase.this;	 Catch:{ all -> 0x0036 }
            r3 = r3.mCurrent;	 Catch:{ all -> 0x0036 }
            r3 = r3.shuffledIdx;	 Catch:{ all -> 0x0036 }
            r4 = 1;
            r2 = r2.getNextValidPlayItemLocked(r3, r4);	 Catch:{ all -> 0x0036 }
            r1.mCurrent = r2;	 Catch:{ all -> 0x0036 }
            r1 = android.support.v4.media.SessionPlaylistAgentImplBase.this;	 Catch:{ all -> 0x0036 }
            r1.updateCurrentIfNeededLocked();	 Catch:{ all -> 0x0036 }
        L_0x0034:
            monitor-exit(r0);	 Catch:{ all -> 0x0036 }
            return;
        L_0x0036:
            r1 = move-exception;
            monitor-exit(r0);	 Catch:{ all -> 0x0036 }
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.SessionPlaylistAgentImplBase$MyPlayerEventCallback.onCurrentDataSourceChanged(android.support.v4.media.BaseMediaPlayer, android.support.v4.media.DataSourceDesc):void");
        }
    }

    SessionPlaylistAgentImplBase(@NonNull MediaSession2ImplBase session, @NonNull BaseMediaPlayer player) {
        if (session == null) {
            throw new IllegalArgumentException("sessionImpl shouldn't be null");
        } else if (player != null) {
            this.mSession = session;
            this.mPlayer = player;
            this.mPlayerCallback = new MyPlayerEventCallback();
            this.mPlayer.registerPlayerEventCallback(this.mSession.getCallbackExecutor(), this.mPlayerCallback);
        } else {
            throw new IllegalArgumentException("player shouldn't be null");
        }
    }

    public void setPlayer(@NonNull BaseMediaPlayer player) {
        if (player != null) {
            synchronized (this.mLock) {
                if (player == this.mPlayer) {
                    return;
                }
                this.mPlayer.unregisterPlayerEventCallback(this.mPlayerCallback);
                this.mPlayer = player;
                this.mPlayer.registerPlayerEventCallback(this.mSession.getCallbackExecutor(), this.mPlayerCallback);
                updatePlayerDataSourceLocked();
                return;
            }
        }
        throw new IllegalArgumentException("player shouldn't be null");
    }

    public void setOnDataSourceMissingHelper(OnDataSourceMissingHelper helper) {
        synchronized (this.mLock) {
            this.mDsmHelper = helper;
        }
    }

    public void clearOnDataSourceMissingHelper() {
        synchronized (this.mLock) {
            this.mDsmHelper = null;
        }
    }

    @Nullable
    public List<MediaItem2> getPlaylist() {
        List unmodifiableList;
        synchronized (this.mLock) {
            unmodifiableList = Collections.unmodifiableList(this.mPlaylist);
        }
        return unmodifiableList;
    }

    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        if (list != null) {
            synchronized (this.mLock) {
                this.mItemDsdMap.clear();
                this.mPlaylist.clear();
                this.mPlaylist.addAll(list);
                applyShuffleModeLocked();
                this.mMetadata = metadata;
                this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                updatePlayerDataSourceLocked();
            }
            notifyPlaylistChanged();
            return;
        }
        throw new IllegalArgumentException("list shouldn't be null");
    }

    @Nullable
    public MediaMetadata2 getPlaylistMetadata() {
        MediaMetadata2 mediaMetadata2;
        synchronized (this.mLock) {
            mediaMetadata2 = this.mMetadata;
        }
        return mediaMetadata2;
    }

    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (metadata == this.mMetadata) {
                return;
            }
            this.mMetadata = metadata;
            notifyPlaylistMetadataChanged();
        }
    }

    public MediaItem2 getCurrentMediaItem() {
        MediaItem2 mediaItem2;
        synchronized (this.mLock) {
            mediaItem2 = this.mCurrent == null ? null : this.mCurrent.mediaItem;
        }
        return mediaItem2;
    }

    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        if (item != null) {
            synchronized (this.mLock) {
                index = clamp(index, this.mPlaylist.size());
                this.mPlaylist.add(index, item);
                if (this.mShuffleMode == 0) {
                    this.mShuffledList.add(index, item);
                } else {
                    this.mShuffledList.add((int) (Math.random() * ((double) (this.mShuffledList.size() + 1))), item);
                }
                if (hasValidItem()) {
                    updateCurrentIfNeededLocked();
                } else {
                    this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                    updatePlayerDataSourceLocked();
                }
            }
            notifyPlaylistChanged();
            return;
        }
        throw new IllegalArgumentException("item shouldn't be null");
    }

    public void removePlaylistItem(@NonNull MediaItem2 item) {
        if (item != null) {
            synchronized (this.mLock) {
                if (this.mPlaylist.remove(item)) {
                    this.mShuffledList.remove(item);
                    this.mItemDsdMap.remove(item);
                    updateCurrentIfNeededLocked();
                    notifyPlaylistChanged();
                    return;
                }
                return;
            }
        }
        throw new IllegalArgumentException("item shouldn't be null");
    }

    /* JADX WARNING: Missing block: B:13:0x0052, code skipped:
            notifyPlaylistChanged();
     */
    /* JADX WARNING: Missing block: B:14:0x0055, code skipped:
            return;
     */
    public void replacePlaylistItem(int r6, @android.support.annotation.NonNull android.support.v4.media.MediaItem2 r7) {
        /*
        r5 = this;
        if (r7 == 0) goto L_0x0059;
    L_0x0002:
        r0 = r5.mLock;
        monitor-enter(r0);
        r1 = r5.mPlaylist;	 Catch:{ all -> 0x0056 }
        r1 = r1.size();	 Catch:{ all -> 0x0056 }
        if (r1 > 0) goto L_0x000f;
    L_0x000d:
        monitor-exit(r0);	 Catch:{ all -> 0x0056 }
        return;
    L_0x000f:
        r1 = r5.mPlaylist;	 Catch:{ all -> 0x0056 }
        r1 = r1.size();	 Catch:{ all -> 0x0056 }
        r2 = 1;
        r1 = r1 - r2;
        r1 = clamp(r6, r1);	 Catch:{ all -> 0x0056 }
        r6 = r1;
        r1 = r5.mShuffledList;	 Catch:{ all -> 0x0056 }
        r3 = r5.mPlaylist;	 Catch:{ all -> 0x0056 }
        r3 = r3.get(r6);	 Catch:{ all -> 0x0056 }
        r1 = r1.indexOf(r3);	 Catch:{ all -> 0x0056 }
        r3 = r5.mItemDsdMap;	 Catch:{ all -> 0x0056 }
        r4 = r5.mShuffledList;	 Catch:{ all -> 0x0056 }
        r4 = r4.get(r1);	 Catch:{ all -> 0x0056 }
        r3.remove(r4);	 Catch:{ all -> 0x0056 }
        r3 = r5.mShuffledList;	 Catch:{ all -> 0x0056 }
        r3.set(r1, r7);	 Catch:{ all -> 0x0056 }
        r3 = r5.mPlaylist;	 Catch:{ all -> 0x0056 }
        r3.set(r6, r7);	 Catch:{ all -> 0x0056 }
        r3 = r5.hasValidItem();	 Catch:{ all -> 0x0056 }
        if (r3 != 0) goto L_0x004e;
    L_0x0043:
        r3 = -1;
        r2 = r5.getNextValidPlayItemLocked(r3, r2);	 Catch:{ all -> 0x0056 }
        r5.mCurrent = r2;	 Catch:{ all -> 0x0056 }
        r5.updatePlayerDataSourceLocked();	 Catch:{ all -> 0x0056 }
        goto L_0x0051;
    L_0x004e:
        r5.updateCurrentIfNeededLocked();	 Catch:{ all -> 0x0056 }
    L_0x0051:
        monitor-exit(r0);	 Catch:{ all -> 0x0056 }
        r5.notifyPlaylistChanged();
        return;
    L_0x0056:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0056 }
        throw r1;
    L_0x0059:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "item shouldn't be null";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.SessionPlaylistAgentImplBase.replacePlaylistItem(int, android.support.v4.media.MediaItem2):void");
    }

    /* JADX WARNING: Missing block: B:17:0x002d, code skipped:
            return;
     */
    public void skipToPlaylistItem(@android.support.annotation.NonNull android.support.v4.media.MediaItem2 r4) {
        /*
        r3 = this;
        if (r4 == 0) goto L_0x0031;
    L_0x0002:
        r0 = r3.mLock;
        monitor-enter(r0);
        r1 = r3.hasValidItem();	 Catch:{ all -> 0x002e }
        if (r1 == 0) goto L_0x002c;
    L_0x000b:
        r1 = r3.mCurrent;	 Catch:{ all -> 0x002e }
        r1 = r1.mediaItem;	 Catch:{ all -> 0x002e }
        r1 = r4.equals(r1);	 Catch:{ all -> 0x002e }
        if (r1 == 0) goto L_0x0016;
    L_0x0015:
        goto L_0x002c;
    L_0x0016:
        r1 = r3.mShuffledList;	 Catch:{ all -> 0x002e }
        r1 = r1.indexOf(r4);	 Catch:{ all -> 0x002e }
        if (r1 >= 0) goto L_0x0020;
    L_0x001e:
        monitor-exit(r0);	 Catch:{ all -> 0x002e }
        return;
    L_0x0020:
        r2 = new android.support.v4.media.SessionPlaylistAgentImplBase$PlayItem;	 Catch:{ all -> 0x002e }
        r2.<init>(r3, r1);	 Catch:{ all -> 0x002e }
        r3.mCurrent = r2;	 Catch:{ all -> 0x002e }
        r3.updateCurrentIfNeededLocked();	 Catch:{ all -> 0x002e }
        monitor-exit(r0);	 Catch:{ all -> 0x002e }
        return;
    L_0x002c:
        monitor-exit(r0);	 Catch:{ all -> 0x002e }
        return;
    L_0x002e:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x002e }
        throw r1;
    L_0x0031:
        r0 = new java.lang.IllegalArgumentException;
        r1 = "item shouldn't be null";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.SessionPlaylistAgentImplBase.skipToPlaylistItem(android.support.v4.media.MediaItem2):void");
    }

    public void skipToPreviousItem() {
        synchronized (this.mLock) {
            if (hasValidItem()) {
                PlayItem prev = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, -1);
                if (prev != this.mEopPlayItem) {
                    this.mCurrent = prev;
                }
                updateCurrentIfNeededLocked();
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0025, code skipped:
            return;
     */
    public void skipToNextItem() {
        /*
        r3 = this;
        r0 = r3.mLock;
        monitor-enter(r0);
        r1 = r3.hasValidItem();	 Catch:{ all -> 0x0026 }
        if (r1 == 0) goto L_0x0024;
    L_0x0009:
        r1 = r3.mCurrent;	 Catch:{ all -> 0x0026 }
        r2 = r3.mEopPlayItem;	 Catch:{ all -> 0x0026 }
        if (r1 != r2) goto L_0x0010;
    L_0x000f:
        goto L_0x0024;
    L_0x0010:
        r1 = r3.mCurrent;	 Catch:{ all -> 0x0026 }
        r1 = r1.shuffledIdx;	 Catch:{ all -> 0x0026 }
        r2 = 1;
        r1 = r3.getNextValidPlayItemLocked(r1, r2);	 Catch:{ all -> 0x0026 }
        r2 = r3.mEopPlayItem;	 Catch:{ all -> 0x0026 }
        if (r1 == r2) goto L_0x001f;
    L_0x001d:
        r3.mCurrent = r1;	 Catch:{ all -> 0x0026 }
    L_0x001f:
        r3.updateCurrentIfNeededLocked();	 Catch:{ all -> 0x0026 }
        monitor-exit(r0);	 Catch:{ all -> 0x0026 }
        return;
    L_0x0024:
        monitor-exit(r0);	 Catch:{ all -> 0x0026 }
        return;
    L_0x0026:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0026 }
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.SessionPlaylistAgentImplBase.skipToNextItem():void");
    }

    public int getRepeatMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mRepeatMode;
        }
        return i;
    }

    /* JADX WARNING: Missing block: B:23:0x003e, code skipped:
            notifyRepeatModeChanged();
     */
    /* JADX WARNING: Missing block: B:24:0x0041, code skipped:
            return;
     */
    public void setRepeatMode(int r5) {
        /*
        r4 = this;
        if (r5 < 0) goto L_0x0045;
    L_0x0002:
        r0 = 3;
        if (r5 <= r0) goto L_0x0006;
    L_0x0005:
        goto L_0x0045;
    L_0x0006:
        r0 = r4.mLock;
        monitor-enter(r0);
        r1 = r4.mRepeatMode;	 Catch:{ all -> 0x0042 }
        if (r1 != r5) goto L_0x000f;
    L_0x000d:
        monitor-exit(r0);	 Catch:{ all -> 0x0042 }
        return;
    L_0x000f:
        r4.mRepeatMode = r5;	 Catch:{ all -> 0x0042 }
        r1 = 1;
        switch(r5) {
            case 0: goto L_0x0037;
            case 1: goto L_0x0027;
            case 2: goto L_0x0016;
            case 3: goto L_0x0016;
            default: goto L_0x0015;
        };	 Catch:{ all -> 0x0042 }
    L_0x0015:
        goto L_0x003d;
    L_0x0016:
        r2 = r4.mCurrent;	 Catch:{ all -> 0x0042 }
        r3 = r4.mEopPlayItem;	 Catch:{ all -> 0x0042 }
        if (r2 != r3) goto L_0x0037;
    L_0x001c:
        r2 = -1;
        r1 = r4.getNextValidPlayItemLocked(r2, r1);	 Catch:{ all -> 0x0042 }
        r4.mCurrent = r1;	 Catch:{ all -> 0x0042 }
        r4.updatePlayerDataSourceLocked();	 Catch:{ all -> 0x0042 }
        goto L_0x0037;
    L_0x0027:
        r2 = r4.mCurrent;	 Catch:{ all -> 0x0042 }
        if (r2 == 0) goto L_0x003d;
    L_0x002b:
        r2 = r4.mCurrent;	 Catch:{ all -> 0x0042 }
        r3 = r4.mEopPlayItem;	 Catch:{ all -> 0x0042 }
        if (r2 == r3) goto L_0x003d;
    L_0x0031:
        r2 = r4.mPlayer;	 Catch:{ all -> 0x0042 }
        r2.loopCurrent(r1);	 Catch:{ all -> 0x0042 }
        goto L_0x003d;
    L_0x0037:
        r1 = r4.mPlayer;	 Catch:{ all -> 0x0042 }
        r2 = 0;
        r1.loopCurrent(r2);	 Catch:{ all -> 0x0042 }
    L_0x003d:
        monitor-exit(r0);	 Catch:{ all -> 0x0042 }
        r4.notifyRepeatModeChanged();
        return;
    L_0x0042:
        r1 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0042 }
        throw r1;
    L_0x0045:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.SessionPlaylistAgentImplBase.setRepeatMode(int):void");
    }

    public int getShuffleMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mShuffleMode;
        }
        return i;
    }

    public void setShuffleMode(int shuffleMode) {
        if (shuffleMode >= 0 && shuffleMode <= 2) {
            synchronized (this.mLock) {
                if (this.mShuffleMode == shuffleMode) {
                    return;
                }
                this.mShuffleMode = shuffleMode;
                applyShuffleModeLocked();
                updateCurrentIfNeededLocked();
                notifyShuffleModeChanged();
            }
        }
    }

    public MediaItem2 getMediaItem(DataSourceDesc dsd) {
        return null;
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public int getCurShuffledIndex() {
        int i;
        synchronized (this.mLock) {
            i = hasValidItem() ? this.mCurrent.shuffledIdx : -2;
        }
        return i;
    }

    private boolean hasValidItem() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mCurrent != null;
        }
        return z;
    }

    private DataSourceDesc retrieveDataSourceDescLocked(MediaItem2 item) {
        DataSourceDesc dsd = item.getDataSourceDesc();
        if (dsd != null) {
            this.mItemDsdMap.put(item, dsd);
            return dsd;
        }
        dsd = (DataSourceDesc) this.mItemDsdMap.get(item);
        if (dsd != null) {
            return dsd;
        }
        OnDataSourceMissingHelper helper = this.mDsmHelper;
        if (helper != null) {
            dsd = helper.onDataSourceMissing(this.mSession.getInstance(), item);
            if (dsd != null) {
                this.mItemDsdMap.put(item, dsd);
            }
        }
        return dsd;
    }

    private PlayItem getNextValidPlayItemLocked(int curShuffledIdx, int direction) {
        int size = this.mPlaylist.size();
        int i = -1;
        if (curShuffledIdx == -1) {
            if (direction <= 0) {
                i = size;
            }
            curShuffledIdx = i;
        }
        int curShuffledIdx2 = curShuffledIdx;
        curShuffledIdx = 0;
        while (true) {
            PlayItem playItem = null;
            if (curShuffledIdx >= size) {
                return null;
            }
            curShuffledIdx2 += direction;
            if (curShuffledIdx2 < 0 || curShuffledIdx2 >= this.mPlaylist.size()) {
                if (this.mRepeatMode == 0) {
                    if (curShuffledIdx != size - 1) {
                        playItem = this.mEopPlayItem;
                    }
                    return playItem;
                }
                curShuffledIdx2 = curShuffledIdx2 < 0 ? this.mPlaylist.size() - 1 : 0;
            }
            DataSourceDesc dsd = retrieveDataSourceDescLocked((MediaItem2) this.mShuffledList.get(curShuffledIdx2));
            if (dsd != null) {
                return new PlayItem(curShuffledIdx2, dsd);
            }
            curShuffledIdx++;
        }
    }

    private void updateCurrentIfNeededLocked() {
        if (hasValidItem() && !this.mCurrent.isValid()) {
            int shuffledIdx = this.mShuffledList.indexOf(this.mCurrent.mediaItem);
            if (shuffledIdx >= 0) {
                this.mCurrent.shuffledIdx = shuffledIdx;
                return;
            }
            if (this.mCurrent.shuffledIdx >= this.mShuffledList.size()) {
                this.mCurrent = getNextValidPlayItemLocked(this.mShuffledList.size() - 1, 1);
            } else {
                this.mCurrent.mediaItem = (MediaItem2) this.mShuffledList.get(this.mCurrent.shuffledIdx);
                if (retrieveDataSourceDescLocked(this.mCurrent.mediaItem) == null) {
                    this.mCurrent = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, 1);
                }
            }
            updatePlayerDataSourceLocked();
        }
    }

    /* JADX WARNING: Missing block: B:11:0x002d, code skipped:
            return;
     */
    private void updatePlayerDataSourceLocked() {
        /*
        r3 = this;
        r0 = r3.mCurrent;
        if (r0 == 0) goto L_0x002d;
    L_0x0004:
        r0 = r3.mCurrent;
        r1 = r3.mEopPlayItem;
        if (r0 != r1) goto L_0x000b;
    L_0x000a:
        goto L_0x002d;
    L_0x000b:
        r0 = r3.mPlayer;
        r0 = r0.getCurrentDataSource();
        r1 = r3.mCurrent;
        r1 = r1.dsd;
        if (r0 == r1) goto L_0x002c;
    L_0x0017:
        r0 = r3.mPlayer;
        r1 = r3.mCurrent;
        r1 = r1.dsd;
        r0.setDataSource(r1);
        r0 = r3.mPlayer;
        r1 = r3.mRepeatMode;
        r2 = 1;
        if (r1 != r2) goto L_0x0028;
    L_0x0027:
        goto L_0x0029;
    L_0x0028:
        r2 = 0;
    L_0x0029:
        r0.loopCurrent(r2);
    L_0x002c:
        return;
    L_0x002d:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.SessionPlaylistAgentImplBase.updatePlayerDataSourceLocked():void");
    }

    private void applyShuffleModeLocked() {
        this.mShuffledList.clear();
        this.mShuffledList.addAll(this.mPlaylist);
        if (this.mShuffleMode == 1 || this.mShuffleMode == 2) {
            Collections.shuffle(this.mShuffledList);
        }
    }

    private static int clamp(int value, int size) {
        if (value < 0) {
            return 0;
        }
        return value > size ? size : value;
    }
}
