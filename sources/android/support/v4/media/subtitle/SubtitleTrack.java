package android.support.v4.media.subtitle;

import android.graphics.Canvas;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.media.SubtitleData2;
import android.support.v4.media.subtitle.MediaTimeProvider.OnMediaTimeListener;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

@RequiresApi(28)
@RestrictTo({Scope.LIBRARY_GROUP})
public abstract class SubtitleTrack implements OnMediaTimeListener {
    private static final String TAG = "SubtitleTrack";
    public boolean DEBUG = false;
    private final ArrayList<Cue> mActiveCues = new ArrayList();
    private CueList mCues;
    private MediaFormat mFormat;
    protected Handler mHandler = new Handler();
    private long mLastTimeMs;
    private long mLastUpdateTimeMs;
    private long mNextScheduledTimeMs = -1;
    private Runnable mRunnable;
    private final LongSparseArray<Run> mRunsByEndTime = new LongSparseArray();
    private final LongSparseArray<Run> mRunsByID = new LongSparseArray();
    protected MediaTimeProvider mTimeProvider;
    protected boolean mVisible;

    static class Cue {
        public long mEndTimeMs;
        public long[] mInnerTimesMs;
        public Cue mNextInRun;
        public long mRunID;
        public long mStartTimeMs;

        Cue() {
        }

        public void onTime(long timeMs) {
        }
    }

    static class CueList {
        private static final String TAG = "CueList";
        public boolean DEBUG = false;
        private SortedMap<Long, ArrayList<Cue>> mCues = new TreeMap();

        class EntryIterator implements Iterator<Pair<Long, Cue>> {
            private long mCurrentTimeMs;
            private boolean mDone;
            private Pair<Long, Cue> mLastEntry;
            private Iterator<Cue> mLastListIterator;
            private Iterator<Cue> mListIterator;
            private SortedMap<Long, ArrayList<Cue>> mRemainingCues;

            public boolean hasNext() {
                return this.mDone ^ 1;
            }

            public Pair<Long, Cue> next() {
                if (this.mDone) {
                    throw new NoSuchElementException("");
                }
                this.mLastEntry = new Pair(Long.valueOf(this.mCurrentTimeMs), this.mListIterator.next());
                this.mLastListIterator = this.mListIterator;
                if (!this.mListIterator.hasNext()) {
                    nextKey();
                }
                return this.mLastEntry;
            }

            public void remove() {
                if (this.mLastListIterator == null || ((Cue) this.mLastEntry.second).mEndTimeMs != ((Long) this.mLastEntry.first).longValue()) {
                    throw new IllegalStateException("");
                }
                this.mLastListIterator.remove();
                this.mLastListIterator = null;
                if (((ArrayList) CueList.this.mCues.get(this.mLastEntry.first)).size() == 0) {
                    CueList.this.mCues.remove(this.mLastEntry.first);
                }
                Cue cue = this.mLastEntry.second;
                CueList.this.removeEvent(cue, cue.mStartTimeMs);
                if (cue.mInnerTimesMs != null) {
                    for (long timeMs : cue.mInnerTimesMs) {
                        CueList.this.removeEvent(cue, timeMs);
                    }
                }
            }

            EntryIterator(SortedMap<Long, ArrayList<Cue>> cues) {
                if (CueList.this.DEBUG) {
                    String str = CueList.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(cues);
                    stringBuilder.append("");
                    Log.v(str, stringBuilder.toString());
                }
                this.mRemainingCues = cues;
                this.mLastListIterator = null;
                nextKey();
            }

