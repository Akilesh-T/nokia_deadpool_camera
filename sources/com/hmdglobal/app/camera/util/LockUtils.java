package com.hmdglobal.app.camera.util;

import android.util.Log;
import android.util.SparseArray;
import com.android.grafika.gles.GlUtil;
import java.util.Random;

public class LockUtils {
    private static boolean mIsIgnoreLock = false;

    /* renamed from: com.hmdglobal.app.camera.util.LockUtils$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$hmdglobal$app$camera$util$LockUtils$LockType = new int[LockType.values().length];

        static {
            try {
                $SwitchMap$com$hmdglobal$app$camera$util$LockUtils$LockType[LockType.MULTILOCK.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private static class Holder {
        private static LockUtils INSTANCE = new LockUtils();

        private Holder() {
        }
    }

    public interface Lock {
        Integer aquireLock();

        Integer aquireLock(int i);

        void ignoreLock(boolean z);

        boolean isIgnoreLock();

        boolean isLocked();

        boolean unlockWithToken(Integer num);
    }

    public enum LockType {
        MULTILOCK
    }

    private class MultiLock implements Lock {
        private Object LOCK_SYNCH;
        private int RANDOM_BOUND;
        private int UNLOCK_DELAY;
        private boolean mIsLocked;
        private Random mRandomGenerator;
        private SparseArray<Integer> mTokenMap;

        private MultiLock() {
            this.mTokenMap = new SparseArray();
            this.mRandomGenerator = new Random(10);
            this.RANDOM_BOUND = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
            this.UNLOCK_DELAY = 0;
            this.LOCK_SYNCH = new Object();
            this.mIsLocked = false;
        }

        /* synthetic */ MultiLock(LockUtils x0, AnonymousClass1 x1) {
            this();
        }

        public Integer aquireLock() {
            int token = this.mRandomGenerator.nextInt(this.RANDOM_BOUND);
            synchronized (this.LOCK_SYNCH) {
                while (this.mTokenMap.get(token, null) != null) {
                    token++;
                    if (token == ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED) {
                        token = this.mRandomGenerator.nextInt(this.RANDOM_BOUND);
                    }
                }
                this.mIsLocked = true;
                this.mTokenMap.put(token, Integer.valueOf(token));
            }
            return Integer.valueOf(token);
        }

        public Integer aquireLock(int hash) {
            synchronized (this.LOCK_SYNCH) {
                if (((Integer) this.mTokenMap.get(hash)) == null) {
                    this.mIsLocked = true;
                    this.mTokenMap.put(hash, Integer.valueOf(hash));
                }
            }
            return Integer.valueOf(hash);
        }

        public boolean unlockWithToken(Integer token) {
            synchronized (this.LOCK_SYNCH) {
                if (this.mTokenMap.get(token.intValue(), null) != null) {
                    this.mTokenMap.remove(token.intValue());
                    if (this.mTokenMap.size() == 0) {
                        this.mIsLocked = false;
                    }
                    return true;
                }
                return false;
            }
        }

        public boolean isLocked() {
            return this.mIsLocked;
        }

        public void ignoreLock(boolean isIgnoreLock) {
            String str = GlUtil.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[Lock] ignoreLock = ");
            stringBuilder.append(isIgnoreLock);
            Log.d(str, stringBuilder.toString());
            LockUtils.mIsIgnoreLock = isIgnoreLock;
        }

        public boolean isIgnoreLock() {
            return LockUtils.mIsIgnoreLock;
        }
    }

    /* synthetic */ LockUtils(AnonymousClass1 x0) {
        this();
    }

    private LockUtils() {
    }

    public static LockUtils getInstance() {
        return Holder.INSTANCE;
    }

    public Lock generateMultiLock(LockType type) {
        if (AnonymousClass1.$SwitchMap$com$hmdglobal$app$camera$util$LockUtils$LockType[type.ordinal()] != 1) {
            return null;
        }
        return new MultiLock(this, null);
    }
}