            private void nextKey() {
                do {
                    try {
                        if (this.mRemainingCues != null) {
                            this.mCurrentTimeMs = ((Long) this.mRemainingCues.firstKey()).longValue();
                            this.mListIterator = ((ArrayList) this.mRemainingCues.get(Long.valueOf(this.mCurrentTimeMs))).iterator();
                            try {
                                this.mRemainingCues = this.mRemainingCues.tailMap(Long.valueOf(this.mCurrentTimeMs + 1));
                            } catch (IllegalArgumentException e) {
                                this.mRemainingCues = null;
                            }
                            this.mDone = false;
                        } else {
                            throw new NoSuchElementException("");
                        }
                    } catch (NoSuchElementException e2) {
                        this.mDone = true;
                        this.mRemainingCues = null;
                        this.mListIterator = null;
                        return;
                    }
                } while (!this.mListIterator.hasNext());
            }
        }

        private boolean addEvent(Cue cue, long timeMs) {
            ArrayList<Cue> cues = (ArrayList) this.mCues.get(Long.valueOf(timeMs));
            if (cues == null) {
                cues = new ArrayList(2);
                this.mCues.put(Long.valueOf(timeMs), cues);
            } else if (cues.contains(cue)) {
                return false;
            }
            cues.add(cue);
            return true;
        }

        private void removeEvent(Cue cue, long timeMs) {
            ArrayList<Cue> cues = (ArrayList) this.mCues.get(Long.valueOf(timeMs));
            if (cues != null) {
                cues.remove(cue);
                if (cues.size() == 0) {
                    this.mCues.remove(Long.valueOf(timeMs));
                }
            }
        }

        public void add(Cue cue) {
            if (cue.mStartTimeMs < cue.mEndTimeMs && addEvent(cue, cue.mStartTimeMs)) {
                long lastTimeMs = cue.mStartTimeMs;
                if (cue.mInnerTimesMs != null) {
                    for (long timeMs : cue.mInnerTimesMs) {
                        if (timeMs > lastTimeMs && timeMs < cue.mEndTimeMs) {
                            addEvent(cue, timeMs);
                            lastTimeMs = timeMs;
                        }
                    }
                }
                addEvent(cue, cue.mEndTimeMs);
            }
        }

        public void remove(Cue cue) {
            removeEvent(cue, cue.mStartTimeMs);
            if (cue.mInnerTimesMs != null) {
                for (long timeMs : cue.mInnerTimesMs) {
                    removeEvent(cue, timeMs);
                }
            }
            removeEvent(cue, cue.mEndTimeMs);
        }

        public Iterable<Pair<Long, Cue>> entriesBetween(long lastTimeMs, long timeMs) {
            final long j = lastTimeMs;
            final long j2 = timeMs;
            return new Iterable<Pair<Long, Cue>>() {
                public Iterator<Pair<Long, Cue>> iterator() {
                    if (CueList.this.DEBUG) {
                        String str = CueList.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("slice (");
                        stringBuilder.append(j);
                        stringBuilder.append(", ");
                        stringBuilder.append(j2);
                        stringBuilder.append("]=");
                        Log.d(str, stringBuilder.toString());
                    }
                    try {
                        return new EntryIterator(CueList.this.mCues.subMap(Long.valueOf(j + 1), Long.valueOf(j2 + 1)));
                    } catch (IllegalArgumentException e) {
                        return new EntryIterator(null);
                    }
                }
            };
        }

        public long nextTimeAfter(long timeMs) {
            try {
                SortedMap<Long, ArrayList<Cue>> tail = this.mCues.tailMap(Long.valueOf(1 + timeMs));
                if (tail != null) {
                    return ((Long) tail.firstKey()).longValue();
                }
                return -1;
            } catch (IllegalArgumentException e) {
                return -1;
            } catch (NoSuchElementException e2) {
                return -1;
            }
        }

        CueList() {
        }
    }

    public interface RenderingWidget {

        public interface OnChangedListener {
            void onChanged(RenderingWidget renderingWidget);
        }

        void draw(Canvas canvas);

        void onAttachedToWindow();

        void onDetachedFromWindow();

        void setOnChangedListener(OnChangedListener onChangedListener);

        void setSize(int i, int i2);

        void setVisible(boolean z);
    }

    private static class Run {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        public long mEndTimeMs;
        public Cue mFirstCue;
        public Run mNextRunAtEndTimeMs;
        public Run mPrevRunAtEndTimeMs;
        public long mRunID;
        private long mStoredEndTimeMs;

        static {
            Class cls = SubtitleTrack.class;
        }

        private Run() {
            this.mEndTimeMs = -1;
            this.mRunID = 0;
            this.mStoredEndTimeMs = -1;
        }

        /* synthetic */ Run(AnonymousClass1 x0) {
            this();
        }

        public void storeByEndTimeMs(LongSparseArray<Run> runsByEndTime) {
            int ix = runsByEndTime.indexOfKey(this.mStoredEndTimeMs);
            if (ix >= 0) {
                if (this.mPrevRunAtEndTimeMs == null) {
                    if (this.mNextRunAtEndTimeMs == null) {
                        runsByEndTime.removeAt(ix);
                    } else {
                        runsByEndTime.setValueAt(ix, this.mNextRunAtEndTimeMs);
                    }
                }
                removeAtEndTimeMs();
            }
            if (this.mEndTimeMs >= 0) {
                this.mPrevRunAtEndTimeMs = null;
                this.mNextRunAtEndTimeMs = (Run) runsByEndTime.get(this.mEndTimeMs);
                if (this.mNextRunAtEndTimeMs != null) {
                    this.mNextRunAtEndTimeMs.mPrevRunAtEndTimeMs = this;
                }
                runsByEndTime.put(this.mEndTimeMs, this);
                this.mStoredEndTimeMs = this.mEndTimeMs;
            }
        }

        public void removeAtEndTimeMs() {
            Run prev = this.mPrevRunAtEndTimeMs;
            if (this.mPrevRunAtEndTimeMs != null) {
                this.mPrevRunAtEndTimeMs.mNextRunAtEndTimeMs = this.mNextRunAtEndTimeMs;
                this.mPrevRunAtEndTimeMs = null;
            }
            if (this.mNextRunAtEndTimeMs != null) {
                this.mNextRunAtEndTimeMs.mPrevRunAtEndTimeMs = prev;
                this.mNextRunAtEndTimeMs = null;
            }
        }
    }

    public abstract RenderingWidget getRenderingWidget();

    public abstract void onData(byte[] bArr, boolean z, long j);

    public abstract void updateView(ArrayList<Cue> arrayList);

    public SubtitleTrack(MediaFormat format) {
        this.mFormat = format;
        this.mCues = new CueList();
        clearActiveCues();
        this.mLastTimeMs = -1;
    }

    public final MediaFormat getFormat() {
        return this.mFormat;
    }

    public void onData(SubtitleData2 data) {
        long runID = data.getStartTimeUs() + 1;
        onData(data.getData(), true, runID);
        setRunDiscardTimeMs(runID, (data.getStartTimeUs() + data.getDurationUs()) / 1000);
    }

    /* Access modifiers changed, original: protected|declared_synchronized */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0022 A:{Catch:{ all -> 0x000a }} */
    public synchronized void updateActiveCues(boolean r8, long r9) {
        /*
        r7 = this;
        monitor-enter(r7);
        if (r8 != 0) goto L_0x000d;
    L_0x0003:
        r0 = r7.mLastUpdateTimeMs;	 Catch:{ all -> 0x000a }
        r0 = (r0 > r9 ? 1 : (r0 == r9 ? 0 : -1));
        if (r0 <= 0) goto L_0x0010;
    L_0x0009:
        goto L_0x000d;
    L_0x000a:
        r8 = move-exception;
        goto L_0x00be;
    L_0x000d:
        r7.clearActiveCues();	 Catch:{ all -> 0x000a }
    L_0x0010:
        r0 = r7.mCues;	 Catch:{ all -> 0x000a }
        r1 = r7.mLastUpdateTimeMs;	 Catch:{ all -> 0x000a }
        r0 = r0.entriesBetween(r1, r9);	 Catch:{ all -> 0x000a }
        r0 = r0.iterator();	 Catch:{ all -> 0x000a }
    L_0x001c:
        r1 = r0.hasNext();	 Catch:{ all -> 0x000a }
        if (r1 == 0) goto L_0x00a3;
    L_0x0022:
        r1 = r0.next();	 Catch:{ all -> 0x000a }
        r1 = (android.util.Pair) r1;	 Catch:{ all -> 0x000a }
        r2 = r1.second;	 Catch:{ all -> 0x000a }
        r2 = (android.support.v4.media.subtitle.SubtitleTrack.Cue) r2;	 Catch:{ all -> 0x000a }
        r3 = r2.mEndTimeMs;	 Catch:{ all -> 0x000a }
        r5 = r1.first;	 Catch:{ all -> 0x000a }
        r5 = (java.lang.Long) r5;	 Catch:{ all -> 0x000a }
        r5 = r5.longValue();	 Catch:{ all -> 0x000a }
        r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
        if (r3 != 0) goto L_0x0065;
    L_0x003a:
        r3 = r7.DEBUG;	 Catch:{ all -> 0x000a }
        if (r3 == 0) goto L_0x0054;
    L_0x003e:
        r3 = "SubtitleTrack";
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x000a }
        r4.<init>();	 Catch:{ all -> 0x000a }
        r5 = "Removing ";
        r4.append(r5);	 Catch:{ all -> 0x000a }
        r4.append(r2);	 Catch:{ all -> 0x000a }
        r4 = r4.toString();	 Catch:{ all -> 0x000a }
        android.util.Log.v(r3, r4);	 Catch:{ all -> 0x000a }
    L_0x0054:
        r3 = r7.mActiveCues;	 Catch:{ all -> 0x000a }
        r3.remove(r2);	 Catch:{ all -> 0x000a }
        r3 = r2.mRunID;	 Catch:{ all -> 0x000a }
        r5 = 0;
        r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
        if (r3 != 0) goto L_0x00a1;
    L_0x0061:
        r0.remove();	 Catch:{ all -> 0x000a }
        goto L_0x00a1;
    L_0x0065:
        r3 = r2.mStartTimeMs;	 Catch:{ all -> 0x000a }
        r5 = r1.first;	 Catch:{ all -> 0x000a }
        r5 = (java.lang.Long) r5;	 Catch:{ all -> 0x000a }
        r5 = r5.longValue();	 Catch:{ all -> 0x000a }
        r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
        if (r3 != 0) goto L_0x009a;
    L_0x0073:
        r3 = r7.DEBUG;	 Catch:{ all -> 0x000a }
        if (r3 == 0) goto L_0x008d;
    L_0x0077:
        r3 = "SubtitleTrack";
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x000a }
        r4.<init>();	 Catch:{ all -> 0x000a }
        r5 = "Adding ";
        r4.append(r5);	 Catch:{ all -> 0x000a }
        r4.append(r2);	 Catch:{ all -> 0x000a }
        r4 = r4.toString();	 Catch:{ all -> 0x000a }
        android.util.Log.v(r3, r4);	 Catch:{ all -> 0x000a }
    L_0x008d:
        r3 = r2.mInnerTimesMs;	 Catch:{ all -> 0x000a }
        if (r3 == 0) goto L_0x0094;
    L_0x0091:
        r2.onTime(r9);	 Catch:{ all -> 0x000a }
    L_0x0094:
        r3 = r7.mActiveCues;	 Catch:{ all -> 0x000a }
        r3.add(r2);	 Catch:{ all -> 0x000a }
        goto L_0x00a1;
    L_0x009a:
        r3 = r2.mInnerTimesMs;	 Catch:{ all -> 0x000a }
        if (r3 == 0) goto L_0x00a1;
    L_0x009e:
        r2.onTime(r9);	 Catch:{ all -> 0x000a }
    L_0x00a1:
        goto L_0x001c;
    L_0x00a3:
        r0 = r7.mRunsByEndTime;	 Catch:{ all -> 0x000a }
        r0 = r0.size();	 Catch:{ all -> 0x000a }
        if (r0 <= 0) goto L_0x00ba;
    L_0x00ab:
        r0 = r7.mRunsByEndTime;	 Catch:{ all -> 0x000a }
        r1 = 0;
        r2 = r0.keyAt(r1);	 Catch:{ all -> 0x000a }
        r0 = (r2 > r9 ? 1 : (r2 == r9 ? 0 : -1));
        if (r0 > 0) goto L_0x00ba;
    L_0x00b6:
        r7.removeRunsByEndTimeIndex(r1);	 Catch:{ all -> 0x000a }
        goto L_0x00a3;
    L_0x00ba:
        r7.mLastUpdateTimeMs = r9;	 Catch:{ all -> 0x000a }
        monitor-exit(r7);
        return;
    L_0x00be:
        monitor-exit(r7);
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.subtitle.SubtitleTrack.updateActiveCues(boolean, long):void");
    }

    private void removeRunsByEndTimeIndex(int ix) {
        Run run = (Run) this.mRunsByEndTime.valueAt(ix);
        while (run != null) {
            Cue cue = run.mFirstCue;
            while (cue != null) {
                this.mCues.remove(cue);
                Cue nextCue = cue.mNextInRun;
                cue.mNextInRun = null;
                cue = nextCue;
            }
            this.mRunsByID.remove(run.mRunID);
            Run nextRun = run.mNextRunAtEndTimeMs;
            run.mPrevRunAtEndTimeMs = null;
            run.mNextRunAtEndTimeMs = null;
            run = nextRun;
        }
        this.mRunsByEndTime.removeAt(ix);
    }

    /* Access modifiers changed, original: protected */
    public void finalize() throws Throwable {
        for (int ix = this.mRunsByEndTime.size() - 1; ix >= 0; ix--) {
            removeRunsByEndTimeIndex(ix);
        }
        super.finalize();
    }

    private synchronized void takeTime(long timeMs) {
        this.mLastTimeMs = timeMs;
    }

    /* Access modifiers changed, original: protected|declared_synchronized */
    public synchronized void clearActiveCues() {
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Clearing ");
            stringBuilder.append(this.mActiveCues.size());
            stringBuilder.append(" active cues");
            Log.v(str, stringBuilder.toString());
        }
        this.mActiveCues.clear();
        this.mLastUpdateTimeMs = -1;
    }

    /* Access modifiers changed, original: protected */
    public void scheduleTimedEvents() {
        if (this.mTimeProvider != null) {
            this.mNextScheduledTimeMs = this.mCues.nextTimeAfter(this.mLastTimeMs);
            if (this.DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sched @");
                stringBuilder.append(this.mNextScheduledTimeMs);
                stringBuilder.append(" after ");
                stringBuilder.append(this.mLastTimeMs);
                Log.d(str, stringBuilder.toString());
            }
            this.mTimeProvider.notifyAt(this.mNextScheduledTimeMs >= 0 ? this.mNextScheduledTimeMs * 1000 : -1, this);
        }
    }

    public void onTimedEvent(long timeUs) {
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onTimedEvent ");
            stringBuilder.append(timeUs);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this) {
            long timeMs = timeUs / 1000;
            updateActiveCues(false, timeMs);
            takeTime(timeMs);
        }
        updateView(this.mActiveCues);
        scheduleTimedEvents();
    }

    public void onSeek(long timeUs) {
        if (this.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSeek ");
            stringBuilder.append(timeUs);
            Log.d(str, stringBuilder.toString());
        }
        synchronized (this) {
            long timeMs = timeUs / 1000;
            updateActiveCues(true, timeMs);
            takeTime(timeMs);
        }
        updateView(this.mActiveCues);
        scheduleTimedEvents();
    }

    public void onStop() {
        synchronized (this) {
            if (this.DEBUG) {
                Log.d(TAG, "onStop");
            }
            clearActiveCues();
            this.mLastTimeMs = -1;
        }
        updateView(this.mActiveCues);
        this.mNextScheduledTimeMs = -1;
        this.mTimeProvider.notifyAt(-1, this);
    }

    public void show() {
        if (!this.mVisible) {
            this.mVisible = true;
            RenderingWidget renderingWidget = getRenderingWidget();
            if (renderingWidget != null) {
                renderingWidget.setVisible(true);
            }
            if (this.mTimeProvider != null) {
                this.mTimeProvider.scheduleUpdate(this);
            }
        }
    }

    public void hide() {
        if (this.mVisible) {
            if (this.mTimeProvider != null) {
                this.mTimeProvider.cancelNotifications(this);
            }
            RenderingWidget renderingWidget = getRenderingWidget();
            if (renderingWidget != null) {
                renderingWidget.setVisible(false);
            }
            this.mVisible = false;
        }
    }

    /* Access modifiers changed, original: protected|declared_synchronized */
    /* JADX WARNING: Missing block: B:40:0x00df, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:52:0x00fe, code skipped:
            return false;
     */
    public synchronized boolean addCue(android.support.v4.media.subtitle.SubtitleTrack.Cue r12) {
        /*
        r11 = this;
        monitor-enter(r11);
        r0 = r11.mCues;	 Catch:{ all -> 0x00ff }
        r0.add(r12);	 Catch:{ all -> 0x00ff }
        r0 = r12.mRunID;	 Catch:{ all -> 0x00ff }
        r2 = 0;
        r0 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1));
        if (r0 == 0) goto L_0x003f;
    L_0x000e:
        r0 = r11.mRunsByID;	 Catch:{ all -> 0x00ff }
        r4 = r12.mRunID;	 Catch:{ all -> 0x00ff }
        r0 = r0.get(r4);	 Catch:{ all -> 0x00ff }
        r0 = (android.support.v4.media.subtitle.SubtitleTrack.Run) r0;	 Catch:{ all -> 0x00ff }
        if (r0 != 0) goto L_0x002d;
    L_0x001a:
        r1 = new android.support.v4.media.subtitle.SubtitleTrack$Run;	 Catch:{ all -> 0x00ff }
        r4 = 0;
        r1.<init>(r4);	 Catch:{ all -> 0x00ff }
        r0 = r1;
        r1 = r11.mRunsByID;	 Catch:{ all -> 0x00ff }
        r4 = r12.mRunID;	 Catch:{ all -> 0x00ff }
        r1.put(r4, r0);	 Catch:{ all -> 0x00ff }
        r4 = r12.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r0.mEndTimeMs = r4;	 Catch:{ all -> 0x00ff }
        goto L_0x0039;
    L_0x002d:
        r4 = r0.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r6 = r12.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r1 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
        if (r1 >= 0) goto L_0x0039;
    L_0x0035:
        r4 = r12.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r0.mEndTimeMs = r4;	 Catch:{ all -> 0x00ff }
    L_0x0039:
        r1 = r0.mFirstCue;	 Catch:{ all -> 0x00ff }
        r12.mNextInRun = r1;	 Catch:{ all -> 0x00ff }
        r0.mFirstCue = r12;	 Catch:{ all -> 0x00ff }
    L_0x003f:
        r0 = -1;
        r4 = r11.mTimeProvider;	 Catch:{ all -> 0x00ff }
        r5 = 1;
        r6 = 0;
        if (r4 == 0) goto L_0x0053;
    L_0x0047:
        r4 = r11.mTimeProvider;	 Catch:{ IllegalStateException -> 0x0052 }
        r7 = r4.getCurrentTimeUs(r6, r5);	 Catch:{ IllegalStateException -> 0x0052 }
        r9 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
        r7 = r7 / r9;
        r0 = r7;
        goto L_0x0053;
    L_0x0052:
        r4 = move-exception;
    L_0x0053:
        r4 = r11.DEBUG;	 Catch:{ all -> 0x00ff }
        if (r4 == 0) goto L_0x0095;
    L_0x0057:
        r4 = "SubtitleTrack";
        r7 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00ff }
        r7.<init>();	 Catch:{ all -> 0x00ff }
        r8 = "mVisible=";
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = r11.mVisible;	 Catch:{ all -> 0x00ff }
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = ", ";
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = r12.mStartTimeMs;	 Catch:{ all -> 0x00ff }
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = " <= ";
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r7.append(r0);	 Catch:{ all -> 0x00ff }
        r8 = ", ";
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = r12.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = " >= ";
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r8 = r11.mLastTimeMs;	 Catch:{ all -> 0x00ff }
        r7.append(r8);	 Catch:{ all -> 0x00ff }
        r7 = r7.toString();	 Catch:{ all -> 0x00ff }
        android.util.Log.v(r4, r7);	 Catch:{ all -> 0x00ff }
    L_0x0095:
        r4 = r11.mVisible;	 Catch:{ all -> 0x00ff }
        if (r4 == 0) goto L_0x00e0;
    L_0x0099:
        r7 = r12.mStartTimeMs;	 Catch:{ all -> 0x00ff }
        r4 = (r7 > r0 ? 1 : (r7 == r0 ? 0 : -1));
        if (r4 > 0) goto L_0x00e0;
    L_0x009f:
        r7 = r12.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r9 = r11.mLastTimeMs;	 Catch:{ all -> 0x00ff }
        r4 = (r7 > r9 ? 1 : (r7 == r9 ? 0 : -1));
        if (r4 < 0) goto L_0x00e0;
    L_0x00a7:
        r2 = r11.mRunnable;	 Catch:{ all -> 0x00ff }
        if (r2 == 0) goto L_0x00b2;
    L_0x00ab:
        r2 = r11.mHandler;	 Catch:{ all -> 0x00ff }
        r3 = r11.mRunnable;	 Catch:{ all -> 0x00ff }
        r2.removeCallbacks(r3);	 Catch:{ all -> 0x00ff }
    L_0x00b2:
        r2 = r11;
        r3 = r0;
        r6 = new android.support.v4.media.subtitle.SubtitleTrack$1;	 Catch:{ all -> 0x00ff }
        r6.<init>(r2, r3);	 Catch:{ all -> 0x00ff }
        r11.mRunnable = r6;	 Catch:{ all -> 0x00ff }
        r6 = r11.mHandler;	 Catch:{ all -> 0x00ff }
        r7 = r11.mRunnable;	 Catch:{ all -> 0x00ff }
        r8 = 10;
        r6 = r6.postDelayed(r7, r8);	 Catch:{ all -> 0x00ff }
        if (r6 == 0) goto L_0x00d3;
    L_0x00c7:
        r6 = r11.DEBUG;	 Catch:{ all -> 0x00ff }
        if (r6 == 0) goto L_0x00de;
    L_0x00cb:
        r6 = "SubtitleTrack";
        r7 = "scheduling update";
        android.util.Log.v(r6, r7);	 Catch:{ all -> 0x00ff }
        goto L_0x00de;
    L_0x00d3:
        r6 = r11.DEBUG;	 Catch:{ all -> 0x00ff }
        if (r6 == 0) goto L_0x00de;
    L_0x00d7:
        r6 = "SubtitleTrack";
        r7 = "failed to schedule subtitle view update";
        android.util.Log.w(r6, r7);	 Catch:{ all -> 0x00ff }
    L_0x00de:
        monitor-exit(r11);
        return r5;
    L_0x00e0:
        r4 = r11.mVisible;	 Catch:{ all -> 0x00ff }
        if (r4 == 0) goto L_0x00fd;
    L_0x00e4:
        r4 = r12.mEndTimeMs;	 Catch:{ all -> 0x00ff }
        r7 = r11.mLastTimeMs;	 Catch:{ all -> 0x00ff }
        r4 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1));
        if (r4 < 0) goto L_0x00fd;
    L_0x00ec:
        r4 = r12.mStartTimeMs;	 Catch:{ all -> 0x00ff }
        r7 = r11.mNextScheduledTimeMs;	 Catch:{ all -> 0x00ff }
        r4 = (r4 > r7 ? 1 : (r4 == r7 ? 0 : -1));
        if (r4 < 0) goto L_0x00fa;
    L_0x00f4:
        r4 = r11.mNextScheduledTimeMs;	 Catch:{ all -> 0x00ff }
        r2 = (r4 > r2 ? 1 : (r4 == r2 ? 0 : -1));
        if (r2 >= 0) goto L_0x00fd;
    L_0x00fa:
        r11.scheduleTimedEvents();	 Catch:{ all -> 0x00ff }
    L_0x00fd:
        monitor-exit(r11);
        return r6;
    L_0x00ff:
        r12 = move-exception;
        monitor-exit(r11);
        throw r12;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.subtitle.SubtitleTrack.addCue(android.support.v4.media.subtitle.SubtitleTrack$Cue):boolean");
    }

    /* JADX WARNING: Missing block: B:14:0x001c, code skipped:
            return;
     */
    public synchronized void setTimeProvider(android.support.v4.media.subtitle.MediaTimeProvider r2) {
        /*
        r1 = this;
        monitor-enter(r1);
        r0 = r1.mTimeProvider;	 Catch:{ all -> 0x001d }
        if (r0 != r2) goto L_0x0007;
    L_0x0005:
        monitor-exit(r1);
        return;
    L_0x0007:
        r0 = r1.mTimeProvider;	 Catch:{ all -> 0x001d }
        if (r0 == 0) goto L_0x0010;
    L_0x000b:
        r0 = r1.mTimeProvider;	 Catch:{ all -> 0x001d }
        r0.cancelNotifications(r1);	 Catch:{ all -> 0x001d }
    L_0x0010:
        r1.mTimeProvider = r2;	 Catch:{ all -> 0x001d }
        r0 = r1.mTimeProvider;	 Catch:{ all -> 0x001d }
        if (r0 == 0) goto L_0x001b;
    L_0x0016:
        r0 = r1.mTimeProvider;	 Catch:{ all -> 0x001d }
        r0.scheduleUpdate(r1);	 Catch:{ all -> 0x001d }
    L_0x001b:
        monitor-exit(r1);
        return;
    L_0x001d:
        r2 = move-exception;
        monitor-exit(r1);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.subtitle.SubtitleTrack.setTimeProvider(android.support.v4.media.subtitle.MediaTimeProvider):void");
    }

    /* Access modifiers changed, original: protected */
    public void finishedRun(long runID) {
        if (runID != 0 && runID != -1) {
            Run run = (Run) this.mRunsByID.get(runID);
            if (run != null) {
                run.storeByEndTimeMs(this.mRunsByEndTime);
            }
        }
    }

    public void setRunDiscardTimeMs(long runID, long timeMs) {
        if (runID != 0 && runID != -1) {
            Run run = (Run) this.mRunsByID.get(runID);
            if (run != null) {
                run.mEndTimeMs = timeMs;
                run.storeByEndTimeMs(this.mRunsByEndTime);
            }
        }
    }

    public int getTrackType() {
        return getRenderingWidget() == null ? 3 : 4;
    }
}
